package com.example.jalkster;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PastSessionEditActivity extends AppCompatActivity {

    private static final String EXTRA_SESSION_ID = PastSessionsActivity.EXTRA_SESSION_ID;

    private ExecutorService executorService;
    private int sessionId = -1;
    private JalkSessionEntity session;

    private TextInputLayout walkRepTimeLayout;
    private TextInputLayout jogRepTimeLayout;
    private TextInputLayout restRepTimeLayout;
    private TextInputLayout walkRepsDoneLayout;
    private TextInputLayout jogRepsDoneLayout;
    private TextInputLayout restRepsDoneLayout;
    private TextInputLayout xcSkiDurationMinutesLayout;
    private TextInputLayout xcSkiDurationLayout;

    private TextInputEditText walkRepTimeInput;
    private TextInputEditText jogRepTimeInput;
    private TextInputEditText restRepTimeInput;
    private TextInputEditText walkRepsDoneInput;
    private TextInputEditText jogRepsDoneInput;
    private TextInputEditText restRepsDoneInput;
    private TextInputEditText xcSkiDurationMinutesInput;
    private TextInputEditText xcSkiDurationInput;

    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private boolean xcSkiSession;
    private LinearLayout xcSkiDurationRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_session_edit);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.title_past_session_edit);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        bindViews();

        saveButton.setEnabled(false);

        sessionId = getIntent().getIntExtra(EXTRA_SESSION_ID, -1);
        if (sessionId == -1) {
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        loadSession(sessionId);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSave();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void bindViews() {
        walkRepTimeLayout = findViewById(R.id.layout_walk_rep_time);
        jogRepTimeLayout = findViewById(R.id.layout_jog_rep_time);
        restRepTimeLayout = findViewById(R.id.layout_rest_rep_time);
        walkRepsDoneLayout = findViewById(R.id.layout_walk_reps_done);
        jogRepsDoneLayout = findViewById(R.id.layout_jog_reps_done);
        restRepsDoneLayout = findViewById(R.id.layout_rest_reps_done);
        xcSkiDurationRow = findViewById(R.id.layout_xc_ski_duration_row);
        xcSkiDurationMinutesLayout = findViewById(R.id.layout_xc_ski_duration_minutes);
        xcSkiDurationLayout = findViewById(R.id.layout_xc_ski_duration_seconds);

        walkRepTimeInput = findViewById(R.id.edit_walk_rep_time);
        jogRepTimeInput = findViewById(R.id.edit_jog_rep_time);
        restRepTimeInput = findViewById(R.id.edit_rest_rep_time);
        walkRepsDoneInput = findViewById(R.id.edit_walk_reps_done);
        jogRepsDoneInput = findViewById(R.id.edit_jog_reps_done);
        restRepsDoneInput = findViewById(R.id.edit_rest_reps_done);
        xcSkiDurationMinutesInput = findViewById(R.id.edit_xc_ski_duration_minutes);
        xcSkiDurationInput = findViewById(R.id.edit_xc_ski_duration_seconds);

        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
    }

    private void loadSession(final int sessionId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final JalkSessionEntity loadedSession = JalkDatabase.getInstance(getApplicationContext())
                        .jalkSessionDao()
                        .getSessionById(sessionId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadedSession == null) {
                            finish();
                        } else {
                            session = loadedSession;
                            populateFields(loadedSession);
                            xcSkiSession = isXcSkiSession(loadedSession);
                            if (xcSkiSession) {
                                configureForXcSkiEditing();
                            } else {
                                saveButton.setEnabled(true);
                            }
                        }
                    }
                });
            }
        });
    }

    private void populateFields(JalkSessionEntity session) {
        walkRepTimeInput.setText(String.valueOf(session.getWalkRepTime()));
        jogRepTimeInput.setText(String.valueOf(session.getJogRepTime()));
        restRepTimeInput.setText(String.valueOf(session.getRestRepTime()));
        walkRepsDoneInput.setText(String.valueOf(session.getWalkRepsDone()));
        jogRepsDoneInput.setText(String.valueOf(session.getJogRepsDone()));
        restRepsDoneInput.setText(String.valueOf(session.getRestRepsDone()));
        long totalSeconds = Math.max(0L, session.getXcskiDurationMs() / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        xcSkiDurationMinutesInput.setText(String.valueOf(minutes));
        xcSkiDurationInput.setText(String.valueOf(seconds));
    }

    private void attemptSave() {
        if (session == null) {
            return;
        }

        clearErrors();

        if (xcSkiSession) {
            Integer xcSkiDurationMinutes = parseNonNegativeInteger(
                    xcSkiDurationMinutesLayout,
                    xcSkiDurationMinutesInput
            );
            Integer xcSkiDurationSeconds = parseSeconds(
                    xcSkiDurationLayout,
                    xcSkiDurationInput
            );
            if (xcSkiDurationMinutes == null || xcSkiDurationSeconds == null) {
                return;
            }

            long durationMillis = ((long) xcSkiDurationMinutes * 60L
                    + (long) xcSkiDurationSeconds) * 1000L;
            session.setXcskiDurationMs(Math.max(0L, durationMillis));
            session.setWalkRepTime(0);
            session.setJogRepTime(0);
            session.setRestRepTime(0);
            session.setWalkRepsDone(0);
            session.setJogRepsDone(0);
            session.setRestRepsDone(0);

            saveSessionAndFinish();
            return;
        }

        Integer walkRepTime = parseNonNegativeInteger(walkRepTimeLayout, walkRepTimeInput);
        Integer jogRepTime = parseNonNegativeInteger(jogRepTimeLayout, jogRepTimeInput);
        Integer restRepTime = parseNonNegativeInteger(restRepTimeLayout, restRepTimeInput);
        Integer walkRepsDone = parseNonNegativeInteger(walkRepsDoneLayout, walkRepsDoneInput);
        Integer jogRepsDone = parseNonNegativeInteger(jogRepsDoneLayout, jogRepsDoneInput);
        Integer restRepsDone = parseNonNegativeInteger(restRepsDoneLayout, restRepsDoneInput);

        if (walkRepTime == null || jogRepTime == null || restRepTime == null
                || walkRepsDone == null || jogRepsDone == null || restRepsDone == null) {
            return;
        }

        session.setWalkRepTime(walkRepTime);
        session.setJogRepTime(jogRepTime);
        session.setRestRepTime(restRepTime);
        session.setWalkRepsDone(walkRepsDone);
        session.setJogRepsDone(jogRepsDone);
        session.setRestRepsDone(restRepsDone);

        saveSessionAndFinish();
    }

    private void saveSessionAndFinish() {
        saveButton.setEnabled(false);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                JalkDatabase.getInstance(getApplicationContext())
                        .jalkSessionDao()
                        .updateSession(session);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        });
    }

    private boolean isXcSkiSession(JalkSessionEntity session) {
        if (session == null) {
            return false;
        }
        String type = session.getSessionType();
        return type != null && JalkSessionEntity.SESSION_TYPE_XCSKI.equalsIgnoreCase(type);
    }

    private void configureForXcSkiEditing() {
        walkRepTimeLayout.setVisibility(View.GONE);
        jogRepTimeLayout.setVisibility(View.GONE);
        restRepTimeLayout.setVisibility(View.GONE);
        walkRepsDoneLayout.setVisibility(View.GONE);
        jogRepsDoneLayout.setVisibility(View.GONE);
        restRepsDoneLayout.setVisibility(View.GONE);
        xcSkiDurationRow.setVisibility(View.VISIBLE);
        saveButton.setEnabled(true);
    }

    private void clearErrors() {
        walkRepTimeLayout.setError(null);
        jogRepTimeLayout.setError(null);
        restRepTimeLayout.setError(null);
        walkRepsDoneLayout.setError(null);
        jogRepsDoneLayout.setError(null);
        restRepsDoneLayout.setError(null);
        xcSkiDurationMinutesLayout.setError(null);
        xcSkiDurationLayout.setError(null);
    }

    private Integer parseNonNegativeInteger(TextInputLayout layout, TextInputEditText input) {
        CharSequence text = input.getText();
        if (TextUtils.isEmpty(text)) {
            layout.setError(getString(R.string.error_required_number));
            return null;
        }

        try {
            int value = Integer.parseInt(text.toString());
            if (value < 0) {
                layout.setError(getString(R.string.error_non_negative_number));
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            layout.setError(getString(R.string.error_required_number));
            return null;
        }
    }

    private Integer parseSeconds(TextInputLayout layout, TextInputEditText input) {
        Integer value = parseNonNegativeInteger(layout, input);
        if (value == null) {
            return null;
        }
        if (value > 59) {
            layout.setError(getString(R.string.error_seconds_range));
            return null;
        }
        return value;
    }
}
