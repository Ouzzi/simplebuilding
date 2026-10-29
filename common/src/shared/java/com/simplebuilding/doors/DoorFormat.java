package com.simplebuilding.doors;

/**
 * The four formats of Simple Doors (WP-17 / BS28-2, docs/ai/KONZEPT-A-WP17-2026-10-29.md). {@code cols} is the number of blocks
 * across, {@code rows} the number up; a half door is one block wide but its leaf only 8 px, and it swings inside its own block.
 */
public enum DoorFormat {
    FULL_2X2("2x2", 2, 2, false),
    FULL_2X3("2x3", 2, 3, false),
    HALF_2("half_2", 1, 2, true),
    HALF_3("half_3", 1, 3, true);

    public final String suffix;
    public final int cols;
    public final int rows;
    public final boolean half;

    DoorFormat(String suffix, int cols, int rows, boolean half) {
        this.suffix = suffix;
        this.cols = cols;
        this.rows = rows;
        this.half = half;
    }

    /** Block and item id of this format for a wood: {@code oak_door_2x2}, {@code oak_door_half_3}. */
    public String id(String wood) {
        return wood + "_door_" + suffix;
    }
}
