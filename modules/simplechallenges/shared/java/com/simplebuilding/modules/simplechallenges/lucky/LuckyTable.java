package com.simplebuilding.modules.simplechallenges.lucky;

import com.simplebuilding.modules.simplechallenges.price.BlockValues;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The odds of the Lucky Block (concept 5): the weight of a block is {@code commonness / (value + 1)^alpha}, both from block_value.json,
 * so what the world and the crafting table hand out freely comes up often and the diamond block almost never. Built once per
 * mode from the registry; {@link #pick} turns one random number in [0, 1) into a block.
 */
public final class LuckyTable {
    /** The first few blocks of a world (the pity): wood and stone first, so nobody starts without tools. */
    public static final List<String> STARTERS = List.of("oak_log", "cobblestone", "oak_log", "dirt", "cobblestone", "sand");

    private final Block[] blocks;
    private final double[] cumulative;

    private LuckyTable(Block[] blocks, double[] cumulative) {
        this.blocks = blocks;
        this.cumulative = cumulative;
    }

    public static LuckyTable build(double alpha) {
        BlockValues values = BlockValues.current();
        List<Block> pool = BlockCatalog.all();
        Block[] blocks = new Block[pool.size()];
        double[] cum = new double[pool.size()];
        double sum = 0;
        int n = 0;
        for (Block b : pool) {
            String id = BlockCatalog.idOf(b);
            double w = weight(values, id, alpha);
            if (w <= 0) continue;
            sum += w;
            blocks[n] = b;
            cum[n++] = sum;
        }
        return new LuckyTable(java.util.Arrays.copyOf(blocks, n), java.util.Arrays.copyOf(cum, n));
    }

    public static double weight(BlockValues values, String id, double alpha) {
        return values.commonness(id) / Math.pow(values.value(id) + 1, alpha);
    }

    public int size() {
        return blocks.length;
    }

    public double total() {
        return cumulative.length == 0 ? 0 : cumulative[cumulative.length - 1];
    }

    /** The block for a random number u in [0, 1). */
    public Block pick(double u) {
        if (blocks.length == 0) return Blocks.DIRT;
        double x = u * total();
        int lo = 0, hi = cumulative.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumulative[mid] > x) hi = mid;
            else lo = mid + 1;
        }
        return blocks[lo];
    }

    /** The pity block for the n-th mined block of a world (n counts from 0), or null once the pity is over. */
    public static Block starter(int n, int pity) {
        if (n < 0 || n >= pity) return null;
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:" + STARTERS.get(n % STARTERS.size())));
    }

    /** Random Drops: the fixed partner of a block in a world (the same seed and block always give the same one). */
    public Block partner(long seed, Block from) {
        long h = seed ^ (long) BlockCatalog.idOf(from).hashCode() * 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return pick((h >>> 11) * 0x1.0p-53);
    }
}
