package com.example.jalkster;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PastSessionsActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "com.example.jalkster.extra.SESSION_ID";

    private ExecutorService executorService;
    private PastSessionsAdapter adapter;
    private FloatingActionButton deleteFab;
    private boolean deleteModeEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_sessions);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_past_sessions);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_past_sessions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PastSessionsAdapter(new SelectionChangeListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                updateDeleteFabState(selectedCount);
            }
        });
        recyclerView.setAdapter(adapter);

        deleteFab = findViewById(R.id.fab_delete_sessions);
        deleteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedSessions();
            }
        });
        deleteFab.setVisibility(View.GONE);
        deleteFab.setEnabled(false);

        executorService = Executors.newSingleThreadExecutor();
        loadSessions();
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
        getMenuInflater().inflate(R.menu.menu_past_sessions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.action_delete_mode);
        if (deleteItem != null) {
            deleteItem.setChecked(deleteModeEnabled);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_delete_mode) {
            toggleDeleteMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSessions() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final List<JalkSessionEntity> sessions = JalkDatabase.getInstance(getApplicationContext())
                        .jalkSessionDao()
                        .getAllSessions();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setSessions(sessions);
                        updateDeleteFabState(adapter.getSelectedCount());
                    }
                });
            }
        });
    }

    private void toggleDeleteMode() {
        setDeleteMode(!deleteModeEnabled);
        invalidateOptionsMenu();
    }

    private void setDeleteMode(boolean enabled) {
        deleteModeEnabled = enabled;
        adapter.setDeleteMode(enabled);
        deleteFab.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            deleteFab.setEnabled(false);
        } else {
            updateDeleteFabState(adapter.getSelectedCount());
        }
    }

    private void updateDeleteFabState(int selectedCount) {
        if (!deleteModeEnabled) {
            deleteFab.setEnabled(false);
            return;
        }
        deleteFab.setEnabled(selectedCount > 0);
    }

    private void deleteSelectedSessions() {
        final List<Integer> ids = adapter.getSelectedSessionIds();
        if (ids.isEmpty()) {
            return;
        }
        deleteFab.setEnabled(false);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                JalkDatabase.getInstance(getApplicationContext())
                        .jalkSessionDao()
                        .deleteByIds(ids);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setDeleteMode(false);
                        invalidateOptionsMenu();
                        loadSessions();
                    }
                });
            }
        });
    }

    private interface SelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private class PastSessionsAdapter extends RecyclerView.Adapter<PastSessionViewHolder> {

        private final List<JalkSessionEntity> sessions = new ArrayList<>();
        private final Set<Integer> selectedSessionIds = new HashSet<>();
        private final SelectionChangeListener selectionChangeListener;
        private boolean deleteMode;

        PastSessionsAdapter(SelectionChangeListener listener) {
            this.selectionChangeListener = listener;
        }

        @NonNull
        @Override
        public PastSessionViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_past_session, parent, false);
            return new PastSessionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PastSessionViewHolder holder, int position) {
            final JalkSessionEntity session = sessions.get(position);
            Date date = new Date(session.getStartTime());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(date);
            boolean isSelected = selectedSessionIds.contains(session.getId());
            boolean isXcSki = isXcSkiSession(session);
            String displayText = formattedDate;
            if (isXcSki) {
                displayText = formattedDate + " | " + formatElapsedDuration(session.getXcskiDurationMs());
            }
            int iconRes = isXcSki ? R.drawable.ic_session_xc_ski : R.drawable.ic_session_jalk;

            if (deleteMode) {
                holder.bind(displayText, iconRes, true, isSelected, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean newState = !selectedSessionIds.contains(session.getId());
                        updateSelection(session.getId(), newState);
                        notifyDataSetChanged();
                    }
                }, new android.widget.CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                        updateSelection(session.getId(), isChecked);
                    }
                });
            } else {
                holder.bind(displayText, iconRes, false, false, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(PastSessionsActivity.this, PastSessionDetailActivity.class);
                        intent.putExtra(EXTRA_SESSION_ID, session.getId());
                        startActivity(intent);
                    }
                }, null);
            }
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        void setSessions(List<JalkSessionEntity> newSessions) {
            sessions.clear();
            if (newSessions != null) {
                sessions.addAll(newSessions);
            }
            if (!deleteMode) {
                selectedSessionIds.clear();
            } else {
                Set<Integer> existingIds = new HashSet<>();
                for (JalkSessionEntity session : sessions) {
                    existingIds.add(session.getId());
                }
                selectedSessionIds.retainAll(existingIds);
            }
            notifyDataSetChanged();
            notifySelectionChanged();
        }

        void setDeleteMode(boolean enabled) {
            if (!enabled) {
                selectedSessionIds.clear();
            }
            deleteMode = enabled;
            notifyDataSetChanged();
            notifySelectionChanged();
        }

        List<Integer> getSelectedSessionIds() {
            return new ArrayList<>(selectedSessionIds);
        }

        int getSelectedCount() {
            return selectedSessionIds.size();
        }

        private void updateSelection(int sessionId, boolean isSelected) {
            if (isSelected) {
                selectedSessionIds.add(sessionId);
            } else {
                selectedSessionIds.remove(sessionId);
            }
            notifySelectionChanged();
        }

        private void notifySelectionChanged() {
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged(selectedSessionIds.size());
            }
        }
    }

    private class PastSessionViewHolder extends RecyclerView.ViewHolder {

        private final android.widget.TextView textView;
        private final android.widget.CheckBox checkBox;
        private final ImageView typeIcon;

        PastSessionViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_session_date);
            checkBox = itemView.findViewById(R.id.checkbox_select_session);
            typeIcon = itemView.findViewById(R.id.image_session_type);
        }

        void bind(String text,
                  int iconRes,
                  boolean showCheckbox,
                  boolean isChecked,
                  View.OnClickListener itemClickListener,
                  android.widget.CompoundButton.OnCheckedChangeListener checkboxListener) {
            textView.setText(text);
            typeIcon.setImageResource(iconRes);
            itemView.setOnClickListener(itemClickListener);
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(isChecked);
            checkBox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
            if (showCheckbox && checkboxListener != null) {
                checkBox.setOnCheckedChangeListener(checkboxListener);
            }
        }
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
