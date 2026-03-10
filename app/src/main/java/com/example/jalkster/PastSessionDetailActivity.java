package com.example.jalkster;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionEntity;
import com.example.jalkster.ui.ActivityDistributionView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PastSessionDetailActivity extends AppCompatActivity {

    private static final String EXTRA_SESSION_ID = PastSessionsActivity.EXTRA_SESSION_ID;

    private ExecutorService executorService;
    private int sessionId = -1;

    private TextView jogRepDurationValue;
    private TextView jogRepsValue;
    private TextView jogTotalValue;
    private TextView walkRepDurationValue;
    private TextView walkRepsValue;
    private TextView walkTotalValue;
    private TextView restRepDurationValue;
    private TextView restRepsValue;
    private TextView restTotalValue;
    private TextView totalMovementValue;
    private TextView sessionStartValue;
    private TextView sessionEndValue;
    private TextView xcSkiDurationValue;
    private ActivityDistributionView activityDistributionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_session_detail);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_past_session_detail);
        }

        bindViews();

        sessionId = getIntent().getIntExtra(EXTRA_SESSION_ID, -1);
        if (sessionId == -1) {
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionId != -1) {
            loadSession(sessionId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_past_session_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_close) {
            finish();
            return true;
        } else if (itemId == R.id.action_edit_session) {
            Intent intent = new Intent(this, PastSessionEditActivity.class);
            intent.putExtra(EXTRA_SESSION_ID, sessionId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindViews() {
        jogRepDurationValue = findViewById(R.id.text_jog_rep_duration_value);
        jogRepsValue = findViewById(R.id.text_jog_reps_value);
        jogTotalValue = findViewById(R.id.text_jog_total_value);
        walkRepDurationValue = findViewById(R.id.text_walk_rep_duration_value);
        walkRepsValue = findViewById(R.id.text_walk_reps_value);
        walkTotalValue = findViewById(R.id.text_walk_total_value);
        restRepDurationValue = findViewById(R.id.text_rest_rep_duration_value);
        restRepsValue = findViewById(R.id.text_rest_reps_value);
        restTotalValue = findViewById(R.id.text_rest_total_value);
        totalMovementValue = findViewById(R.id.text_total_movement_value);
        sessionStartValue = findViewById(R.id.text_session_start_value);
        sessionEndValue = findViewById(R.id.text_session_end_value);
        xcSkiDurationValue = findViewById(R.id.text_xc_ski_duration_value);
        activityDistributionView = findViewById(R.id.activityDistributionView);
        if (activityDistributionView != null) {
            activityDistributionView.setTimes(0, 0, 0);
        }
    }

    private void loadSession(final int sessionId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final JalkSessionEntity session = JalkDatabase.getInstance(getApplicationContext())
                        .jalkSessionDao()
                        .getSessionById(sessionId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (session == null) {
                            finish();
                        } else {
                            displaySession(session);
                        }
                    }
                });
            }
        });
    }

    private void displaySession(JalkSessionEntity session) {
        boolean isXcSki = isXcSkiSession(session);
        int jogRepSeconds = session.getJogRepTime();
        int walkRepSeconds = session.getWalkRepTime();
        int restRepSeconds = session.getRestRepTime();

        jogRepDurationValue.setText(formatDuration(jogRepSeconds));
        jogRepsValue.setText(String.format(Locale.getDefault(), "%d", session.getJogRepsDone()));
        long totalJogSeconds = (long) jogRepSeconds * session.getJogRepsDone();
        jogTotalValue.setText(formatDuration(totalJogSeconds));

        walkRepDurationValue.setText(formatDuration(walkRepSeconds));
        walkRepsValue.setText(String.format(Locale.getDefault(), "%d", session.getWalkRepsDone()));
        long totalWalkSeconds = (long) walkRepSeconds * session.getWalkRepsDone();
        walkTotalValue.setText(formatDuration(totalWalkSeconds));

        restRepDurationValue.setText(formatDuration(restRepSeconds));
        restRepsValue.setText(String.format(Locale.getDefault(), "%d", session.getRestRepsDone()));
        long totalRestSeconds = (long) restRepSeconds * session.getRestRepsDone();
        restTotalValue.setText(formatDuration(totalRestSeconds));

        long totalMovementSeconds = totalJogSeconds + totalWalkSeconds;
        totalMovementValue.setText(formatDuration(totalMovementSeconds));

        if (activityDistributionView != null) {
            activityDistributionView.setTimes(totalJogSeconds, totalWalkSeconds, totalRestSeconds);
        }

        if (sessionStartValue != null) {
            sessionStartValue.setText(formatDateTime(session.getStartTime()));
        }
        if (sessionEndValue != null) {
            sessionEndValue.setText(formatDateTime(session.getEndTime()));
        }
        if (xcSkiDurationValue != null) {
            if (isXcSki) {
                xcSkiDurationValue.setText(formatElapsedDuration(session.getXcskiDurationMs()));
            } else {
                xcSkiDurationValue.setText(getString(R.string.value_not_available));
            }
        }
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
    }

    private String formatDateTime(long timeMillis) {
        if (timeMillis <= 0L) {
            return getString(R.string.value_not_available);
        }
        DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()
        );
        return format.format(new Date(timeMillis));
    }

    private boolean isXcSkiSession(JalkSessionEntity session) {
        if (session == null) {
            return false;
        }
        String type = session.getSessionType();
        return type != null && JalkSessionEntity.SESSION_TYPE_XCSKI.equalsIgnoreCase(type);
    }

    private String formatElapsedDuration(long durationMillis) {
        long totalSeconds = Math.max(0L, durationMillis) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d",
                    hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
