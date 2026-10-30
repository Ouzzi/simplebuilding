package com.simplestorage.pipe;

/**
 * The four steps of the pipe (ST27-1): the pipe, then Reinforced, Netherite and Enderite like the chests. The pipe moves
 * {@link #amount} items every {@link #cooldown} ticks (a Vanilla hopper: 1 every 8). The first step may only point down or
 * sideways; from Reinforced on the output may also point up ({@link #canPointUp}).
 */
public enum PipeTier {
    BASIC("pipe", 8, 1, false),
    REINFORCED("reinforced_pipe", 6, 1, true),
    NETHERITE("netherite_pipe", 4, 2, true),
    ENDERITE("enderite_pipe", 2, 4, true);

    /** Ids of the four pipes (block and item), in tier order. */
    public static final java.util.List<String> IDS = java.util.List.of(BASIC.id, REINFORCED.id, NETHERITE.id, ENDERITE.id);

    private final String id;
    private final int cooldown;
    private final int amount;
    private final boolean canPointUp;

    PipeTier(String id, int cooldown, int amount, boolean canPointUp) {
        this.id = id;
        this.cooldown = cooldown;
        this.amount = amount;
        this.canPointUp = canPointUp;
    }

    public String id() {
        return this.id;
    }

    /** Ticks between two transfers. */
    public int cooldown() {
        return this.cooldown;
    }

    /** Items per transfer. */
    public int amount() {
        return this.amount;
    }

    /** The filter key (ST27-1, ABN30-3): from Reinforced on; the first step moves everything. */
    public boolean canFilter() {
        return this != BASIC;
    }

    public boolean canPointUp() {
        return this.canPointUp;
    }
}
