package com.simplebuilding.modules.simpleinterfaces;

/** Pure parts of the zoom and leggings features (no client classes, so GameTests can run them). */
public final class ZoomMath {
    public static final double WIDEST = 0.8, WHEEL_STEP = 0.88;

    private ZoomMath() {}

    /** One wheel step: scrolling up (positive) zooms in, bounded between 1/maxMagnification and {@link #WIDEST}. */
    public static double factorAfter(double current, double wheel, int maxMagnification) {
        return Math.clamp(current * Math.pow(WHEEL_STEP, wheel), 1.0 / Math.max(2, maxMagnification), WIDEST);
    }

    /** Leggings underside (BS29-4) on ARGB pixels of a 64*s wide layer: fills transparent underside pixels from the front hem row. Returns the count. */
    public static int fillLeggingsUnderside(int[] argb, int width, int height) {
        int s = width / 64;
        if (s < 1 || width % 64 != 0 || height < 32 * s || argb.length < width * height) return 0;
        int filled = 0;
        for (int i = 0; i < 4 * s; i++) {
            int hem = argb[(32 * s - 1) * width + 4 * s + i];
            if ((hem >>> 24) == 0) continue;
            for (int j = 0; j < 4 * s; j++) {
                int at = (16 * s + j) * width + 8 * s + i;
                if ((argb[at] >>> 24) != 0) continue;
                argb[at] = hem;
                filled++;
            }
        }
        return filled;
    }
}
