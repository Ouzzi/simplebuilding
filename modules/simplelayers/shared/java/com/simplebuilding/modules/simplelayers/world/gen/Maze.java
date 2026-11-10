package com.simplebuilding.modules.simplelayers.world.gen;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Region labyrinths of the deeper Backrooms levels (BACK20, plan docs/ai/PLAN-B-BACK20-2026-11-10.md): every region of
 * {@code size} x {@code size} cells holds a perfect maze - a randomised depth-first spanning tree grown from the region's root
 * cell ({@link GenHash#rootX}), so the root (the level's goal: lift, hatch, exit room) is reachable from every cell of the region,
 * by long winding ways. {@code loops} opens that share of the remaining lines too (fewer dead ends, easier); each border between
 * two regions has one gate. Pure function of seed and position like every generator here; the trees are cached per region
 * (bounded, cleared when full). No Minecraft class.
 */
public final class Maze {
    private record Key(long seed, int salt, int size, int rx, int rz) {}

    private static final int CACHE_LIMIT = 4096;
    private static final ConcurrentHashMap<Key, byte[]> TREES = new ConcurrentHashMap<>();
    private static final int EAST = 1, SOUTH = 2;

    private Maze() {}

    /** Whether the west line of cell (cx, cz) can be passed (= the east line of (cx - 1, cz)). */
    public static boolean west(long seed, int cx, int cz, int size, int salt, double loops) {
        int rx = Math.floorDiv(cx, size), rz = Math.floorDiv(cz, size), lx = Math.floorMod(cx, size), lz = Math.floorMod(cz, size);
        if (lx == 0) {
            // region border: one gate per border, a few loops more
            return lz == gate(seed, rx, rz, size, salt, 0) || GenHash.unit(seed, cx, cz, salt + 3) < loops * 0.5;
        }
        return (tree(seed, salt, size, rx, rz)[lz * size + lx - 1] & EAST) != 0 || GenHash.unit(seed, cx, cz, salt + 3) < loops;
    }

    /** Whether the north line of cell (cx, cz) can be passed. */
    public static boolean north(long seed, int cx, int cz, int size, int salt, double loops) {
        int rx = Math.floorDiv(cx, size), rz = Math.floorDiv(cz, size), lx = Math.floorMod(cx, size), lz = Math.floorMod(cz, size);
        if (lz == 0) {
            return lx == gate(seed, rx, rz, size, salt, 1) || GenHash.unit(seed, cx, cz, salt + 4) < loops * 0.5;
        }
        return (tree(seed, salt, size, rx, rz)[(lz - 1) * size + lx] & SOUTH) != 0 || GenHash.unit(seed, cx, cz, salt + 4) < loops;
    }

    /** Whether (cx, cz) is its region's root cell (the goal every way of the tree leads to). */
    public static boolean root(long seed, int cx, int cz, int size, int salt) {
        int rx = Math.floorDiv(cx, size), rz = Math.floorDiv(cz, size);
        return cx == GenHash.rootX(seed, rx, rz, size, salt) && cz == GenHash.rootZ(seed, rx, rz, size, salt);
    }

    /** Cell offset (1 .. size - 2) of the gate in the west (side 0) or north (side 1) border of region (rx, rz). */
    static int gate(long seed, int rx, int rz, int size, int salt, int side) {
        return 1 + (int) (GenHash.unit(seed, rx, rz, salt + 5 + side) * (size - 2));
    }

    /** The spanning tree of a region: per cell bit 1 = open to the east neighbour, bit 2 = open to the south neighbour. */
    static byte[] tree(long seed, int salt, int size, int rx, int rz) {
        Key key = new Key(seed, salt, size, rx, rz);
        byte[] t = TREES.get(key);
        if (t != null) return t;
        if (TREES.size() > CACHE_LIMIT) TREES.clear();
        t = grow(seed, salt, size, rx, rz);
        TREES.put(key, t);
        return t;
    }

    private static byte[] grow(long seed, int salt, int size, int rx, int rz) {
        int n = size * size;
        byte[] open = new byte[n];
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int top = 0;
        long state = GenHash.mix(seed ^ GenHash.mix(rx * 0x9E3779B97F4A7C15L + rz * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L));
        int start = (GenHash.rootX(seed, rx, rz, size, salt) - rx * size) + (GenHash.rootZ(seed, rx, rz, size, salt) - rz * size) * size;
        seen[start] = true;
        stack[top++] = start;
        int[] next = new int[4];
        while (top > 0) {
            int c = stack[top - 1], x = c % size, z = c / size, k = 0;
            if (x > 0 && !seen[c - 1]) next[k++] = c - 1;
            if (x < size - 1 && !seen[c + 1]) next[k++] = c + 1;
            if (z > 0 && !seen[c - size]) next[k++] = c - size;
            if (z < size - 1 && !seen[c + size]) next[k++] = c + size;
            if (k == 0) {
                top--;
                continue;
            }
            state = GenHash.mix(state + 0x9E3779B97F4A7C15L);
            int to = next[(int) ((state >>> 33) % k)];
            if (to == c + 1) open[c] |= EAST;
            else if (to == c - 1) open[to] |= EAST;
            else if (to == c + size) open[c] |= SOUTH;
            else open[to] |= SOUTH;
            seen[to] = true;
            stack[top++] = to;
        }
        return open;
    }
}
