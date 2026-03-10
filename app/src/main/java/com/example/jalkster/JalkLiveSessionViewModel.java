package com.example.jalkster;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jalkster.live.ActMode;
import com.example.jalkster.live.LiveMode;
import com.example.jalkster.live.LiveSessionEvent;
import com.example.jalkster.live.LiveSessionState;
import com.example.jalkster.timer.TimerContract;

public class JalkLiveSessionViewModel extends AndroidViewModel {

    private static final long DEFAULT_JOG_DURATION_MILLIS = 60000L;
    private static final long DEFAULT_WALK_DURATION_MILLIS = 60000L;
    private static final long DEFAULT_REST_DURATION_MILLIS = 30000L;

    private final MutableLiveData<LiveSessionState> state = new MutableLiveData<>();

    public JalkLiveSessionViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<LiveSessionState> getState() {
        return state;
    }

    public void seedDurations(long jogMillis, long walkMillis, long restMillis) {
        LiveSessionState current = state.getValue();
        if (current == null) {
            current = LiveSessionState.initial(
                    jogMillis > 0L ? jogMillis : DEFAULT_JOG_DURATION_MILLIS,
                    walkMillis > 0L ? walkMillis : DEFAULT_WALK_DURATION_MILLIS,
                    restMillis > 0L ? restMillis : DEFAULT_REST_DURATION_MILLIS
            );
        } else {
            current = current.withDurations(
                    jogMillis > 0L ? jogMillis : null,
                    walkMillis > 0L ? walkMillis : null,
                    restMillis > 0L ? restMillis : null
            );
        }
        state.setValue(current);
    }

    public void updateFromBroadcast(Intent intent) {
        if (intent == null || !TimerContract.ACTION_TIMER_UPDATE.equals(intent.getAction())) {
            return;
        }
        LiveSessionState current = state.getValue();
        String modeString = intent.getStringExtra(TimerContract.EXTRA_MODE);
        ActMode actMode = ActMode.fromId(modeString);
        LiveMode liveMode = parseLiveMode(intent, actMode);

        long jogMs = readDuration(intent, JalkTimerService.EXTRA_JOG_DURATION,
                current != null ? current.getDurationMillis(ActMode.JOG) : DEFAULT_JOG_DURATION_MILLIS,
                DEFAULT_JOG_DURATION_MILLIS);
        long walkMs = readDuration(intent, JalkTimerService.EXTRA_WALK_DURATION,
                current != null ? current.getDurationMillis(ActMode.WALK) : DEFAULT_WALK_DURATION_MILLIS,
                DEFAULT_WALK_DURATION_MILLIS);
        long restMs = readDuration(intent, JalkTimerService.EXTRA_REST_DURATION,
                current != null ? current.getDurationMillis(ActMode.REST) : DEFAULT_REST_DURATION_MILLIS,
                DEFAULT_REST_DURATION_MILLIS);

        if (liveMode == LiveMode.INIT && current != null && current.getMode() == LiveMode.INIT) {
            long storedJog = current.getDurationMillis(ActMode.JOG);
            long storedWalk = current.getDurationMillis(ActMode.WALK);
            long storedRest = current.getDurationMillis(ActMode.REST);
            if (storedJog > 0L) {
                jogMs = storedJog;
            }
            if (storedWalk > 0L) {
                walkMs = storedWalk;
            }
            if (storedRest > 0L) {
                restMs = storedRest;
            }
        }

        LiveSessionState updated = LiveSessionState.initial(jogMs, walkMs, restMs);
        updated = updated.withRepsAndTimes(
                intent.getIntExtra(TimerContract.EXTRA_JOG_REPS_COMPLETED, 0),
                intent.getIntExtra(TimerContract.EXTRA_WALK_REPS_COMPLETED, 0),
                intent.getIntExtra(TimerContract.EXTRA_REST_REPS_COMPLETED, 0),
                intent.getLongExtra(TimerContract.EXTRA_JOG_TIME_COMPLETED, 0L),
                intent.getLongExtra(TimerContract.EXTRA_WALK_TIME_COMPLETED, 0L),
                intent.getLongExtra(TimerContract.EXTRA_REST_TIME_COMPLETED, 0L)
        );

        updated = updated.withCurrentAct(actMode)
                .withMode(liveMode)
                .withRemainingMillis(intent.getLongExtra(TimerContract.EXTRA_REMAINING_MS, 0L));

        state.postValue(updated);
    }

    public void requestStateSync() {
        Intent requestIntent = new Intent(getApplication(), JalkTimerService.class)
                .setAction(TimerContract.ACTION_REQUEST_STATE);
        getApplication().startService(requestIntent);
    }

    public void sendEvent(LiveSessionEvent event) {
        if (event == null) {
            return;
        }
        Intent intent = buildServiceIntent(event, state.getValue());
        if (intent == null) {
            return;
        }
        if (event instanceof LiveSessionEvent.TapAct) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication().startForegroundService(intent);
            } else {
                getApplication().startService(intent);
            }
        } else {
            getApplication().startService(intent);
        }
    }

    private Intent buildServiceIntent(LiveSessionEvent event, LiveSessionState current) {
        if (event instanceof LiveSessionEvent.TapAct) {
            LiveSessionEvent.TapAct tapAct = (LiveSessionEvent.TapAct) event;
            ActMode actMode = tapAct.getActMode();
            if (actMode == null) {
                return null;
            }
            Intent intent = new Intent(getApplication(), JalkTimerService.class)
                    .setAction(TimerContract.ACTION_START_MODE)
                    .putExtra(TimerContract.EXTRA_MODE, actMode.getId());

            Long jogMs = resolveDuration(tapAct.getJogMs(), current, ActMode.JOG);
            Long walkMs = resolveDuration(tapAct.getWalkMs(), current, ActMode.WALK);
            Long restMs = resolveDuration(tapAct.getRestMs(), current, ActMode.REST);

            putDurationExtra(intent, JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS, jogMs);
            putDurationExtra(intent, JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS, walkMs);
            putDurationExtra(intent, JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS, restMs);

            if (actMode == ActMode.JOG) {
                putDurationExtra(intent, JalkTimerService.EXTRA_DURATION_MILLIS, jogMs);
            } else if (actMode == ActMode.WALK) {
                putDurationExtra(intent, JalkTimerService.EXTRA_DURATION_MILLIS, walkMs);
            } else if (actMode == ActMode.REST) {
                putDurationExtra(intent, JalkTimerService.EXTRA_DURATION_MILLIS, restMs);
            }

            putDurationExtra(intent, JalkTimerService.EXTRA_JOG_DURATION, jogMs);
            putDurationExtra(intent, JalkTimerService.EXTRA_WALK_DURATION, walkMs);
            putDurationExtra(intent, JalkTimerService.EXTRA_REST_DURATION, restMs);

            return intent;
        }

        if (event instanceof LiveSessionEvent.TapPauseResume) {
            Intent intent = new Intent(getApplication(), JalkTimerService.class);
            LiveMode mode = current != null ? current.getMode() : LiveMode.INIT;
            if (mode == LiveMode.PAUSED) {
                intent.setAction(JalkTimerService.ACTION_RESUME);
            } else {
                intent.setAction(JalkTimerService.ACTION_PAUSE);
            }
            return intent;
        }

        if (event instanceof LiveSessionEvent.TapStop) {
            return new Intent(getApplication(), JalkTimerService.class)
                    .setAction(TimerContract.ACTION_STOP_SESSION);
        }

        return null;
    }

    private static void putDurationExtra(Intent intent, String key, Long value) {
        if (intent == null || value == null || value <= 0L) {
            return;
        }
        intent.putExtra(key, value);
    }

    private static Long resolveDuration(Long eventValue, LiveSessionState current, ActMode actMode) {
        if (eventValue != null && eventValue > 0L) {
            return eventValue;
        }
        if (current != null) {
            long stored = current.getDurationMillis(actMode);
            if (stored > 0L) {
                return stored;
            }
        }
        return null;
    }

    private static long readDuration(Intent intent, String key, long fallback, long defaultValue) {
        if (intent != null && intent.hasExtra(key)) {
            long value = intent.getLongExtra(key, -1L);
            if (value > 0L) {
                return value;
            }
        }
        if (fallback > 0L) {
            return fallback;
        }
        return defaultValue;
    }

    private static LiveMode parseLiveMode(Intent intent, ActMode actMode) {
        if (intent == null) {
            return actMode != null ? LiveMode.ACTIVE : LiveMode.INIT;
        }
        String liveModeName = intent.getStringExtra(TimerContract.EXTRA_LIVE_MODE);
        if (liveModeName != null) {
            try {
                return LiveMode.valueOf(liveModeName);
            } catch (IllegalArgumentException ignored) {
            }
        }
        boolean paused = intent.getBooleanExtra(TimerContract.EXTRA_IS_PAUSED, false);
        boolean alerting = intent.getBooleanExtra(TimerContract.EXTRA_IS_ALERTING, false);
        if (actMode == null) {
            return LiveMode.INIT;
        }
        if (paused) {
            return LiveMode.PAUSED;
        }
        if (alerting) {
            return LiveMode.TIMEREND;
        }
        return LiveMode.ACTIVE;
    }
}
