package com.simplebuilding.modules.simplevault.gen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * The Bedrock Vault blocks (UNDER21): pure function of seed and position, MC-free. A position is open when a carved box of its
 * {@link VaultLayout} holds it and no fill box does; an open position gets its decoration or air. A solid position that touches
 * an open one (any of its 26 neighbours) is cladding - floor (open above), ceiling (open below) or wall in the style of the
 * neighbouring box, with ceiling lights on the box's grid - and every other solid position is bedrock. So every room, corridor,
 * floor and ceiling is one cosmetic layer in front of bedrock, by construction.
 */
public final class VaultGen {
    public static final int HEIGHT = 128;

    private VaultGen() {}

    /** The pieces of a box inside one region, index (y * d + z) * w + x. */
    public static VaultPiece[] fill(long seed, int x0, int y0, int z0, int w, int h, int d) {
        VaultLayout l = VaultLayout.at(seed, x0, z0);
        VaultPiece[] out = new VaultPiece[w * h * d];
        int W = w + 2, H = h + 2, D = d + 2;
        List<VaultLayout.Box> near = new ArrayList<>();
        for (VaultLayout.Box b : l.boxes) if (b.touches(x0 - 1, y0 - 1, z0 - 1, x0 + w, y0 + h, z0 + d)) near.add(b);
        // carve id per position of the box plus a border of one: -1 solid, -2 solid inside a fill, else carve index in `near`
        int[] id = new int[W * H * D];
        int lo = l.minY() - 1, hi = l.maxY() + 1;
        for (int y = 0; y < H; y++) {
            int wy = y0 - 1 + y;
            for (int z = 0; z < D; z++) {
                int wz = z0 - 1 + z;
                for (int x = 0; x < W; x++) {
                    int wx = x0 - 1 + x, v = -1;
                    if (wy >= lo && wy <= hi && !near.isEmpty()) {
                        int carve = -1;
                        boolean filled = false;
                        for (int i = 0; i < near.size(); i++) {
                            VaultLayout.Box b = near.get(i);
                            if (!b.contains(wx, wy, wz)) continue;
                            if (b.fill()) {
                                filled = true;
                                v = -2 - i;
                                break;
                            }
                            if (carve < 0) carve = i;
                        }
                        if (!filled && carve >= 0) v = carve;
                        else if (!filled) v = -1;
                    }
                    id[(y * D + z) * W + x] = v;
                }
            }
        }
        for (int y = 0; y < h; y++) {
            int wy = y0 + y;
            for (int z = 0; z < d; z++) {
                int wz = z0 + z;
                for (int x = 0; x < w; x++) {
                    int wx = x0 + x;
                    out[(y * d + z) * w + x] = piece(l, near, id, W, D, x + 1, y + 1, z + 1, wx, wy, wz);
                }
            }
        }
        return out;
    }

    /** One position (a column of the chunk generator's base column, the tests). */
    public static VaultPiece at(long seed, int x, int y, int z) {
        if (y < 0 || y >= HEIGHT) return VaultPiece.AIR;
        return fill(seed, x, y, z, 1, 1, 1)[0];
    }

    private static VaultPiece piece(VaultLayout l, List<VaultLayout.Box> near, int[] id, int W, int D, int x, int y, int z, int wx, int wy, int wz) {
        int self = id[(y * D + z) * W + x];
        VaultPiece decor = l.decorAt(wx, wy, wz);
        if (self >= 0) return decor != null ? decor : VaultPiece.AIR;
        if (decor != null) return decor;
        int above = id[((y + 1) * D + z) * W + x], below = id[((y - 1) * D + z) * W + x];
        if (self <= -2) { // inside a fill (pillar, gallery, stair mass): its own style
            VaultLayout.Box f = near.get(-2 - self);
            VaultPiece[] s = VaultLayout.STYLES[f.style()];
            if (!touches(id, W, D, x, y, z)) return VaultPiece.BEDROCK;
            return above >= 0 ? s[0] : below >= 0 ? s[5] : s[3];
        }
        if (above >= 0) return floor(near.get(above), wx, wz);
        if (below >= 0) return ceiling(near.get(below), wx, wz);
        int n = neighbour(id, W, D, x, y, z);
        if (n < 0) return VaultPiece.BEDROCK;
        VaultLayout.Box b = near.get(n);
        VaultPiece[] s = VaultLayout.STYLES[b.style()];
        if (wy == b.y0()) return s[2];
        if (wy == b.y1()) return s[4];
        return s[3];
    }

    private static VaultPiece floor(VaultLayout.Box b, int x, int z) {
        VaultPiece[] s = VaultLayout.STYLES[b.style()];
        return ((Math.floorDiv(x - b.x0(), 2) + Math.floorDiv(z - b.z0(), 2)) & 1) == 0 ? s[0] : s[1];
    }

    private static VaultPiece ceiling(VaultLayout.Box b, int x, int z) {
        if (b.step() > 0 && Math.floorMod(x - b.x0() - b.off(), b.step()) == 0 && Math.floorMod(z - b.z0() - b.off(), b.step()) == 0
                && x > b.x0() && x < b.x1() && z > b.z0() && z < b.z1()) {
            return VaultPiece.SEA_LANTERN;
        }
        return VaultLayout.STYLES[b.style()][5];
    }

    private static boolean touches(int[] id, int W, int D, int x, int y, int z) {
        return neighbour(id, W, D, x, y, z) >= 0;
    }

    /** A carved box id among the 26 neighbours (side neighbours first), or -1. */
    private static int neighbour(int[] id, int W, int D, int x, int y, int z) {
        int[][] sides = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        for (int[] s : sides) {
            int v = id[((y + s[1]) * D + z + s[2]) * W + x + s[0]];
            if (v >= 0) return v;
        }
        for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int v = id[((y + dy) * D + z + dz) * W + x + dx];
            if (v >= 0) return v;
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------------------------- checks (tests, rounds)

    /** The whole complex of a region (box: local ORIGIN-2 .. ORIGIN+3*SLOT+1, y minY-2 .. maxY+2). */
    public static Volume complex(long seed, int rx, int rz) {
        VaultLayout l = VaultLayout.of(seed, rx, rz);
        int x0 = l.bx + VaultLayout.ORIGIN - 2, z0 = l.bz + VaultLayout.ORIGIN - 2, y0 = Math.max(0, l.minY() - 2);
        int w = 3 * VaultLayout.SLOT + 4, h = Math.min(HEIGHT, l.maxY() + 3) - y0;
        return new Volume(l, x0, y0, z0, w, h, w, fill(seed, x0, y0, z0, w, h, w));
    }

    /** A filled box with lookup helpers. */
    public record Volume(VaultLayout layout, int x0, int y0, int z0, int w, int h, int d, VaultPiece[] p) {
        public VaultPiece get(int x, int y, int z) {
            int lx = x - x0, ly = y - y0, lz = z - z0;
            if (lx < 0 || ly < 0 || lz < 0 || lx >= w || ly >= h || lz >= d) return VaultPiece.BEDROCK;
            return p[(ly * d + lz) * w + lx];
        }

        boolean walk(int x, int y, int z) {
            return get(x, y, z).open;
        }

        /** A body can stand here: feet and head free, something to stand on (or water). */
        public boolean stand(int x, int y, int z) {
            VaultPiece below = get(x, y - 1, z);
            return walk(x, y, z) && walk(x, y + 1, z) && get(x, y, z) != VaultPiece.GATE_OPEN && (!below.open || below == VaultPiece.WATER || get(x, y, z) == VaultPiece.WATER);
        }

        /**
         * Positions reachable on foot from {@code start}: 4 directions, step up 1 (jump or stair), down up to 3, swimming. Returns
         * a mask over the volume.
         */
        public boolean[] reach(int[] start) {
            return reach(start, true);
        }

        /** {@code jump} false: a step up only onto a stair (walkable without jumping, the B stairs rule 10.11.). */
        public boolean[] reach(int[] start, boolean jump) {
            boolean[] seen = new boolean[p.length];
            ArrayDeque<int[]> q = new ArrayDeque<>();
            if (stand(start[0], start[1], start[2])) {
                q.add(start);
                seen[idx(start[0], start[1], start[2])] = true;
            }
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] dd : dirs) {
                    int nx = c[0] + dd[0], nz = c[2] + dd[1];
                    for (int dy = 1; dy >= -3; dy--) {
                        int ny = c[1] + dy;
                        if (dy == 1 && !walk(c[0], c[1] + 2, c[2])) continue; // head room to jump
                        if (dy == 1 && !jump && !get(nx, ny - 1, nz).name().contains("STAIRS")) continue;
                        if (dy < 0 && !(walk(nx, c[1], nz) && walk(nx, c[1] + 1, nz))) break;
                        if (stand(nx, ny, nz)) {
                            int i = idx(nx, ny, nz);
                            if (i >= 0 && !seen[i]) {
                                seen[i] = true;
                                q.add(new int[] {nx, ny, nz});
                            }
                            break;
                        }
                    }
                }
                if (get(c[0], c[1], c[2]) == VaultPiece.WATER && stand(c[0], c[1] + 1, c[2])) { // swim up
                    int i = idx(c[0], c[1] + 1, c[2]);
                    if (i >= 0 && !seen[i]) {
                        seen[i] = true;
                        q.add(new int[] {c[0], c[1] + 1, c[2]});
                    }
                }
            }
            return seen;
        }

        public int idx(int x, int y, int z) {
            int lx = x - x0, ly = y - y0, lz = z - z0;
            if (lx < 0 || ly < 0 || lz < 0 || lx >= w || ly >= h || lz >= d) return -1;
            return (ly * d + lz) * w + lx;
        }

        /** Whether a stand spot next to (x, y, z) (same height or one below, any side) is reached. */
        public boolean reachable(boolean[] seen, int x, int y, int z) {
            for (int dy = 0; dy >= -2; dy--) {
                for (int[] dd : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}}) {
                    int i = idx(x + dd[0], y + dy, z + dd[1]);
                    if (i >= 0 && seen[i]) return true;
                }
            }
            return false;
        }

        /**
         * Stair problems (rule of the B stairs fix 10.11.): every stair block of a run has three open blocks above it, and the next
         * step of its run is one higher (walkable without jumping). Returns a short description per problem.
         */
        public List<String> stairProblems() {
            List<String> out = new ArrayList<>();
            for (int y = 0; y < h - 4; y++) for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
                VaultPiece q = p[(y * d + z) * w + x];
                if (!q.name().contains("STAIRS")) continue;
                for (int k = 1; k <= 3; k++) {
                    if (!p[((y + k) * d + z) * w + x].open) {
                        out.add("stair at " + (x0 + x) + " " + (y0 + y) + " " + (z0 + z) + " without head room (" + k + ")");
                        break;
                    }
                }
            }
            return out;
        }

        /**
         * Hull problems: an open position whose straight line in any of the six directions leaves the open space through anything
         * but "cladding, then bedrock" - except where the first solid block belongs to a thin inner part (fill, decoration) and
         * open space follows. Also: the volume's border must be bedrock. Returns a short description per problem (max 20).
         */
        public List<String> hullProblems() {
            List<String> out = new ArrayList<>();
            int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int y = 0; y < h && out.size() < 20; y++) {
                for (int z = 0; z < d && out.size() < 20; z++) {
                    for (int x = 0; x < w && out.size() < 20; x++) {
                        boolean border = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                        VaultPiece here = p[(y * d + z) * w + x];
                        if (border && here != VaultPiece.BEDROCK) out.add("border " + (x0 + x) + " " + (y0 + y) + " " + (z0 + z) + " " + here);
                    }
                }
            }
            // carved-open mask and fill mask once
            VaultLayout l = layout;
            boolean[] open = new boolean[p.length], filled = new boolean[p.length];
            int[] group = new int[p.length];
            for (VaultLayout.Box b : l.boxes) {
                for (int y = Math.max(b.y0(), y0); y <= Math.min(b.y1(), y0 + h - 1); y++) for (int z = Math.max(b.z0(), z0); z <= Math.min(b.z1(), z0 + d - 1); z++)
                    for (int x = Math.max(b.x0(), x0); x <= Math.min(b.x1(), x0 + w - 1); x++) {
                        int i = idx(x, y, z);
                        if (b.fill()) filled[i] = true;
                        else {
                            open[i] = true;
                            group[i] = b.group();
                        }
                    }
            }
            for (int i = 0; i < p.length; i++) if (filled[i]) open[i] = false;
            // a separation of one or two solid blocks between open spaces has no bedrock core: only thin inner parts (fills) may
            for (int y = 1; y < h - 3 && out.size() < 20; y++) for (int z = 1; z < d - 3 && out.size() < 20; z++) for (int x = 1; x < w - 3 && out.size() < 20; x++) {
                int i = (y * d + z) * w + x;
                if (open[i] && p[i] == VaultPiece.BEDROCK) out.add("bedrock in the open at " + (x0 + x) + " " + (y0 + y) + " " + (z0 + z));
                if (!open[i]) continue;
                for (int[] dd : dirs) {
                    if (dd[0] < 0 || dd[1] < 0 || dd[2] < 0) continue; // each pair once
                    int a = idx(x0 + x + dd[0], y0 + y + dd[1], z0 + z + dd[2]), b = idx(x0 + x + 2 * dd[0], y0 + y + 2 * dd[1], z0 + z + 2 * dd[2]),
                            c = idx(x0 + x + 3 * dd[0], y0 + y + 3 * dd[1], z0 + z + 3 * dd[2]);
                    if (a < 0 || open[a] || filled[a]) continue;
                    if (p[a] == VaultPiece.BEDROCK) {
                        out.add("open next to bedrock at " + (x0 + x) + " " + (y0 + y) + " " + (z0 + z));
                        continue;
                    }
                    boolean one = b >= 0 && open[b] && group[b] != group[i], two = b >= 0 && !open[b] && !filled[b] && p[b] != VaultPiece.BEDROCK && c >= 0 && open[c] && group[c] != group[i];
                    if (one || two) out.add("wall of " + (one ? 1 : 2) + " without bedrock at " + (x0 + x) + " " + (y0 + y) + " " + (z0 + z) + " dir " + dd[0] + dd[1] + dd[2]);
                }
            }
            return out;
        }

        private static boolean openCarved(VaultLayout l, int x, int y, int z) {
            boolean carved = false;
            for (VaultLayout.Box b : l.boxes) {
                if (!b.contains(x, y, z)) continue;
                if (b.fill()) return false;
                carved = true;
            }
            return carved;
        }

        /** Inside a fill box, or a decoration on a carved spot (thin inner parts may have open space behind them). */
        private static boolean thin(VaultLayout l, int x, int y, int z) {
            for (VaultLayout.Box b : l.boxes) if (b.fill() && b.contains(x, y, z)) return true;
            return false;
        }
    }
}
