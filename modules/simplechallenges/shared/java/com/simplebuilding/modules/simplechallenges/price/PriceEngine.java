package com.simplebuilding.modules.simplechallenges.price;

import com.simplebuilding.modules.simplechallenges.preset.Currency;
import com.simplebuilding.modules.simplechallenges.preset.Difficulty;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The price of a cell (concept 3). Pure and deterministic: the same seed, dimension, cell and inputs always give the
 * same price, so a recomputation never moves a price (the service caches it in the world state anyway).
 * <ul>
 * <li>Easy: an item the start area offers (hand-reachable), small amount.</li>
 * <li>Medium: an item of the cell's own surroundings (biome), common ground (dirt, gravel) less likely.</li>
 * <li>Hard: the progression ladder (stone, coal, iron, gold, diamond) by how many cells are already unlocked.</li>
 * <li>Block unlock (cell 1): amount and item follow the value of the column.</li>
 * </ul>
 * The amount grows with the cell edge and is capped at two stacks.
 */
public final class PriceEngine {
    /** A rung of the hard ladder: from this many unlocked cells on the price is paid in {@code item}; counts shrink with {@code divisor}. */
    public record Rung(int from, String item, int divisor, int maxStack) {}

    public static final List<Rung> LADDER = List.of(
            new Rung(0, "minecraft:cobblestone", 1, 64), new Rung(6, "minecraft:coal", 2, 64),
            new Rung(20, "minecraft:iron_ingot", 4, 64), new Rung(60, "minecraft:gold_ingot", 8, 64),
            new Rung(150, "minecraft:diamond", 16, 64));
    private static final String FALLBACK_ITEM = "minecraft:dirt";
    /** Ground items that make poor medium-tier prices (everything is made of them). */
    private static final List<String> COMMON = List.of("minecraft:dirt", "minecraft:grass_block", "minecraft:gravel");

    private final long seed;
    private final UnlockMode mode;

    public PriceEngine(long seed, UnlockMode mode) {
        this.seed = seed;
        this.mode = mode;
    }

    public static long mix(long seed, String dimension, int cx, int cz) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h = h * 31 + dimension.hashCode();
        h = h * 0xBF58476D1CE4E5B9L + cx;
        h = h * 0x94D049BB133111EBL + cz;
        h ^= h >>> 29;
        return h;
    }

    /**
     * @param region what the whole unlocked area offers (easy tier, fallback)
     * @param own what the cell itself offers (medium tier, column value)
     * @param unlockedCells how many cells are free now (hard tier)
     */
    public Price price(String dimension, int cx, int cz, CellInfo own, List<WeightedItem> region, int unlockedCells) {
        SplittableRandom rng = new SplittableRandom(mix(seed, dimension, cx, cz));
        int area = mode.cell() * mode.cell();
        Difficulty diff = mode.difficulty();
        if (mode.currency() == Currency.CREDITS) return Price.credits(1);
        if (mode.currency() == Currency.XP) {
            int levels = 1 + (int) Math.floor(Math.log(area) / Math.log(4)) + (diff == Difficulty.HARD ? 2 : diff == Difficulty.MEDIUM ? 1 : 0)
                    + unlockedCells / 100;
            return Price.xp(Math.min(30, levels));
        }
        if (mode.cell() == 1) return blockPrice(rng, own, region, unlockedCells);
        double k = diff == Difficulty.EASY ? 0.5 : 1.0;
        int base = (int) Math.max(1, Math.ceil(Math.sqrt(area) * k));
        switch (diff) {
            case EASY -> {
                WeightedItem it = pick(rng, region, false);
                if (it == null) it = pick(rng, own.reachable(), false);
                return it == null ? fallback(base) : cap(it.id(), base, it.maxStack());
            }
            case MEDIUM -> {
                WeightedItem it = pick(rng, own.reachable(), true);
                if (it == null) it = pick(rng, region, true);
                return it == null ? fallback(base) : cap(it.id(), base, it.maxStack());
            }
            default -> {
                Rung r = rung(unlockedCells);
                return cap(r.item(), Math.max(1, base / r.divisor()), r.maxStack());
            }
        }
    }

    /** Block unlock: price by the value of the column (rare blocks in it cost more, ores need smelted items). */
    private Price blockPrice(SplittableRandom rng, CellInfo own, List<WeightedItem> region, int unlockedCells) {
        double v = Math.max(0, own.columnValue());
        double mult = mode.difficulty() == Difficulty.EASY ? 0.5 : mode.difficulty() == Difficulty.HARD ? 2.0 : 1.0;
        int rank = v < 4 ? 0 : Math.min(LADDER.size() - 1, 1 + (int) (Math.log(v / 4) / Math.log(2)));
        if (mode.difficulty() == Difficulty.HARD) rank = Math.max(rank, LADDER.indexOf(rung(unlockedCells)));
        if (rank == 0) {
            WeightedItem it = pick(rng, mode.difficulty() == Difficulty.EASY ? region : own.reachable(), mode.difficulty() != Difficulty.EASY);
            if (it == null) it = pick(rng, region, false);
            int n = (int) Math.max(1, Math.round(Math.max(1, v) * mult * 0.5));
            return it == null ? fallback(n) : cap(it.id(), n, it.maxStack());
        }
        Rung r = LADDER.get(rank);
        return cap(r.item(), (int) Math.max(1, Math.round(v * mult / (4.0 * r.divisor()))), r.maxStack());
    }

    public static Rung rung(int unlockedCells) {
        Rung best = LADDER.get(0);
        for (Rung r : LADDER) if (unlockedCells >= r.from()) best = r;
        return best;
    }

    private static Price cap(String item, int count, int maxStack) {
        return Price.items(item, Math.max(1, Math.min(count, UnlockMode.MAX_STACKS * Math.max(1, maxStack))));
    }

    private static Price fallback(int count) {
        return cap(FALLBACK_ITEM, count, 64);
    }

    /** Weighted pick; with {@code avoidCommon} the ground items count a quarter. Null when the list is empty. */
    private static WeightedItem pick(SplittableRandom rng, List<WeightedItem> items, boolean avoidCommon) {
        if (items == null || items.isEmpty()) return null;
        double total = 0;
        for (WeightedItem w : items) total += weight(w, avoidCommon);
        if (total <= 0) return null;
        double roll = rng.nextDouble() * total;
        for (WeightedItem w : items) {
            roll -= weight(w, avoidCommon);
            if (roll < 0) return w;
        }
        return items.get(items.size() - 1);
    }

    private static double weight(WeightedItem w, boolean avoidCommon) {
        double x = Math.max(0, w.weight());
        return avoidCommon && COMMON.contains(w.id()) ? x * 0.25 : x;
    }
}
