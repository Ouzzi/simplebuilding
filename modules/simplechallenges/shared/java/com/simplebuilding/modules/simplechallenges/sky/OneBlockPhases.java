package com.simplebuilding.modules.simplechallenges.sky;

import java.util.List;

/**
 * The One Block's sequence: the n-th block is picked by a hash of seed and n (so the same world always gives the same blocks),
 * from the phase n belongs to. Every {@link #PHASE_LENGTH} blocks the next phase starts, with more valuable blocks; the last
 * phase repeats. Pure data, no world access.
 */
public final class OneBlockPhases {
    public static final int PHASE_LENGTH = 40;

    public record Entry(String block, int weight) {}

    public static final List<List<Entry>> PHASES = List.of(
            // 0 meadow
            List.of(new Entry("minecraft:grass_block", 10), new Entry("minecraft:dirt", 8), new Entry("minecraft:oak_log", 6), new Entry("minecraft:oak_leaves", 4),
                    new Entry("minecraft:sand", 3), new Entry("minecraft:oak_sapling", 1), new Entry("minecraft:sweet_berry_bush", 1)),
            // 1 stone age
            List.of(new Entry("minecraft:stone", 12), new Entry("minecraft:cobblestone", 8), new Entry("minecraft:gravel", 5), new Entry("minecraft:coal_ore", 4),
                    new Entry("minecraft:andesite", 3), new Entry("minecraft:birch_log", 3), new Entry("minecraft:copper_ore", 2)),
            // 2 caves
            List.of(new Entry("minecraft:stone", 8), new Entry("minecraft:deepslate", 6), new Entry("minecraft:iron_ore", 5), new Entry("minecraft:copper_ore", 4),
                    new Entry("minecraft:coal_ore", 4), new Entry("minecraft:redstone_ore", 3), new Entry("minecraft:lapis_ore", 2), new Entry("minecraft:spruce_log", 3)),
            // 3 deep
            List.of(new Entry("minecraft:deepslate", 8), new Entry("minecraft:gold_ore", 4), new Entry("minecraft:diamond_ore", 2), new Entry("minecraft:iron_ore", 4),
                    new Entry("minecraft:lapis_ore", 3), new Entry("minecraft:glow_lichen", 1), new Entry("minecraft:amethyst_block", 2), new Entry("minecraft:tuff", 4)),
            // 4 nether
            List.of(new Entry("minecraft:netherrack", 12), new Entry("minecraft:nether_quartz_ore", 4), new Entry("minecraft:nether_gold_ore", 4), new Entry("minecraft:soul_sand", 4),
                    new Entry("minecraft:glowstone", 2), new Entry("minecraft:magma_block", 2), new Entry("minecraft:ancient_debris", 1), new Entry("minecraft:crimson_stem", 2)),
            // 5 end
            List.of(new Entry("minecraft:end_stone", 12), new Entry("minecraft:obsidian", 4), new Entry("minecraft:purpur_block", 4), new Entry("minecraft:chorus_flower", 1),
                    new Entry("minecraft:diamond_ore", 2), new Entry("minecraft:ancient_debris", 2)));

    private OneBlockPhases() {}

    public static int phaseOf(int count) {
        return Math.min(Math.max(0, count) / PHASE_LENGTH, PHASES.size() - 1);
    }

    /** The block of the n-th refill (n = 0 is the first block that stands, always grass). */
    public static String block(long seed, int count) {
        if (count <= 0) return "minecraft:grass_block";
        List<Entry> phase = PHASES.get(phaseOf(count));
        int total = 0;
        for (Entry e : phase) total += e.weight();
        long h = mix(seed ^ (count * 0x9E3779B97F4A7C15L));
        int roll = (int) Math.floorMod(h, (long) total);
        for (Entry e : phase) {
            roll -= e.weight();
            if (roll < 0) return e.block();
        }
        return phase.get(0).block();
    }

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
