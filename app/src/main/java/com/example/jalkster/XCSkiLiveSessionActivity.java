package com.example.jalkster;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionEntity;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class XCSkiLiveSessionActivity extends AppCompatActivity {

    private static final long UPDATE_INTERVAL_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = isPaused ? pausedElapsedMillis
                    : SystemClock.elapsedRealtime() - baseElapsedRealtime;
            updateTimerDisplay(elapsed);
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private TextView timerTextView;
    private MaterialButton pauseResumeButton;
    private MaterialButton stopButton;

    private boolean isPaused;
    private long baseElapsedRealtime;
    private long pausedElapsedMillis;
    private long sessionStartTimeMillis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_xc_ski_live_session);

        timerTextView = findViewById(R.id.text_xc_ski_timer);
        pauseResumeButton = findViewById(R.id.button_pause_resume);
        stopButton = findViewById(R.id.button_stop);

        pauseResumeButton.setOnClickListener(v -> togglePauseResume());
        stopButton.setOnClickListener(v -> stopAndExit());

        isPaused = false;
        pausedElapsedMillis = 0L;
        baseElapsedRealtime = SystemClock.elapsedRealtime();
        sessionStartTimeMillis = System.currentTimeMillis();
        updatePauseResumeLabel();
        updateTimerDisplay(0L);
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(tickRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(tickRunnable);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }

    private void togglePauseResume() {
        if (!isPaused) {
            pausedElapsedMillis = SystemClock.elapsedRealtime() - baseElapsedRealtime;
            isPaused = true;
        } else {
            baseElapsedRealtime = SystemClock.elapsedRealtime() - pausedElapsedMillis;
            isPaused = false;
        }
        updatePauseResumeLabel();
        long elapsed = isPaused ? pausedElapsedMillis
                : SystemClock.elapsedRealtime() - baseElapsedRealtime;
        updateTimerDisplay(elapsed);
    }

    private void updatePauseResumeLabel() {
        pauseResumeButton.setText(isPaused ? getString(R.string.resume_label)
                : getString(R.string.pause_label));
    }

    private void updateTimerDisplay(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        String formatted = String.format(Locale.getDefault(), "%02d:%02d",
                minutes, seconds);
        timerTextView.setText(formatted);
    }

    private void stopAndExit() {
        long elapsedDurationMs = isPaused
                ? pausedElapsedMillis
                : SystemClock.elapsedRealtime() - baseElapsedRealtime;
        long endTimeMillis = System.currentTimeMillis();
        JalkSessionEntity session = new JalkSessionEntity(
                sessionStartTimeMillis,
                endTimeMillis,
                0,
                0,
                0,
                0,
                0,
                0
        );
        session.setSessionType(JalkSessionEntity.SESSION_TYPE_XCSKI);
        session.setXcskiDurationMs(Math.max(0L, elapsedDurationMs));
        new Thread(() -> JalkDatabase.getInstance(getApplicationContext())
                .jalkSessionDao()
                .insert(session)).start();

        Intent mainIntent = new Intent(this, JalkMainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
    }
}
