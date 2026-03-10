package com.example.jalkster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public final class NotificationUtils {

    public static final String CHANNEL_STATUS = "jalk_timer_status";
    public static final String CHANNEL_ALERT_SOUND = "jalk_timer_alert_sound";
    public static final String CHANNEL_ALERT_VIBRATE = "jalk_timer_alert_vibrate";

    private static final long[] ALERT_VIBRATION_PATTERN = new long[]{0L, 250L, 150L, 250L};

    private NotificationUtils() {
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                return;
            }

            List<NotificationChannel> channels = new ArrayList<>(2);

            NotificationChannel alertSoundChannel = new NotificationChannel(
                    CHANNEL_ALERT_SOUND,
                    "Jalk Timer Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertSoundChannel.setDescription("Sound and vibration alerts when a timer finishes");
            alertSoundChannel.enableVibration(true);
            alertSoundChannel.setVibrationPattern(ALERT_VIBRATION_PATTERN);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            alertSoundChannel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    audioAttributes
            );
            alertSoundChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                alertSoundChannel.setBypassDnd(false);
            }
            channels.add(alertSoundChannel);

            NotificationChannel alertVibrateChannel = new NotificationChannel(
                    CHANNEL_ALERT_VIBRATE,
                    "Jalk Vibrate-Only Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertVibrateChannel.setDescription("Vibration-only alerts when a timer finishes");
            alertVibrateChannel.enableVibration(true);
            alertVibrateChannel.setVibrationPattern(ALERT_VIBRATION_PATTERN);
            alertVibrateChannel.setSound(null, null);
            alertVibrateChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                alertVibrateChannel.setBypassDnd(false);
            }
            channels.add(alertVibrateChannel);

            notificationManager.createNotificationChannels(channels);
        }
    }
}
