package com.simplebuilding.modules.simplelayers.backrooms.stack;

/**
 * The 112 grid of the stacked Backrooms (BACK22 core §1.1, owner fk1 a): one dimension {@code min_y −816}, height 1136 (71 sections);
 * level i (1 = top) is open in {@code y = 208 − 112(i−1) … +95}, below it one full bedrock section (the only thing that keeps the
 * renderer from meshing the neighbour levels, concept §0), a bedrock deck 304..319 above level 1, bedrock floor −816..−801.
 * Generators only see band-local {@code ly 0..95}; only this class converts.
 */
public final class BandStack {
    public static final int BANDS = 10, GRID = 112, OPEN = 96, SEPARATOR = 16, MIN_Y = -816, HEIGHT = 1136, TOP_BASE_Y = 208;
    public static final int MAX_Y = MIN_Y + HEIGHT - 1;

    private BandStack() {}

    /** World y of ly 0 of level {@code band} (1..10). */
    public static int baseY(int band) {
        return TOP_BASE_Y - GRID * (band - 1);
    }

    public static int worldY(int band, int ly) {
        return baseY(band) + ly;
    }

    /** The level whose open part holds {@code y}, or 0 inside bedrock (deck, separators, floor). */
    public static int bandAt(int y) {
        if (y > baseY(1) + OPEN - 1 || y < baseY(BANDS)) return 0;
        int band = Math.floorDiv(baseY(1) + OPEN - 1 - y, GRID) + 1;
        int ly = y - baseY(band);
        return ly >= 0 && ly < OPEN ? band : 0;
    }

    /** Band-local height of {@code y} in its level (meaningless when {@link #bandAt} is 0). */
    public static int localY(int y) {
        return y - baseY(Math.max(1, bandAt(y)));
    }

    /** The level a y belongs to for biomes and debug: inside bedrock the level just above (the deck: level 1). */
    public static int nearestBand(int y) {
        int b = bandAt(y);
        if (b != 0) return b;
        if (y > baseY(1)) return 1;
        if (y < baseY(BANDS)) return BANDS;
        return Math.min(BANDS, Math.floorDiv(baseY(1) - 1 - y, GRID) + 2);
    }
}
