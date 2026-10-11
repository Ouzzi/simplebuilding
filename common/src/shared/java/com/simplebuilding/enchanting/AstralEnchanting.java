package com.simplebuilding.enchanting;

import com.simplebuilding.blocks.ModBlocks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The rules of the Astral Enchanter (owner 2026-10-09, queue N27; strength reworked N31 2026-10-10,
 * docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md).
 *
 * <p><b>Strength.</b> Shelves stand where Vanilla's do ({@link EnchantingTableBlock#BOOKSHELF_OFFSETS}, air gap between
 * table and shelf). A Vanilla bookshelf (anything in {@code minecraft:enchantment_power_provider}) is worth
 * {@value #NORMAL_SHELF} point, a blazewood bookshelf {@value #BLAZE_SHELF}; the best {@value #MAX_SHELVES} shelves count.
 * Up to {@value #MAX_SHELVES} points the tier is {@code 2 x points} like Vanilla (15 bookshelves = 30); the points above
 * that (only blazewood shelves give them) climb evenly on to {@value #TIER_SHELVES_MAX} at {@value #MAX_POINTS} points - so the
 * same 15 shelves, all blazewood, reach {@value #TIER_SHELVES_MAX}. The 5x5 floor of blazing obsidian under the table adds
 * {@value #FLOOR_BONUS} once the shelves stand at 30 (15 bookshelves on the floor = 40). So 30-40 needs blazewood
 * shelves OR the floor, and {@value #TIER_MAX} needs both (owner N32, 2026-10-10).
 *
 * <p><b>Price (owner N35, 2026-10-11).</b> Points are XP levels. Every enchantment has a value at its maximum level:
 * a base by its Vanilla anvil cost (1: 4, 2: 6, up to 4: 9, more: 12) plus {@value #VALUE_PER_MAX_LEVEL} per maximum
 * level. A slider level costs its share, {@code ceil(value x level / max)}. Combining is taxed: each further chosen
 * enchantment adds a quarter of the sum, {@code raw = ceil(sum x (3 + n) / 4)} with {@code n} chosen enchantments.
 * When the full choice of an offer would cost more than {@value #MAX_PRICE}, the price is scaled, not capped (owner
 * N37: every level less costs less), see {@link #price}. The budget is the tier ({@value #MIN_BUDGET} without any shelf) up to {@value #MAX_PRICE}; since no price
 * exceeds {@value #MAX_PRICE}, only tier {@value #TIER_MAX} lets every slider reach its maximum at once. Enchanting uses up
 * the price in levels (the player needs them), lapis 1 per started {@value #LEVELS_PER_LAPIS} levels (1-5), blaze powder
 * twice that.
 */
public final class AstralEnchanting {
    public static final int MAX_SHELVES = 15;
    public static final int NORMAL_SHELF = 1;
    public static final int BLAZE_SHELF = 2;
    public static final int MAX_POINTS = 30;
    /** Tier of 15 bookshelves (Vanilla's maximum). */
    public static final int TIER_SHELVES = 30;
    /** Highest tier without the floor: 15 blazewood shelves. */
    public static final int TIER_SHELVES_MAX = 40;
    /** The highest tier: every slider free. Needs 15 blazewood shelves AND the floor. */
    public static final int TIER_MAX = 50;
    /** What the blazing obsidian floor adds once the shelves give {@value #TIER_SHELVES}. */
    public static final int FLOOR_BONUS = 10;
    /** Floor: 5x5 blazing obsidian right under the table (radius 2). */
    public static final int FLOOR_RADIUS = 2;
    public static final int MIN_BUDGET = 3;
    /** Highest price in levels; the budget at tier {@value #TIER_MAX}, where every slider is free. */
    public static final int MAX_PRICE = 50;
    /** Value added per maximum level of an enchantment. */
    public static final int VALUE_PER_MAX_LEVEL = 2;
    /** Levels per lapis lazuli (started). */
    public static final int LEVELS_PER_LAPIS = 10;
    public static final int BLAZE_PER_LAPIS = 2;
    public static final int CHOICES = 3;

    private AstralEnchanting() {
    }

    // ------------------------------------------------------------------ pure rules

    /** Points of the shelves around the table: the best {@value #MAX_SHELVES} values, together at most {@value #MAX_POINTS}. */
    public static int shelfPoints(int[] shelfValues) {
        int[] sorted = Arrays.stream(shelfValues).filter(v -> v > 0).sorted().toArray();
        int sum = 0;
        for (int i = sorted.length - 1, n = 0; i >= 0 && n < MAX_SHELVES; i--, n++) {
            sum += sorted[i];
        }
        return Math.min(MAX_POINTS, sum);
    }

    /**
     * The tier from the shelf points and the floor (rule 2026-10-10, N32): up to 15 points {@code 2 x points} (15
     * bookshelves = 30, like Vanilla); the 15 points above that (only blazewood shelves give them) climb evenly on to
     * {@value #TIER_SHELVES_MAX} at {@value #MAX_POINTS} points; the floor adds {@value #FLOOR_BONUS} once the shelves
     * give 30. {@value #TIER_MAX} therefore needs both: all 15 shelves blazewood and the floor.
     */
    public static int tier(int points, boolean floor) {
        int p = Math.clamp(points, 0, MAX_POINTS);
        int tier = p <= MAX_SHELVES ? 2 * p
                : TIER_SHELVES + Math.round((p - MAX_SHELVES) * (float) (TIER_SHELVES_MAX - TIER_SHELVES) / (MAX_POINTS - MAX_SHELVES));
        if (floor && p >= MAX_SHELVES) tier += FLOOR_BONUS;
        return Math.min(TIER_MAX, tier);
    }

    /** What the next tier steps need (tooltip hint), see {@link #nextStep}. */
    public static final int STEP_BOOKSHELVES = 0;
    public static final int STEP_SHELF_OR_FLOOR = 1;
    public static final int STEP_FLOOR = 2;
    public static final int STEP_BLAZE_SHELVES = 3;
    public static final int STEP_MAX = 4;

    /** The condition that is missing for the next stage: more bookshelves, blazewood shelves or floor, or the floor, or blazewood shelves, or nothing. */
    public static int nextStep(int points, boolean floor) {
        int p = Math.clamp(points, 0, MAX_POINTS);
        if (p < MAX_SHELVES) return STEP_BOOKSHELVES;
        if (p >= MAX_POINTS) return floor ? STEP_MAX : STEP_FLOOR;
        return floor ? STEP_BLAZE_SHELVES : STEP_SHELF_OR_FLOOR;
    }

    /** Levels that may be spent at this tier: the tier, at least {@value #MIN_BUDGET}, at most {@value #MAX_PRICE}. */
    public static int budget(int tier) {
        return Math.clamp(tier, MIN_BUDGET, MAX_PRICE);
    }

    /** Base value by the enchantment's Vanilla anvil cost (1 common, 2 uncommon, 4 rare, 8 very rare). */
    public static int rarityValue(int anvilCost) {
        if (anvilCost <= 1) return 4;
        if (anvilCost <= 2) return 6;
        if (anvilCost <= 4) return 9;
        return 12;
    }

    /** Value of an enchantment at its maximum level: rarity base + {@value #VALUE_PER_MAX_LEVEL} per maximum level. */
    public static int value(int anvilCost, int maxLevel) {
        return rarityValue(anvilCost) + VALUE_PER_MAX_LEVEL * Math.max(1, maxLevel);
    }

    public static int value(Enchantment enchantment) {
        return value(enchantment.getAnvilCost(), enchantment.getMaxLevel());
    }

    /** Price of one slider before the tax: its share of the value, {@code ceil(value x level / max)}. */
    public static int levelPrice(int level, int maxLevel, int value) {
        if (level <= 0 || maxLevel <= 0) return 0;
        return Math.ceilDiv(value * Math.min(level, maxLevel), maxLevel);
    }

    /** Sum of the slider prices before the tax. */
    public static int basePrice(int[] levels, int[] maxLevels, int[] values) {
        int sum = 0;
        for (int i = 0; i < levels.length; i++) sum += levelPrice(levels[i], maxLevels[i], values[i]);
        return sum;
    }

    /** How many sliders stand above 0. */
    public static int chosenCount(int[] levels) {
        int n = 0;
        for (int level : levels) if (level > 0) n++;
        return n;
    }

    /** The price before the scaling: the sum plus a quarter of it per further enchantment. */
    public static int rawPrice(int[] levels, int[] maxLevels, int[] values) {
        int n = chosenCount(levels);
        if (n == 0) return 0;
        return Math.ceilDiv(basePrice(levels, maxLevels, values) * (3 + n), 4);
    }

    /**
     * The price in levels (owner N37: every level less must cost less, {@value #MAX_PRICE} only for the full choice).
     * Offers whose full choice stays within {@value #MAX_PRICE} pay the {@link #rawPrice raw price}. Dearer offers are
     * scaled instead of capped: one level per chosen slider level plus the raw price scaled so the full choice lands
     * on exactly {@value #MAX_PRICE}, {@code c + ceil(raw x (50 - M) / full)} with {@code c} chosen and {@code M} all
     * levels - never above the raw price. Both terms drop with every level taken away, so the price falls strictly.
     */
    public static int price(int[] levels, int[] maxLevels, int[] values) {
        int raw = rawPrice(levels, maxLevels, values);
        int full = rawPrice(maxLevels, maxLevels, values);
        if (raw == 0 || full <= MAX_PRICE) return raw;
        int chosen = 0, all = 0;
        for (int i = 0; i < levels.length; i++) {
            int max = Math.max(0, maxLevels[i]);
            all += max;
            chosen += Math.clamp(levels[i], 0, max);
        }
        return Math.min(raw, chosen + Math.ceilDiv(raw * Math.max(0, MAX_PRICE - all), full));
    }

    /** The highest level slider {@code row} may take with the others as they are, within its maximum and the budget. */
    public static int maxAffordable(int row, int[] levels, int[] maxLevels, int[] values, int budget) {
        int[] test = levels.clone();
        for (int level = maxLevels[row]; level > 0; level--) {
            test[row] = level;
            if (price(test, maxLevels, values) <= budget) return level;
        }
        return 0;
    }

    /** Lapis lazuli: one per started {@value #LEVELS_PER_LAPIS} levels of the price (1-5). */
    public static int lapisCost(int price) {
        return price <= 0 ? 0 : Math.ceilDiv(price, LEVELS_PER_LAPIS);
    }

    public static int blazePowderCost(int price) {
        return BLAZE_PER_LAPIS * lapisCost(price);
    }

    // ------------------------------------------------------------------ the world

    /** Whether {@code state} is a blazewood bookshelf (worth {@value #BLAZE_SHELF}). */
    public static boolean isBlazeShelf(BlockState state) {
        for (Block shelf : ModBlocks.BLAZEWOOD_BOOKSHELVES) {
            if (state.is(shelf)) return true;
        }
        return false;
    }

    /** Value of the shelf at {@code table + offset}: 0 without shelf or when the gap is not free (Vanilla's rule). */
    public static int shelfValue(Level level, BlockPos table, BlockPos offset) {
        BlockPos gap = table.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2);
        if (!level.getBlockState(gap).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)) return 0;
        BlockState shelf = level.getBlockState(table.offset(offset));
        if (isBlazeShelf(shelf)) return BLAZE_SHELF;
        return shelf.is(BlockTags.ENCHANTMENT_POWER_PROVIDER) ? NORMAL_SHELF : 0;
    }

    public static int shelfPoints(Level level, BlockPos table) {
        List<BlockPos> offsets = EnchantingTableBlock.BOOKSHELF_OFFSETS;
        int[] values = new int[offsets.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = shelfValue(level, table, offsets.get(i));
        }
        return shelfPoints(values);
    }

    /** Whether the 5x5 under the table is blazing obsidian. */
    public static boolean hasFloor(Level level, BlockPos table) {
        if (ModBlocks.BLAZING_OBSIDIAN == null) return false;
        for (int dx = -FLOOR_RADIUS; dx <= FLOOR_RADIUS; dx++) {
            for (int dz = -FLOOR_RADIUS; dz <= FLOOR_RADIUS; dz++) {
                if (!level.getBlockState(table.offset(dx, -1, dz)).is(ModBlocks.BLAZING_OBSIDIAN)) return false;
            }
        }
        return true;
    }

    public static int tierAt(Level level, BlockPos table) {
        return tier(shelfPoints(level, table), hasFloor(level, table));
    }

    /**
     * Up to three random enchantments that fit {@code stack} (no treasure: {@code minecraft:in_enchanting_table}, like
     * Vanilla; a book takes all of them) and go together, drawn from the player's enchantment seed: the same seed gives
     * the same three, a new seed (after each enchanting) new ones.
     */
    public static List<Holder<Enchantment>> choices(RegistryAccess access, ItemStack stack, int seed) {
        if (stack.isEmpty() || !stack.isEnchantable()) return List.of();
        Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) return List.of();
        boolean book = stack.is(Items.BOOK);
        List<Holder<Enchantment>> pool = new ArrayList<>();
        for (Holder<Enchantment> holder : tag.get()) {
            if (book || holder.value().isPrimaryItem(stack)) pool.add(holder);
        }
        java.util.Collections.shuffle(pool, new java.util.Random(seed));
        List<Holder<Enchantment>> out = new ArrayList<>();
        for (Holder<Enchantment> holder : pool) {
            if (out.size() == CHOICES) break;
            boolean fits = true;
            for (Holder<Enchantment> chosen : out) {
                if (!Enchantment.areCompatible(chosen, holder)) fits = false;
            }
            if (fits) out.add(holder);
        }
        return out;
    }
}
