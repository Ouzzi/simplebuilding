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
 * The rules of the Astral Enchanting Table (owner 2026-10-09, queue N27, docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md).
 *
 * <p><b>Strength.</b> Shelves stand where Vanilla's do ({@link EnchantingTableBlock#BOOKSHELF_OFFSETS}, air gap between
 * table and shelf). A Vanilla bookshelf (anything in {@code minecraft:enchantment_power_provider}) is worth
 * {@value #NORMAL_SHELF} point, a blazewood bookshelf {@value #BLAZE_SHELF}; the best {@value #MAX_SHELVES} shelves count.
 * The tier is {@code min(30, 2 x points)} like Vanilla (15 bookshelves = 30). With the 5x5 floor of blazing obsidian
 * under the table it rises to {@value #TIER_FLOOR_MID} from {@value #FLOOR_MID_POINTS} points and to
 * {@value #TIER_FLOOR_MAX} from {@value #FLOOR_MAX_POINTS} (all 15 shelves blazewood).
 *
 * <p><b>Budget.</b> The tier is the budget in points ({@value #MIN_BUDGET} without any shelf: one level of a common
 * enchantment); at {@value #TIER_FLOOR_MAX} every slider is free up to its maximum. Each slider level costs points by the
 * enchantment's rarity (Vanilla weight): common/uncommon {@value #COMMON_COST}, rare {@value #RARE_COST}, very rare
 * {@value #VERY_RARE_COST}.
 *
 * <p><b>Price.</b> Levels used up: 1-3 by the share of 30 points spent (like Vanilla's 1-3), {@value #FLOOR_MID_LEVELS}
 * at tier 40 and {@value #FLOOR_MAX_LEVELS} at tier 50. Lapis = levels used up, blaze powder twice that. The player needs
 * at least as many levels as points spent (at most 30, at least the levels used up) - Vanilla asks for the slot's level.
 */
public final class AstralEnchanting {
    public static final int MAX_SHELVES = 15;
    public static final int NORMAL_SHELF = 1;
    public static final int BLAZE_SHELF = 2;
    public static final int MAX_POINTS = 30;
    public static final int TIER_SHELVES_MAX = 30;
    public static final int TIER_FLOOR_MID = 40;
    public static final int TIER_FLOOR_MAX = 50;
    public static final int FLOOR_MID_POINTS = 20;
    public static final int FLOOR_MAX_POINTS = 30;
    /** Floor: 5x5 blazing obsidian right under the table (radius 2). */
    public static final int FLOOR_RADIUS = 2;
    public static final int MIN_BUDGET = 3;
    /** Budget at tier 50: every slider to its maximum. */
    public static final int UNLIMITED = Short.MAX_VALUE;
    public static final int COMMON_COST = 3;
    public static final int RARE_COST = 6;
    public static final int VERY_RARE_COST = 10;
    public static final int FLOOR_MID_LEVELS = 4;
    public static final int FLOOR_MAX_LEVELS = 5;
    /** Points per used-up level below tier 40 (30 points = 3 levels). */
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

    /** The tier: {@code min(30, 2 x points)}, with the floor 40 from 20 points and 50 from 30 points. */
    public static int tier(int points, boolean floor) {
        if (floor && points >= FLOOR_MAX_POINTS) return TIER_FLOOR_MAX;
        if (floor && points >= FLOOR_MID_POINTS) return TIER_FLOOR_MID;
        return Math.min(TIER_SHELVES_MAX, 2 * Math.max(0, points));
    }

    /** Points to spend at this tier; {@link #UNLIMITED} at tier 50. */
    public static int budget(int tier) {
        if (tier >= TIER_FLOOR_MAX) return UNLIMITED;
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

    /** Levels used up: 0 without a choice, 1-3 by the share of 30 points below tier 40, 4 at tier 40, 5 at tier 50. */
    public static int levelCost(int spent, int tier) {
        if (spent <= 0) return 0;
        if (tier >= TIER_FLOOR_MAX) return FLOOR_MAX_LEVELS;
        if (tier >= TIER_FLOOR_MID) return FLOOR_MID_LEVELS;
        return Math.clamp((spent + POINTS_PER_LEVEL - 1) / POINTS_PER_LEVEL, 1, 3);
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
