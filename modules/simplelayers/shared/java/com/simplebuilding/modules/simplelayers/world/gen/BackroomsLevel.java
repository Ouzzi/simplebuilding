package com.simplebuilding.modules.simplelayers.world.gen;

/**
 * The Backrooms levels (BACK18, plan docs/ai/PLAN-B-BACK18-2026-11-10.md), each its own dimension of the same chunk generator
 * ({@code simplelayers:backrooms} with field {@code level}). Ways between them keep x/z: noclip rifts in walls and maintenance
 * doors lead one level deeper (Lobby → Warehouse → Pipes; the Pipes' sealed hatch → the exit level), holes in the floor drop into
 * the Poolrooms. Exit Doors in the Lobby, the Poolrooms and the exit level lead back to the Overworld.
 */
public enum BackroomsLevel {
    LOBBY("lobby", "backrooms"),
    WAREHOUSE("warehouse", "backrooms_warehouse"),
    PIPES("pipes", "backrooms_pipes"),
    POOLS("pools", "backrooms_pools"),
    END("end", "backrooms_end");

    /** Codec value of the generator field {@code level}. */
    public final String id;
    /** Path of the dimension id in namespace {@code simplelayers}. */
    public final String dimension;

    BackroomsLevel(String id, String dimension) {
        this.id = id;
        this.dimension = dimension;
    }

    public static BackroomsLevel byId(String id) {
        for (BackroomsLevel l : values()) if (l.id.equals(id)) return l;
        return LOBBY;
    }

    public static BackroomsLevel byDimension(String path) {
        for (BackroomsLevel l : values()) if (l.dimension.equals(path)) return l;
        return null;
    }

    public Piece at(long seed, int x, int y, int z) {
        return switch (this) {
            case LOBBY -> LobbyGen.at(LobbyGen.LOBBY, seed, x, y, z);
            case WAREHOUSE -> WarehouseGen.at(seed, x, y, z);
            case PIPES -> PipesGen.at(seed, x, y, z);
            case POOLS -> PoolsGen.at(seed, x, y, z);
            case END -> LobbyGen.at(LobbyGen.END, seed, x, y, z);
        };
    }

    /** Free standing position {x, y, z} for an arrival above (x, z). */
    public int[] arrival(long seed, int x, int z) {
        return switch (this) {
            case LOBBY -> LobbyGen.arrival(LobbyGen.LOBBY, seed, x, z);
            case WAREHOUSE -> WarehouseGen.arrival(seed, x, z);
            case PIPES -> PipesGen.arrival(seed, x, z);
            case POOLS -> PoolsGen.arrival(seed, x, z);
            case END -> LobbyGen.arrival(LobbyGen.END, seed, x, z);
        };
    }

    /**
     * Supply chests (the cache feature) whose position lies in the square [x0, x0 + size) x [z0, z0 + size): {x, y, z} each.
     * Warehouse racks, dead ends of the Pipes, the exit rooms of the Poolrooms and the exit level; none in the Lobby (it has its
     * storage rooms).
     */
    public java.util.List<int[]> caches(long seed, int x0, int z0, int size) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        int cell = switch (this) {
            case WAREHOUSE -> WarehouseGen.BAY;
            case PIPES -> PipesGen.NODE;
            case POOLS -> PoolsGen.ROOM;
            default -> LobbyGen.CELL;
        };
        for (int cx = Math.floorDiv(x0, cell); cx <= Math.floorDiv(x0 + size - 1, cell); cx++) {
            for (int cz = Math.floorDiv(z0, cell); cz <= Math.floorDiv(z0 + size - 1, cell); cz++) {
                switch (this) {
                    case WAREHOUSE -> {
                        if (WarehouseGen.hasCache(seed, cx, cz)) add(out, x0, z0, size, cx * cell + WarehouseGen.CACHE[0], WarehouseGen.CACHE[1], cz * cell + WarehouseGen.CACHE[2]);
                    }
                    case PIPES -> {
                        for (int storey = 0; storey < 2; storey++) {
                            if (PipesGen.hasCache(seed, storey, cx, cz)) {
                                add(out, x0, z0, size, cx * cell + PipesGen.CACHE[0], storey * PipesGen.STOREY + PipesGen.CACHE[1], cz * cell + PipesGen.CACHE[2]);
                            }
                        }
                    }
                    case POOLS -> {
                        if (PoolsGen.room(seed, cx, cz) == PoolsGen.Room.EXIT) add(out, x0, z0, size, cx * cell + PoolsGen.CACHE[0], PoolsGen.CACHE[1], cz * cell + PoolsGen.CACHE[2]);
                    }
                    case END -> {
                        if (LobbyGen.exitCell(LobbyGen.END, seed, cx, cz)) add(out, x0, z0, size, cx * cell + 2, LobbyGen.FLOOR_Y + 1, cz * cell + 6);
                    }
                    default -> {
                    }
                }
            }
        }
        return out;
    }

    private static void add(java.util.List<int[]> out, int x0, int z0, int size, int x, int y, int z) {
        if (x >= x0 && x < x0 + size && z >= z0 && z < z0 + size) out.add(new int[] {x, y, z});
    }

    /** The level a maintenance door or a wall rift leads to (null: none deeper). */
    public BackroomsLevel deeper() {
        return switch (this) {
            case LOBBY -> WAREHOUSE;
            case WAREHOUSE -> PIPES;
            case PIPES -> END;
            default -> null;
        };
    }

    /** Where a rift at height y leads: a hole in the floor (y 1) of the Warehouse or the Pipes drops into the Poolrooms. */
    public BackroomsLevel riftTarget(int y) {
        if (y <= 1 && (this == WAREHOUSE || this == PIPES)) return POOLS;
        return deeper();
    }
}
