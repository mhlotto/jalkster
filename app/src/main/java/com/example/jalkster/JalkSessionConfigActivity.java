package com.example.jalkster;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jalkster.ui.util.RangeInputFilter;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class JalkSessionConfigActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_MODE = "com.example.jalkster.extra.SESSION_MODE";
    public static final String EXTRA_JOG_DURATION_MILLIS = "com.example.jalkster.extra.JOG_DURATION_MILLIS";
    public static final String EXTRA_WALK_DURATION_MILLIS = "com.example.jalkster.extra.WALK_DURATION_MILLIS";
    public static final String EXTRA_REST_DURATION_MILLIS = "com.example.jalkster.extra.REST_DURATION_MILLIS";

    public static final String MODE_WALK = "walk";
    public static final String MODE_JOG = "jog";
    public static final String MODE_REST = "rest";

    private TextInputEditText jogMinutesEditText;
    private TextInputEditText jogSecondsEditText;
    private TextInputEditText walkMinutesEditText;
    private TextInputEditText walkSecondsEditText;
    private TextInputEditText restMinutesEditText;
    private TextInputEditText restSecondsEditText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jalk_session_config);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        jogMinutesEditText = findViewById(R.id.jog_minutes);
        jogSecondsEditText = findViewById(R.id.jog_seconds);
        walkMinutesEditText = findViewById(R.id.walk_minutes);
        walkSecondsEditText = findViewById(R.id.walk_seconds);
        restMinutesEditText = findViewById(R.id.rest_minutes);
        restSecondsEditText = findViewById(R.id.rest_seconds);

        applyRangeFilter(jogMinutesEditText);
        applyRangeFilter(jogSecondsEditText);
        applyRangeFilter(walkMinutesEditText);
        applyRangeFilter(walkSecondsEditText);
        applyRangeFilter(restMinutesEditText);
        applyRangeFilter(restSecondsEditText);

        attachFormattingListener(jogMinutesEditText);
        attachFormattingListener(jogSecondsEditText);
        attachFormattingListener(walkMinutesEditText);
        attachFormattingListener(walkSecondsEditText);
        attachFormattingListener(restMinutesEditText);
        attachFormattingListener(restSecondsEditText);

        Button startButton = findViewById(R.id.button_start_jalk);
        Button cancelButton = findViewById(R.id.button_cancel);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchLiveSession(v);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        resetTimers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchLiveSession(View anchorView) {
        normalizeAllFields();

        long jogMillis = calculateDurationMillis(jogMinutesEditText, jogSecondsEditText);
        long walkMillis = calculateDurationMillis(walkMinutesEditText, walkSecondsEditText);
        long restMillis = calculateDurationMillis(restMinutesEditText, restSecondsEditText);

        if (jogMillis == 0 && walkMillis == 0) {
            Snackbar.make(anchorView, R.string.error_non_zero_walk_or_jog_timer, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (SessionManager.isActive(this)) {
            SessionManager.SessionState state = SessionManager.getState(this);
            Toast.makeText(this, R.string.session_already_active, Toast.LENGTH_SHORT).show();
            long activeJog = state != null ? state.jogDurationMs : jogMillis;
            long activeWalk = state != null ? state.walkDurationMs : walkMillis;
            long activeRest = state != null ? state.restDurationMs : restMillis;
            String activeMode = (state != null && state.mode != null) ? state.mode : MODE_WALK;
            startActivity(SessionIntentFactory.buildLiveSessionIntent(
                    this,
                    activeJog,
                    activeWalk,
                    activeRest,
                    activeMode
            ));
            return;
        }

        startActivity(SessionIntentFactory.buildLiveSessionIntent(
                this,
                jogMillis,
                walkMillis,
                restMillis,
                MODE_WALK
        ));
    }

    private long calculateDurationMillis(TextInputEditText minutesField, TextInputEditText secondsField) {
        int minutes = parseFieldValue(minutesField);
        int seconds = parseFieldValue(secondsField);
        return ((minutes * 60L) + seconds) * 1000L;
    }

    private int parseFieldValue(TextInputEditText field) {
        if (field.getText() == null) {
            return 0;
        }

        String value = field.getText().toString().trim();
        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void applyRangeFilter(TextInputEditText editText) {
        InputFilter[] filters = new InputFilter[]{new RangeInputFilter(0, 59)};
        editText.setFilters(filters);
    }

    private void attachFormattingListener(final TextInputEditText editText) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    normalizeField(editText);
                }
            }
        });
    }

    private void normalizeAllFields() {
        normalizeField(jogMinutesEditText);
        normalizeField(jogSecondsEditText);
        normalizeField(walkMinutesEditText);
        normalizeField(walkSecondsEditText);
        normalizeField(restMinutesEditText);
        normalizeField(restSecondsEditText);
    }

    private void normalizeField(TextInputEditText editText) {
        int value = parseFieldValue(editText);
        editText.setText(formatTwoDigits(value));
    }

    private String formatTwoDigits(int value) {
        return String.format(Locale.US, "%02d", value);
    }

    private void resetTimers() {
        setTime(jogMinutesEditText, jogSecondsEditText, 1, 0);
        setTime(walkMinutesEditText, walkSecondsEditText, 1, 0);
        setTime(restMinutesEditText, restSecondsEditText, 0, 30);
    }

    private void setTime(TextInputEditText minutesField, TextInputEditText secondsField, int minutes, int seconds) {
        minutesField.setText(formatTwoDigits(minutes));
        secondsField.setText(formatTwoDigits(seconds));
    }
}
