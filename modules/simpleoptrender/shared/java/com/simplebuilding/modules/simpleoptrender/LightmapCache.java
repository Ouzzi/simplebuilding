package com.simplebuilding.modules.simpleoptrender;

/**
 * O7 light-map cache, own technique (BadOptimizations describes "recompute the lightmap only when its inputs change").
 * Vanilla 26.3 re-uploads the 16x16 light map every tick because the block-light flicker always moves a little. The
 * texture is 8 bits per channel, so an input change below half a step (0.5/255) cannot change a texel: the upload is
 * skipped while every input stays within that distance of the values last uploaded (never drifting further, since the
 * comparison is against the uploaded values, not the previous tick).
 */
public final class LightmapCache {
    /** 6 scalars + 4 colors (r, g, b) of {@code LightmapRenderState}. */
    public static final int SIZE = 18;
    public static final float EPSILON = 0.5f / 255f;

    private LightmapCache() {}

    /** True when {@code now} is within {@link #EPSILON} of {@code last} everywhere; otherwise copies now into last. */
    public static boolean unchanged(float[] last, float[] now, boolean hasLast) {
        if (hasLast) {
            boolean same = true;
            for (int i = 0; i < SIZE && same; i++) same = Math.abs(now[i] - last[i]) < EPSILON;
            if (same) return true;
        }
        System.arraycopy(now, 0, last, 0, SIZE);
        return false;
    }
}
