package com.simplebuilding.modules.simplelayers.world.gen;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Breadth-first search over a level's cell graph (BACK18): the number of cells from a start cell to the nearest goal cell, or
 * -1 when none lies within {@code limit}. The gametests and the round images use it to prove the escape ways.
 */
public final class EscapeCheck {
    private EscapeCheck() {}

    /**
     * @param westOpen  (cx, cz) → whether the west line of the cell can be passed
     * @param northOpen (cx, cz) → whether the north line can be passed
     */
    public static int distance(int startX, int startZ, int limit, BiPredicate<Integer, Integer> westOpen, BiPredicate<Integer, Integer> northOpen,
                               Predicate<long[]> goal) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        Map<Long, Integer> seen = new HashMap<>();
        queue.add(new int[] {startX, startZ});
        seen.put(key(startX, startZ), 0);
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int d = seen.get(key(c[0], c[1]));
            if (goal.test(new long[] {c[0], c[1]})) return d;
            if (d >= limit) continue;
            int x = c[0], z = c[1];
            if (westOpen.test(x, z)) visit(queue, seen, x - 1, z, d + 1);
            if (westOpen.test(x + 1, z)) visit(queue, seen, x + 1, z, d + 1);
            if (northOpen.test(x, z)) visit(queue, seen, x, z - 1, d + 1);
            if (northOpen.test(x, z + 1)) visit(queue, seen, x, z + 1, d + 1);
        }
        return -1;
    }

    private static void visit(ArrayDeque<int[]> queue, Map<Long, Integer> seen, int x, int z, int d) {
        if (seen.putIfAbsent(key(x, z), d) == null) queue.add(new int[] {x, z});
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Lobby: cells from the cell of (x, z) to the nearest exit cell. */
    public static int lobbyToExit(long seed, int x, int z, int limit) {
        LobbyGen.Style s = LobbyGen.LOBBY;
        return distance(Math.floorDiv(x, LobbyGen.CELL), Math.floorDiv(z, LobbyGen.CELL), limit,
                (cx, cz) -> LobbyGen.west(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                (cx, cz) -> LobbyGen.north(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                c -> LobbyGen.exitCell(s, seed, (int) c[0], (int) c[1]));
    }

    /** Exit level: cells to the nearest exit cell. */
    public static int endToExit(long seed, int x, int z, int limit) {
        LobbyGen.Style s = LobbyGen.END;
        return distance(Math.floorDiv(x, LobbyGen.CELL), Math.floorDiv(z, LobbyGen.CELL), limit,
                (cx, cz) -> LobbyGen.west(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                (cx, cz) -> LobbyGen.north(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                c -> LobbyGen.exitCell(s, seed, (int) c[0], (int) c[1]));
    }

    /** Pipes (ground storey): nodes to the nearest sealed hatch. */
    public static int pipesToHatch(long seed, int x, int z, int limit) {
        return distance(Math.floorDiv(x, PipesGen.NODE), Math.floorDiv(z, PipesGen.NODE), limit,
                (nx, nz) -> PipesGen.west(seed, 0, nx, nz), (nx, nz) -> PipesGen.north(seed, 0, nx, nz),
                c -> PipesGen.node(seed, 0, (int) c[0], (int) c[1]) == PipesGen.Node.HATCH);
    }

    /** Poolrooms: rooms to the nearest exit room. */
    public static int poolsToExit(long seed, int x, int z, int limit) {
        return distance(Math.floorDiv(x, PoolsGen.ROOM), Math.floorDiv(z, PoolsGen.ROOM), limit,
                (rx, rz) -> PoolsGen.west(seed, rx, rz), (rx, rz) -> PoolsGen.north(seed, rx, rz),
                c -> PoolsGen.room(seed, (int) c[0], (int) c[1]) == PoolsGen.Room.EXIT);
    }

    /** Warehouse: bays to the nearest freight lift (maintenance door to the Pipes). */
    public static int warehouseToLift(long seed, int x, int z, int limit) {
        return distance(Math.floorDiv(x, WarehouseGen.BAY), Math.floorDiv(z, WarehouseGen.BAY), limit,
                (bx, bz) -> WarehouseGen.west(seed, bx, bz) != WarehouseGen.Edge.WALL,
                (bx, bz) -> WarehouseGen.north(seed, bx, bz) != WarehouseGen.Edge.WALL,
                c -> WarehouseGen.bay(seed, (int) c[0], (int) c[1]) == WarehouseGen.Bay.LIFT);
    }

    /** Lobby ground floor: cells to the nearest noclip rift or stairwell (the ways deeper). */
    public static int lobbyToDeeper(long seed, int x, int z, int limit) {
        LobbyGen.Style s = LobbyGen.LOBBY;
        return distance(Math.floorDiv(x, LobbyGen.CELL), Math.floorDiv(z, LobbyGen.CELL), limit,
                (cx, cz) -> LobbyGen.west(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                (cx, cz) -> LobbyGen.north(s, seed, 0, cx, cz) != LobbyGen.Edge.WALL,
                c -> {
                    int cx = (int) c[0], cz = (int) c[1];
                    return LobbyGen.stairwell(s, seed, cx, cz)
                            || LobbyGen.riftIn(s, seed, LobbyGen.west(s, seed, 0, cx, cz), cx, cz, 0)
                            || LobbyGen.riftIn(s, seed, LobbyGen.north(s, seed, 0, cx, cz), cx, cz, 1);
                });
    }
}
