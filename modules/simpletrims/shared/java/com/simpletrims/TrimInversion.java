package com.simpletrims;

/**
 * The Inverting upgrade as pure pixel maths (no client classes, so tests can run it): the trim layer of an armor piece
 * is a texture in armor UV layout whose pattern pixels are opaque and everything else transparent. Inverted, the
 * pattern is cut out and every other pixel takes the trim colour - the same pattern, its mask flipped over the armor.
 * Three stages (owner WA24-1, one smithing round each), no transition (WC24-2): the border around the cut-out is
 * 1, 2 or 3 pixels wide ({@link Mode#W1}..{@link Mode#W3}), so the higher the stage, the less trim colour is left outside.
 * The border is a subtraction (WB24-1): the fill colour minus {@link #RIM_SUBTRACT} per channel, no colour of its own.
 */
public final class TrimInversion {
    public enum Mode { W1, W2, W3 }

    /** Highest stage; stage n (1..MAX_LEVEL) draws {@link Mode#values()}{@code [n - 1]}. */
    public static final int MAX_LEVEL = Mode.values().length;

    /** Border width in pixels per mode. */
    private static final int[] BORDER = {1, 2, 3};

    /** Per colour channel (0..255) taken off the fill for the border. */
    public static final int RIM_SUBTRACT = 80;

    private TrimInversion() {}

    public static Mode mode(int level) {
        return Mode.values()[Math.max(1, Math.min(level, MAX_LEVEL)) - 1];
    }

    /** Fill and rim colour of a trim layer: the palette's third-brightest colour (the main shade of Vanilla patterns) or the layer's own average, and that fill minus {@link #RIM_SUBTRACT}. */
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
            return new int[] {fill, subtract(fill, RIM_SUBTRACT)};
        }
        Integer[] order = new Integer[source.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(i -> luma(source[i])));
        int fill = 0xFF000000 | source[order[Math.max(0, order.length - 3)]];
        return new int[] {fill, subtract(fill, RIM_SUBTRACT)};
    }

    /** The inverted layer of {@code pattern} (w*h ARGB, row-major) in the given mode. */
    public static int[] invert(int[] pattern, int w, int h, int fill, int rim, Mode mode) {
        int border = BORDER[mode.ordinal()];
        int[] out = new int[pattern.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if ((pattern[i] >>> 24) != 0) continue;
                int d = distance(pattern, w, h, x, y, border);
                out[i] = d == 0 ? fill : rim;
            }
        }
        return out;
    }

    /** Steps (up/down/left/right) from (x, y) to the nearest pattern pixel, 0 if none within {@code max}. */
    private static int distance(int[] pattern, int w, int h, int x, int y, int max) {
        for (int d = 1; d <= max; d++) {
            for (int dx = -d; dx <= d; dx++) {
                int dy = d - Math.abs(dx);
                if (opaque(pattern, w, h, x + dx, y + dy) || opaque(pattern, w, h, x + dx, y - dy)) return d;
            }
        }
        return 0;
    }

    private static boolean opaque(int[] pattern, int w, int h, int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h && (pattern[y * w + x] >>> 24) != 0;
    }

    private static double luma(int argb) {
        return 0.299 * ((argb >> 16) & 255) + 0.587 * ((argb >> 8) & 255) + 0.114 * (argb & 255);
    }

    /** {@code argb} minus {@code amount} on each colour channel, not below 0. */
    public static int subtract(int argb, int amount) {
        return 0xFF000000 | Math.max(0, (argb >> 16 & 255) - amount) << 16 | Math.max(0, (argb >> 8 & 255) - amount) << 8 | Math.max(0, (argb & 255) - amount);
    }
}
