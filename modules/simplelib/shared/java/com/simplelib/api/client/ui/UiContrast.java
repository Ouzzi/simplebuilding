package com.simplelib.api.client.ui;

/**
 * Readability helper for every drawn text (owner rule N67, 2026-10-15: text needs a contrast of at least 4.5:1 to its
 * background, never dark text with a dark shadow on grey): WCAG 2.x contrast ratio, a fixer that nudges a text colour
 * just far enough, and three ready-made "ink" colours that read on every light box of the style (inventory grey,
 * paper, the tier palettes). Plain ints (ARGB), no client classes - safe on a dedicated server and in tests.
 */
public final class UiContrast {
    /** Smallest accepted contrast ratio of text to its background (WCAG AA for normal text). */
    public static final double MIN = 4.5;
    /** Green "ok" ink: at least {@link #MIN} on {@link UiPalette#INVENTORY} grey and on paper (#E2D3AA). */
    public static final int INK_OK = 0xFF205723;
    /** Red "error" ink with the same guarantee as {@link #INK_OK}. */
    public static final int INK_ERROR = 0xFF8F1E18;
    /** Brown "soft / secondary" ink with the same guarantee as {@link #INK_OK}. */
    public static final int INK_SOFT = 0xFF554C37;

    private UiContrast() {}

    /** WCAG relative luminance (0 black .. 1 white) of an RGB colour (alpha ignored). */
    public static double luminance(int color) {
        return 0.2126 * channel(color >> 16) + 0.7152 * channel(color >> 8) + 0.0722 * channel(color);
    }

    private static double channel(int c) {
        double v = (c & 255) / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /** Contrast ratio 1..21 of two opaque colours (order does not matter). */
    public static double ratio(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** {@code fg} (RGB, alpha ignored) if it reads on {@code bg} with at least {@link #MIN}. */
    public static boolean readable(int fg, int bg) {
        return ratio(fg, bg) >= MIN;
    }

    /**
     * {@code fg} itself when it reads on {@code bg}; otherwise {@code fg} mixed towards black or white in steps of 5 %
     * (the direction that is reached first, black on a tie, so the hue survives), at worst pure black or white.
     */
    public static int ensure(int fg, int bg) {
        if (readable(fg, bg)) {
            return fg;
        }
        for (int step = 1; step <= 20; step++) {
            double t = step * 0.05;
            int dark = UiPalette.mix(fg, 0xFF000000, t);
            if (readable(dark, bg)) {
                return dark;
            }
            int light = UiPalette.mix(fg, 0xFFFFFFFF, t);
            if (readable(light, bg)) {
                return light;
            }
        }
        return ratio(0xFF000000, bg) >= ratio(0xFFFFFFFF, bg) ? 0xFF000000 : 0xFFFFFFFF;
    }
}
