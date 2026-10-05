package com.simplelib.crucible;

import com.simplelib.config.LibConfig;

/**
 * The four crucible tiers (owner F4: 6/9/18/27 slots). Slots are laid out as 3-column grids,
 * grid-major: index = grid * (rows * 3) + row * 3 + column. Enderite lives in SimpleBuilding (it
 * registers its block through {@code com.simplelib.api}); the tier data is here so the menu, screen
 * and logic stay in one place.
 */
public enum CrucibleTier {
    IRON("iron", 1, 2, 1),
    REINFORCED("reinforced", 1, 3, 1),
    NETHERITE("netherite", 2, 3, 1),
    ENDERITE("enderite", 3, 3, 2);

    public static final int COLUMNS = 3;

    private final String id;
    private final int grids;
    private final int rows;
    private final int stackMultiplier;

    CrucibleTier(String id, int grids, int rows, int stackMultiplier) {
        this.id = id;
        this.grids = grids;
        this.rows = rows;
        this.stackMultiplier = stackMultiplier;
    }

    public String id() { return id; }
    public int grids() { return grids; }
    public int rows() { return rows; }
    public int slots() { return grids * rows * COLUMNS; }
    public int stackMultiplier() { return stackMultiplier; }

    /** Cooking speed of the tier (owner F5: like the SimpleBuilding furnaces, 1/2/4/8x). */
    public int speed() {
        return switch (this) {
            case IRON -> 1;
            case REINFORCED -> LibConfig.reinforcedSpeed;
            case NETHERITE -> LibConfig.netheriteSpeed;
            case ENDERITE -> LibConfig.enderiteSpeed;
        };
    }

    /** Afterglow in ticks after the heat source is removed (owner F29: 2/4/8/16 s). */
    public int afterglowTicks() {
        return LibConfig.afterglowSeconds[ordinal()] * 20;
    }

    /** Netherite and Enderite count every finished item twice for experience (owner F6). */
    public boolean doubleExperience() {
        return this == NETHERITE || this == ENDERITE;
    }

    public int grid(int slot) { return slot / (rows * COLUMNS); }
    public int row(int slot) { return slot % (rows * COLUMNS) / COLUMNS; }
    public int column(int slot) { return slot % COLUMNS; }

    /** The slot directly below in the same grid, or -1 in the bottom row (owner F13). */
    public int below(int slot) {
        return row(slot) < rows - 1 ? slot + COLUMNS : -1;
    }

    public static CrucibleTier byId(int ordinal) {
        CrucibleTier[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
