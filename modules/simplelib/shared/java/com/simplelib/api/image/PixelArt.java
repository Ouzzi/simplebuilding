package com.simplelib.api.image;

/**
 * Turns a picture (ARGB pixels, row by row from the top left) into palette indices: fit to a size limit keeping the
 * aspect ratio, scale by area average, then quantize to a {@link PixelPalette} with or without dithering. Pixels with
 * alpha below 128 stay free ({@link PixelRle#NONE}). Pure maths, the same input always gives the same output.
 */
public final class PixelArt {
    /** How colours between palette entries are shown. */
    public enum Dither {
        /** Nearest colour only: clean areas, good for logos and text. */
        NONE,
        /** Floyd-Steinberg error diffusion (left to right, row by row): smooth gradients, good for photos. */
        FLOYD_STEINBERG,
        /** Ordered 4x4 Bayer pattern: regular texture, no error trails. */
        BAYER
    }

    /** Opaque from this alpha on. */
    public static final int ALPHA_CUTOFF = 128;
    /** Strength of the Bayer pattern in sRGB steps (0..255 scale). */
    public static final float BAYER_SPREAD = 48f;
    private static final int[] BAYER4 = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private PixelArt() {}

    /**
     * Size {@code {w, h}} for a {@code width x height} picture whose longer side is at most {@code maxSide}: unchanged
     * when it fits, else scaled down keeping the aspect ratio (each side at least 1).
     */
    public static int[] fit(int width, int height, int maxSide) {
        if (width <= 0 || height <= 0 || maxSide <= 0) {
            throw new IllegalArgumentException("size " + width + "x" + height + " max " + maxSide);
        }
        int longer = Math.max(width, height);
        if (longer <= maxSide) {
            return new int[]{width, height};
        }
        double scale = (double) maxSide / longer;
        return new int[]{Math.max(1, Math.min(maxSide, (int) Math.round(width * scale))),
                Math.max(1, Math.min(maxSide, (int) Math.round(height * scale)))};
    }

    /**
     * {@code argb} ({@code width x height}) scaled to {@code toW x toH}: each target pixel is the alpha-weighted
     * average of the source pixels it covers (at least one), so scaling up repeats pixels and scaling down averages.
     */
    public static int[] scale(int[] argb, int width, int height, int toW, int toH) {
        if (argb.length < width * height) {
            throw new IllegalArgumentException("pixels " + argb.length + " < " + width + "x" + height);
        }
        int[] out = new int[toW * toH];
        for (int y = 0; y < toH; y++) {
            int y0 = (int) ((long) y * height / toH);
            int y1 = Math.max(y0 + 1, (int) ((long) (y + 1) * height / toH));
            for (int x = 0; x < toW; x++) {
                int x0 = (int) ((long) x * width / toW);
                int x1 = Math.max(x0 + 1, (int) ((long) (x + 1) * width / toW));
                long a = 0, r = 0, g = 0, b = 0;
                int n = 0;
                for (int sy = y0; sy < y1; sy++) {
                    for (int sx = x0; sx < x1; sx++) {
                        int p = argb[sy * width + sx];
                        int pa = p >>> 24;
                        a += pa;
                        r += (long) (p >> 16 & 255) * pa;
                        g += (long) (p >> 8 & 255) * pa;
                        b += (long) (p & 255) * pa;
                        n++;
                    }
                }
                int alpha = (int) (a / n);
                out[y * toW + x] = a == 0 ? 0 : alpha << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
            }
        }
        return out;
    }

    /** Palette index per pixel ({@link PixelRle#NONE} where alpha is below {@link #ALPHA_CUTOFF}). */
    public static int[] quantize(int[] argb, int width, int height, PixelPalette palette, Dither dither) {
        int[] out = new int[width * height];
        float[] error = dither == Dither.FLOYD_STEINBERG ? new float[(width + 2) * 2 * 3] : null;
        for (int y = 0; y < height; y++) {
            if (error != null) {
                // two error rows, swapped: this one (y) and the next (y + 1)
                int cur = (y & 1) * (width + 2) * 3, next = ((y + 1) & 1) * (width + 2) * 3;
                java.util.Arrays.fill(error, next, next + (width + 2) * 3, 0f);
                for (int x = 0; x < width; x++) {
                    int p = argb[y * width + x];
                    if ((p >>> 24) < ALPHA_CUTOFF) {
                        out[y * width + x] = PixelRle.NONE;
                        continue;
                    }
                    int e = cur + (x + 1) * 3;
                    float r = (p >> 16 & 255) + error[e], g = (p >> 8 & 255) + error[e + 1], b = (p & 255) + error[e + 2];
                    int index = palette.nearest(r, g, b);
                    out[y * width + x] = index;
                    int c = palette.rgb(index);
                    float[] diff = {clamp(r) - (c >> 16 & 255), clamp(g) - (c >> 8 & 255), clamp(b) - (c & 255)};
                    for (int k = 0; k < 3; k++) {
                        error[e + 3 + k] += diff[k] * 7 / 16;
                        error[next + x * 3 + k] += diff[k] * 3 / 16;
                        error[next + (x + 1) * 3 + k] += diff[k] * 5 / 16;
                        error[next + (x + 2) * 3 + k] += diff[k] / 16;
                    }
                }
                continue;
            }
            for (int x = 0; x < width; x++) {
                int p = argb[y * width + x];
                if ((p >>> 24) < ALPHA_CUTOFF) {
                    out[y * width + x] = PixelRle.NONE;
                    continue;
                }
                float offset = dither == Dither.BAYER ? ((BAYER4[(y & 3) * 4 + (x & 3)] + 0.5f) / 16f - 0.5f) * BAYER_SPREAD : 0f;
                out[y * width + x] = palette.nearest((p >> 16 & 255) + offset, (p >> 8 & 255) + offset, (p & 255) + offset);
            }
        }
        return out;
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(255, v));
    }
}
