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
 * <p><b>Budget.</b> The tier is the budget in points ({@value #MIN_BUDGET} without any shelf: one level of a common
 * enchantment); at {@value #TIER_MAX} every slider is free up to its maximum. Each slider level costs points by the
 * enchantment's rarity (Vanilla weight): common/uncommon {@value #COMMON_COST}, rare {@value #RARE_COST}, very rare
 * {@value #VERY_RARE_COST}.
 *
 * <p><b>Price.</b> Levels used up: 1 per started {@value #POINTS_PER_LEVEL} points spent (1-3 up to 30 like Vanilla's
 * 1-3, up to {@value #MAX_LEVEL_COST} above), always {@value #MAX_LEVEL_COST} at tier {@value #TIER_MAX}. Lapis = levels
 * used up, blaze powder twice that. The player needs at least as many levels as points spent (at most 30, at least the
 * levels used up) - Vanilla asks for the slot's level.
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
    /** Budget at tier 50: every slider to its maximum. */
    public static final int UNLIMITED = Short.MAX_VALUE;
    public static final int COMMON_COST = 3;
    public static final int RARE_COST = 6;
    public static final int VERY_RARE_COST = 10;
    /** Levels used up at most (and always at tier {@value #TIER_MAX}). */
    public static final int MAX_LEVEL_COST = 5;
    /** Points per used-up level (30 points = 3 levels). */
    public static final int POINTS_PER_LEVEL = 10;
    public static final int MAX_REQUIRED_LEVEL = 30;
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

    /** Points to spend at this tier; {@link #UNLIMITED} at {@value #TIER_MAX}. */
    public static int budget(int tier) {
        if (tier >= TIER_MAX) return UNLIMITED;
        return Math.max(MIN_BUDGET, tier);
    }

    /** Points one slider level costs, by the enchantment's Vanilla weight (10/5 common, 2 rare, 1 very rare). */
    public static int pointsPerLevel(int weight) {
        if (weight >= 5) return COMMON_COST;
        if (weight >= 2) return RARE_COST;
        return VERY_RARE_COST;
    }

    /** Points the chosen slider levels cost together. */
    public static int spent(int[] levels, int[] pointsPerLevel) {
        int sum = 0;
        for (int i = 0; i < levels.length; i++) {
            sum += Math.max(0, levels[i]) * pointsPerLevel[i];
        }
        return sum;
    }

    /** The highest level slider {@code row} may take with the others as they are, within {@code maxLevel} and the budget. */
    public static int maxAffordable(int row, int[] levels, int[] pointsPerLevel, int maxLevel, int budget) {
        int others = 0;
        for (int i = 0; i < levels.length; i++) {
            if (i != row) others += Math.max(0, levels[i]) * pointsPerLevel[i];
        }
        int left = budget - others;
        if (left <= 0 || pointsPerLevel[row] <= 0) return 0;
        return Math.min(maxLevel, left / pointsPerLevel[row]);
    }

    /** Levels used up: 0 without a choice, 1 per started 10 points (at most 5), always 5 at tier {@value #TIER_MAX}. */
    public static int levelCost(int spent, int tier) {
        if (spent <= 0) return 0;
        if (tier >= TIER_MAX) return MAX_LEVEL_COST;
        return Math.clamp((spent + POINTS_PER_LEVEL - 1) / POINTS_PER_LEVEL, 1, MAX_LEVEL_COST);
    }

    /** Levels the player must have: the points spent (at most 30), never fewer than the levels used up. */
    public static int requiredLevel(int spent, int tier) {
        if (spent <= 0) return 0;
        return Math.max(levelCost(spent, tier), Math.min(MAX_REQUIRED_LEVEL, spent));
    }

    public static int lapisCost(int levelCost) {
        return levelCost;
    }

    public static int blazePowderCost(int levelCost) {
        return BLAZE_PER_LAPIS * levelCost;
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
