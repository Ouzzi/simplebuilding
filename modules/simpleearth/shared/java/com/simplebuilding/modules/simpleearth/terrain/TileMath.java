package com.simplebuilding.modules.simpleearth.terrain;

/**
 * The global sampling grid (concept §4.3): sample spacing 2^e blocks, tiles of {@link #TILE} x {@link #TILE} samples,
 * independent of any player's activation point so all players share the cache. Pure maths for JUnit.
 */
public final class TileMath {
    public static final int TILE = 64, AREA = TILE * TILE;
    /** Grid exponents: 2^0 = 1 block (tiny developer earths, E29-7) to 2^14 = 16384 blocks per sample. */
    public static final int MIN_E = 0, MAX_E = 14;
    /** Largest face image (pixels per side). */
    public static final int MAX_RES = 1024;
    /** Vanilla world border (blocks); samples beyond it stay unknown. */
    public static final int WORLD_LIMIT = 29_999_984;

    private TileMath() {}

    /** Block coordinate of sample index {@code s} (centre of its cell). */
    public static long sampleBlock(long s, int e) {
        return (s << e) + (1L << e) / 2;
    }

    /** Sample index covering block coordinate {@code c}. */
    public static long sampleOf(double c, int e) {
        return (long) Math.floor(c / (1L << e));
    }

    public static int tileOf(long sample) {
        return (int) Math.floorDiv(sample, TILE);
    }

    public static int inTile(long sample) {
        return (int) Math.floorMod(sample, TILE);
    }

    /** Grid exponent so a face of {@code edge} blocks gets about {@code resolution} pixels (never finer than 2^MIN_E). */
    public static int exponentFor(double edge, int resolution) {
        double blocksPerPixel = edge / Math.max(1, resolution);
        int e = (int) Math.round(Math.log(Math.max(1, blocksPerPixel)) / Math.log(2));
        return Math.max(MIN_E, Math.min(MAX_E, e));
    }

    /**
     * Face resolution per ring (WD25-16 detail level): the version's {@code faceResolution} times 2^(detail - 2) near the
     * player (detail 2 = before 25.10., standard 3 = twice as fine), halved per ring beyond {@code detailRange} and with the
     * eye height as before; never finer than the server's grid, at most {@link #MAX_RES} pixels.
     */
    public static int[] ringResolutions(double edge, double h, int faceResolution, int detail, int detailRange, int minGrid) {
        int[] out = new int[3];
        double near = faceResolution * Math.pow(2, detail - 2);
        int heightDiv = h < 0.6 ? 1 : h < 1.6 ? 2 : 4;
        for (int r = 0; r < 3; r++) {
            int res = (int) Math.max(64, near / heightDiv / (1 << Math.max(0, r - detailRange)));
            int e = Math.max(minGrid, exponentFor(edge, Math.min(MAX_RES, res)));
            out[r] = Math.max(16, (int) Math.min(MAX_RES, Math.round(edge / (1L << Math.min(MAX_E, e)))));
        }
        return out;
    }

    public static long key(int e, int tx, int tz) {
        return ((long) e << 58) ^ (((long) tx & 0x1FFFFFFFL) << 29) ^ ((long) tz & 0x1FFFFFFFL);
    }

    /**
     * Vanilla map brightness (MapItem): 0 = low, 1 = normal, 2 = high. Land compares the height with the sample to the north
     * ({@code relief} 1 = Vanilla's slope at this scale); water uses the depth with Vanilla's checker dither.
     */
    public static int brightness(boolean water, int height, int northHeight, int seaLevel, int e, double relief, int i, int j) {
        int dither = (i + j) & 1;
        if (water) {
            double depth = Math.max(0, seaLevel - height) * 0.1 + dither * 0.2;
            return depth < 0.5 ? 2 : depth > 0.9 ? 0 : 1;
        }
        double scale = 1 << e;
        double d = (height - northHeight) * 4.0 / (Math.sqrt(scale) + 4) * relief + (dither - 0.5) * 0.4;
        return d > 0.6 ? 2 : d < -0.6 ? 0 : 1;
    }
}
