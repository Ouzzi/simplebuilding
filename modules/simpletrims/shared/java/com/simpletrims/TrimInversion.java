package com.simpletrims;

/**
 * The Inverting upgrade as pure pixel maths (no client classes, so tests can run it): the trim layer of an armor piece
 * is a texture in armor UV layout whose pattern pixels are opaque and everything else transparent. Inverted, the
 * pattern is cut out and every other pixel takes the trim colour - the same pattern, its mask flipped over the armor.
 * Variants (owner picks one from the preview, {@link #DEFAULT}): {@link Mode#HARD} flat fill, {@link Mode#RIM} fill
 * with a one-pixel dark border around the cut-out (the pattern stays readable), {@link Mode#GHOST} fill with the
 * pattern left in as a faint veil.
 */
public final class TrimInversion {
    public enum Mode { HARD, RIM, GHOST }

    /** Recommended: RIM, a flat fill alone hides the pattern's edge on bright armor (see docs/ai/PLAN-B-TRIMS24-2026-10-24.md). */
    public static final Mode DEFAULT = Mode.RIM;
    /** Alpha of the pattern pixels that stay visible in {@link Mode#GHOST}. */
    public static final int GHOST_ALPHA = 70;

    private TrimInversion() {}

    /** Fill and rim colour of a trim layer: the palette's third-brightest colour (the main shade of Vanilla patterns) and its darkest colour (or the layer's own average). */
    public static int[] colours(int[] palette, int[] pattern) {
        int[] source = palette;
        if (source == null || source.length == 0) {
            long r = 0, g = 0, b = 0, n = 0;
            for (int argb : pattern) {
                if ((argb >>> 24) == 0) continue;
                r += (argb >> 16) & 255;
                g += (argb >> 8) & 255;
                b += argb & 255;
                n++;
            }
            if (n == 0) return new int[] {0xFF808080, 0xFF404040};
            int fill = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            return new int[] {fill, darker(fill)};
        }
        Integer[] order = new Integer[source.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> luma(source[i])));
        int fill = 0xFF000000 | source[order[Math.max(0, order.length - 3)]];
        int rim = 0xFF000000 | source[order[0]];
        return new int[] {fill, rim};
    }

    /** The inverted layer of {@code pattern} (w*h ARGB, row-major) in the given mode. */
    public static int[] invert(int[] pattern, int w, int h, int fill, int rim, Mode mode) {
        int[] out = new int[pattern.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if ((pattern[i] >>> 24) != 0) {
                    out[i] = mode == Mode.GHOST ? (pattern[i] & 0xFFFFFF) | GHOST_ALPHA << 24 : 0;
                } else {
                    out[i] = mode == Mode.RIM && touchesPattern(pattern, w, h, x, y) ? rim : fill;
                }
            }
        }
        return out;
    }

    private static boolean touchesPattern(int[] pattern, int w, int h, int x, int y) {
        return opaque(pattern, w, h, x - 1, y) || opaque(pattern, w, h, x + 1, y) || opaque(pattern, w, h, x, y - 1) || opaque(pattern, w, h, x, y + 1);
    }

    private static boolean opaque(int[] pattern, int w, int h, int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h && (pattern[y * w + x] >>> 24) != 0;
    }

    private static double luma(int argb) {
        return 0.299 * ((argb >> 16) & 255) + 0.587 * ((argb >> 8) & 255) + 0.114 * (argb & 255);
    }

    private static int darker(int argb) {
        return 0xFF000000 | (((argb >> 16) & 255) * 5 / 8) << 16 | (((argb >> 8) & 255) * 5 / 8) << 8 | ((argb & 255) * 5 / 8);
    }
}
