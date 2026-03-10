package com.example.jalkster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.example.jalkster.timer.TimerContract;

public class TimerExpiredReceiver extends BroadcastReceiver {

    public static final String ACTION_TIMER_EXPIRED = "com.example.jalkster.ACTION_TIMER_EXPIRED";
    public static final String EXTRA_MODE = "extra_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!ACTION_TIMER_EXPIRED.equals(action)) {
            return;
        }
        Intent serviceIntent = new Intent(context, JalkTimerService.class)
                .setAction(JalkTimerService.ACTION_TIMER_EXPIRED)
                .putExtra(TimerContract.EXTRA_MODE, intent.getStringExtra(EXTRA_MODE));
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
