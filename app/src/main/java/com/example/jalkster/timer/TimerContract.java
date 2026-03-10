package com.example.jalkster.timer;

public final class TimerContract {
    private TimerContract() {}

    // Actions
    public static final String ACTION_TIMER_UPDATE = "com.example.jalkster.ACTION_TIMER_UPDATE";
    public static final String ACTION_START_MODE = "com.example.jalkster.ACTION_START_MODE";
    public static final String ACTION_STOP_SESSION = "com.example.jalkster.ACTION_STOP_SESSION";
    public static final String ACTION_REQUEST_STATE = "com.example.jalkster.ACTION_REQUEST_STATE";

    // Extras
    public static final String EXTRA_MODE = "extra_mode"; // String: "walk"|"jog"|"rest"
    public static final String EXTRA_REMAINING_MS = "extra_remaining_ms"; // long
    public static final String EXTRA_IS_ALERTING = "extra_is_alerting"; // boolean (true once at/after zero)
    public static final String EXTRA_IS_PAUSED = "extra_is_paused"; // boolean (true when session paused)
    public static final String EXTRA_JOG_TIME_COMPLETED = "extra_jog_time_completed"; // long millis
    public static final String EXTRA_JOG_REPS_COMPLETED = "extra_jog_reps_completed"; // int
    public static final String EXTRA_WALK_TIME_COMPLETED = "extra_walk_time_completed"; // long millis
    public static final String EXTRA_WALK_REPS_COMPLETED = "extra_walk_reps_completed"; // int
    public static final String EXTRA_REST_TIME_COMPLETED = "extra_rest_time_completed"; // long millis
    public static final String EXTRA_REST_REPS_COMPLETED = "extra_rest_reps_completed"; // int
    public static final String EXTRA_LIVE_MODE = "extra_live_mode"; // String: "INIT"|"ACTIVE"|"TIMEREND"|"PAUSED"|"STOPPED"
}
