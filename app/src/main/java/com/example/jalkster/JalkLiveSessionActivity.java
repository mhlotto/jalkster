package com.example.jalkster;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.jalkster.live.ActMode;
import com.example.jalkster.live.LiveMode;
import com.example.jalkster.live.LiveSessionEvent;
import com.example.jalkster.live.LiveSessionState;
import com.example.jalkster.timer.TimerContract;

import java.util.Locale;

public class JalkLiveSessionActivity extends AppCompatActivity {

    private static final String TAG = "VRI[JalkTimer]";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 100;

    private TextView modeTimerTextView;
    private TextView jogTimeTextView;
    private TextView jogRepsTextView;
    private TextView walkTimeTextView;
    private TextView walkRepsTextView;
    private TextView restTimeTextView;
    private TextView restRepsTextView;
    private Button jogButton;
    private Button walkButton;
    private Button restButton;
    private Button pauseButton;
    private Button resumeButton;
    private Button stopButton;

    private BroadcastReceiver timerReceiver;
    private boolean flashRunning = false;
    private ObjectAnimator flashAnimator;

    private long jogDurationInMillis;
    private long walkDurationInMillis;
    private long restDurationInMillis;

    private JalkLiveSessionViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure this activity can appear above the lock screen
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

        setContentView(R.layout.activity_jalk_live_session);

        requestPostNotificationsPermissionIfNeeded();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        bindViews();

        viewModel = new ViewModelProvider(this).get(JalkLiveSessionViewModel.class);
        viewModel.getState().observe(this, this::renderState);

        Intent initialIntent = getIntent();
        extractDurationsFromIntent(initialIntent);
        if (initialIntent == null
                || !initialIntent.hasExtra(JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS)
                || !initialIntent.hasExtra(JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS)
                || !initialIntent.hasExtra(JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS)) {
            viewModel.requestStateSync();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra(JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS)) {
            extractDurationsFromIntent(intent);
        } else {
            viewModel.requestStateSync();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS granted");
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS denied");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (timerReceiver == null) {
            timerReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    viewModel.updateFromBroadcast(intent);
                }
            };
        }

        IntentFilter filter = new IntentFilter(TimerContract.ACTION_TIMER_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, filter);

        if (shouldRequestStateSync()) {
            viewModel.requestStateSync();
        }
        Log.d(TAG, "Registered timerReceiver");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (timerReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(timerReceiver);
            Log.d(TAG, "Unregistered timerReceiver");
        }

        // stop any flashing when activity is not visible
        stopFlashing();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindViews() {
        modeTimerTextView = findViewById(R.id.text_mode_timer);
        jogTimeTextView = findViewById(R.id.text_jog_time_total);
        jogRepsTextView = findViewById(R.id.text_jog_reps_total);
        walkTimeTextView = findViewById(R.id.text_walk_time_total);
        walkRepsTextView = findViewById(R.id.text_walk_reps_total);
        restTimeTextView = findViewById(R.id.text_rest_time_total);
        restRepsTextView = findViewById(R.id.text_rest_reps_total);

        jogButton = findViewById(R.id.button_jog);
        walkButton = findViewById(R.id.button_walk);
        restButton = findViewById(R.id.button_rest);
        pauseButton = findViewById(R.id.button_pause);
        resumeButton = findViewById(R.id.button_resume);
        stopButton = findViewById(R.id.button_stop);

        jogButton.setOnClickListener(v -> viewModel.sendEvent(
                new LiveSessionEvent.TapJog(jogDurationInMillis, walkDurationInMillis, restDurationInMillis)));
        walkButton.setOnClickListener(v -> viewModel.sendEvent(
                new LiveSessionEvent.TapWalk(jogDurationInMillis, walkDurationInMillis, restDurationInMillis)));
        restButton.setOnClickListener(v -> viewModel.sendEvent(
                new LiveSessionEvent.TapRest(jogDurationInMillis, walkDurationInMillis, restDurationInMillis)));
        pauseButton.setOnClickListener(v -> viewModel.sendEvent(new LiveSessionEvent.TapPauseResume()));
        resumeButton.setOnClickListener(v -> viewModel.sendEvent(new LiveSessionEvent.TapPauseResume()));
        stopButton.setOnClickListener(v -> stopSessionAndExit());
    }

    private void requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
    }

    private void extractDurationsFromIntent(@Nullable Intent intent) {
        if (intent == null) {
            if (jogDurationInMillis <= 0L) {
                jogDurationInMillis = 60000L;
            }
            if (walkDurationInMillis <= 0L) {
                walkDurationInMillis = 60000L;
            }
            if (restDurationInMillis <= 0L) {
                restDurationInMillis = 30000L;
            }
            viewModel.seedDurations(jogDurationInMillis, walkDurationInMillis, restDurationInMillis);
            return;
        }

        if (intent.hasExtra(JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS)) {
            jogDurationInMillis = intent.getLongExtra(JalkSessionConfigActivity.EXTRA_JOG_DURATION_MILLIS, 60000L);
        } else if (intent.hasExtra(JalkTimerService.EXTRA_JOG_DURATION)) {
            jogDurationInMillis = intent.getLongExtra(JalkTimerService.EXTRA_JOG_DURATION,
                    jogDurationInMillis > 0L ? jogDurationInMillis : 60000L);
        } else if (jogDurationInMillis <= 0L) {
            jogDurationInMillis = 60000L;
        }

        if (intent.hasExtra(JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS)) {
            walkDurationInMillis = intent.getLongExtra(JalkSessionConfigActivity.EXTRA_WALK_DURATION_MILLIS, 60000L);
        } else if (intent.hasExtra(JalkTimerService.EXTRA_WALK_DURATION)) {
            walkDurationInMillis = intent.getLongExtra(JalkTimerService.EXTRA_WALK_DURATION,
                    walkDurationInMillis > 0L ? walkDurationInMillis : 60000L);
        } else if (walkDurationInMillis <= 0L) {
            walkDurationInMillis = 60000L;
        }

        if (intent.hasExtra(JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS)) {
            restDurationInMillis = intent.getLongExtra(JalkSessionConfigActivity.EXTRA_REST_DURATION_MILLIS, 30000L);
        } else if (intent.hasExtra(JalkTimerService.EXTRA_REST_DURATION)) {
            restDurationInMillis = intent.getLongExtra(JalkTimerService.EXTRA_REST_DURATION,
                    restDurationInMillis > 0L ? restDurationInMillis : 30000L);
        } else if (restDurationInMillis <= 0L) {
            restDurationInMillis = 30000L;
        }

        viewModel.seedDurations(jogDurationInMillis, walkDurationInMillis, restDurationInMillis);
    }

    private void renderState(@Nullable LiveSessionState state) {
        if (state == null) {
            showIdleState();
            return;
        }
        syncDurationsFromState(state);
        updateModeTimerText(state);
        updateModeTotals(state);
        updateButtons(state);

        if (state.getMode() == LiveMode.TIMEREND) {
            startFlashing();
        } else {
            stopFlashing();
        }
    }

    private void syncDurationsFromState(LiveSessionState state) {
        long jog = state.getDurationMillis(ActMode.JOG);
        long walk = state.getDurationMillis(ActMode.WALK);
        long rest = state.getDurationMillis(ActMode.REST);
        if (jog > 0L) {
            jogDurationInMillis = jog;
        }
        if (walk > 0L) {
            walkDurationInMillis = walk;
        }
        if (rest > 0L) {
            restDurationInMillis = rest;
        }
    }

    private void updateModeTimerText(LiveSessionState state) {
        ActMode actMode = state.getCurrentAct();
        if (state.getMode() == LiveMode.INIT || actMode == null) {
            modeTimerTextView.setText(getString(R.string.waiting_for_mode));
            return;
        }

        long totalSeconds = state.getRemainingMillis() / 1000L;
        boolean overtime = totalSeconds < 0L;
        long absSeconds = Math.abs(totalSeconds);
        long minutes = absSeconds / 60L;
        long seconds = absSeconds % 60L;

        String modeLabel = capitalize(actMode.getId());
        if (state.getMode() == LiveMode.PAUSED) {
            modeLabel = modeLabel + getString(R.string.mode_paused_suffix);
        }

        String label = String.format(
                Locale.getDefault(),
                "%s - %s%02d:%02d",
                modeLabel,
                overtime ? "-" : "",
                minutes,
                seconds
        );
        modeTimerTextView.setText(label);
    }

    private void updateButtons(LiveSessionState state) {
        LiveMode mode = state.getMode();
        boolean enableModeSelection = mode == LiveMode.INIT || mode == LiveMode.TIMEREND;
        jogButton.setEnabled(enableModeSelection);
        walkButton.setEnabled(enableModeSelection);
        restButton.setEnabled(enableModeSelection);

        if (mode == LiveMode.PAUSED) {
            pauseButton.setVisibility(View.GONE);
            resumeButton.setVisibility(View.VISIBLE);
            resumeButton.setEnabled(true);
        } else {
            pauseButton.setVisibility(View.VISIBLE);
            resumeButton.setVisibility(View.GONE);
            pauseButton.setEnabled(mode == LiveMode.ACTIVE);
        }

        stopButton.setEnabled(true);
    }

    private void updateModeTotals(LiveSessionState state) {
        updateModeTotals(
                state.getTimeCompletedMillis(ActMode.JOG),
                state.getRepsDone(ActMode.JOG),
                state.getTimeCompletedMillis(ActMode.WALK),
                state.getRepsDone(ActMode.WALK),
                state.getTimeCompletedMillis(ActMode.REST),
                state.getRepsDone(ActMode.REST)
        );
    }

    private void updateModeTotals(long jogTimeMillis, int jogReps,
                                  long walkTimeMillis, int walkReps,
                                  long restTimeMillis, int restReps) {
        if (jogTimeTextView == null || jogRepsTextView == null
                || walkTimeTextView == null || walkRepsTextView == null
                || restTimeTextView == null || restRepsTextView == null) {
            return;
        }
        final String jogTimeText = formatTime(jogTimeMillis);
        final String walkTimeText = formatTime(walkTimeMillis);
        final String restTimeText = formatTime(restTimeMillis);
        final String jogRepsText = String.valueOf(Math.max(0, jogReps));
        final String walkRepsText = String.valueOf(Math.max(0, walkReps));
        final String restRepsText = String.valueOf(Math.max(0, restReps));
        jogTimeTextView.setText(jogTimeText);
        jogRepsTextView.setText(jogRepsText);
        walkTimeTextView.setText(walkTimeText);
        walkRepsTextView.setText(walkRepsText);
        restTimeTextView.setText(restTimeText);
        restRepsTextView.setText(restRepsText);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void startFlashing() {
        if (flashRunning) {
            return;
        }
        flashRunning = true;
        if (flashAnimator == null) {
            flashAnimator = ObjectAnimator.ofFloat(modeTimerTextView, "alpha", 1f, 0.3f);
            flashAnimator.setDuration(300);
            flashAnimator.setRepeatMode(ValueAnimator.REVERSE);
            flashAnimator.setRepeatCount(ValueAnimator.INFINITE);
        }
        flashAnimator.start();
    }

    private void stopFlashing() {
        if (!flashRunning) {
            return;
        }
        flashRunning = false;
        if (flashAnimator != null) {
            flashAnimator.cancel();
        }
        modeTimerTextView.setAlpha(1f);
    }

    private void showIdleState() {
        modeTimerTextView.setText(getString(R.string.waiting_for_mode));
        updateModeTotals(0L, 0, 0L, 0, 0L, 0);
        LiveSessionState idle = LiveSessionState.initial(
                jogDurationInMillis > 0L ? jogDurationInMillis : 60000L,
                walkDurationInMillis > 0L ? walkDurationInMillis : 60000L,
                restDurationInMillis > 0L ? restDurationInMillis : 30000L
        );
        updateButtons(idle);
    }

    private void stopSessionAndExit() {
        viewModel.sendEvent(new LiveSessionEvent.TapStop());

        Intent mainIntent = new Intent(this, JalkMainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);

        finish();
    }

    private boolean shouldRequestStateSync() {
        if (SessionManager.isActive(this)) {
            return true;
        }
        return jogDurationInMillis <= 0L || walkDurationInMillis <= 0L || restDurationInMillis <= 0L;
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
