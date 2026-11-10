package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * The Backrooms entrance in the Overworld (BACK18): a fenced-off maintenance shaft. Relative to the shaft centre on the ground
 * (dy 0 = the pad): a concrete pad 7 x 7 on a gravel apron 11 x 11, a 3 x 3 shaft with a yellow-and-black collar, stone brick
 * walls and a ladder down to a floor of rifts (the portal, dy {@link #PORTAL_DY}) on glitched bedrock; around it a barrier of
 * posts and caution tape (jumpable) with caution signs facing out and red warning lights (lit copper bulbs) on the
 * corners, a hoist frame with a chain over the shaft. {@link #piece} returns null where the terrain stays.
 */
public final class ShaftTemplate {
    public static final int RADIUS = 6, DEPTH = 13, HEIGHT = 4, PORTAL_DY = -12;

    private ShaftTemplate() {}

    public static Piece piece(int dx, int dy, int dz) {
        int ax = Math.abs(dx), az = Math.abs(dz), ring = Math.max(ax, az);
        if (ring > RADIUS || dy < -DEPTH || dy > HEIGHT) return null;
        boolean shaft = ax <= 1 && az <= 1, wall = ring == 2;
        if (dy < 0) {
            if (shaft) {
                if (dy == -DEPTH) return Piece.GLITCHED_BEDROCK;
                if (dy == PORTAL_DY) return Piece.RIFT;
                return dx == 0 && dz == -1 ? Piece.LADDER_S : Piece.AIR;
            }
            if (wall) return ((dx * 7 + dy * 3 + dz * 5) & 7) == 0 ? Piece.CRACKED_STONE_BRICKS : Piece.STONE_BRICKS;
            return ring <= 5 && dy >= -2 ? Piece.STONE : null;
        }
        if (dy == 0) {
            if (shaft) return dx == 0 && dz == -1 ? Piece.LADDER_S : Piece.AIR;
            if (wall) return ((dx + dz) & 1) == 0 ? Piece.MARKING : Piece.HAZARD_BLACK;
            if (ring <= 3) return Piece.PAD;
            return ring <= 5 ? Piece.GRAVEL : null;
        }
        // Above the ground: a hoist frame with a chain over the shaft, the barrier on ring 5, signs on ring 6, air elsewhere.
        if (dz == 0 && ax == 2 && dy <= 3 || dz == 0 && ax <= 2 && dy == 4) return Piece.POST;
        if (dx == 0 && dz == 0 && dy <= 3) return Piece.CHAIN;
        if (ring == 5) {
            boolean corner = ax == 5 && az == 5, post = corner || ax == 0 || az == 0;
            if (post) {
                if (corner) return dy <= 2 ? Piece.POST : dy == 3 ? Piece.WARNING_LAMP : Piece.AIR;
                return dy <= 2 ? Piece.POST : Piece.AIR;
            }
            if (dy == 1) return az == 5 ? Piece.TAPE_X : Piece.TAPE_Z;
            return Piece.AIR;
        }
        if (ring == 6) {
            if (dy != 2) return null;
            if (dx == 0 && dz == -6) return Piece.SIGN_N;
            if (dx == 0 && dz == 6) return Piece.SIGN_S;
            if (dz == 0 && dx == 6) return Piece.SIGN_E;
            if (dz == 0 && dx == -6) return Piece.SIGN_W;
            return null;
        }
        return Piece.AIR;
    }
}
