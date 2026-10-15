package com.simplebuilding.modules.simplestats;

/** Duck interface mixed into {@code ServerPlayer}: the small amount of transient counting state of one player. */
public interface StatsPlayer {
    State simplestats$state();

    /** Transient (never saved): distances collected since the last hand-over to the items, and the running flight. */
    final class State {
        public long lastAccrue = -1L;
        public long walked, sprinted, sneaked, swum, flown;
        public long flightCm;
        public boolean flying;
    }
}
