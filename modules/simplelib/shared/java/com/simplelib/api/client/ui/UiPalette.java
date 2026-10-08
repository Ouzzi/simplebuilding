package com.simplelib.api.client.ui;

/**
 * Colours of one box in the N12 container style (owner images 3/4, docs/ai/PLAN-CRUCIBLE-N12B-2026-10-06.md):
 * fill, light inner line, shade, slot, slot top line, label. Plain ints (ARGB), no client classes - safe to load
 * on a dedicated server (tests, registries).
 */
public record UiPalette(int fill, int light, int shade, int slot, int slotTop, int label) {
    /** Light inventory box below every container box (image 3). */
    public static final UiPalette INVENTORY = new UiPalette(0xFFE3E6E9, 0xFFF8F9FA, 0xFFC5CACE, 0xFFB4BABF, 0xFF979DA3, 0xFF404040);
    /** Copper-brown box of a barrel attached to a crucible (N12c). */
    public static final UiPalette BARREL = new UiPalette(0xFFB9774F, 0xFFD08F68, 0xFF955839, 0xFF94573A, 0xFF74412B, 0xFF404040);
    /** Label colour on light boxes. */
    public static final int DARK_LABEL = 0xFF2E3034;
    /** Label colour on dark boxes. */
    public static final int LIGHT_LABEL = 0xFFF2EEE8;

    /**
     * A palette from one fill colour with the crucible's ratios - light inner line ~1.27 x fill (mixed towards white
     * for very dark or very light fills), shade 0.82, slot 0.78, slot top line 0.64 - and a label readable on it.
     * Same rule as {@code derive} in {@code tools/ui/simplecontainers_preview.py}; keep both in step.
     */
    public static UiPalette derived(int fill) {
        double l = luminance(fill);
        int light = l < 110 ? mix(fill, 0xFFFFFFFF, 0.26) : l < 190 ? scale(fill, 1.27) : mix(fill, 0xFFFFFFFF, 0.6);
        return new UiPalette(fill | 0xFF000000, light, scale(fill, 0.82), scale(fill, 0.78), scale(fill, 0.64),
                l < 125 ? LIGHT_LABEL : DARK_LABEL);
    }

    /** {@code color} with its RGB scaled by {@code f} (clamped), alpha opaque. */
    public static int scale(int color, double f) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * f), gr = (int) Math.min(255, ((color >> 8) & 255) * f),
                b = (int) Math.min(255, (color & 255) * f);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    /** Perceived brightness 0..255 (Rec. 601 weights). */
    public static double luminance(int color) {
        return ((color >> 16) & 255) * 0.299 + ((color >> 8) & 255) * 0.587 + (color & 255) * 0.114;
    }

    /** {@code a} mixed with {@code b}: {@code t} = 0 gives a, 1 gives b (truncated like the preview tool). Alpha opaque. */
    public static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }
}
