package com.simpledinos;

/**
 * A progress with a pause rule, used for brushing a fossil and for brooding an egg (concept 3.2 and 3.3): every unit of work
 * adds ticks; after {@code grace} ticks without work the progress runs back by 2 per tick, so a pause of 1 s takes 2 s of
 * progress away. Pure arithmetic on game-time stamps, no ticking object is needed (the block settles it when it is looked at).
 */
public final class Progress {
    /** Rollback speed: ticks of progress lost per tick of pause. */
    public static final int REWIND = 2;

    private final int grace;
    private int value;
    private long lastWork;
    private long settledAt;

    public Progress(int grace) {
        this.grace = grace;
    }

    public int value() {
        return value;
    }

    public long lastWork() {
        return lastWork;
    }

    /** Applies the rollback up to {@code now} (idempotent for the same {@code now}). */
    public void settle(long now) {
        long from = Math.max(settledAt, lastWork + grace);
        if (value > 0 && now > from) value = (int) Math.max(0L, value - (long) REWIND * (now - from));
        if (now > settledAt) settledAt = now;
    }

    /** Adds {@code amount} ticks of work at {@code now}, capped at {@code max}; returns the new value. */
    public int add(long now, int amount, int max) {
        settle(now);
        value = Math.min(max, value + amount);
        lastWork = now;
        settledAt = now;
        return value;
    }

    public void set(int value, long lastWork, long settledAt) {
        this.value = Math.max(0, value);
        this.lastWork = lastWork;
        this.settledAt = settledAt;
    }

    public long settledAt() {
        return settledAt;
    }
}
