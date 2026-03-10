package com.example.jalkster.live;

public final class LiveSessionReducer {

    private LiveSessionReducer() {
    }

    public static LiveSessionState reduce(LiveSessionState state, LiveSessionEvent event) {
        if (state == null || event == null) {
            return state;
        }
        if (event instanceof LiveSessionEvent.TapAct) {
            LiveSessionEvent.TapAct tapAct = (LiveSessionEvent.TapAct) event;
            LiveSessionState updated = applyDurations(state, tapAct);
            return handleTapAct(updated, tapAct.getActMode());
        }
        if (event instanceof LiveSessionEvent.TapPauseResume) {
            return handlePauseResume(state);
        }
        if (event instanceof LiveSessionEvent.TapStop) {
            return state.withMode(LiveMode.STOPPED)
                    .withCurrentAct(null)
                    .withRemainingMillis(0L);
        }
        if (event instanceof LiveSessionEvent.Tick) {
            LiveSessionEvent.Tick tick = (LiveSessionEvent.Tick) event;
            return handleTick(state, tick.getDeltaMillis());
        }
        if (event instanceof LiveSessionEvent.TimerFinished) {
            return handleTimerFinished(state);
        }
        return state;
    }

    private static LiveSessionState applyDurations(LiveSessionState state, LiveSessionEvent.TapAct tapAct) {
        return state.withDurations(tapAct.getJogMs(), tapAct.getWalkMs(), tapAct.getRestMs());
    }

    private static LiveSessionState handleTapAct(LiveSessionState state, ActMode actMode) {
        if (actMode == null) {
            return state;
        }
        LiveMode mode = state.getMode();
        if (mode != LiveMode.INIT && mode != LiveMode.TIMEREND) {
            return state;
        }
        long duration = state.getDurationMillis(actMode);
        return state.withMode(LiveMode.ACTIVE)
                .withCurrentAct(actMode)
                .withRemainingMillis(duration);
    }

    private static LiveSessionState handlePauseResume(LiveSessionState state) {
        if (state.getMode() == LiveMode.ACTIVE) {
            return state.withMode(LiveMode.PAUSED);
        }
        if (state.getMode() == LiveMode.PAUSED) {
            return state.withMode(LiveMode.ACTIVE);
        }
        return state;
    }

    private static LiveSessionState handleTick(LiveSessionState state, long deltaMillis) {
        if (state.getMode() != LiveMode.ACTIVE || state.getCurrentAct() == null) {
            return state;
        }
        if (deltaMillis <= 0L) {
            return state;
        }
        long remaining = state.getRemainingMillis();
        long elapsed = Math.min(remaining, deltaMillis);
        long nextRemaining = Math.max(0L, remaining - elapsed);
        LiveSessionState updated = state
                .withRemainingMillis(nextRemaining)
                .addCompletedTime(state.getCurrentAct(), elapsed);
        if (nextRemaining <= 0L) {
            updated = updated.incrementReps(state.getCurrentAct())
                    .withMode(LiveMode.TIMEREND);
        }
        return updated;
    }

    private static LiveSessionState handleTimerFinished(LiveSessionState state) {
        if (state.getMode() != LiveMode.ACTIVE || state.getCurrentAct() == null) {
            return state;
        }
        long remaining = state.getRemainingMillis();
        if (remaining <= 0L) {
            return state.withMode(LiveMode.TIMEREND);
        }
        return state.withRemainingMillis(0L)
                .addCompletedTime(state.getCurrentAct(), remaining)
                .incrementReps(state.getCurrentAct())
                .withMode(LiveMode.TIMEREND);
    }
}
