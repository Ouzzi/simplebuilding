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

    public enum Bay { RACKS, PALLETS, EMPTY, DRAIN, LIFT }

    private WarehouseGen() {}

    public static Edge west(long seed, int bx, int bz) {
        return edge(unit(seed, bx, bz, 61));
    }

    public static Edge north(long seed, int bx, int bz) {
        return edge(unit(seed, bx, bz, 62));
    }

    static Edge edge(double u) {
        return u < 0.5 ? Edge.OPEN : u < 0.88 ? Edge.GATE : Edge.WALL;
    }

    public static Bay bay(long seed, int bx, int bz) {
        double u = unit(seed, bx, bz, 63);
        if (u < 0.03) return Bay.DRAIN;
        if (u < 0.06) return Bay.LIFT;
        if (u < 0.56) return Bay.RACKS;
        return u < 0.80 ? Bay.PALLETS : Bay.EMPTY;
    }

    public static boolean dark(long seed, int bx, int bz) {
        return unit(seed, bx, bz, 64) < 0.08;
    }

    /** Racks bay with a supply chest (placed by the cache feature at {@link #cache}). */
    public static boolean hasCache(long seed, int bx, int bz) {
        return LobbyGen.supplies && bay(seed, bx, bz) == Bay.RACKS && unit(seed, bx, bz, 65) < 0.35;
    }

    /** Bay-local position of the cache chest (west end of the middle aisle). */
    public static final int[] CACHE = {2, FLOOR_Y + 1, 6};

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

    /** Arrival: the bay centre at y 2 (always the free middle aisle). */
    public static int[] arrival(long seed, int x, int z) {
        return new int[] {Math.floorDiv(x, BAY) * BAY + 6, FLOOR_Y + 1, Math.floorDiv(z, BAY) * BAY + 6};
    }
}
