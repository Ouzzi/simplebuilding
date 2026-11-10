package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * The Backrooms entrance in the Overworld since BACK20 (plan docs/ai/PLAN-B-BACK20-2026-11-10.md): an abandoned cottage in
 * Vanilla style (cobblestone footing, plank walls, log corners, glass panes, a door, a gable roof of stairs) with a cellar under it
 * (trapdoor and ladder, stone bricks, barrels, cobwebs, a lantern). The cellar's west wall is concreted shut: a grey concrete plug
 * in a yellow-and-black frame, caution tape across it, caution signs at both sides and a warning light above - clearly dangerous.
 * Behind the plug the old maintenance shaft of BACK18 survives underground (a collar of hazard stripes, a ladder down to a floor of
 * rifts on glitched bedrock = the portal into Level 0); the shaft is the reason the cellar was sealed.
 *
 * <p>Relative to the house centre on the ground ({@code dy 0} = the floor, built into the ground's top block). {@link #piece}
 * returns null where the terrain stays. The wood pieces ({@code H_*}, roof) take the biome's wood in the feature.
 */
public final class HouseTemplate {
    /** Footprint: x -9 (shaft) .. 5 (roof), z -4 .. 4; the house walls at |x| = 4, |z| = 3. */
    public static final int MIN_X = -9, MAX_X = 5, HALF_Z = 4, DEPTH = 14, HEIGHT = 7, PORTAL_DY = -13, CELLAR_FLOOR = -4;
    /** The plug and the shaft centre (x), the shaft's floor of rifts. */
    public static final int PLUG_X = -4, SHAFT_X = -7;

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
            // the sealed passage: plug 3 wide x 2 high, the frame of hazard stripes, the warning light on top
            if (az <= 1 && dy <= -2) return Piece.PAD;
            if (dy == -1 && dz == 0) return Piece.WARNING_LAMP;
            return ((dz + dy) & 1) == 0 ? Piece.MARKING : Piece.HAZARD_BLACK;
        }
        if (ax == 4 || az == 3) return ((dx * 7 + dy * 3 + dz * 5) & 7) == 0 ? Piece.CRACKED_STONE_BRICKS : ((dx + dz * 3 + dy) & 7) == 1 ? Piece.MOSSY_STONE_BRICKS : Piece.STONE_BRICKS;
        if (dx == 3 && dz == 2) return Piece.LADDER_W;
        if (dx == -3 && dy == -2 && az <= 1) return Piece.TAPE_Z;
        if (dx == -3 && dy == -2 && az == 2) return Piece.SIGN_E;
        if (dy == -3 && dz == -2 && (dx == 1 || dx == 2)) return dx == 1 ? Piece.BARREL_LOOT : Piece.COUNTER;
        if (dy == -1 && dx == 0 && dz == 0) return Piece.LANTERN;
        if (dy == -1 && (dx == 2 && dz == -2 || dx == -2 && dz == 2)) return Piece.COBWEB;
        return Piece.AIR;
    }

    /** Behind the plug: a short passage (x -5), the shaft (x -8 .. -6) down to the rift floor. */
    static Piece shaft(int dx, int dy, int dz) {
        int az = Math.abs(dz);
        if (dy >= 0) return null;
        if (dx == -5) {
            if (az <= 1 && dy >= -3 && dy <= -2) return Piece.AIR;
            if (az <= 2 && dy >= CELLAR_FLOOR && dy <= -1) return dy == CELLAR_FLOOR && az <= 1 ? (((dz) & 1) == 0 ? Piece.MARKING : Piece.HAZARD_BLACK) : Piece.STONE_BRICKS;
            return null;
        }
        int sx = dx - SHAFT_X, ring = Math.max(Math.abs(sx), az);
        if (ring > 2 || dy < -DEPTH || dy > -1) return null;
        if (ring == 2) {
            return ((dx * 7 + dy * 3 + dz * 5) & 7) == 0 ? Piece.CRACKED_STONE_BRICKS : Piece.STONE_BRICKS;
        }
        if (dy == -DEPTH) return Piece.GLITCHED_BEDROCK;
        if (dy == PORTAL_DY) return Piece.RIFT;
        if (dy == -1) return Piece.STONE_BRICKS;
        if (sx == 0 && dz == -1 && dy <= -2) return Piece.LADDER_S;
        return Piece.AIR;
    }
}
