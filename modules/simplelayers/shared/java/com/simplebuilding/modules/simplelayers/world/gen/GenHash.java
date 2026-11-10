package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * The hash every Backrooms level is built from (BACK18, plan docs/ai/PLAN-B-BACK18-2026-11-10.md): a uniform number per
 * (seed, a, b, salt), no state, so every level is a pure function of the world seed and the position. Same formula as the
 * original {@code BackroomsGrid} (LAYERS10), so Level 0 keeps its layout. No Minecraft class here (the round images run it
 * without the game).
 */
public final class GenHash {
    private GenHash() {}

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** A uniform number in [0, 1) for (a, b, salt). */
    public static double unit(long seed, int a, int b, int salt) {
        long h = mix(seed ^ mix(a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L));
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Directions a cell's escape door can point to (see {@link #toward}). */
    public static final int NONE = 0, EAST = 1, WEST = 2, SOUTH = 3, NORTH = 4;

    /** The root cell of region (rx, rz): 2 cells away from the region border, never on it. */
    public static int rootX(long seed, int rx, int rz, int size, int salt) {
        return rx * size + 2 + (int) (unit(seed, rx, rz, salt) * (size - 4));
    }

    public static int rootZ(long seed, int rx, int rz, int size, int salt) {
        return rz * size + 2 + (int) (unit(seed, rx, rz, salt + 1) * (size - 4));
    }

    /**
     * The escape tree: every cell of a region forces one door one step closer to the region's root cell (along x or z, chosen by
     * the hash where both bring it closer), so from every cell a way of at most 2 * (size - 3) cells leads to the root - whatever
     * the random doors do. Half of all lines carry such a door, spread evenly over both directions (no straight corridors).
     */
    public static int toward(long seed, int cx, int cz, int size, int salt) {
        int rx = Math.floorDiv(cx, size), rz = Math.floorDiv(cz, size);
        int ex = rootX(seed, rx, rz, size, salt), ez = rootZ(seed, rx, rz, size, salt);
        int x = cx < ex ? EAST : cx > ex ? WEST : NONE, z = cz < ez ? SOUTH : cz > ez ? NORTH : NONE;
        if (x == NONE) return z;
        if (z == NONE) return x;
        return unit(seed, cx, cz, salt + 2) < 0.5 ? x : z;
    }

    public static boolean root(long seed, int cx, int cz, int size, int salt) {
        return toward(seed, cx, cz, size, salt) == NONE;
    }

    /** Whether the cell lies in the row or column of its region's root (the straight last leg of every escape way). */
    public static boolean onRootCross(long seed, int cx, int cz, int size, int salt) {
        int rx = Math.floorDiv(cx, size), rz = Math.floorDiv(cz, size);
        return cx == rootX(seed, rx, rz, size, salt) || cz == rootZ(seed, rx, rz, size, salt);
    }

    /** Forced door on the west line of (cx, cz): the west neighbour points east or the cell itself points west. */
    public static boolean forcedWest(long seed, int cx, int cz, int size, int salt) {
        return toward(seed, cx - 1, cz, size, salt) == EAST || toward(seed, cx, cz, size, salt) == WEST;
    }

    public static boolean forcedNorth(long seed, int cx, int cz, int size, int salt) {
        return toward(seed, cx, cz - 1, size, salt) == SOUTH || toward(seed, cx, cz, size, salt) == NORTH;
    }
}
