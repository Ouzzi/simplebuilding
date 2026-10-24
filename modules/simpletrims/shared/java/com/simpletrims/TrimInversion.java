package com.simpletrims;

/**
 * The Inverting upgrade as pure pixel maths (no client classes, so tests can run it): the trim layer of an armor piece
 * is a texture in armor UV layout whose pattern pixels are opaque and everything else transparent. Inverted, the
 * pattern is cut out and every other pixel takes the trim colour - the same pattern, its mask flipped over the armor.
 * Three stages (owner WA24-1, one smithing round each): {@link Mode#HARD} flat fill, {@link Mode#RIM} fill with a
 * one-pixel dark border around the cut-out (the pattern stays readable), {@link Mode#SOFT} a two-pixel border plus
 * a one-pixel transition (border and fill mixed) for a soft gradient into the fill.
 */
public final class TrimInversion {
    public enum Mode { HARD, RIM, SOFT }

    /** Highest stage; stage n (1..MAX_LEVEL) draws {@link Mode#values()}{@code [n - 1]}. */
    public static final int MAX_LEVEL = Mode.values().length;

    /** Border width in pixels per mode (SOFT: plus one transition pixel). */
    private static final int[] BORDER = {0, 1, 2};

    private TrimInversion() {}

    public static Mode mode(int level) {
        return Mode.values()[Math.max(1, Math.min(level, MAX_LEVEL)) - 1];
    }

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
        int border = BORDER[mode.ordinal()];
        int reach = mode == Mode.SOFT ? border + 1 : border;
        int mid = mix(fill, rim);
        int[] out = new int[pattern.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if ((pattern[i] >>> 24) != 0) continue;
                int d = distance(pattern, w, h, x, y, reach);
                out[i] = d == 0 || d > reach ? fill : d <= border ? rim : mid;
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

    private static int mix(int a, int b) {
        return 0xFF000000 | (((a >> 16 & 255) + (b >> 16 & 255)) / 2) << 16 | (((a >> 8 & 255) + (b >> 8 & 255)) / 2) << 8 | ((a & 255) + (b & 255)) / 2;
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
