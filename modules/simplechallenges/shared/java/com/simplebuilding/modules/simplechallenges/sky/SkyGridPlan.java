package com.simplebuilding.modules.simplechallenges.sky;

import java.util.List;

/**
 * The sky grid: one block at every coordinate that is a multiple of {@code spacing} in all three directions, its kind picked from
 * a weighted list by a hash of seed and position (the same world always gives the same grid). The column at 0,0 around the
 * spawn is left to the start platform; everything is pure data.
 */
public final class SkyGridPlan {
    public record Entry(String block, int weight) {}

    public static final List<Entry> BLOCKS = List.of(
            new Entry("minecraft:grass_block", 20), new Entry("minecraft:dirt", 14), new Entry("minecraft:stone", 16), new Entry("minecraft:sand", 8),
            new Entry("minecraft:gravel", 6), new Entry("minecraft:oak_log", 8), new Entry("minecraft:birch_log", 4), new Entry("minecraft:spruce_log", 4),
            new Entry("minecraft:oak_leaves", 6), new Entry("minecraft:coal_ore", 5), new Entry("minecraft:iron_ore", 3), new Entry("minecraft:copper_ore", 3),
            new Entry("minecraft:gold_ore", 1), new Entry("minecraft:redstone_ore", 1), new Entry("minecraft:lapis_ore", 1), new Entry("minecraft:diamond_ore", 1),
            new Entry("minecraft:clay", 3), new Entry("minecraft:snow_block", 2), new Entry("minecraft:water", 2), new Entry("minecraft:pumpkin", 1),
            new Entry("minecraft:melon", 1), new Entry("minecraft:mossy_cobblestone", 1));

    private SkyGridPlan() {}

    public static boolean isNode(int spacing, int x, int y, int z) {
        return Math.floorMod(x, spacing) == 0 && Math.floorMod(y, spacing) == 0 && Math.floorMod(z, spacing) == 0;
    }

    /** The grid block at a node, or null where there is none. */
    public static String blockAt(long seed, int spacing, int x, int y, int z) {
        if (!isNode(spacing, x, y, z)) return null;
        int total = 0;
        for (Entry e : BLOCKS) total += e.weight();
        long h = OneBlockPhases.mix(seed ^ OneBlockPhases.mix(x * 73856093L ^ y * 19349663L ^ z * 83492791L));
        int roll = (int) Math.floorMod(h, (long) total);
        for (Entry e : BLOCKS) {
            roll -= e.weight();
            if (roll < 0) return e.block();
        }
        return BLOCKS.get(0).block();
    }

    /** Receives one grid block. */
    public interface Sink {
        void accept(int x, int y, int z, String block);
    }

    /** Half width of the box round the start platform that the grid leaves empty (it reaches 4 blocks below and 10 above the platform height). */
    public static final int STARTER_RADIUS = 7;

    public static boolean isStarter(int x, int y, int z, int platformY) {
        return Math.abs(x) <= STARTER_RADIUS && Math.abs(z) <= STARTER_RADIUS && y >= platformY - 4 && y <= platformY + 10;
    }

    /** Hands every node of the 16 x 16 columns starting at (x0, z0) between minY and maxY (inclusive) to the sink; returns how many. */
    public static int nodes(long seed, int spacing, int x0, int z0, int minY, int maxY, Sink sink) {
        int n = 0;
        for (int x = x0; x < x0 + 16; x++) {
            if (Math.floorMod(x, spacing) != 0) continue;
            for (int z = z0; z < z0 + 16; z++) {
                if (Math.floorMod(z, spacing) != 0) continue;
                for (int y = minY; y <= maxY; y++) {
                    String id = blockAt(seed, spacing, x, y, z);
                    if (id == null) continue;
                    sink.accept(x, y, z, id);
                    n++;
                }
            }
        }
        return n;
    }

    /** Number of nodes in a chunk column of the given height range (for tests and the preview). */
    public static int nodesPerChunk(int spacing, int minY, int maxY) {
        int perAxis = 0;
        for (int x = 0; x < 16; x++) if (Math.floorMod(x, spacing) == 0) perAxis++;
        int ys = 0;
        for (int y = minY; y < maxY; y++) if (Math.floorMod(y, spacing) == 0) ys++;
        return perAxis * perAxis * ys;
    }
}
