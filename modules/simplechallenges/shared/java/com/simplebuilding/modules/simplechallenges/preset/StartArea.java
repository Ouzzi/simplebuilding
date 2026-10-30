package com.simplebuilding.modules.simplechallenges.preset;

import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The start area as the editor works with it: a set of cells that is turned into the preset's list of rectangles and back. Pure logic. */
public final class StartArea {
    private StartArea() {}

    /** The cells of the rectangles ({@code [x0,z0,x1,z1]}, corners in any order); the origin cell is always part of it. */
    public static Set<Long> cells(List<int[]> rects) {
        Set<Long> out = new HashSet<>();
        for (int[] r : rects) {
            for (int x = Math.min(r[0], r[2]); x <= Math.max(r[0], r[2]); x++) {
                for (int z = Math.min(r[1], r[3]); z <= Math.max(r[1], r[3]); z++) out.add(UnlockGrid.key(x, z));
            }
        }
        out.add(UnlockGrid.key(0, 0));
        return out;
    }

    /** Few rectangles that cover exactly the cells: columns first, then the column run is widened while the next columns match. */
    public static List<int[]> cover(Set<Long> cells) {
        Set<Long> left = new HashSet<>(cells);
        List<Long> order = new ArrayList<>(cells);
        order.sort(java.util.Comparator.<Long>comparingInt(UnlockGrid::keyX).thenComparingInt(UnlockGrid::keyZ));
        List<int[]> out = new ArrayList<>();
        for (long k : order) {
            if (!left.contains(k)) continue;
            int x = UnlockGrid.keyX(k), z0 = UnlockGrid.keyZ(k), z1 = z0;
            while (left.contains(UnlockGrid.key(x, z1 + 1))) z1++;
            int x1 = x;
            while (columnFree(left, x1 + 1, z0, z1)) x1++;
            for (int xx = x; xx <= x1; xx++) for (int zz = z0; zz <= z1; zz++) left.remove(UnlockGrid.key(xx, zz));
            out.add(new int[]{x, z0, x1, z1});
        }
        return out;
    }

    private static boolean columnFree(Set<Long> left, int x, int z0, int z1) {
        for (int z = z0; z <= z1; z++) if (!left.contains(UnlockGrid.key(x, z))) return false;
        return true;
    }

    /** Smallest box round the cells: {minX, minZ, maxX, maxZ}. */
    public static int[] bounds(Set<Long> cells) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (long k : cells) {
            b[0] = Math.min(b[0], UnlockGrid.keyX(k));
            b[1] = Math.min(b[1], UnlockGrid.keyZ(k));
            b[2] = Math.max(b[2], UnlockGrid.keyX(k));
            b[3] = Math.max(b[3], UnlockGrid.keyZ(k));
        }
        return b;
    }
}
