package com.simplebuilding.modules.simplechallenges.skyline;

/**
 * Where the n-th block of a Skyline stands, in cell units (one cell = two blocks, so one block of air lies between neighbours).
 * {@code LINEAR}: (n, 0). {@code GRID}: the positive quadrant in shells, shell k = (k,0)..(k,k) then (k-1,k)..(0,k), which gives
 * 0,0 -> 1,0 -> 1,1 -> 0,1 -> 2,0 -> 2,1 -> 2,2 -> ... {@code RING}: layer k round the origin has 8k cells, starting at (k, -(k-1))
 * and going up, left, down, right. Both directions are O(1), so a chunk asks the layout, nothing is stored.
 */
public enum Layout {
    LINEAR("linear"), GRID("grid"), RING("ring");

    public final String id;

    Layout(String id) {
        this.id = id;
    }

    public static Layout parse(String id) {
        for (Layout l : values()) if (l.id.equals(id)) return l;
        return null;
    }

    /** The cell {@code {cx, cz}} of block number n (n >= 0). */
    public int[] cell(int n) {
        switch (this) {
            case LINEAR:
                return new int[]{n, 0};
            case GRID: {
                int k = (int) Math.floor(Math.sqrt(n));
                while ((long) k * k > n) k--;
                while ((long) (k + 1) * (k + 1) <= n) k++;
                int r = n - k * k;
                return r <= k ? new int[]{k, r} : new int[]{2 * k - r, k};
            }
            default: {
                if (n == 0) return new int[]{0, 0};
                int k = ringOf(n);
                int m = n - (4 * k * (k - 1) + 1), side = m / (2 * k), t = m % (2 * k);
                return switch (side) {
                    case 0 -> new int[]{k, -(k - 1) + t};
                    case 1 -> new int[]{k - 1 - t, k};
                    case 2 -> new int[]{-k, k - 1 - t};
                    default -> new int[]{-k + 1 + t, -k};
                };
            }
        }
    }

    /** The number of the block at a cell, or -1 when no block stands there. */
    public int index(int cx, int cz) {
        switch (this) {
            case LINEAR:
                return cz == 0 && cx >= 0 ? cx : -1;
            case GRID: {
                if (cx < 0 || cz < 0) return -1;
                int k = Math.max(cx, cz);
                return cx == k ? k * k + cz : k * k + 2 * k - cx;
            }
            default: {
                int k = Math.max(Math.abs(cx), Math.abs(cz));
                if (k == 0) return 0;
                int base = 4 * k * (k - 1) + 1, m;
                if (cx == k && cz > -k) m = cz + k - 1;
                else if (cz == k && cx < k) m = 2 * k + (k - 1 - cx);
                else if (cx == -k && cz < k) m = 4 * k + (k - 1 - cz);
                else m = 6 * k + (cx + k - 1);
                return base + m;
            }
        }
    }

    /** The ring (layer) a block number n >= 1 lies in: the k with 4k(k-1) < n <= 4k(k+1). */
    private static int ringOf(int n) {
        int k = (int) ((Math.sqrt(n) + 1) / 2);
        while (k > 1 && 4 * k * (k - 1) >= n) k--;
        while (4 * k * (k + 1) < n) k++;
        return Math.max(k, 1);
    }
}
