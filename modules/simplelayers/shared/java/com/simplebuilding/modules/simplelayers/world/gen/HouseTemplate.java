package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * The Backrooms entrance in the Overworld since BACK20 (plan docs/ai/PLAN-B-BACK20-2026-11-10.md): an abandoned cottage in
 * Vanilla style (cobblestone footing, plank walls, log corners, glass panes, a door, a gable roof of stairs) with a cellar under it
 * (trapdoor and ladder, stone bricks, barrels, cobwebs, a lantern). BACK22 (core K6): the cellar's west wall holds the cellar
 * double door into the Lobby (a portal door pair, placed by the feature) in a yellow-and-black frame with a warning light above,
 * caution tape and signs beside it - clearly dangerous. The old shaft with its rift floor is gone (fg11 a); behind the door is wall.
 *
 * <p>Relative to the house centre on the ground ({@code dy 0} = the floor, built into the ground's top block). {@link #piece}
 * returns null where the terrain stays. The wood pieces ({@code H_*}, roof) take the biome's wood in the feature.
 */
public final class HouseTemplate {
    /** Footprint: x -5 (wall behind the cellar door) .. 5 (roof), z -4 .. 4; the house walls at |x| = 4, |z| = 3. */
    public static final int MIN_X = -5, MAX_X = 5, HALF_Z = 4, DEPTH = 4, HEIGHT = 7, CELLAR_FLOOR = -4;
    /** The cellar door wall (x); the door's left leaf (walking west into it: south leaf) at z 1, the right one at z 0, lower half at dy -3. */
    public static final int PLUG_X = -4, DOOR_DY = -3, DOOR_LEFT_Z = 1;

    private HouseTemplate() {}

    /** Which window (0-5) is broken - one per house, from its position. */
    public static int brokenWindow(int x, int z) {
        return Math.floorMod(x * 31 + z * 17, 6);
    }

    public static Piece piece(int dx, int dy, int dz, int broken) {
        int ax = Math.abs(dx), az = Math.abs(dz);
        if (dx < MIN_X || dx > MAX_X || az > HALF_Z || dy < -DEPTH || dy > HEIGHT) return null;
        if (dx <= -5) return shaft(dx, dy, dz);
        if (dy < 0) return cellar(dx, dy, dz, ax, az);
        if (dy == 0) {
            if (ax > 4 || az > 3) return null;
            if (dx == 3 && dz == 2) return Piece.H_TRAPDOOR;
            return ax == 4 || az == 3 ? Piece.COBBLE : Piece.H_FLOOR;
        }
        return house(dx, dy, dz, ax, az, broken);
    }

    static Piece house(int dx, int dy, int dz, int ax, int az, int broken) {
        // roof: a gable along x, eaves one block out (z = +-4 at dy 3), the ridge at dy 7
        int slope = 7 - dy; // |z| of the roof row at this height
        if (dy >= 3 && ax <= 5 && az == slope && slope >= 1 && slope <= 4) return dz < 0 ? Piece.ROOF_N : Piece.ROOF_S;
        if (dy == 7 && ax <= 5 && dz == 0) return Piece.ROOF_TOP;
        if (ax > 4 || az > 3) return null;
        boolean wall = ax == 4 || az == 3;
        if (dy <= 3 && wall) {
            if (ax == 4 && az == 3) return Piece.H_LOG;
            if (dz == 3 && dx == 0 && dy <= 2) return dy == 1 ? Piece.H_DOOR_LOWER : Piece.H_DOOR_UPPER;
            int window = dy == 2 ? window(dx, dz) : -1;
            if (window >= 0) return window == broken ? Piece.AIR : az == 3 ? Piece.GLASS_X : Piece.GLASS_Z;
            return Piece.H_PLANKS;
        }
        if (dy >= 4 && ax == 4 && az < slope) return Piece.H_PLANKS; // gable ends under the roof
        if (dy == 3 && az == 3) return Piece.H_PLANKS;
        // inside: an old crafting table, a barrel, cobwebs in two corners
        if (dy == 1 && dx == -3 && dz == -2) return Piece.CRAFTING_TABLE;
        if (dy == 1 && dx == -3 && dz == 2) return Piece.BARREL_LOOT;
        if (dy == 3 && (dx == -3 && dz == -2 || dx == 3 && dz == -2)) return Piece.COBWEB;
        return Piece.AIR;
    }

    /** Window slots (dy 2): 0/1 south wall x -2/+2, 2/3 north wall, 4/5 east and west wall at z 0; -1 = none. */
    static int window(int dx, int dz) {
        if (Math.abs(dx) == 2 && Math.abs(dz) == 3) return (dz > 0 ? 0 : 2) + (dx > 0 ? 1 : 0);
        if (Math.abs(dx) == 4 && dz == 0) return dx > 0 ? 4 : 5;
        return -1;
    }

    static Piece cellar(int dx, int dy, int dz, int ax, int az) {
        if (ax > 4 || az > 3 || dy < CELLAR_FLOOR) return null;
        if (dy == CELLAR_FLOOR) return Piece.STONE_BRICKS;
        if (dx == PLUG_X && az <= 2) {
            // the cellar double door (BACK22): two leaves (placed by the feature), the frame of hazard stripes, the warning light on top
            if ((dz == 0 || dz == DOOR_LEFT_Z) && dy <= -2) return Piece.AIR;
            if (dy == -1 && dz == 0) return Piece.WARNING_LAMP;
            return ((dz + dy) & 1) == 0 ? Piece.MARKING : Piece.HAZARD_BLACK;
        }
        if (ax == 4 || az == 3) return ((dx * 7 + dy * 3 + dz * 5) & 7) == 0 ? Piece.CRACKED_STONE_BRICKS : ((dx + dz * 3 + dy) & 7) == 1 ? Piece.MOSSY_STONE_BRICKS : Piece.STONE_BRICKS;
        if (dx == 3 && dz == 2) return Piece.LADDER_W;
        if (dx == -3 && dy == -2 && dz == -1) return Piece.TAPE_Z;
        if (dx == -3 && dy == -2 && az == 2) return Piece.SIGN_E;
        if (dy == -3 && dz == -2 && (dx == 1 || dx == 2)) return dx == 1 ? Piece.BARREL_LOOT : Piece.COUNTER;
        if (dy == -1 && dx == 0 && dz == 0) return Piece.LANTERN;
        if (dy == -1 && (dx == 2 && dz == -2 || dx == -2 && dz == 2)) return Piece.COBWEB;
        return Piece.AIR;
    }

    /** Behind the cellar door: solid wall (the door leads through portals, not through a passage). */
    static Piece shaft(int dx, int dy, int dz) {
        if (dx != -5 || dy >= 0 || dy < CELLAR_FLOOR || Math.abs(dz) > 2) return null;
        return Piece.STONE_BRICKS;
    }
}
