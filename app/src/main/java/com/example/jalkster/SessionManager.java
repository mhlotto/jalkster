package com.example.jalkster;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {

    private static final String PREF_ACTIVE_SESSION = "active_session";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_MODE = "mode";
    private static final String KEY_JOG_MS = "jogMs";
    private static final String KEY_WALK_MS = "walkMs";
    private static final String KEY_REST_MS = "restMs";
    private static final String KEY_REP = "rep";
    private static final String KEY_SESSION_START = "sessionStart";
    private static final String KEY_END_ELAPSED = "endElapsed";
    private static final String KEY_REMAINING = "remaining";
    private static final String KEY_IS_PAUSED = "isPaused";
    private static final String KEY_HAS_ALERT = "hasAlert";
    private static final String KEY_JOG_REPS = "jogReps";
    private static final String KEY_WALK_REPS = "walkReps";
    private static final String KEY_REST_REPS = "restReps";
    private static final String KEY_JOG_TIME = "jogTime";
    private static final String KEY_WALK_TIME = "walkTime";
    private static final String KEY_REST_TIME = "restTime";
    private static final String KEY_SESSION_STARTED = "sessionStarted";
    private static final String KEY_CURRENT_MODE_DURATION = "currentModeDuration";
    private static final String KEY_LAST_TICK_REMAINING = "lastTickRemaining";
    private static final String KEY_PAUSED_REMAINING = "pausedRemaining";

    private SessionManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_ACTIVE_SESSION, Context.MODE_PRIVATE);
    }

    public static boolean isActive(Context context) {
        return prefs(context).getBoolean(KEY_ACTIVE, false);
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    public static void persist(Context context, SessionState state) {
        if (state == null) {
            clear(context);
            return;
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_ACTIVE, true);
        editor.putString(KEY_MODE, state.mode);
        editor.putLong(KEY_JOG_MS, state.jogDurationMs);
        editor.putLong(KEY_WALK_MS, state.walkDurationMs);
        editor.putLong(KEY_REST_MS, state.restDurationMs);
        editor.putInt(KEY_REP, state.currentRep);
        editor.putLong(KEY_SESSION_START, state.sessionStartTime);
        editor.putLong(KEY_END_ELAPSED, state.endElapsedRealtime);
        editor.putLong(KEY_REMAINING, state.remainingMillis);
        editor.putBoolean(KEY_IS_PAUSED, state.isPaused);
        editor.putBoolean(KEY_HAS_ALERT, state.hasAlertFired);
        editor.putInt(KEY_JOG_REPS, state.jogRepsCompleted);
        editor.putInt(KEY_WALK_REPS, state.walkRepsCompleted);
        editor.putInt(KEY_REST_REPS, state.restRepsCompleted);
        editor.putLong(KEY_JOG_TIME, state.jogTimeCompletedMillis);
        editor.putLong(KEY_WALK_TIME, state.walkTimeCompletedMillis);
        editor.putLong(KEY_REST_TIME, state.restTimeCompletedMillis);
        editor.putBoolean(KEY_SESSION_STARTED, state.hasSessionStarted);
        editor.putLong(KEY_CURRENT_MODE_DURATION, state.currentModeDurationMillis);
        editor.putLong(KEY_LAST_TICK_REMAINING, state.lastTickRemainingMillis);
        editor.putLong(KEY_PAUSED_REMAINING, state.pausedRemainingMillis);
        editor.apply();
    }

    public static SessionState getState(Context context) {
        SharedPreferences preferences = prefs(context);
        if (!preferences.getBoolean(KEY_ACTIVE, false)) {
            return null;
        }
        return new SessionState(
                preferences.getString(KEY_MODE, null),
                preferences.getLong(KEY_JOG_MS, 60000L),
                preferences.getLong(KEY_WALK_MS, 60000L),
                preferences.getLong(KEY_REST_MS, 30000L),
                preferences.getInt(KEY_REP, 0),
                preferences.getLong(KEY_SESSION_START, 0L),
                preferences.getLong(KEY_END_ELAPSED, 0L),
                preferences.getLong(KEY_REMAINING, 0L),
                preferences.getBoolean(KEY_IS_PAUSED, false),
                preferences.getBoolean(KEY_HAS_ALERT, false),
                preferences.getInt(KEY_JOG_REPS, 0),
                preferences.getInt(KEY_WALK_REPS, 0),
                preferences.getInt(KEY_REST_REPS, 0),
                preferences.getLong(KEY_JOG_TIME, 0L),
                preferences.getLong(KEY_WALK_TIME, 0L),
                preferences.getLong(KEY_REST_TIME, 0L),
                preferences.getBoolean(KEY_SESSION_STARTED, false),
                preferences.getLong(KEY_CURRENT_MODE_DURATION, 0L),
                preferences.getLong(KEY_LAST_TICK_REMAINING, 0L),
                preferences.getLong(KEY_PAUSED_REMAINING, 0L)
        );
    }

    public static final class SessionState {
        public final String mode;
        public final long jogDurationMs;
        public final long walkDurationMs;
        public final long restDurationMs;
        public final int currentRep;
        public final long sessionStartTime;
        public final long endElapsedRealtime;
        public final long remainingMillis;
        public final boolean isPaused;
        public final boolean hasAlertFired;
        public final int jogRepsCompleted;
        public final int walkRepsCompleted;
        public final int restRepsCompleted;
        public final long jogTimeCompletedMillis;
        public final long walkTimeCompletedMillis;
        public final long restTimeCompletedMillis;
        public final boolean hasSessionStarted;
        public final long currentModeDurationMillis;
        public final long lastTickRemainingMillis;
        public final long pausedRemainingMillis;

        public SessionState(String mode,
                             long jogDurationMs,
                             long walkDurationMs,
                             long restDurationMs,
                             int currentRep,
                             long sessionStartTime,
                             long endElapsedRealtime,
                             long remainingMillis,
                             boolean isPaused,
                             boolean hasAlertFired,
                             int jogRepsCompleted,
                             int walkRepsCompleted,
                             int restRepsCompleted,
                             long jogTimeCompletedMillis,
                             long walkTimeCompletedMillis,
                             long restTimeCompletedMillis,
                             boolean hasSessionStarted,
                             long currentModeDurationMillis,
                             long lastTickRemainingMillis,
                             long pausedRemainingMillis) {
            this.mode = mode;
            this.jogDurationMs = jogDurationMs;
            this.walkDurationMs = walkDurationMs;
            this.restDurationMs = restDurationMs;
            this.currentRep = currentRep;
            this.sessionStartTime = sessionStartTime;
            this.endElapsedRealtime = endElapsedRealtime;
            this.remainingMillis = remainingMillis;
            this.isPaused = isPaused;
            this.hasAlertFired = hasAlertFired;
            this.jogRepsCompleted = jogRepsCompleted;
            this.walkRepsCompleted = walkRepsCompleted;
            this.restRepsCompleted = restRepsCompleted;
            this.jogTimeCompletedMillis = jogTimeCompletedMillis;
            this.walkTimeCompletedMillis = walkTimeCompletedMillis;
            this.restTimeCompletedMillis = restTimeCompletedMillis;
            this.hasSessionStarted = hasSessionStarted;
            this.currentModeDurationMillis = currentModeDurationMillis;
            this.lastTickRemainingMillis = lastTickRemainingMillis;
            this.pausedRemainingMillis = pausedRemainingMillis;
        }
    }
}
