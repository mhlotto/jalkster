package com.example.jalkster.live;

public abstract class LiveSessionEvent {

    private LiveSessionEvent() {
    }

    public static abstract class TapAct extends LiveSessionEvent {
        private final Long jogMs;
        private final Long walkMs;
        private final Long restMs;

        TapAct(Long jogMs, Long walkMs, Long restMs) {
            this.jogMs = jogMs;
            this.walkMs = walkMs;
            this.restMs = restMs;
        }

        public Long getJogMs() {
            return jogMs;
        }

        public Long getWalkMs() {
            return walkMs;
        }

        public Long getRestMs() {
            return restMs;
        }

        public abstract ActMode getActMode();
    }

    public static final class TapJog extends TapAct {
        public TapJog() {
            this(null, null, null);
        }

        public TapJog(Long jogMs, Long walkMs, Long restMs) {
            super(jogMs, walkMs, restMs);
        }

        @Override
        public ActMode getActMode() {
            return ActMode.JOG;
        }
    }

    public static final class TapWalk extends TapAct {
        public TapWalk() {
            this(null, null, null);
        }

        public TapWalk(Long jogMs, Long walkMs, Long restMs) {
            super(jogMs, walkMs, restMs);
        }

        @Override
        public ActMode getActMode() {
            return ActMode.WALK;
        }
    }

    public static final class TapRest extends TapAct {
        public TapRest() {
            this(null, null, null);
        }

        public TapRest(Long jogMs, Long walkMs, Long restMs) {
            super(jogMs, walkMs, restMs);
        }

        @Override
        public ActMode getActMode() {
            return ActMode.REST;
        }
    }

    public static final class TapPauseResume extends LiveSessionEvent {
        public TapPauseResume() {
        }
    }

    public static final class TapStop extends LiveSessionEvent {
        public TapStop() {
        }
    }

    public static final class Tick extends LiveSessionEvent {
        private final long deltaMillis;

        public Tick(long deltaMillis) {
            this.deltaMillis = deltaMillis;
        }

        public long getDeltaMillis() {
            return deltaMillis;
        }
    }

    public static final class TimerFinished extends LiveSessionEvent {
        public TimerFinished() {
        }
    }
}
