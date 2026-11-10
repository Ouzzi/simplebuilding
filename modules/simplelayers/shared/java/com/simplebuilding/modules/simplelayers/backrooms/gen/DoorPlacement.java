package com.simplebuilding.modules.simplelayers.backrooms.gen;

import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.world.gen.GenHash;
import org.jspecify.annotations.Nullable;

/**
 * The core's standard door spot (BACK22 core §4.2/§5.4, until a level reserves its own niches): the free standing spot nearest to the
 * preferred chunk-local (px, pz) - solid below, two passable above - with the door one step further in the first direction (seeded
 * order) whose door, frame, wall behind and floor stay inside the chunk. The core stamps the door chamber there (door, frame, wall,
 * floor), so the result is always a framed door with a free spot in front. Pure.
 */
public final class DoorPlacement {
    public static final int MIN = 3, MAX = 12, SALT = 2300;

    private DoorPlacement() {}

    public static @Nullable DoorEnd find(BandLayout layout, long seed, int chunkX, int chunkZ, int px, int pz, int width) {
        int x0 = chunkX * 16, z0 = chunkZ * 16;
        int[] a = layout.arrival(seed, x0 + px, z0 + pz);
        DoorEnd e = at(layout, seed, chunkX, chunkZ, a[0] - x0, a[1], a[2] - z0, width);
        if (e != null) return e;
        for (int r = 0; r <= MAX - MIN; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int lx = px + dx, lz = pz + dz;
                    if (lx < MIN || lx > MAX || lz < MIN || lz > MAX) continue;
                    for (int ly = 1; ly < BandLayout.HEIGHT - 4; ly++) {
                        e = at(layout, seed, chunkX, chunkZ, lx, ly, lz, width);
                        if (e != null) return e;
                    }
                }
            }
        }
        return null;
    }

    /** The door for standing spot (lx, ly, lz), or null when the spot is not free or no direction fits the chunk. */
    static @Nullable DoorEnd at(BandLayout layout, long seed, int chunkX, int chunkZ, int lx, int ly, int lz, int width) {
        if (lx < MIN || lx > MAX || lz < MIN || lz > MAX || ly < 1 || ly > BandLayout.HEIGHT - 5) return null;
        int x = chunkX * 16 + lx, z = chunkZ * 16 + lz;
        BandPiece below = layout.at(seed, x, ly - 1, z), feet = layout.at(seed, x, ly, z), head = layout.at(seed, x, ly + 1, z);
        if (!below.solid() || !feet.passable() || !head.passable()) return null;
        int start = (int) (GenHash.unit(seed, x, z, SALT + ly) * 4);
        for (int k = 0; k < 4; k++) {
            int f = (start + k) & 3;
            DoorEnd e = new DoorEnd(x + DoorEnd.DX[f], ly, z + DoorEnd.DZ[f], f, width);
            if (fits(e, chunkX, chunkZ)) return e;
        }
        return null;
    }

    /** Door, frame (one beyond each side), wall behind and the spots in front all inside the chunk. */
    public static boolean fits(DoorEnd e, int chunkX, int chunkZ) {
        int cw = (e.facing() + 1) & 3;
        for (int i = -1; i <= e.width(); i++) {
            int x = e.x() + DoorEnd.DX[cw] * i, z = e.z() + DoorEnd.DZ[cw] * i;
            for (int d = -1; d <= 1; d++) {
                int px = x + DoorEnd.DX[e.facing()] * d, pz = z + DoorEnd.DZ[e.facing()] * d;
                if ((px >> 4) != chunkX || (pz >> 4) != chunkZ) return false;
            }
        }
        return true;
    }

    /** Whether (x, ly, z) lies in the door chamber of {@code e} (frame, door, wall behind, spots in front, floor, lintel). */
    public static boolean inFootprint(DoorEnd e, int x, int ly, int z) {
        if (ly < e.ly() - 1 || ly > e.ly() + 2) return false;
        int cw = (e.facing() + 1) & 3;
        for (int i = -1; i <= e.width(); i++) {
            for (int d = -1; d <= 1; d++) {
                if (x == e.x() + DoorEnd.DX[cw] * i + DoorEnd.DX[e.facing()] * d && z == e.z() + DoorEnd.DZ[cw] * i + DoorEnd.DZ[e.facing()] * d) return true;
            }
        }
        return false;
    }

    /** Whether the chambers of two door ends touch (then the second one is dropped: one-way pair or no entrance). */
    public static boolean overlaps(DoorEnd a, DoorEnd b) {
        if (Math.abs(a.ly() - b.ly()) > 4) return false;
        int cw = (b.facing() + 1) & 3;
        for (int i = -1; i <= b.width(); i++) {
            for (int d = -1; d <= 1; d++) {
                int x = b.x() + DoorEnd.DX[cw] * i + DoorEnd.DX[b.facing()] * d, z = b.z() + DoorEnd.DZ[cw] * i + DoorEnd.DZ[b.facing()] * d;
                for (int ly = b.ly() - 1; ly <= b.ly() + 2; ly++) {
                    for (int m = -1; m <= 1; m++) {
                        if (inFootprint(a, x + m, ly, z) || inFootprint(a, x, ly, z + m)) return true;
                    }
                }
            }
        }
        return false;
    }
}
