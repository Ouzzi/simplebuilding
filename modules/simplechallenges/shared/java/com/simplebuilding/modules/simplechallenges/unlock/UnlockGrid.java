package com.simplebuilding.modules.simplechallenges.unlock;

import java.util.ArrayList;
import java.util.List;

/**
 * The unlocked area of one dimension: square columns of {@code cell} x {@code cell} blocks (full height), a cell is free
 * or locked. Pure logic (no Minecraft types): the movement limits, the neighbour/frontier queries and the nearest free
 * cell are tested without a game. Cell (cx, cz) covers blocks cx*cell .. cx*cell+cell-1.
 */
public final class UnlockGrid {
    /** Allowed cell sizes in blocks (R5): 1 = block column, 16 = chunk, 48 = three chunks. */
    public static final int[] CELL_SIZES = {1, 2, 4, 8, 16, 32, 48};
    /** Gap kept to a locked cell so the box never reads as inside it (above {@link #MAX_EPS}). */
    public static final double GAP = 1.0E-4;
    private static final double MAX_EPS = 1.0E-6;
    /** Most cells one movement step may cross when looking for a locked one (a step is a few blocks). */
    private static final int MAX_STEP_CELLS = 4096;
    public static final long NONE = Long.MIN_VALUE + 1;

    private final int cell;
    private final LongSet free = new LongSet();

    public UnlockGrid(int cell) {
        if (!validCell(cell)) throw new IllegalArgumentException("cell size " + cell);
        this.cell = cell;
    }

    public static boolean validCell(int cell) {
        for (int c : CELL_SIZES) if (c == cell) return true;
        return false;
    }

    public int cell() {
        return cell;
    }

    public int size() {
        return free.size();
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public static int keyX(long key) {
        return (int) (key >> 32);
    }

    public static int keyZ(long key) {
        return (int) key;
    }

    public int cellOf(double blockCoord) {
        return (int) Math.floor(blockCoord / cell);
    }

    /** Cell holding the upper edge of a box: the edge itself belongs to the next cell. */
    private int cellOfMax(double blockCoord) {
        return (int) Math.floor((blockCoord - MAX_EPS) / cell);
    }

    public boolean isFree(int cx, int cz) {
        return free.contains(key(cx, cz));
    }

    public boolean isFreeKey(long key) {
        return free.contains(key);
    }

    public boolean isFreeAtBlock(double x, double z) {
        return isFree(cellOf(x), cellOf(z));
    }

    /** @return true when the cell was locked before. */
    public boolean unlock(int cx, int cz) {
        return free.add(key(cx, cz));
    }

    /** Adds the rectangle (cell units, inclusive, corners in any order). */
    public void addRect(int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) unlock(x, z);
        }
    }

    public long[] keys() {
        return free.toSortedArray();
    }

    public static UnlockGrid fromKeys(int cell, long[] keys) {
        UnlockGrid g = new UnlockGrid(cell);
        for (long k : keys) g.free.add(k);
        return g;
    }

    /** True when every cell the box touches is free. */
    public boolean boxFree(double minX, double maxX, double minZ, double maxZ) {
        for (int x = cellOf(minX); x <= cellOfMax(maxX); x++) {
            for (int z = cellOf(minZ); z <= cellOfMax(maxZ); z++) if (!isFree(x, z)) return false;
        }
        return true;
    }

    /** Largest allowed move along X (same sign as dx, at most |dx|) for a box that is inside the free area. */
    public double limitX(double minX, double maxX, double minZ, double maxZ, double dx) {
        if (dx == 0) return 0;
        int z0 = cellOf(minZ), z1 = cellOfMax(maxZ);
        if (dx > 0) {
            int from = cellOfMax(maxX) + 1, to = Math.min(cellOfMax(maxX + dx), from + MAX_STEP_CELLS);
            for (int c = from; c <= to; c++) {
                for (int z = z0; z <= z1; z++) if (!isFree(c, z)) return Math.min(dx, Math.max(0, c * (double) cell - GAP - maxX));
            }
            return dx;
        }
        int from = cellOf(minX) - 1, to = Math.max(cellOf(minX + dx), from - MAX_STEP_CELLS);
        for (int c = from; c >= to; c--) {
            for (int z = z0; z <= z1; z++) if (!isFree(c, z)) return Math.max(dx, Math.min(0, (c + 1) * (double) cell + GAP - minX));
        }
        return dx;
    }

    /** Like {@link #limitX} along Z. */
    public double limitZ(double minX, double maxX, double minZ, double maxZ, double dz) {
        if (dz == 0) return 0;
        int x0 = cellOf(minX), x1 = cellOfMax(maxX);
        if (dz > 0) {
            int from = cellOfMax(maxZ) + 1, to = Math.min(cellOfMax(maxZ + dz), from + MAX_STEP_CELLS);
            for (int c = from; c <= to; c++) {
                for (int x = x0; x <= x1; x++) if (!isFree(x, c)) return Math.min(dz, Math.max(0, c * (double) cell - GAP - maxZ));
            }
            return dz;
        }
        int from = cellOf(minZ) - 1, to = Math.max(cellOf(minZ + dz), from - MAX_STEP_CELLS);
        for (int c = from; c >= to; c--) {
            for (int x = x0; x <= x1; x++) if (!isFree(x, c)) return Math.max(dz, Math.min(0, (c + 1) * (double) cell + GAP - minZ));
        }
        return dz;
    }

    /**
     * The horizontal move a box may make: X first, then Z from the new X (so sliding along a wall works). Returns
     * {dx, dz}; the same values back when nothing limits it. A box that already touches a locked cell is not limited
     * (the rescue tick brings it out; limiting would trap it inside).
     */
    public double[] clamp(double minX, double maxX, double minZ, double maxZ, double dx, double dz) {
        if (!boxFree(minX, maxX, minZ, maxZ)) return new double[]{dx, dz};
        double ax = limitX(minX, maxX, minZ, maxZ, dx);
        double az = limitZ(minX + ax, maxX + ax, minZ, maxZ, dz);
        return new double[]{ax, az};
    }

    /** The four edge neighbours of a cell as keys (W, E, N, S order: -x, +x, -z, +z). */
    public static long[] neighbours(int cx, int cz) {
        return new long[]{key(cx - 1, cz), key(cx + 1, cz), key(cx, cz - 1), key(cx, cz + 1)};
    }

    public boolean touchesFree(int cx, int cz) {
        return isFree(cx - 1, cz) || isFree(cx + 1, cz) || isFree(cx, cz - 1) || isFree(cx, cz + 1);
    }

    /** Locked cells next to a free one (4-neighbourhood), sorted by key. */
    public long[] frontier() {
        LongSet seen = new LongSet();
        List<Long> out = new ArrayList<>();
        for (long k : free.toSortedArray()) {
            for (long n : neighbours(keyX(k), keyZ(k))) if (!free.contains(n) && seen.add(n)) out.add(n);
        }
        long[] a = new long[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        java.util.Arrays.sort(a);
        return a;
    }

    /** The free cell nearest to (cx, cz) within {@code maxRadius} cells (Euclidean among the rings), or {@link #NONE}. */
    public long nearestFree(int cx, int cz, int maxRadius) {
        if (free.size() == 0) return NONE;
        if (isFree(cx, cz)) return key(cx, cz);
        long best = NONE;
        double bestD = Double.MAX_VALUE;
        for (int r = 1; r <= maxRadius; r++) {
            if (best != NONE && r > Math.ceil(Math.sqrt(bestD)) + 1) break;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || !isFree(cx + dx, cz + dz)) continue;
                    double d = (double) dx * dx + (double) dz * dz;
                    if (d < bestD) {
                        bestD = d;
                        best = key(cx + dx, cz + dz);
                    }
                }
            }
        }
        return best;
    }

    /** Border edges of free cells inside a window (cell units, inclusive): {cx, cz, side}, side 0=-x 1=+x 2=-z 3=+z, locked neighbour beyond. */
    public List<int[]> borderEdges(int minCx, int minCz, int maxCx, int maxCz) {
        List<int[]> out = new ArrayList<>();
        for (int x = minCx; x <= maxCx; x++) {
            for (int z = minCz; z <= maxCz; z++) {
                if (!isFree(x, z)) continue;
                if (!isFree(x - 1, z)) out.add(new int[]{x, z, 0});
                if (!isFree(x + 1, z)) out.add(new int[]{x, z, 1});
                if (!isFree(x, z - 1)) out.add(new int[]{x, z, 2});
                if (!isFree(x, z + 1)) out.add(new int[]{x, z, 3});
            }
        }
        return out;
    }
}
