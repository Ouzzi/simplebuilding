package com.simplebuilding.client.gui;

/**
 * Where a mod HUD box goes (pure arithmetic, no client classes, so the game tests can check it on
 * the server). Used by {@link ModHud}.
 */
public final class HudLayout {
    /** Gap in GUI pixels between a HUD box and the screen edge. */
    public static final int MARGIN = 10;

    private HudLayout() {
    }

    /**
     * Top-left corner, in screen GUI pixels, of a box {@code width} x {@code height} (unscaled) that is
     * drawn at {@code scale}: the free room between the margins is split by the position percentages,
     * so 0 puts the box at the left/top margin, 100 at the right/bottom one and 50 in the middle.
     */
    public static int[] origin(int screenWidth, int screenHeight, int width, int height, float scale, int percentX, int percentY) {
        double w = width * scale;
        double h = height * scale;
        double x = MARGIN + Math.max(0.0, screenWidth - 2.0 * MARGIN - w) * clampPercent(percentX) / 100.0;
        double y = MARGIN + Math.max(0.0, screenHeight - 2.0 * MARGIN - h) * clampPercent(percentY) / 100.0;
        return new int[] {(int) Math.round(x), (int) Math.round(y)};
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }
}
