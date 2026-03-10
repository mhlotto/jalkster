package com.example.jalkster;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jalkster.timer.TimerContract;

public class JalkMainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_TIMER_SOUND = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jalk_main);

        Button newJalkButton = findViewById(R.id.button_new_jalk);
        Button newXcSkiButton = findViewById(R.id.button_new_xc_ski);
        Button reviewPastButton = findViewById(R.id.button_review_past);
        Button setSoundButton = findViewById(R.id.button_set_sound);
        Button statsButton = findViewById(R.id.statsButton);
        Button exitButton = findViewById(R.id.button_exit);

        newJalkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JalkMainActivity.this, JalkSessionConfigActivity.class);
                startActivity(intent);
            }
        });

        newXcSkiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JalkMainActivity.this, XCSkiSessionConfigActivity.class);
                startActivity(intent);
            }
        });

        reviewPastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JalkMainActivity.this, PastSessionsActivity.class);
                startActivity(intent);
            }
        });

        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(JalkMainActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });

        setSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSoundPicker();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });

        if (SoundPreferenceManager.getSelectedSoundUri(this) == null) {
            launchSoundPicker();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_PICK_TIMER_SOUND) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (pickedUri != null) {
                SoundPreferenceManager.saveSelectedSoundUri(this, pickedUri);
                NotificationUtils.createNotificationChannels(this);
            } else {
                Toast.makeText(this, R.string.timer_sound_not_selected, Toast.LENGTH_SHORT).show();
            }
        } else if (SoundPreferenceManager.getSelectedSoundUri(this) == null) {
            Toast.makeText(this, R.string.timer_sound_required_message, Toast.LENGTH_LONG).show();
        }
    }

    private void launchSoundPicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_RINGTONE | RingtoneManager.TYPE_NOTIFICATION | RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_timer_sound));
        Uri existing = SoundPreferenceManager.getSelectedSoundUri(this);
        if (existing != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_TIMER_SOUND);
    }

    private void exitApp() {
        // Stop any running timer session/service before closing the task.
        Intent stopIntent = new Intent(this, JalkTimerService.class)
                .setAction(TimerContract.ACTION_STOP_SESSION);
        startService(stopIntent);
        stopService(new Intent(this, JalkTimerService.class));

        // Clear any persisted "active" state and visible notifications.
        SessionManager.clear(this);
        androidx.core.app.NotificationManagerCompat.from(this).cancelAll();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                for (ActivityManager.AppTask task : am.getAppTasks()) {
                    task.finishAndRemoveTask();
                }
            }
        }

        finishAffinity();
        finishAndRemoveTask(); // API 21+
        moveTaskToBack(true);

        // TODO: Revisit whether this forced process kill is still needed once
        // service/task shutdown is fully reliable through normal Android flows.
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
