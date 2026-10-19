package com.simplelib.api.image;

/**
 * A fixed list of colours with nearest-colour lookup in CIE Lab (D65, ΔE 1976). Pure maths without Minecraft
 * classes, shared by the picture blueprint of SimpleBuilding and later the picture maps: Lab distances follow what
 * the eye sees far better than RGB distances (dark blues and greens do not swallow each other). Ties keep the
 * lower index, so the result is deterministic.
 */
public final class PixelPalette {
    private final int[] rgb;
    private final float[] lab;

    /** {@code rgb}: one {@code 0xRRGGBB} per entry (the alpha byte is ignored); at least one entry. */
    public PixelPalette(int... rgb) {
        if (rgb.length == 0) {
            throw new IllegalArgumentException("empty palette");
        }
        this.rgb = new int[rgb.length];
        this.lab = new float[rgb.length * 3];
        for (int i = 0; i < rgb.length; i++) {
            this.rgb[i] = rgb[i] & 0xFFFFFF;
            float[] l = lab(rgb[i] >> 16 & 255, rgb[i] >> 8 & 255, rgb[i] & 255);
            System.arraycopy(l, 0, lab, i * 3, 3);
        }
    }

    public int size() {
        return rgb.length;
    }

    /** Colour of entry {@code index} as {@code 0xRRGGBB}. */
    public int rgb(int index) {
        return rgb[index];
    }

    /** Nearest entry to {@code 0xRRGGBB}. */
    public int nearest(int rgb) {
        return nearest(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255);
    }

    /** Nearest entry to an sRGB colour with channels 0..255 (values outside are clamped; dithering adds errors). */
    public int nearest(float r, float g, float b) {
        float[] l = lab(r, g, b);
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < rgb.length; i++) {
            float dl = l[0] - lab[i * 3], da = l[1] - lab[i * 3 + 1], db = l[2] - lab[i * 3 + 2];
            float d = dl * dl + da * da + db * db;
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    /** Squared Lab distance (ΔE76²) between two {@code 0xRRGGBB} colours. */
    public static float distance2(int a, int b) {
        float[] x = lab(a >> 16 & 255, a >> 8 & 255, a & 255);
        float[] y = lab(b >> 16 & 255, b >> 8 & 255, b & 255);
        float dl = x[0] - y[0], da = x[1] - y[1], db = x[2] - y[2];
        return dl * dl + da * da + db * db;
    }

    /** sRGB (0..255 per channel, clamped) to CIE Lab with the D65 white point: {L, a, b}. */
    public static float[] lab(float r, float g, float b) {
        double lr = linear(r), lg = linear(g), lb = linear(b);
        double x = (0.4124 * lr + 0.3576 * lg + 0.1805 * lb) / 0.95047;
        double y = 0.2126 * lr + 0.7152 * lg + 0.0722 * lb;
        double z = (0.0193 * lr + 0.1192 * lg + 0.9505 * lb) / 1.08883;
        double fx = f(x), fy = f(y), fz = f(z);
        return new float[]{(float) (116 * fy - 16), (float) (500 * (fx - fy)), (float) (200 * (fy - fz))};
    }

    private static double linear(float channel) {
        double c = Math.max(0, Math.min(255, channel)) / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double f(double t) {
        return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116;
    }
}
