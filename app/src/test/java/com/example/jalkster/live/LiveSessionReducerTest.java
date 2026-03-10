package com.example.jalkster.live;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LiveSessionReducerTest {

    @Test
    public void activeTransitionsToTimerEndOnTick() {
        LiveSessionState state = LiveSessionState.initial(1000L, 2000L, 3000L);
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapJog(1000L, 2000L, 3000L));

        assertEquals(LiveMode.ACTIVE, state.getMode());
        assertEquals(ActMode.JOG, state.getCurrentAct());
        assertEquals(1000L, state.getRemainingMillis());

        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.Tick(1000L));

        assertEquals(LiveMode.TIMEREND, state.getMode());
        assertEquals(0L, state.getRemainingMillis());
        assertEquals(1, state.getRepsDone(ActMode.JOG));
        assertEquals(1000L, state.getTimeCompletedMillis(ActMode.JOG));
    }

    @Test
    public void timerEndCanStartNewAct() {
        LiveSessionState state = LiveSessionState.initial(1000L, 2000L, 3000L);
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapJog(1000L, 2000L, 3000L));
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.Tick(1000L));

        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapWalk());

        assertEquals(LiveMode.ACTIVE, state.getMode());
        assertEquals(ActMode.WALK, state.getCurrentAct());
        assertEquals(2000L, state.getRemainingMillis());
    }

    @Test
    public void pauseResumeKeepsRemaining() {
        LiveSessionState state = LiveSessionState.initial(1000L, 2000L, 3000L);
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapWalk(1000L, 2000L, 3000L));
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.Tick(500L));

        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapPauseResume());
        assertEquals(LiveMode.PAUSED, state.getMode());
        assertEquals(1500L, state.getRemainingMillis());

        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapPauseResume());
        assertEquals(LiveMode.ACTIVE, state.getMode());
        assertEquals(1500L, state.getRemainingMillis());
    }

    @Test
    public void stopKeepsRepTotalsForLogging() {
        LiveSessionState state = LiveSessionState.initial(1000L, 2000L, 3000L);
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapRest(1000L, 2000L, 3000L));
        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.Tick(3000L));

        state = LiveSessionReducer.reduce(state, new LiveSessionEvent.TapStop());

        assertEquals(LiveMode.STOPPED, state.getMode());
        assertTrue(state.hasCompletedReps());
        assertEquals(1, state.getRepsDone(ActMode.REST));
    }
}
