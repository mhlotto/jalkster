package com.example.jalkster;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionEntity;
import com.example.jalkster.live.ActMode;
import com.example.jalkster.live.LiveMode;
import com.example.jalkster.live.LiveSessionEvent;
import com.example.jalkster.live.LiveSessionReducer;
import com.example.jalkster.live.LiveSessionState;
import com.example.jalkster.timer.TimerContract;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class JalkTimerService extends Service {

    public static final String EXTRA_DURATION_MILLIS = "extra_duration_millis";
    public static final String EXTRA_JOG_DURATION = "extra_jog_duration";
    public static final String EXTRA_WALK_DURATION = "extra_walk_duration";
    public static final String EXTRA_REST_DURATION = "extra_rest_duration";

    private static final String TAG = "VRI[JalkTimer]";

    // Notification action constants
    private static final String ACTION_NOTIF_JOG = "com.example.jalkster.ACTION_NOTIF_JOG";
    private static final String ACTION_NOTIF_WALK = "com.example.jalkster.ACTION_NOTIF_WALK";
    private static final String ACTION_NOTIF_REST = "com.example.jalkster.ACTION_NOTIF_REST";
    private static final String ACTION_NOTIF_PAUSE = "com.example.jalkster.ACTION_NOTIF_PAUSE";
    private static final String ACTION_NOTIF_RESUME = "com.example.jalkster.ACTION_NOTIF_RESUME";

    // Notification IDs
    static final int NOTIF_ALERT_ID = 2001;
    private static final int NOTIF_STATUS_ID = 2002;

    // Channel IDs
    private static final String CHANNEL_STATUS = "jalk_timer_status";
    private static final String CHANNEL_ALERT_MANUAL_SOUND = "jalk_timer_alert_manual_sound";
    private static final String CHANNEL_ALERT_VIB = "jalk_timer_alert_vibrate";

    public static final String ACTION_PAUSE = "com.example.jalkster.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.example.jalkster.ACTION_RESUME";
    public static final String ACTION_TIMER_EXPIRED = "com.example.jalkster.ACTION_TIMER_EXPIRED";
    private static final int REQUEST_CODE_NOTIF_JOG = 2001;
    private static final int REQUEST_CODE_NOTIF_WALK = 2002;
    private static final int REQUEST_CODE_NOTIF_REST = 2003;
    private static final int REQUEST_CODE_ACTION_PAUSE = 2004;
    private static final int REQUEST_CODE_ACTION_RESUME = 2005;
    private static final long TICK_INTERVAL_MILLIS = 1000L;
    private static final int REQUEST_CODE_TIMER_EXPIRED = 3001;
    private static final long DEFAULT_JOG_DURATION_MILLIS = 60000L;
    private static final long DEFAULT_WALK_DURATION_MILLIS = 60000L;
    private static final long DEFAULT_REST_DURATION_MILLIS = 30000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (sessionState == null || sessionState.getMode() != LiveMode.ACTIVE) {
                tickScheduled = false;
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (endTimeElapsedRealtime <= 0L) {
                handler.postDelayed(this, TICK_INTERVAL_MILLIS);
                tickScheduled = true;
                return;
            }
            long remaining = Math.max(0L, endTimeElapsedRealtime - now);
            long previousRemaining = sessionState.getRemainingMillis();
            long elapsed = Math.max(0L, previousRemaining - remaining);
            if (elapsed > 0L) {
                dispatchEvent(new LiveSessionEvent.Tick(elapsed));
            }
            if (sessionState != null && sessionState.getMode() == LiveMode.ACTIVE) {
                handler.postDelayed(this, TICK_INTERVAL_MILLIS);
                tickScheduled = true;
            } else {
                tickScheduled = false;
            }
        }
    };

    private LiveSessionState sessionState;
    private long endTimeElapsedRealtime;
    private boolean tickScheduled;

    private long sessionStartTime;
    private long sessionEndTime;
    private boolean hasSessionStarted;

    private Vibrator vibrator;
    private Ringtone ringtone;
    private NotificationManagerCompat notificationManager;
    private boolean isForeground;
    private AlarmManager alarmManager;
    private PendingIntent expirationPendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = NotificationManagerCompat.from(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        NotificationUtils.createNotificationChannels(this);
        sessionState = LiveSessionState.initial(
                DEFAULT_JOG_DURATION_MILLIS,
                DEFAULT_WALK_DURATION_MILLIS,
                DEFAULT_REST_DURATION_MILLIS
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager statusManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (statusManager != null) {
                NotificationChannel statusChannel = new NotificationChannel(
                        CHANNEL_STATUS,
                        "Jalk Timer Status",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                statusChannel.setDescription("Status updates for the active Jalk timer session");
                statusChannel.setShowBadge(false);
                statusChannel.enableVibration(false);
                statusChannel.setSound(null, null);
                statusChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                statusManager.createNotificationChannel(statusChannel);
                Log.d(TAG, "Status channel lockscreen visibility=" + statusChannel.getLockscreenVisibility());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            boolean restored = restoreSessionState();
            if (restored) {
                broadcastState();
                updateNotification();
            }
            return START_STICKY;
        }
        String action = intent.getAction();
        boolean handledNotificationAction = false;
        if (TimerContract.ACTION_START_MODE.equals(action)) {
            Log.d(TAG, "Received ACTION_START_MODE");
            handleStartMode(intent);
        } else if (TimerContract.ACTION_STOP_SESSION.equals(action)) {
            Log.d(TAG, "Received ACTION_STOP_SESSION");
            dispatchEvent(new LiveSessionEvent.TapStop());
        } else if (TimerContract.ACTION_REQUEST_STATE.equals(action)) {
            Log.d(TAG, "Received ACTION_REQUEST_STATE");
            if (hasLiveSessionInMemory()) {
                broadcastState();
                persistSessionState();
                updateNotification();
                return START_STICKY;
            }
            boolean restored = restoreSessionState();
            if (!restored && sessionState == null) {
                sessionState = LiveSessionState.initial(
                        DEFAULT_JOG_DURATION_MILLIS,
                        DEFAULT_WALK_DURATION_MILLIS,
                        DEFAULT_REST_DURATION_MILLIS
                );
            }
            broadcastState();
            updateNotification();
            return START_STICKY;
        } else if (ACTION_NOTIF_JOG.equals(action)) {
            Log.d(TAG, "Received ACTION_NOTIF_JOG");
            handleStartMode(ActMode.JOG, null, null, null);
            handledNotificationAction = true;
        } else if (ACTION_NOTIF_WALK.equals(action)) {
            Log.d(TAG, "Received ACTION_NOTIF_WALK");
            handleStartMode(ActMode.WALK, null, null, null);
            handledNotificationAction = true;
        } else if (ACTION_NOTIF_REST.equals(action)) {
            Log.d(TAG, "Received ACTION_NOTIF_REST");
            handleStartMode(ActMode.REST, null, null, null);
            handledNotificationAction = true;
        } else if (ACTION_TIMER_EXPIRED.equals(action)) {
            Log.d(TAG, "Received ACTION_TIMER_EXPIRED");
            handleTimerExpiredFromAlarm(intent);
        } else if (ACTION_PAUSE.equals(action) || ACTION_NOTIF_PAUSE.equals(action)) {
            Log.d(TAG, "Received ACTION_PAUSE");
            dispatchEvent(new LiveSessionEvent.TapPauseResume());
            handledNotificationAction = true;
        } else if (ACTION_RESUME.equals(action) || ACTION_NOTIF_RESUME.equals(action)) {
            Log.d(TAG, "Received ACTION_RESUME");
            dispatchEvent(new LiveSessionEvent.TapPauseResume());
            handledNotificationAction = true;
        }

        if (handledNotificationAction) {
            cancelAlertNotification();
            updateNotification();
        }
        return START_STICKY;
    }

    private boolean hasLiveSessionInMemory() {
        if (sessionState == null || sessionState.getMode() == LiveMode.STOPPED) {
            return false;
        }
        return hasSessionStarted
                || sessionState.getMode() != LiveMode.INIT
                || sessionState.getCurrentAct() != null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tickRunnable);
        cancelExpirationAlarm();
        stopAlerts();
    }

    private void handleStartMode(Intent intent) {
        if (intent == null) {
            return;
        }
        String modeString = intent.getStringExtra(TimerContract.EXTRA_MODE);
        ActMode mode = ActMode.fromId(modeString);
        Long jogMs = readDurationExtra(intent,
                JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS,
                EXTRA_JOG_DURATION);
        Long walkMs = readDurationExtra(intent,
                JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS,
                EXTRA_WALK_DURATION);
        Long restMs = readDurationExtra(intent,
                JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS,
                EXTRA_REST_DURATION);
        if (mode != null && intent.hasExtra(EXTRA_DURATION_MILLIS)) {
            long modeDuration = intent.getLongExtra(EXTRA_DURATION_MILLIS, -1L);
            if (modeDuration > 0L) {
                if (mode == ActMode.JOG) {
                    jogMs = modeDuration;
                } else if (mode == ActMode.WALK) {
                    walkMs = modeDuration;
                } else if (mode == ActMode.REST) {
                    restMs = modeDuration;
                }
            }
        }
        handleStartMode(mode, jogMs, walkMs, restMs);
    }

    private void handleStartMode(ActMode mode, Long jogMs, Long walkMs, Long restMs) {
        if (mode == null) {
            Log.w(TAG, "handleStartMode with null mode");
            return;
        }
        stopAlerts();
        LiveSessionEvent.TapAct event;
        if (mode == ActMode.JOG) {
            event = new LiveSessionEvent.TapJog(jogMs, walkMs, restMs);
        } else if (mode == ActMode.WALK) {
            event = new LiveSessionEvent.TapWalk(jogMs, walkMs, restMs);
        } else {
            event = new LiveSessionEvent.TapRest(jogMs, walkMs, restMs);
        }
        dispatchEvent(event);
    }

    private void handleTimerExpiredFromAlarm(@Nullable Intent sourceIntent) {
        if (sessionState == null) {
            restoreSessionState();
        }
        if (sessionState == null) {
            return;
        }
        if (sessionState.getMode() == LiveMode.PAUSED) {
            Log.d(TAG, "Ignoring timer expiration alarm while paused");
            return;
        }
        dispatchEvent(new LiveSessionEvent.TimerFinished());
    }

    private void dispatchEvent(LiveSessionEvent event) {
        if (event == null) {
            return;
        }
        if (sessionState == null) {
            sessionState = LiveSessionState.initial(
                    DEFAULT_JOG_DURATION_MILLIS,
                    DEFAULT_WALK_DURATION_MILLIS,
                    DEFAULT_REST_DURATION_MILLIS
            );
        }
        LiveSessionState previous = sessionState;
        sessionState = LiveSessionReducer.reduce(sessionState, event);
        handleStateTransition(previous, sessionState, event);
    }

    private void handleStateTransition(LiveSessionState previous,
                                       LiveSessionState current,
                                       LiveSessionEvent event) {
        if (current == null) {
            return;
        }
        LiveMode previousMode = previous != null ? previous.getMode() : LiveMode.INIT;
        LiveMode currentMode = current.getMode();

        if (!hasSessionStarted && previousMode == LiveMode.INIT && currentMode == LiveMode.ACTIVE) {
            sessionStartTime = System.currentTimeMillis();
            sessionEndTime = 0L;
            hasSessionStarted = true;
        }

        if (previousMode != LiveMode.ACTIVE && currentMode == LiveMode.ACTIVE) {
            startTicking(current.getRemainingMillis());
        } else if (previousMode == LiveMode.ACTIVE && currentMode != LiveMode.ACTIVE) {
            stopTicking();
        }

        if (previousMode != LiveMode.TIMEREND && currentMode == LiveMode.TIMEREND) {
            stopTicking();
            stopAlerts();
            triggerTimerAlert();
        } else if (previousMode == LiveMode.TIMEREND && currentMode != LiveMode.TIMEREND) {
            stopAlerts();
        }

        if (currentMode == LiveMode.PAUSED) {
            stopTicking();
        }

        if (currentMode == LiveMode.STOPPED) {
            sessionEndTime = System.currentTimeMillis();
            saveCurrentSession(current);
            stopAlerts();
            stopTicking();
            stopForeground(true);
            isForeground = false;
            clearPersistedSessionState();
            stopSelf();
            return;
        }

        ensureForegroundIfNeeded(current);
        broadcastState();
        persistSessionState();
        updateNotification();
    }

    private void startTicking(long remainingMillis) {
        if (remainingMillis <= 0L || sessionState == null || sessionState.getCurrentAct() == null) {
            return;
        }
        handler.removeCallbacks(tickRunnable);
        long now = SystemClock.elapsedRealtime();
        endTimeElapsedRealtime = now + remainingMillis;
        handler.postDelayed(tickRunnable, TICK_INTERVAL_MILLIS);
        tickScheduled = true;
        scheduleExpirationAlarm(sessionState.getCurrentAct(), endTimeElapsedRealtime);
    }

    private void stopTicking() {
        handler.removeCallbacks(tickRunnable);
        tickScheduled = false;
        endTimeElapsedRealtime = 0L;
        cancelExpirationAlarm();
    }

    private void broadcastState() {
        if (sessionState == null) {
            return;
        }
        ActMode actMode = sessionState.getCurrentAct();
        Intent updateIntent = new Intent(TimerContract.ACTION_TIMER_UPDATE)
                .putExtra(TimerContract.EXTRA_MODE, actMode != null ? actMode.getId() : null)
                .putExtra(TimerContract.EXTRA_REMAINING_MS, sessionState.getRemainingMillis())
                .putExtra(TimerContract.EXTRA_IS_ALERTING, sessionState.getMode() == LiveMode.TIMEREND)
                .putExtra(TimerContract.EXTRA_IS_PAUSED, sessionState.getMode() == LiveMode.PAUSED)
                .putExtra(TimerContract.EXTRA_JOG_TIME_COMPLETED, sessionState.getTimeCompletedMillis(ActMode.JOG))
                .putExtra(TimerContract.EXTRA_JOG_REPS_COMPLETED, sessionState.getRepsDone(ActMode.JOG))
                .putExtra(TimerContract.EXTRA_WALK_TIME_COMPLETED, sessionState.getTimeCompletedMillis(ActMode.WALK))
                .putExtra(TimerContract.EXTRA_WALK_REPS_COMPLETED, sessionState.getRepsDone(ActMode.WALK))
                .putExtra(TimerContract.EXTRA_REST_TIME_COMPLETED, sessionState.getTimeCompletedMillis(ActMode.REST))
                .putExtra(TimerContract.EXTRA_REST_REPS_COMPLETED, sessionState.getRepsDone(ActMode.REST))
                .putExtra(EXTRA_JOG_DURATION, sessionState.getDurationMillis(ActMode.JOG))
                .putExtra(EXTRA_WALK_DURATION, sessionState.getDurationMillis(ActMode.WALK))
                .putExtra(EXTRA_REST_DURATION, sessionState.getDurationMillis(ActMode.REST))
                .putExtra(JalkSessionConfigActivity.EXTRA_SESSION_MODE, actMode != null ? actMode.getId() : null)
                .putExtra(TimerContract.EXTRA_LIVE_MODE, sessionState.getMode().name());
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
    }

    private void scheduleExpirationAlarm(ActMode mode, long alarmTime) {
        if (mode == null) {
            return;
        }
        Intent intent = new Intent(this, TimerExpiredReceiver.class)
                .setAction(TimerExpiredReceiver.ACTION_TIMER_EXPIRED)
                .putExtra(TimerExpiredReceiver.EXTRA_MODE, mode.getId());
        expirationPendingIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_TIMER_EXPIRED,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.alarmManager = alarmManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Exact alarms are not permitted. Requesting user to enable permission.");
                Intent intentSettings = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentSettings);
                Log.e(TAG, "Exact alarm scheduling blocked; user redirected to settings.");
                return;
            }
        }
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    alarmTime,
                    expirationPendingIntent
            );
            Log.d(TAG, "Scheduled exact expiration alarm for mode=" + mode.getId() + " at=" + alarmTime);
        } else {
            Log.e(TAG, "AlarmManager unavailable; cannot schedule expiration alarm.");
        }
    }

    private void cancelExpirationAlarm() {
        if (alarmManager == null || expirationPendingIntent == null) {
            return;
        }
        alarmManager.cancel(expirationPendingIntent);
        expirationPendingIntent.cancel();
        expirationPendingIntent = null;
    }

    private void updateNotification() {
        if (!isForeground || notificationManager == null || sessionState == null) {
            return;
        }
        Notification notification = buildStatusNotification(sessionState);
        notifyStatusNotification(notification);
    }

    private Notification buildStatusNotification(LiveSessionState state) {
        ActMode actMode = state.getCurrentAct();
        String modeLabel = getModeLabel(actMode);
        long remainingMs = state.getRemainingMillis();

        Intent activityIntent = SessionIntentFactory.buildLiveSessionIntent(
                this,
                state.getDurationMillis(ActMode.JOG),
                state.getDurationMillis(ActMode.WALK),
                state.getDurationMillis(ActMode.REST),
                actMode != null ? actMode.getId() : null
        );
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String contentTitle = actMode != null ? modeLabel : getString(R.string.status_notification_title);
        String contentText = formatTime(remainingMs);
        String bigText = "Mode: " + modeLabel + "\n" +
                "Remaining: " + contentText + "\n" +
                "Swipe down for controls";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_STATUS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        if (state.getMode() == LiveMode.PAUSED) {
            builder.setSubText(getString(R.string.pause_label));
        }

        if (state.getMode() == LiveMode.INIT || state.getMode() == LiveMode.TIMEREND) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_jog,
                    getString(R.string.mode_jog),
                    buildServiceActionPendingIntent(ACTION_NOTIF_JOG, REQUEST_CODE_NOTIF_JOG)
            ));
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_walk,
                    getString(R.string.mode_walk),
                    buildServiceActionPendingIntent(ACTION_NOTIF_WALK, REQUEST_CODE_NOTIF_WALK)
            ));
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_rest,
                    getString(R.string.mode_rest),
                    buildServiceActionPendingIntent(ACTION_NOTIF_REST, REQUEST_CODE_NOTIF_REST)
            ));
        }

        if (state.getMode() == LiveMode.ACTIVE) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause,
                    getString(R.string.pause_label),
                    buildServiceActionPendingIntent(ACTION_PAUSE, REQUEST_CODE_ACTION_PAUSE)
            ));
        } else if (state.getMode() == LiveMode.PAUSED) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_resume,
                    getString(R.string.resume_label),
                    buildServiceActionPendingIntent(ACTION_RESUME, REQUEST_CODE_ACTION_RESUME)
            ));
        }

        return builder.build();
    }

    private void triggerTimerAlert() {
        if (sessionState == null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences(TimerPreferences.PREFS_NAME, MODE_PRIVATE);
        boolean vibrateOnly = prefs.getBoolean(TimerPreferences.KEY_VIBRATE_ONLY, false);

        String channelId = vibrateOnly ? CHANNEL_ALERT_VIB : CHANNEL_ALERT_MANUAL_SOUND;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Timer finished")
                .setContentText("Select next action")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Jog · Walk · Rest"))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setAutoCancel(false);

        Intent jogIntent = new Intent(this, JalkTimerService.class).setAction(ACTION_NOTIF_JOG);
        PendingIntent jogPending = PendingIntent.getService(this, 0, jogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.ic_jog, "Jog", jogPending);

        Intent walkIntent = new Intent(this, JalkTimerService.class).setAction(ACTION_NOTIF_WALK);
        PendingIntent walkPending = PendingIntent.getService(this, 1, walkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.ic_walk, "Walk", walkPending);

        Intent restIntent = new Intent(this, JalkTimerService.class).setAction(ACTION_NOTIF_REST);
        PendingIntent restPending = PendingIntent.getService(this, 2, restIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.ic_rest, "Rest", restPending);

        Intent fsIntent = SessionIntentFactory.buildLiveSessionIntent(
                this,
                sessionState.getDurationMillis(ActMode.JOG),
                sessionState.getDurationMillis(ActMode.WALK),
                sessionState.getDurationMillis(ActMode.REST),
                sessionState.getCurrentAct() != null ? sessionState.getCurrentAct().getId() : null
        );
        PendingIntent fsPending = PendingIntent.getActivity(
                this, 99, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setFullScreenIntent(fsPending, true);

        Notification notification = builder.build();

        Log.d(TAG, "Posting alert notification...");
        notifyAlertNotification(notification);

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0L, 250L, 150L, 250L}, -1));
            } else {
                vibrator.vibrate(new long[]{0L, 250L, 150L, 250L}, -1);
            }
        }

        if (!vibrateOnly) {
            try {
                if (ringtone != null && ringtone.isPlaying()) {
                    ringtone.stop();
                }
            } catch (Throwable ignored) {
            }

            Uri uri = SoundPreferenceManager.getSelectedSoundUri(this);
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (ringtone != null) {
                ringtone.play();
            }
        }

        Log.d(TAG, "triggerTimerAlert: mode=" + (vibrateOnly ? "VIBRATE_ONLY" : "AUDIO_AND_VIBRATE"));
    }

    private PendingIntent buildServiceActionPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, JalkTimerService.class)
                .setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void ensureForegroundIfNeeded(LiveSessionState state) {
        if (state.getMode() == LiveMode.INIT || state.getMode() == LiveMode.STOPPED) {
            return;
        }
        Notification statusNotification = buildStatusNotification(state);
        if (!isForeground) {
            startForeground(
                    NOTIF_STATUS_ID,
                    statusNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
            isForeground = true;
        } else if (notificationManager != null) {
            notifyStatusNotification(statusNotification);
        }
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void notifyStatusNotification(Notification notification) {
        if (notificationManager == null || !canPostNotifications()) {
            return;
        }
        try {
            notificationManager.notify(NOTIF_STATUS_ID, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to post status notification.", e);
        }
    }

    private void notifyAlertNotification(Notification notification) {
        if (!canPostNotifications()) {
            return;
        }
        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ALERT_ID, notification);
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to post alert notification.", e);
        }
    }

    private void persistSessionState() {
        if (sessionState == null || sessionState.getMode() == LiveMode.STOPPED) {
            clearPersistedSessionState();
            return;
        }
        if (sessionState.getMode() == LiveMode.INIT && !hasSessionStarted) {
            clearPersistedSessionState();
            return;
        }
        int currentRep = Math.max(sessionState.getRepsDone(ActMode.JOG),
                Math.max(sessionState.getRepsDone(ActMode.WALK), sessionState.getRepsDone(ActMode.REST)));
        SessionManager.SessionState state = new SessionManager.SessionState(
                sessionState.getCurrentAct() != null ? sessionState.getCurrentAct().getId() : null,
                sessionState.getDurationMillis(ActMode.JOG),
                sessionState.getDurationMillis(ActMode.WALK),
                sessionState.getDurationMillis(ActMode.REST),
                currentRep,
                sessionStartTime,
                endTimeElapsedRealtime,
                sessionState.getRemainingMillis(),
                sessionState.getMode() == LiveMode.PAUSED,
                sessionState.getMode() == LiveMode.TIMEREND,
                sessionState.getRepsDone(ActMode.JOG),
                sessionState.getRepsDone(ActMode.WALK),
                sessionState.getRepsDone(ActMode.REST),
                sessionState.getTimeCompletedMillis(ActMode.JOG),
                sessionState.getTimeCompletedMillis(ActMode.WALK),
                sessionState.getTimeCompletedMillis(ActMode.REST),
                hasSessionStarted,
                0L,
                0L,
                0L
        );
        SessionManager.persist(this, state);
    }

    private void clearPersistedSessionState() {
        SessionManager.clear(this);
    }

    private boolean restoreSessionState() {
        SessionManager.SessionState stored = SessionManager.getState(this);
        if (stored == null) {
            return false;
        }
        ActMode actMode = ActMode.fromId(stored.mode);
        LiveMode liveMode;
        if (actMode == null) {
            liveMode = LiveMode.INIT;
        } else if (stored.isPaused) {
            liveMode = LiveMode.PAUSED;
        } else if (stored.hasAlertFired) {
            liveMode = LiveMode.TIMEREND;
        } else {
            liveMode = LiveMode.ACTIVE;
        }
        LiveSessionState restored = LiveSessionState.initial(
                stored.jogDurationMs,
                stored.walkDurationMs,
                stored.restDurationMs
        );
        restored = restored.withRepsAndTimes(
                stored.jogRepsCompleted,
                stored.walkRepsCompleted,
                stored.restRepsCompleted,
                stored.jogTimeCompletedMillis,
                stored.walkTimeCompletedMillis,
                stored.restTimeCompletedMillis
        );
        restored = restored.withCurrentAct(actMode).withMode(liveMode);

        long now = SystemClock.elapsedRealtime();
        long remaining = Math.max(0L, stored.remainingMillis);
        if (liveMode == LiveMode.ACTIVE) {
            if (stored.endElapsedRealtime > 0L) {
                remaining = stored.endElapsedRealtime - now;
            }
            if (remaining <= 0L) {
                restored = restored.withRemainingMillis(0L).withMode(LiveMode.TIMEREND);
                liveMode = LiveMode.TIMEREND;
            }
        }
        restored = restored.withRemainingMillis(Math.max(0L, remaining));

        sessionState = restored;
        sessionStartTime = stored.sessionStartTime;
        sessionEndTime = 0L;
        hasSessionStarted = stored.hasSessionStarted;

        stopTicking();
        if (liveMode == LiveMode.ACTIVE && restored.getRemainingMillis() > 0L) {
            endTimeElapsedRealtime = now + restored.getRemainingMillis();
            handler.postDelayed(tickRunnable, TICK_INTERVAL_MILLIS);
            tickScheduled = true;
            scheduleExpirationAlarm(restored.getCurrentAct(), endTimeElapsedRealtime);
        }

        ensureForegroundIfNeeded(restored);
        persistSessionState();
        return true;
    }

    private void saveCurrentSession(LiveSessionState state) {
        if (state == null) {
            return;
        }
        final long startTime = sessionStartTime;
        if (startTime == 0L) {
            Log.d(TAG, "saveCurrentSession: no active session");
            sessionEndTime = 0L;
            return;
        }

        final long endTime = sessionEndTime;
        if (endTime == 0L) {
            Log.d(TAG, "saveCurrentSession: missing end time, using current time");
        }

        final int walkRepTimeSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(state.getDurationMillis(ActMode.WALK));
        final int jogRepTimeSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(state.getDurationMillis(ActMode.JOG));
        final int restRepTimeSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(state.getDurationMillis(ActMode.REST));

        final int finalWalkReps = state.getRepsDone(ActMode.WALK);
        final int finalJogReps = state.getRepsDone(ActMode.JOG);
        final int finalRestReps = state.getRepsDone(ActMode.REST);

        if (!state.hasCompletedReps()) {
            Log.d(TAG, "Skipping save, no reps completed");
            resetSessionTracking();
            return;
        }

        final JalkSessionEntity session = new JalkSessionEntity(
                startTime,
                endTime != 0L ? endTime : System.currentTimeMillis(),
                walkRepTimeSeconds,
                jogRepTimeSeconds,
                restRepTimeSeconds,
                finalWalkReps,
                finalJogReps,
                finalRestReps
        );

        Log.d(TAG, "saveCurrentSession: start=" + session.getStartTime()
                + " end=" + session.getEndTime()
                + " walkTimeSeconds=" + walkRepTimeSeconds
                + " jogTimeSeconds=" + jogRepTimeSeconds
                + " restTimeSeconds=" + restRepTimeSeconds
                + " walkReps=" + finalWalkReps
                + " jogReps=" + finalJogReps
                + " restReps=" + finalRestReps);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JalkDatabase.getInstance(getApplicationContext()).jalkSessionDao().insert(session);
                    Log.d(TAG, "saveCurrentSession: session persisted");
                } catch (Exception e) {
                    Log.e(TAG, "saveCurrentSession: failed to persist session", e);
                }
            }
        }).start();

        resetSessionTracking();
    }

    private void resetSessionTracking() {
        sessionStartTime = 0L;
        sessionEndTime = 0L;
        hasSessionStarted = false;
    }

    private void stopAlerts() {
        cancelAlertNotification();
        try {
            if (ringtone != null) {
                ringtone.stop();
            }
        } catch (Throwable ignored) {
        } finally {
            ringtone = null;
        }
        try {
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    private void cancelAlertNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIF_ALERT_ID);
        }
    }

    private String getModeLabel(ActMode actMode) {
        if (actMode == null) {
            return getString(R.string.waiting_for_mode);
        }
        if (actMode == ActMode.JOG) {
            return getString(R.string.mode_jog);
        } else if (actMode == ActMode.WALK) {
            return getString(R.string.mode_walk);
        } else if (actMode == ActMode.REST) {
            return getString(R.string.mode_rest);
        }
        return actMode.getId();
    }

    private String formatTime(long millis) {
        boolean isNegative = millis < 0L;
        long absMillis = Math.abs(millis);
        long totalSeconds = absMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        return isNegative ? "-" + formatted : formatted;
    }

    private static Long readDurationExtra(Intent intent, String primaryKey, String fallbackKey) {
        if (intent == null) {
            return null;
        }
        if (intent.hasExtra(primaryKey)) {
            long value = intent.getLongExtra(primaryKey, -1L);
            return value > 0L ? value : null;
        }
        if (intent.hasExtra(fallbackKey)) {
            long value = intent.getLongExtra(fallbackKey, -1L);
            return value > 0L ? value : null;
        }
        return null;
    }
}
