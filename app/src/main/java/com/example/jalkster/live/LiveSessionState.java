package com.example.jalkster.live;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class LiveSessionState {

    private final LiveMode mode;
    private final ActMode currentAct;
    private final long remainingMillis;
    private final EnumMap<ActMode, Long> durationsMillis;
    private final EnumMap<ActMode, Integer> repsDone;
    private final EnumMap<ActMode, Long> timeCompletedMillis;

    private LiveSessionState(LiveMode mode,
                             ActMode currentAct,
                             long remainingMillis,
                             EnumMap<ActMode, Long> durationsMillis,
                             EnumMap<ActMode, Integer> repsDone,
                             EnumMap<ActMode, Long> timeCompletedMillis) {
        this.mode = mode;
        this.currentAct = currentAct;
        this.remainingMillis = Math.max(0L, remainingMillis);
        this.durationsMillis = copyLongMap(durationsMillis);
        this.repsDone = copyIntMap(repsDone);
        this.timeCompletedMillis = copyLongMap(timeCompletedMillis);
    }

    public static LiveSessionState initial(long jogMillis, long walkMillis, long restMillis) {
        EnumMap<ActMode, Long> durations = new EnumMap<>(ActMode.class);
        durations.put(ActMode.JOG, Math.max(0L, jogMillis));
        durations.put(ActMode.WALK, Math.max(0L, walkMillis));
        durations.put(ActMode.REST, Math.max(0L, restMillis));
        EnumMap<ActMode, Integer> reps = new EnumMap<>(ActMode.class);
        reps.put(ActMode.JOG, 0);
        reps.put(ActMode.WALK, 0);
        reps.put(ActMode.REST, 0);
        EnumMap<ActMode, Long> times = new EnumMap<>(ActMode.class);
        times.put(ActMode.JOG, 0L);
        times.put(ActMode.WALK, 0L);
        times.put(ActMode.REST, 0L);
        return new LiveSessionState(LiveMode.INIT, null, 0L, durations, reps, times);
    }

    public LiveMode getMode() {
        return mode;
    }

    public ActMode getCurrentAct() {
        return currentAct;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public long getDurationMillis(ActMode actMode) {
        if (actMode == null) {
            return 0L;
        }
        Long value = durationsMillis.get(actMode);
        return value != null ? value : 0L;
    }

    public int getRepsDone(ActMode actMode) {
        if (actMode == null) {
            return 0;
        }
        Integer value = repsDone.get(actMode);
        return value != null ? value : 0;
    }

    public long getTimeCompletedMillis(ActMode actMode) {
        if (actMode == null) {
            return 0L;
        }
        Long value = timeCompletedMillis.get(actMode);
        return value != null ? value : 0L;
    }

    public LiveSessionState withMode(LiveMode newMode) {
        if (newMode == mode) {
            return this;
        }
        return new LiveSessionState(newMode, currentAct, remainingMillis,
                durationsMillis, repsDone, timeCompletedMillis);
    }

    public LiveSessionState withCurrentAct(ActMode newAct) {
        if (newAct == currentAct) {
            return this;
        }
        return new LiveSessionState(mode, newAct, remainingMillis,
                durationsMillis, repsDone, timeCompletedMillis);
    }

    public LiveSessionState withRemainingMillis(long remaining) {
        if (remainingMillis == remaining) {
            return this;
        }
        return new LiveSessionState(mode, currentAct, remaining,
                durationsMillis, repsDone, timeCompletedMillis);
    }

    public LiveSessionState withDurations(Long jogMillis, Long walkMillis, Long restMillis) {
        EnumMap<ActMode, Long> updated = copyLongMap(durationsMillis);
        if (jogMillis != null && jogMillis > 0L) {
            updated.put(ActMode.JOG, jogMillis);
        }
        if (walkMillis != null && walkMillis > 0L) {
            updated.put(ActMode.WALK, walkMillis);
        }
        if (restMillis != null && restMillis > 0L) {
            updated.put(ActMode.REST, restMillis);
        }
        return new LiveSessionState(mode, currentAct, remainingMillis,
                updated, repsDone, timeCompletedMillis);
    }

    public LiveSessionState withRepsAndTimes(int jogReps,
                                             int walkReps,
                                             int restReps,
                                             long jogTimeMillis,
                                             long walkTimeMillis,
                                             long restTimeMillis) {
        EnumMap<ActMode, Integer> updatedReps = new EnumMap<>(ActMode.class);
        updatedReps.put(ActMode.JOG, Math.max(0, jogReps));
        updatedReps.put(ActMode.WALK, Math.max(0, walkReps));
        updatedReps.put(ActMode.REST, Math.max(0, restReps));
        EnumMap<ActMode, Long> updatedTimes = new EnumMap<>(ActMode.class);
        updatedTimes.put(ActMode.JOG, Math.max(0L, jogTimeMillis));
        updatedTimes.put(ActMode.WALK, Math.max(0L, walkTimeMillis));
        updatedTimes.put(ActMode.REST, Math.max(0L, restTimeMillis));
        return new LiveSessionState(mode, currentAct, remainingMillis,
                durationsMillis, updatedReps, updatedTimes);
    }

    public LiveSessionState incrementReps(ActMode actMode) {
        if (actMode == null) {
            return this;
        }
        EnumMap<ActMode, Integer> updated = copyIntMap(repsDone);
        int current = getRepsDone(actMode);
        updated.put(actMode, current + 1);
        return new LiveSessionState(mode, currentAct, remainingMillis,
                durationsMillis, updated, timeCompletedMillis);
    }

    public LiveSessionState addCompletedTime(ActMode actMode, long millis) {
        if (actMode == null || millis <= 0L) {
            return this;
        }
        EnumMap<ActMode, Long> updated = copyLongMap(timeCompletedMillis);
        long current = getTimeCompletedMillis(actMode);
        updated.put(actMode, current + millis);
        return new LiveSessionState(mode, currentAct, remainingMillis,
                durationsMillis, repsDone, updated);
    }

    public boolean hasCompletedReps() {
        return getRepsDone(ActMode.JOG) > 0
                || getRepsDone(ActMode.WALK) > 0
                || getRepsDone(ActMode.REST) > 0;
    }

    private static EnumMap<ActMode, Long> copyLongMap(Map<ActMode, Long> source) {
        EnumMap<ActMode, Long> copy = new EnumMap<>(ActMode.class);
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }

    private static EnumMap<ActMode, Integer> copyIntMap(Map<ActMode, Integer> source) {
        EnumMap<ActMode, Integer> copy = new EnumMap<>(ActMode.class);
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LiveSessionState)) {
            return false;
        }
        LiveSessionState other = (LiveSessionState) obj;
        return remainingMillis == other.remainingMillis
                && mode == other.mode
                && currentAct == other.currentAct
                && Objects.equals(durationsMillis, other.durationsMillis)
                && Objects.equals(repsDone, other.repsDone)
                && Objects.equals(timeCompletedMillis, other.timeCompletedMillis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, currentAct, remainingMillis,
                durationsMillis, repsDone, timeCompletedMillis);
    }
}
