package com.simplebuilding.modules.simplelayers.world.gen;

import static com.simplebuilding.modules.simplelayers.world.gen.GenHash.unit;

/**
 * The Poolrooms (dimension {@code simplelayers:backrooms_pools}, BACK18): calm, bright, tiled. Rooms of 10 x 10 behind
 * arched openings, low (ceiling y 8) or tall (y 12); a tiled walkway (y 2) around shallow pools (water y 2 over pool tiles y 1),
 * flooded rooms, column halls with a water channel, empty tiled rooms. Sea lanterns light every room. An escape tree per region
 * leads to the region's exit room (Exit Door back to the Overworld, a supply chest). Bedrock y 0 and above the ceiling.
 */
public final class PoolsGen {
    public static final int ROOM = 10, REGION = 8, WALK_Y = 3, HULL_TOP_Y = 63, SALT = 500;
    /** BACK20: region maze with many loops - calm and open, yet confusing. */
    public static final double LOOPS = 0.4;

    public enum Room { POOL, DEEP, FLOODED, COLUMNS, DRY, EXIT, GALLERY, TERRACE }

    /** BACK20: the gallery's balcony floor (walk on y 8). */
    public static final int BALCONY_Y = WALK_Y + 4;

    private PoolsGen() {}

    public static boolean west(long seed, int rx, int rz) {
        return Maze.west(seed, rx, rz, REGION, SALT + 21, LOOPS);
    }

    public static boolean north(long seed, int rx, int rz) {
        return Maze.north(seed, rx, rz, REGION, SALT + 21, LOOPS);
    }

    /** Some open lines have no wall at all (only the corner columns stay): rooms merge into pillared halls. */
    static boolean noWall(long seed, int rx, int rz, int side) {
        return unit(seed, rx, rz, SALT + 5 + side) < 0.22;
    }

    /** POOL rooms: a square basin or a lane across the room (east-west or north-south). */
    static int shape(long seed, int rx, int rz) {
        return (int) (unit(seed, rx, rz, SALT + 7) * 3);
    }

    public static Room room(long seed, int rx, int rz) {
        if (GenHash.root(seed, rx, rz, REGION, SALT + 21)) return Room.EXIT;
        double u = unit(seed, rx, rz, SALT + 3);
        if (u < 0.08) return Room.GALLERY;
        if (u < 0.16) return Room.TERRACE;
        if (u < 0.42) return Room.POOL;
        if (u < 0.56) return Room.DEEP;
        if (u < 0.72) return Room.FLOODED;
        return u < 0.88 ? Room.COLUMNS : Room.DRY;
    }

    public static int ceiling(long seed, int rx, int rz) {
        Room room = room(seed, rx, rz);
        return room == Room.COLUMNS || room == Room.GALLERY || room != Room.EXIT && unit(seed, rx, rz, SALT + 4) < 0.3 ? 13 : 9;
    }

    /** The exit room's supply chest (cache feature), room-local. */
    public static final int[] CACHE = {8, WALK_Y + 1, 8};

    public static Piece at(long seed, int x, int y, int z) {
        if (y < 0 || y > HULL_TOP_Y) return Piece.AIR;
        if (y == 0) return Piece.BEDROCK;
        int rx = Math.floorDiv(x, ROOM), rz = Math.floorDiv(z, ROOM), lx = Math.floorMod(x, ROOM), lz = Math.floorMod(z, ROOM);
        int top = ceiling(seed, rx, rz);
        if (lx == 0 || lz == 0) {
            // A wall line takes the higher ceiling of its two rooms so no tall room shows a gap.
            int other = lx == 0 ? ceiling(seed, rx - 1, rz) : ceiling(seed, rx, rz - 1);
            if (lx == 0 && lz == 0) other = Math.max(other, Math.max(ceiling(seed, rx - 1, rz - 1), ceiling(seed, rx, rz - 1)));
            int wallTop = Math.max(top, other);
            if (y > wallTop) return Piece.BEDROCK;
            if (y <= WALK_Y || y == wallTop) return Piece.TILE;
            if (lx == 0 && lz == 0) return Piece.TILE_COLUMN;
            boolean open = lx == 0 ? west(seed, rx, rz) : north(seed, rx, rz);
            int along = lx == 0 ? lz : lx;
            if (open && noWall(seed, rx, rz, lx == 0 ? 0 : 1) && along > 0) {
                if (y == WALK_Y) return Piece.TILE_FLOOR;
                return y < WALK_Y ? Piece.TILE : Piece.AIR;
            }
            if (open && along >= 3 && along <= 6) {
                if (y <= WALK_Y + 3) return Piece.AIR;
                if (y == WALK_Y + 4 && (along == 3 || along == 6)) {
                    if (lx == 0) return along == 3 ? Piece.ARCH_N : Piece.ARCH_S;
                    return along == 3 ? Piece.ARCH_W : Piece.ARCH_E;
                }
                if (y == WALK_Y + 4) return Piece.AIR;
            }
            return Piece.TILE;
        }
        if (y > top) return Piece.BEDROCK;
        Room room = room(seed, rx, rz);
        if (y == top) return (lx == 3 || lx == 7) && (lz == 3 || lz == 7) ? Piece.SEA_LANTERN : Piece.TILE;
        boolean walk = lx <= 2 || lx >= 8 || lz <= 2 || lz >= 8;
        if (room == Room.POOL) {
            int shape = shape(seed, rx, rz);
            if (shape == 1) walk = lz <= 3 || lz >= 7;
            else if (shape == 2) walk = lx <= 3 || lx >= 7;
        }
        boolean pool = !walk;
        if (room == Room.GALLERY) {
            // a tall hall with a balcony along the north wall (lz 1-3, a parapet at its edge) and a stair up the east walkway
            if (lz <= 3 && y == BALCONY_Y) return Piece.TILE_FLOOR;
            if (lz == 3 && y == BALCONY_Y + 1 && lx <= 8) return Piece.TILE;
            if (lx == 9 && lz >= 4 && lz <= 7 && y == WALK_Y + 1 + (7 - lz)) return Piece.STAIR_N;
        }
        if (room == Room.TERRACE && !walk && y < WALK_Y) {
            // steps down into the water: shallow in the west, two deep in the east
            int floor = lx <= 4 ? WALK_Y - 1 : WALK_Y - 2;
            return y < floor ? Piece.TILE : y == floor ? Piece.POOL_TILE : Piece.WATER;
        }
        if (y < WALK_Y - 1) return room == Room.DEEP && pool ? Piece.POOL_TILE : Piece.TILE;
        switch (room) {
            case POOL -> {
                if (y == WALK_Y - 1) return pool ? Piece.POOL_TILE : Piece.TILE;
                if (y == WALK_Y) return pool ? Piece.WATER : Piece.TILE_FLOOR;
            }
            case DEEP -> {
                // two blocks deep: swim, the pool tiles one further down
                if (y == WALK_Y - 1 || y == WALK_Y) return pool ? Piece.WATER : y == WALK_Y ? Piece.TILE_FLOOR : Piece.TILE;
            }
            case FLOODED -> {
                if (y == WALK_Y - 1) return Piece.POOL_TILE;
                if (y == WALK_Y) return Piece.WATER;
            }
            case COLUMNS -> {
                if (y == WALK_Y - 1) return lz == 5 ? Piece.POOL_TILE : Piece.TILE;
                if (y == WALK_Y) return lz == 5 ? Piece.WATER : Piece.TILE_FLOOR;
                if ((lx == 3 || lx == 7) && (lz == 3 || lz == 7)) return Piece.TILE_COLUMN;
            }
            case GALLERY, TERRACE -> {
                if (y == WALK_Y - 1) return pool ? (room == Room.GALLERY ? Piece.POOL_TILE : Piece.WATER) : Piece.TILE;
                if (y == WALK_Y) return pool ? Piece.WATER : Piece.TILE_FLOOR;
            }
            case DRY -> {
                if (y == WALK_Y - 1) return Piece.TILE;
                if (y == WALK_Y) return Piece.TILE_FLOOR;
            }
            case EXIT -> {
                if (y == WALK_Y - 1) return Piece.TILE;
                if (y == WALK_Y) return Piece.TILE_FLOOR;
                if (lz == 5 && lx >= 3 && lx <= 7 && y <= WALK_Y + 5) {
                    if (lx == 5 && y == WALK_Y + 1) return Piece.EXIT_DOOR_LOWER;
                    if (lx == 5 && y == WALK_Y + 2) return Piece.EXIT_DOOR_UPPER;
                    if (lx == 5 && y == WALK_Y + 3) return Piece.EXIT_SIGN;
                    return Piece.TILE;
                }
            }
        }
        return Piece.AIR;
    }

    /** Arrival: on the west walkway of the room at (x, z), y 3 (in shallow water in a flooded room). */
    public static int[] arrival(long seed, int x, int z) {
        return new int[] {Math.floorDiv(x, ROOM) * ROOM + 2, WALK_Y + 1, Math.floorDiv(z, ROOM) * ROOM + 6};
    }
}
