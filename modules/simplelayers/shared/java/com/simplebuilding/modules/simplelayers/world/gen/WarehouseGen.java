package com.simplebuilding.modules.simplelayers.world.gen;

import static com.simplebuilding.modules.simplelayers.world.gen.GenHash.unit;

/**
 * Level 1, the warehouse (dimension {@code simplelayers:backrooms_warehouse}, BACK18): an endless concrete hall, 8 blocks high,
 * cut into bays of 12 x 12 by 2 x 2 pillars. Bay lines are open (half), a wall with a wide gate, or a solid wall (dead ends).
 * Bays hold storage racks (crates = wood, cobwebs = string), loose crate stacks, nothing at all, a drain (a hole in the floor down
 * to the Poolrooms) or a freight lift (a maintenance door to Level 2, framed in yellow and black). Some walls have a noclip spot
 * (a rift, also to Level 2). Bedrock y 0 and y 11-63.
 */
public final class WarehouseGen {
    public static final int BAY = 12, FLOOR_Y = 1, CEILING_Y = 10, HULL_TOP_Y = 63;

    public enum Edge { OPEN, GATE, WALL }

    public enum Bay { RACKS, PALLETS, EMPTY, DRAIN, LIFT, MEZZANINE, DOCK }

    /** BACK20: the mezzanine's deck (top slabs at y 5, walk on y 6) and the loading dock's top (y 2, walk on y 3). */
    public static final int DECK_Y = 5, DOCK_Y = 2;

    private WarehouseGen() {}

    /** BACK20: bays form region mazes ({@link Maze}, 8 x 8 bays, a quarter of the other lines open): medium hard, the lift at the root. */
    public static final int REGION = 8, SALT = 600;
    public static final double LOOPS = 0.25;

    public static Edge west(long seed, int bx, int bz) {
        return edge(Maze.west(seed, bx, bz, REGION, SALT, LOOPS), unit(seed, bx, bz, 61));
    }

    public static Edge north(long seed, int bx, int bz) {
        return edge(Maze.north(seed, bx, bz, REGION, SALT, LOOPS), unit(seed, bx, bz, 62));
    }

    static Edge edge(boolean passable, double u) {
        return !passable ? Edge.WALL : u < 0.45 ? Edge.OPEN : Edge.GATE;
    }

    public static Bay bay(long seed, int bx, int bz) {
        if (Maze.root(seed, bx, bz, REGION, SALT)) return Bay.LIFT;
        double u = unit(seed, bx, bz, 63);
        if (u < 0.03) return Bay.DRAIN;
        if (u < 0.06) return Bay.EMPTY;
        if (u < 0.14) return Bay.MEZZANINE;
        if (u < 0.20) return Bay.DOCK;
        if (u < 0.60) return Bay.RACKS;
        return u < 0.80 ? Bay.PALLETS : Bay.EMPTY;
    }

    public static boolean dark(long seed, int bx, int bz) {
        return unit(seed, bx, bz, 64) < 0.08;
    }

    /** Racks bay with a supply chest (placed by the cache feature at {@link #cache}); every mezzanine keeps one up on its deck. */
    public static boolean hasCache(long seed, int bx, int bz) {
        Bay bay = bay(seed, bx, bz);
        return LobbyGen.supplies && (bay == Bay.RACKS && unit(seed, bx, bz, 65) < 0.35 || bay == Bay.MEZZANINE);
    }

    /** Bay-local position of the cache chest (west end of the middle aisle). */
    public static final int[] CACHE = {2, FLOOR_Y + 1, 6};
    /** BACK20: the mezzanine's chest, up on the deck at the back wall. */
    public static final int[] DECK_CACHE = {9, DECK_Y + 1, 10};

    /** Bay-local cache position of a bay that has one. */
    public static int[] cache(long seed, int bx, int bz) {
        return bay(seed, bx, bz) == Bay.MEZZANINE ? DECK_CACHE : CACHE;
    }

    static Piece crate(long seed, int x, int y, int z) {
        return unit(seed, x, z, 70 + y) < 0.5 ? Piece.CRATE_OAK : Piece.CRATE_SPRUCE;
    }

    public static Piece at(long seed, int x, int y, int z) {
        if (y < 0 || y > HULL_TOP_Y) return Piece.AIR;
        if (y == 0 || y > CEILING_Y) return Piece.BEDROCK;
        int bx = Math.floorDiv(x, BAY), bz = Math.floorDiv(z, BAY), lx = Math.floorMod(x, BAY), lz = Math.floorMod(z, BAY);
        if (lx <= 1 && lz <= 1) return y == FLOOR_Y ? Piece.CONCRETE_FLOOR : y == CEILING_Y ? Piece.CONCRETE_CEILING : Piece.PILLAR;
        Bay bay = bay(seed, bx, bz);
        if (y == FLOOR_Y) return floor(seed, bay, lx, lz);
        if (y == CEILING_Y) {
            if (lz == 6 && lx >= 3 && lx <= 9 && (lx & 1) == 1) {
                double u = unit(seed, x, z, 66);
                if (dark(seed, bx, bz) || u < 0.08) return Piece.NEON_BROKEN;
                return u < 0.16 ? Piece.NEON_FLICKER : Piece.NEON_ON;
            }
            return Piece.CONCRETE_CEILING;
        }
        if (lx == 0 || lz == 0) {
            Edge edge = lx == 0 ? west(seed, bx, bz) : north(seed, bx, bz);
            int along = lx == 0 ? lz : lx;
            if (edge == Edge.OPEN) return Piece.AIR;
            if (edge == Edge.GATE && along >= 4 && along <= 9 && y <= 7) return Piece.AIR;
            if (edge == Edge.WALL && along == 6 && y <= 3 && unit(seed, bx, bz, lx == 0 ? 67 : 68) < 0.12) return Piece.RIFT;
            return y == 2 ? Piece.MARKING : Piece.CONCRETE_WALL;
        }
        return content(seed, bay, bx, bz, x, y, z, lx, lz);
    }

    static Piece floor(long seed, Bay bay, int lx, int lz) {
        if (bay == Bay.DRAIN && lx >= 8 && lx <= 10 && lz >= 8 && lz <= 10) {
            if (lx == 9 && lz == 9) return Piece.RIFT;
            return ((lx + lz) & 1) == 0 ? Piece.HAZARD_BLACK : Piece.MARKING;
        }
        if (bay == Bay.RACKS && (lz == 5 || lz == 7) && lx >= 3 && lx <= 9) return Piece.MARKING;
        return Piece.CONCRETE_FLOOR;
    }

    static Piece content(long seed, Bay bay, int bx, int bz, int x, int y, int z, int lx, int lz) {
        switch (bay) {
            case RACKS -> {
                if ((lz == 4 || lz == 8) && lx >= 3 && lx <= 9) {
                    if (lx == 3 || lx == 9) return y <= 7 ? Piece.RACK_POST : Piece.AIR;
                    if (y == 3 || y == 5) return Piece.SHELF;
                    double u = unit(seed, x, z, 80 + y);
                    if (y == 2) return u < 0.8 ? crate(seed, x, y, z) : Piece.AIR;
                    if (y == 4) return u < 0.6 ? crate(seed, x, y, z) : Piece.AIR;
                    if (y == 6) return u < 0.35 ? crate(seed, x, y, z) : u < 0.5 && LobbyGen.supplies ? Piece.COBWEB : Piece.AIR;
                }
                return Piece.AIR;
            }
            case PALLETS -> {
                boolean group = (lx >= 3 && lx <= 4 && lz >= 3 && lz <= 4) || (lx >= 8 && lx <= 9 && lz >= 8 && lz <= 9)
                        || (lx >= 8 && lx <= 9 && lz >= 3 && lz <= 4 && unit(seed, bx, bz, 81) < 0.5);
                if (!group) return Piece.AIR;
                int height = 1 + (int) (unit(seed, x, z, 82) * 3);
                if (y < 2 + height) return crate(seed, x, y, z);
                if (y == 2 + height && LobbyGen.supplies && unit(seed, x, z, 83) < 0.2) return Piece.COBWEB;
                return Piece.AIR;
            }
            case MEZZANINE -> {
                // A deck over the back half of the bay (lz 7-11), a railing of bars at its edge, three posts under it and a
                // ladder up the middle post; crates on the deck and under it.
                if (lz >= 7 && y == DECK_Y) return lx == 6 && lz == 7 ? Piece.PILLAR : Piece.DECK;
                if (lz == 7 && y < DECK_Y && (lx == 3 || lx == 6 || lx == 9)) return Piece.PILLAR;
                if (lz == 6 && lx == 6 && y < DECK_Y + 1) return Piece.LADDER_N;
                if (lz == 7 && y == DECK_Y + 1) return lx == 6 ? Piece.AIR : Piece.RACK_POST;
                if (lz == 11 && lx <= 4 && (y == DECK_Y + 1 || y == DECK_Y + 2 && lx <= 2)) return crate(seed, x, y, z);
                if (lz >= 10 && lx >= 8 && y == FLOOR_Y + 1 && unit(seed, x, z, 84) < 0.6) return crate(seed, x, y, z);
                return Piece.AIR;
            }
            case DOCK -> {
                // A loading dock over the front of the bay (lz 1-5): one block up, edged by slabs as a step all round, a few
                // crates waiting on it.
                if (lz <= 5 && y == DOCK_Y) return lx == 1 || lx == 11 || lz == 1 || lz == 5 ? Piece.STONE_SLAB : Piece.CONCRETE_FLOOR;
                if (lz >= 2 && lz <= 3 && lx >= 3 && lx <= 4 && y > DOCK_Y && y <= DOCK_Y + 1 + (int) (unit(seed, x, z, 85) * 2)) return crate(seed, x, y, z);
                return Piece.AIR;
            }
            case LIFT -> {
                // A freight lift: a wall across the bay with a maintenance door, framed in yellow and black.
                if (lz == 3 && lx >= 4 && lx <= 8 && y <= 6) {
                    if (lx == 6 && y == 2) return Piece.LEVEL_DOOR_LOWER;
                    if (lx == 6 && y == 3) return Piece.LEVEL_DOOR_UPPER;
                    if ((lx == 5 || lx == 7) && y <= 4 || lx == 6 && y == 4) return ((lx + y) & 1) == 0 ? Piece.HAZARD_BLACK : Piece.MARKING;
                    return Piece.CONCRETE_WALL;
                }
                return Piece.AIR;
            }
            default -> {
                return Piece.AIR;
            }
        }
    }

    /** Arrival: the bay centre at y 2 (always the free middle aisle; in front of a mezzanine's ladder). */
    public static int[] arrival(long seed, int x, int z) {
        int bx = Math.floorDiv(x, BAY), bz = Math.floorDiv(z, BAY);
        return new int[] {bx * BAY + 6, FLOOR_Y + 1, bz * BAY + (bay(seed, bx, bz) == Bay.MEZZANINE ? 4 : 6)};
    }
}
