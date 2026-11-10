package com.simplebuilding.modules.simplelayers.world.gen;

import static com.simplebuilding.modules.simplelayers.world.gen.GenHash.unit;

/**
 * Level 2, the pipe corridors (dimension {@code simplelayers:backrooms_pipes}, BACK18): narrow, warm, dim. Two storeys (floor
 * y 1 / ceiling y 6 and floor y 8 / ceiling y 13, bedrock between and above) of 3-wide corridors on a 6-block node grid; about
 * half the links are open plus an escape tree per region, so there are many dead ends but every node reaches its region's
 * root. Copper pipes run under the ceiling, copper bulbs light some junctions. Ladder shafts join the storeys; holes in the floor
 * drop to the Poolrooms; the root of every ground-storey region holds the sealed maintenance hatch to the exit level, opened by
 * three valve levers in the right positions ({@link #pattern}).
 */
public final class PipesGen {
    public static final int NODE = 6, REGION = 8, STOREY = 7, HULL_TOP_Y = 63, SALT = 300;

    public enum Node { PLAIN, SHAFT, HOLE, HATCH }

    private PipesGen() {}

    static int salt(int storey) {
        return SALT + storey * 50;
    }

    public static boolean west(long seed, int storey, int nx, int nz) {
        return GenHash.forcedWest(seed, nx, nz, REGION, salt(storey) + 21) || unit(seed, nx, nz, salt(storey) + 1) < 0.22;
    }

    public static boolean north(long seed, int storey, int nx, int nz) {
        return GenHash.forcedNorth(seed, nx, nz, REGION, salt(storey) + 21) || unit(seed, nx, nz, salt(storey) + 2) < 0.22;
    }

    public static Node node(long seed, int storey, int nx, int nz) {
        if (GenHash.root(seed, nx, nz, REGION, salt(storey) + 21)) return storey == 0 ? Node.HATCH : Node.PLAIN;
        if (storey == 1) return Node.PLAIN;
        double u = unit(seed, nx, nz, SALT + 3);
        if (u < 0.07) return Node.SHAFT;
        return u < 0.095 ? Node.HOLE : Node.PLAIN;
    }

    /** Open links of a node (0-4): one means a dead end. */
    public static int links(long seed, int storey, int nx, int nz) {
        return (west(seed, storey, nx, nz) ? 1 : 0) + (west(seed, storey, nx + 1, nz) ? 1 : 0)
                + (north(seed, storey, nx, nz) ? 1 : 0) + (north(seed, storey, nx, nz + 1) ? 1 : 0);
    }

    /** Dead ends sometimes keep a supply chest (cache feature, at {@link #CACHE} on the storey's floor + 1). */
    public static boolean hasCache(long seed, int storey, int nx, int nz) {
        return LobbyGen.supplies && node(seed, storey, nx, nz) == Node.PLAIN && links(seed, storey, nx, nz) == 1
                && unit(seed, nx, nz, salt(storey) + 4) < 0.3;
    }

    public static final int[] CACHE = {2, 2, 4};

    /** The hatch's valve pattern: bit 0 north lever, bit 1 east, bit 2 west (1 = on); never all off. */
    public static int pattern(long seed, int nx, int nz) {
        int p = (int) (unit(seed, nx, nz, SALT + 9) * 7) + 1;
        return Math.min(7, Math.max(1, p));
    }

    static boolean lit(long seed, int storey, int nx, int nz) {
        return unit(seed, nx, nz, salt(storey) + 5) < (storey == 0 ? 0.85 : 0.55);
    }

    public static Piece at(long seed, int x, int y, int z) {
        if (y < 0 || y > HULL_TOP_Y) return Piece.AIR;
        int nx = Math.floorDiv(x, NODE), nz = Math.floorDiv(z, NODE), lx = Math.floorMod(x, NODE), lz = Math.floorMod(z, NODE);
        Node ground = node(seed, 0, nx, nz);
        if (ground == Node.SHAFT) {
            if (lx == 2 && lz == 2 && y >= 2 && y <= 12) return Piece.COPPER_BOX;
            if (lx == 3 && lz == 2 && y >= 2 && y <= 9) return Piece.LADDER_E;
        }
        if (y == 0 || y == STOREY) return Piece.BEDROCK;
        int storey = y < STOREY ? 0 : 1;
        if (storey == 1 && y > STOREY + 6) return Piece.BEDROCK;
        return storeyAt(seed, storey, storey == 0 ? ground : node(seed, 1, nx, nz), nx, nz, x, y - storey * STOREY, z, lx, lz);
    }

    static boolean open(long seed, int storey, int nx, int nz, int lx, int lz) {
        boolean coreX = lx >= 2 && lx <= 4, coreZ = lz >= 2 && lz <= 4;
        if (coreX && coreZ) return true;
        if (coreZ && lx <= 1) return west(seed, storey, nx, nz);
        if (coreZ && lx == 5) return west(seed, storey, nx + 1, nz);
        if (coreX && lz <= 1) return north(seed, storey, nx, nz);
        if (coreX && lz == 5) return north(seed, storey, nx, nz + 1);
        return false;
    }

    /** One storey; ly = 1 floor, 2-5 corridor, 6 ceiling. */
    static Piece storeyAt(long seed, int storey, Node node, int nx, int nz, int x, int ly, int z, int lx, int lz) {
        boolean core = lx >= 2 && lx <= 4 && lz >= 2 && lz <= 4;
        if (ly == 1) {
            if (node == Node.HOLE && lx == 4 && lz == 4) return Piece.RIFT;
            if (core && lx == 3 && lz == 3 && unit(seed, nx, nz, salt(storey) + 6) < 0.4) return Piece.GRATE;
            return open(seed, storey, nx, nz, lx, lz) ? Piece.PIPE_FLOOR : Piece.PIPE_WALL;
        }
        if (ly == 6) {
            if (lx == 3 && lz == 3 && lit(seed, storey, nx, nz)) return Piece.BULB;
            return Piece.PIPE_CEILING;
        }
        if (!open(seed, storey, nx, nz, lx, lz)) return Piece.PIPE_WALL;
        if (node == Node.HATCH) {
            if (lx == 3 && lz == 3 && ly <= 4) return ly == 2 ? Piece.SEALED_DOOR_LOWER : ly == 3 ? Piece.SEALED_DOOR_UPPER : Piece.COPPER_BOX;
            if (ly == 2 && lx == 3 && lz == 2) return Piece.LEVER_N;
            if (ly == 2 && lx == 4 && lz == 3) return Piece.LEVER_E;
            if (ly == 2 && lx == 2 && lz == 3) return Piece.LEVER_W;
        }
        if (ly == 5) {
            // A copper duct along the middle of every link, ending in a ring of boxes around the junction (light from the
            // bulb above falls through its free centre); thin pipes along both walls.
            if (core) return lx == 3 && lz == 3 ? Piece.AIR : Piece.COPPER_BOX;
            boolean alongX = lx <= 1 || lx == 5;
            if (alongX) return lz == 3 ? Piece.COPPER_BOX : Piece.PIPE_X;
            return lx == 3 ? Piece.COPPER_BOX : Piece.PIPE_Z;
        }
        if (ly == 4 && !core) {
            boolean alongX = lx <= 1 || lx == 5;
            if (unit(seed, nx, nz, salt(storey) + 8 + (alongX ? 0 : 1)) < 0.6) {
                if (alongX && (lz == 2 || lz == 4)) return Piece.PIPE_X;
                if (!alongX && (lx == 2 || lx == 4)) return Piece.PIPE_Z;
            }
        }
        if (ly == 2 && !core && unit(seed, nx, nz, salt(storey) + 7 + (lx <= 1 || lx == 5 ? 0 : 1)) < 0.3) {
            // a low pipe along one wall of some links (step over it)
            if ((lx <= 1 || lx == 5) && lz == 2) return Piece.PIPE_X;
            if ((lz <= 1 || lz == 5) && lx == 2) return Piece.PIPE_Z;
        }
        return Piece.AIR;
    }

    /** Arrival: the junction of the node at (x, z), ground storey; south of the hatch in a hatch node. */
    public static int[] arrival(long seed, int x, int z) {
        int nx = Math.floorDiv(x, NODE), nz = Math.floorDiv(z, NODE);
        return new int[] {nx * NODE + 3, 2, nz * NODE + (node(seed, 0, nx, nz) == Node.HATCH ? 4 : 3)};
    }
}
