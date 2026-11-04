package com.simpleenchanting.test;

import com.simpleenchanting.EnchanterStage;
import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.EnchantingRules;
import com.simpleenchanting.SimpleEnchanting;
import com.simpleenchanting.block.EnchanterBlockEntity;
import com.simpleenchanting.config.EnchantingConfig;
import com.simpleenchanting.menu.EnchanterMenu;
import com.simplelib.api.InWorldUpgrades;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Enchanting (ENCH02, docs/ai/PLAN-B-ENCH02-2026-11-03.md; the table rules since N27): registration and id moves, the
 * building blocks by installation, the pure budget/cost rules of both stages, the shelf and floor detection, the shipped
 * upgrades, what breaking drops, the stock that stays in the block, and that enchanting uses up exactly what the rules say.
 * Loader-neutral; the Fabric adapter registers {@link #ALL} as module_game_test_<name> on {@code simpleenchanting:empty} (8x8x8);
 * every test runs standalone (Astral upgrade and drops only checked with Simple Ores).
 */
public final class EnchantingTests {
    /** Test name -> body, in catalogue order (the Fabric adapter names them module_game_test_<name>). */
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", EnchantingTests::registered);
        ALL.put("old_ids_move", EnchantingTests::oldIdsMove);
        ALL.put("building_blocks_by_installation", EnchantingTests::buildingBlocksByInstallation);
        ALL.put("budget_and_cost_rules", EnchantingTests::budgetAndCostRules);
        ALL.put("shelves_and_floor_set_the_tier", EnchantingTests::shelvesAndFloorSetTheTier);
        ALL.put("upgrades_are_loaded", EnchantingTests::upgradesAreLoaded);
        ALL.put("breaking_drops_table_nuggets_and_stock", EnchantingTests::breakingDropsTableNuggetAndStock);
        ALL.put("stock_stays_in_the_table", EnchantingTests::stockStaysInTheTable);
        ALL.put("enchanting_uses_up_what_the_rules_say", EnchantingTests::enchantingUsesUpWhatTheRulesSay);
        ALL.put("golden_apple_becomes_enchanted", EnchantingTests::goldenAppleBecomesEnchanted);
        ALL.put("endgame_level_rules", EnchantingTests::endgameLevelRules);
        ALL.put("endgame_levels_at_the_table", EnchantingTests::endgameLevelsAtTheTable);
        ALL.put("config_takes_over_the_endgame_switch", EnchantingTests::configTakesOverTheEndgameSwitch);
        ALL.put("end_shelves_count_double", EnchantingTests::endShelvesCountDouble);
        ALL.put("chiseled_shelves_take_mod_books", EnchantingTests::chiseledShelvesTakeModBooks);
        ALL.put("end_shelf_drops_its_books", EnchantingTests::endShelfDropsItsBooks);
        ALL.put("discover_recipe_unlocks", com.simpleenchanting.discover.EnchantingDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleenchanting.discover.EnchantingDiscover::behaviourHints);
        ALL.put("guide_book", com.simpleenchanting.guide.EnchantingGuide::gameTest);
    }
    /** Tick budget of the hammer test: 20 blows of 20 ticks run inside one game tick, the rest is slack. */
    public static final int MAX_TICKS = 40;
    private static final BlockPos TABLE = new BlockPos(3, 2, 3);

    private EnchantingTests() {
    }

    // ------------------------------------------------------------------ module

    /** Both enchanters with items, one block entity type for both, the menu type; the guide and the tab. */
    public static void registered(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleEnchanting.id("dark_enchanter")) == EnchantingRegistry.DARK_ENCHANTER, "dark_enchanter");
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleEnchanting.id("astral_enchanter")) == EnchantingRegistry.ASTRAL_ENCHANTER, "astral_enchanter");
        helper.assertTrue(EnchantingRegistry.DARK_ENCHANTER_ITEM.asItem() != Items.AIR && EnchantingRegistry.ASTRAL_ENCHANTER_ITEM != Items.AIR, "items");
        helper.assertTrue(EnchantingRegistry.ENCHANTER_BE.isValid(EnchantingRegistry.DARK_ENCHANTER.defaultBlockState())
                && EnchantingRegistry.ENCHANTER_BE.isValid(EnchantingRegistry.ASTRAL_ENCHANTER.defaultBlockState()), "one block entity type for both tables");
        helper.assertTrue(BuiltInRegistries.MENU.getValue(SimpleEnchanting.id("enchanter")) == EnchantingRegistry.ENCHANTER_MENU, "menu type");
        helper.assertTrue(BuiltInRegistries.ITEM.getValue(SimpleEnchanting.id("guide_book")) != Items.AIR, "guide book");
        helper.setBlock(TABLE, EnchantingRegistry.ASTRAL_ENCHANTER);
        var entity = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        helper.assertTrue(entity.stage() == EnchanterStage.ASTRAL, "the block entity of the Astral table knows its stage");
        helper.assertTrue(entity.getDisplayName().getString().contains("Astral") || !entity.getDisplayName().getString().isEmpty(), "name");
        helper.succeed();
    }

    /** SimpleBuilding's Astral Enchanting Table resolves to the Dark Enchanter (block, item, block entity type). */
    public static void oldIdsMove(GameTestHelper helper) {
        Identifier old = SimpleEnchanting.old(SimpleEnchanting.OLD_TABLE);
        helper.assertTrue(BuiltInRegistries.BLOCK.getValue(old) == EnchantingRegistry.DARK_ENCHANTER, "old table block -> Dark Enchanter");
        helper.assertTrue(BuiltInRegistries.ITEM.getValue(old) == EnchantingRegistry.DARK_ENCHANTER_ITEM, "old table item -> Dark Enchanter");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(old) == EnchantingRegistry.ENCHANTER_BE, "old block entity type -> enchanter");
        helper.succeed();
    }

    /**
     * The building blocks (owner 03.11.): with SimpleBuilding they are its own and this module registers no twins (a twin id of a
     * world played alone resolves to SimpleBuilding's block); alone this module brings them and SimpleBuilding's ids resolve to
     * the twins. Either way they are in the floor and shelf tags and the twins' recipes load only without SimpleBuilding.
     */
    public static void buildingBlocksByInstallation(GameTestHelper helper) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (String path : SimpleEnchanting.BUILDING_BLOCKS) {
            Block sb = BuiltInRegistries.BLOCK.getValue(SimpleEnchanting.old(path));
            Block own = BuiltInRegistries.BLOCK.getValue(SimpleEnchanting.id(path));
            helper.assertTrue(sb != Blocks.AIR && sb == own, path + ": both ids give the same block");
            String ns = BuiltInRegistries.BLOCK.getKey(sb).getNamespace();
            helper.assertTrue(ns.equals(SimpleEnchanting.buildingNamespace()), path + " is registered by " + ns);
            boolean twinRecipe = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleEnchanting.id(path))).isPresent();
            helper.assertTrue(twinRecipe != SimpleEnchanting.simpleBuilding(), path + ": the twin recipe loads only alone");
        }
        helper.assertTrue(SimpleEnchanting.simpleBuilding() || EnchantingRegistry.ownBuildingBlocks().size() == SimpleEnchanting.BUILDING_BLOCKS.size(),
                "twins registered alone");
        helper.assertTrue(building("nihil_obsidian").defaultBlockState().is(EnchantingRules.NIHIL_FLOOR), "Nihil floor tag");
        helper.assertTrue(building("blazing_obsidian").defaultBlockState().is(EnchantingRules.OBSIDIAN_FLOOR), "obsidian floor tag");
        helper.assertTrue(building("warped_blazewood_bookshelf").defaultBlockState().is(EnchantingRules.BLAZE_SHELVES), "blaze shelf tag");
        helper.assertTrue(building("crimson_blazewood_bookshelf").defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER),
                "blazewood shelves count at the Vanilla table");
        helper.assertTrue(building("nihil_obsidian").defaultBlockState().getLightEmission() > 0, "Nihil Obsidian glows");
        helper.succeed();
    }

    /** A missing module config takes SimpleBuilding's {@code server.features.endgameEnchantLevels} over once. */
    public static void configTakesOverTheEndgameSwitch(GameTestHelper helper) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simpleenchanting-test");
            java.nio.file.Path legacy = dir.resolve(EnchantingConfig.LEGACY_FILE);
            java.nio.file.Files.writeString(legacy, "{\"server\": {\"features\": {\"endgameEnchantLevels\": false}}}");
            helper.assertTrue(Boolean.FALSE.equals(EnchantingConfig.endgameFromSimpleBuilding(legacy)), "the old switch is read");
            java.nio.file.Files.writeString(legacy, "{}");
            helper.assertTrue(EnchantingConfig.endgameFromSimpleBuilding(legacy) == null, "no old switch: nothing taken");
            helper.assertTrue(EnchantingConfig.endgameFromSimpleBuilding(dir.resolve("missing.json")) == null, "no file: nothing taken");
        } catch (java.io.IOException e) {
            helper.fail("temp file: " + e);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ pure rules

    /**
     * The numbers of the concept: tier 2 x points up to 30 (15 bookshelves), then on to 40 at 30 points (15 blazewood
     * shelves), the floor +10 from 15 points, 50 only with both (owner N32); price and budget of owner N35: value by anvil cost and
     * max level, +1/4 tax per further enchantment, at most 50 levels, budget = tier (3..50); the concept's example table;
     * lapis 1 per started 10 levels, blaze powder twice that.
     */
    public static void budgetAndCostRules(GameTestHelper helper) {
        eq(helper, EnchantingRules.tier(0, false), 0, "tier without shelves");
        eq(helper, EnchantingRules.tier(15, false), 30, "tier of 15 bookshelves");
        eq(helper, EnchantingRules.tier(30, false), 40, "tier of 15 blazewood shelves without floor (owner N32: 40)");
        eq(helper, EnchantingRules.tier(20, false), 33, "tier of 10 bookshelves + 5 blazewood shelves");
        eq(helper, EnchantingRules.tier(16, false), 31, "one blazewood shelf more than 15 bookshelves");
        eq(helper, EnchantingRules.tier(14, true), 28, "tier of 14 points on the floor (below 15 the floor adds nothing)");
        eq(helper, EnchantingRules.tier(15, true), 40, "tier of 15 bookshelves on the floor");
        eq(helper, EnchantingRules.tier(23, true), 45, "tier of 23 points on the floor");
        eq(helper, EnchantingRules.tier(30, true), 50, "tier of 15 blazewood shelves on the floor: the maximum needs both");
        // Every combination of shelf points and floor (owner N32): 0-30 bookshelves only, 30-40 blazewood shelves OR floor, 50 both.
        for (int points = 0; points <= EnchantingRules.MAX_POINTS; points++) {
            for (boolean floor : new boolean[] {false, true}) {
                int tier = EnchantingRules.tier(points, floor);
                String at = " at " + points + " points, floor " + floor;
                if (points > 0) {
                    helper.assertTrue(tier >= EnchantingRules.tier(points - 1, floor), "a further shelf point never lowers the tier" + at);
                }
                helper.assertTrue(tier >= EnchantingRules.tier(points, false), "the floor never lowers the tier" + at);
                helper.assertTrue(tier <= 30 || points > EnchantingRules.MAX_SHELVES || floor, "above 30 needs blazewood shelves or the floor" + at);
                helper.assertTrue(tier <= 40 || (floor && points > EnchantingRules.MAX_SHELVES), "above 40 needs both" + at);
                helper.assertTrue((tier == 50) == (floor && points == EnchantingRules.MAX_POINTS), "50 exactly with all-blazewood shelves and floor" + at);
                helper.assertTrue(floor || tier <= 40, "no floor: at most 40" + at);
                helper.assertTrue(points > EnchantingRules.MAX_SHELVES || tier <= (floor && points == EnchantingRules.MAX_SHELVES ? 40 : 30),
                        "up to 15 points (bookshelves) 30, with floor 40 at 15" + at);
                int step = EnchantingRules.nextStep(points, floor);
                helper.assertTrue(step == (points < 15 ? EnchantingRules.STEP_BOOKSHELVES
                        : points == 30 ? (floor ? EnchantingRules.STEP_MAX : EnchantingRules.STEP_FLOOR)
                        : floor ? EnchantingRules.STEP_BLAZE_SHELVES : EnchantingRules.STEP_SHELF_OR_FLOOR), "next step" + at);
            }
        }
        eq(helper, EnchantingRules.tier(30, true), EnchantingRules.TIER_MAX, "the maximum is reachable");
        int[] fifteenBlaze = new int[32];
        java.util.Arrays.fill(fifteenBlaze, 0, 15, EnchantingRules.BLAZE_SHELF);
        eq(helper, EnchantingRules.shelfPoints(fifteenBlaze), 30, "points of 15 blazewood shelves");
        int[] mixed = new int[32];
        java.util.Arrays.fill(mixed, 1);
        mixed[0] = 2;
        mixed[1] = 2;
        eq(helper, EnchantingRules.shelfPoints(mixed), 17, "best 15 of 2 blazewood + 30 bookshelves");
        // Budget (owner N35): the tier in levels, at least 3, at most 50 - no "unlimited" any more.
        eq(helper, EnchantingRules.budget(0), 3, "budget without shelves");
        eq(helper, EnchantingRules.budget(30), 30, "budget at 30");
        eq(helper, EnchantingRules.budget(40), 40, "budget at 40");
        eq(helper, EnchantingRules.budget(50), 50, "budget at 50");
        eq(helper, EnchantingRules.budget(60), 60, "budget at 60 (Astral Enchanter)");
        eq(helper, EnchantingRules.budget(70), 60, "never above 60");
        // ENCH01 (owner 03.11.): the stages. Dark: any obsidian floor +10 (Nihil too); Astral: Nihil +20, Blazing half (+10).
        var dark = EnchanterStage.DARK;
        var astral = EnchanterStage.ASTRAL;
        for (int points = 0; points <= EnchantingRules.MAX_POINTS; points++) {
            eq(helper, EnchantingRules.tier(dark, points, EnchantingRules.FLOOR_BLAZING), EnchantingRules.tier(points, true), "Dark + Blazing = the old table at " + points);
            eq(helper, EnchantingRules.tier(dark, points, EnchantingRules.FLOOR_NIHIL), EnchantingRules.tier(points, true), "Dark + Nihil = Dark + Blazing at " + points);
            eq(helper, EnchantingRules.tier(astral, points, EnchantingRules.FLOOR_NONE), EnchantingRules.tier(points, false), "Astral without floor = shelves at " + points);
            eq(helper, EnchantingRules.tier(astral, points, EnchantingRules.FLOOR_BLAZING), EnchantingRules.tier(points, true), "Astral + Blazing works half at " + points);
            helper.assertTrue(EnchantingRules.tier(astral, points, EnchantingRules.FLOOR_NIHIL) >= EnchantingRules.tier(astral, points, EnchantingRules.FLOOR_BLAZING),
                    "Nihil never below Blazing at " + points);
        }
        eq(helper, EnchantingRules.tier(astral, 30, EnchantingRules.FLOOR_NIHIL), EnchantingRules.TIER_END, "Astral + Nihil + 15 blazewood shelves = 60");
        eq(helper, EnchantingRules.tier(astral, 15, EnchantingRules.FLOOR_NIHIL), 50, "Astral + Nihil + 15 bookshelves = 50");
        eq(helper, EnchantingRules.tier(dark, 30, EnchantingRules.FLOOR_NIHIL), EnchantingRules.TIER_MAX, "Dark never above 50");
        eq(helper, EnchantingRules.nextStep(astral, 30, EnchantingRules.FLOOR_BLAZING), EnchantingRules.STEP_NIHIL_FLOOR, "Astral on Blazing: Nihil next");
        eq(helper, EnchantingRules.nextStep(astral, 30, EnchantingRules.FLOOR_NIHIL), EnchantingRules.STEP_MAX, "Astral on Nihil: done");
        eq(helper, EnchantingRules.nextStep(dark, 30, EnchantingRules.FLOOR_NIHIL), EnchantingRules.STEP_MAX, "Dark on Nihil: done");
        for (int tier = 1; tier <= 50; tier++) {
            helper.assertTrue(EnchantingRules.budget(tier) >= EnchantingRules.budget(tier - 1), "the budget grows with the tier at " + tier);
        }
        // Value: rarity base by Vanilla anvil cost + 2 per maximum level.
        eq(helper, EnchantingRules.value(1, 5), 14, "value of Sharpness V / Efficiency V");
        eq(helper, EnchantingRules.value(4, 3), 15, "value of Looting III / Fortune III");
        eq(helper, EnchantingRules.value(4, 2), 13, "value of Fire Aspect II");
        eq(helper, EnchantingRules.value(2, 3), 12, "value of Unbreaking III");
        eq(helper, EnchantingRules.value(8, 1), 14, "value of Silk Touch");
        eq(helper, EnchantingRules.value(1, 4), 12, "value of Protection IV");
        eq(helper, EnchantingRules.levelPrice(1, 5, 14), 3, "Sharpness I");
        eq(helper, EnchantingRules.levelPrice(3, 5, 14), 9, "Sharpness III");
        eq(helper, EnchantingRules.levelPrice(5, 5, 14), 14, "Sharpness V");
        eq(helper, EnchantingRules.levelPrice(0, 5, 14), 0, "level 0 is free");
        // The concept's examples (tax: +1/4 of the sum per further enchantment, at most 50).
        int[] sword = {5, 3, 2}, swordMax = {5, 3, 2}, swordValue = {14, 15, 13};
        eq(helper, EnchantingRules.basePrice(sword, swordMax, swordValue), 42, "sword sum");
        eq(helper, EnchantingRules.price(sword, swordMax, swordValue), 50, "Sharpness V + Looting III + Fire Aspect II");
        int[] pick = {5, 3, 3}, pickMax = {5, 3, 3}, pickValue = {14, 15, 12};
        eq(helper, EnchantingRules.price(pick, pickMax, pickValue), 50, "Efficiency V + Fortune III + Unbreaking III");
        int[] weakBook = {4, 2, 3}, weakMax = {4, 2, 3}, weakValue = {12, 10, 10};
        eq(helper, EnchantingRules.price(weakBook, weakMax, weakValue), 48, "book Protection IV + Knockback II + Loyalty III: three weak below 50");
        eq(helper, EnchantingRules.price(new int[] {2, 1, 1}, new int[] {5, 3, 2}, new int[] {14, 12, 10}), 22, "Sharpness II + Unbreaking I + Knockback I (scaled, N37)");
        eq(helper, EnchantingRules.price(new int[] {5, 0, 0}, swordMax, swordValue), 14, "one enchantment: no tax");
        eq(helper, EnchantingRules.price(new int[] {5, 3, 0}, swordMax, swordValue), 32, "two enchantments: +1/4, scaled (N37)");
        eq(helper, EnchantingRules.price(new int[] {5, 0, 3}, pickMax, pickValue), 29, "Efficiency V + Unbreaking III (scaled, N37)");
        eq(helper, EnchantingRules.price(new int[3], swordMax, swordValue), 0, "nothing chosen");
        eq(helper, EnchantingRules.price(new int[] {1, 0, 0}, swordMax, swordValue), 3, "Sharpness I without shelves fits the budget of 3");
        // Max 50 only for the full choice, and every level less costs less (owner N37: 50 stayed 50 one level lower).
        int[][][] offers = {{swordMax, swordValue}, {pickMax, pickValue}, {weakMax, weakValue}, {{3, 3, 1}, {12, 12, 14}}};
        for (int[][] offer : offers) {
            int[] max = offer[0], value = offer[1];
            for (int a = 0; a <= max[0]; a++) {
                for (int b = 0; b <= max[1]; b++) {
                    for (int c = 0; c <= max[2]; c++) {
                        int[] levels = {a, b, c};
                        int price = EnchantingRules.price(levels, max, value);
                        boolean full = a == max[0] && b == max[1] && c == max[2];
                        helper.assertTrue(full ? price <= EnchantingRules.MAX_PRICE : price < EnchantingRules.MAX_PRICE,
                                "price " + price + " at " + a + "/" + b + "/" + c);
                        for (int row = 0; row < 3; row++) {
                            if (levels[row] == 0) continue;
                            int[] less = levels.clone();
                            less[row]--;
                            helper.assertTrue(EnchantingRules.price(less, max, value) < price,
                                    "one level less at row " + row + " does not cost less than " + price + " at " + a + "/" + b + "/" + c);
                        }
                    }
                }
            }
        }
        eq(helper, EnchantingRules.price(new int[] {3, 2, 1}, new int[] {3, 3, 1}, new int[] {12, 12, 14}), 45,
                "Thrift III + Unbreaking II + Silk Touch: one level below the full 50 (owner screenshot N37)");
        eq(helper, EnchantingRules.maxAffordable(2, new int[] {5, 3, 0}, swordMax, swordValue, EnchantingRules.budget(50)), 2, "tier 50: all three at max");
        eq(helper, EnchantingRules.maxAffordable(2, new int[] {5, 3, 0}, swordMax, swordValue, EnchantingRules.budget(49)), 1, "tier 49: the third only at level 1");
        eq(helper, EnchantingRules.maxAffordable(1, new int[] {5, 0, 0}, swordMax, swordValue, EnchantingRules.budget(30)), 2, "tier 30: Looting II next to Sharpness V (30 levels)");
        eq(helper, EnchantingRules.maxAffordable(1, new int[] {5, 0, 0}, swordMax, swordValue, EnchantingRules.budget(40)), 3, "tier 40: Looting III next to Sharpness V");
        eq(helper, EnchantingRules.maxAffordable(0, new int[3], swordMax, swordValue, EnchantingRules.budget(0)), 1, "without shelves: Sharpness I");
        eq(helper, EnchantingRules.maxAffordable(1, new int[3], swordMax, swordValue, EnchantingRules.budget(0)), 0, "without shelves: Looting greyed out");
        eq(helper, EnchantingRules.maxAffordable(0, new int[] {0, 3, 3}, pickMax, pickValue, EnchantingRules.budget(40)), 2, "tier 40: next to Fortune III + Unbreaking III Efficiency II (scaled price, N37)");
        eq(helper, EnchantingRules.maxAffordable(0, new int[] {0, 3, 3}, pickMax, pickValue, EnchantingRules.budget(50)), 5, "tier 50: Efficiency V as well");
        // Lapis 1 per started 10 levels, blaze powder twice that.
        eq(helper, EnchantingRules.lapisCost(0), 0, "no lapis without a choice");
        eq(helper, EnchantingRules.lapisCost(3), 1, "lapis for 3 levels");
        eq(helper, EnchantingRules.lapisCost(30), 3, "lapis for 30 levels");
        eq(helper, EnchantingRules.lapisCost(50), 5, "lapis for 50 levels");
        // N37: the price is the level needed; enchanting uses up 1 level per started 10 of it (1-5), like Vanilla's 1-3.
        eq(helper, EnchantingRules.levelCost(0), 0, "nothing chosen costs nothing");
        eq(helper, EnchantingRules.levelCost(3), 1, "Sharpness I needs Lv 3, costs 1");
        eq(helper, EnchantingRules.levelCost(27), 3, "needs Lv 27, costs 3");
        eq(helper, EnchantingRules.levelCost(50), 5, "needs Lv 50, costs 5");
        eq(helper, EnchantingRules.blazePowderCost(50), 10, "blaze powder twice the lapis");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the world

    private static void floor(GameTestHelper helper, Block block) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(TABLE.offset(dx, -1, dz), block);
            }
        }
    }

    /** Fills Vanilla's 32 shelf spots: the first {@code blaze} with blazewood shelves, the rest with bookshelves. */
    private static void shelves(GameTestHelper helper, int count, int blaze) {
        List<BlockPos> offsets = EnchantingTableBlock.BOOKSHELF_OFFSETS;
        for (int i = 0; i < offsets.size(); i++) {
            Block block = i >= count ? Blocks.AIR : i < blaze ? building("crimson_blazewood_bookshelf") : Blocks.BOOKSHELF;
            helper.setBlock(TABLE.offset(offsets.get(i)), block);
        }
    }

    private static int tier(GameTestHelper helper) {
        return EnchantingRules.tierAt(helper.getLevel(), helper.absolutePos(TABLE));
    }

    private static int astralTier(GameTestHelper helper) {
        return EnchantingRules.tierAt(helper.getLevel(), helper.absolutePos(TABLE), EnchanterStage.ASTRAL);
    }

    /** A building block of this installation: SimpleBuilding's, or this module's twin without it. */
    // ------------------------------------------------------------------ DW04C-3: End bookshelves, chiseled bookshelves, mod books

    private static Block byId(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    private static net.minecraft.world.item.Item itemById(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * Simple End's bookshelves are worth {@value EnchantingRules#BLAZE_SHELF} at the enchanters and count like a bookshelf at
     * Vanilla's table; every chiseled bookshelf (Vanilla's, the blazewood ones, the End ones) counts nothing, like Vanilla's.
     */
    public static void endShelvesCountDouble(GameTestHelper helper) {
        BlockPos offset = EnchantingTableBlock.BOOKSHELF_OFFSETS.getFirst();
        BlockPos table = helper.absolutePos(TABLE);
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        for (String wood : new String[] {"ashwood", "duskwood"}) {
            Block end = byId("simpleend:" + wood + "_bookshelf");
            if (end == Blocks.AIR) continue;     // Simple End not installed (standalone run)
            helper.assertTrue(end.defaultBlockState().is(EnchantingRules.END_SHELVES), wood + " bookshelf in end_shelves");
            helper.assertTrue(end.defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER), wood + " counts at Vanilla's table");
            helper.setBlock(TABLE.offset(offset), end);
            int value = EnchantingRules.shelfValue(helper.getLevel(), table, offset);
            helper.assertTrue(value == EnchantingRules.BLAZE_SHELF, wood + " bookshelf worth " + EnchantingRules.BLAZE_SHELF + ", got " + value);
        }
        List<Block> chiseled = new java.util.ArrayList<>(List.of(Blocks.CHISELED_BOOKSHELF, building("chiseled_crimson_blazewood_bookshelf"),
                building("chiseled_warped_blazewood_bookshelf"), byId("simpleend:chiseled_ashwood_bookshelf"), byId("simpleend:chiseled_duskwood_bookshelf")));
        chiseled.removeIf(b -> b == Blocks.AIR);
        for (Block block : chiseled) {
            helper.setBlock(TABLE.offset(offset), block);
            helper.assertTrue(EnchantingRules.shelfValue(helper.getLevel(), table, offset) == 0, BuiltInRegistries.BLOCK.getKey(block) + " counts nothing");
            helper.assertTrue(!block.defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER), BuiltInRegistries.BLOCK.getKey(block) + " no power");
        }
        helper.succeed();
    }

    /**
     * Every chiseled bookshelf (Vanilla's and the mod ones) takes Vanilla books and, with SimpleBuilding, the Blaze and Nihilit
     * Books (tag minecraft:bookshelf_books); a book sets its slot's state and the comparator reads the last slot + 1 like Vanilla.
     */
    public static void chiseledShelvesTakeModBooks(GameTestHelper helper) {
        List<Block> shelves = new java.util.ArrayList<>(List.of(Blocks.CHISELED_BOOKSHELF, building("chiseled_crimson_blazewood_bookshelf"),
                building("chiseled_warped_blazewood_bookshelf"), byId("simpleend:chiseled_ashwood_bookshelf"), byId("simpleend:chiseled_duskwood_bookshelf")));
        shelves.removeIf(b -> b == Blocks.AIR);
        helper.assertTrue(shelves.size() >= 3, "Vanilla's and both blazewood chiseled bookshelves exist");
        List<ItemStack> books = new java.util.ArrayList<>(List.of(new ItemStack(Items.BOOK), new ItemStack(Items.ENCHANTED_BOOK)));
        for (String id : new String[] {"simplebuilding:blaze_book", "simplebuilding:nihilith_book"}) {
            var item = itemById(id);
            if (item != Items.AIR) books.add(new ItemStack(item));
            helper.assertTrue(item != Items.AIR || !SimpleEnchanting.simpleBuilding(), id + " exists with SimpleBuilding");
        }
        BlockPos at = new BlockPos(1, 2, 1);
        for (Block block : shelves) {
            helper.setBlock(at, block);
            var entity = helper.getBlockEntity(at, net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity.class);
            for (int slot = 0; slot < books.size() && slot < 6; slot++) {
                ItemStack book = books.get(slot).copy();
                helper.assertTrue(entity.canPlaceItem(slot, book), BuiltInRegistries.BLOCK.getKey(block) + " takes " + book.getItem());
                entity.setItem(slot, book);
                var state = helper.getBlockState(at);
                helper.assertTrue(state.getValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot)),
                        BuiltInRegistries.BLOCK.getKey(block) + " slot " + slot + " shows the book");
                int signal = state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(at), Direction.NORTH);
                helper.assertTrue(signal == slot + 1, BuiltInRegistries.BLOCK.getKey(block) + " comparator " + signal + " after slot " + slot);
            }
            helper.assertTrue(!entity.canPlaceItem(5, new ItemStack(Items.PAPER)), "paper is no book");
            helper.setBlock(at, Blocks.AIR);
        }
        helper.succeed();
    }

    /** An End bookshelf broken without Silk Touch drops 3 books: Nihilit Books with SimpleBuilding (it is made of them), else books. */
    public static void endShelfDropsItsBooks(GameTestHelper helper) {
        Block end = byId("simpleend:ashwood_bookshelf");
        if (end == Blocks.AIR) {
            helper.succeed();
            return;
        }
        var expected = SimpleEnchanting.simpleBuilding() ? itemById("simplebuilding:nihilith_book") : Items.BOOK;
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, end);
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        AABB box = new AABB(helper.absolutePos(at)).inflate(2);
        int count = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(e -> e.getItem().is(expected)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(count == 3, "3 x " + BuiltInRegistries.ITEM.getKey(expected) + " dropped, got " + count);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    static Block building(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(SimpleEnchanting.buildingNamespace(), path));
    }

    static net.minecraft.world.item.Item item(String namespace, String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace, path));
    }

    static boolean ores() {
        return item("simpleores", "enderite_nugget") != Items.AIR;
    }

    /**
     * Shelves count like Vanilla's (blocked gap = no shelf), blazewood shelves twice; the 5x5 blazing obsidian floor
     * adds 10 from 15 points on, and only a complete floor counts.
     */
    public static void shelvesAndFloorSetTheTier(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        eq(helper, tier(helper), 0, "tier without shelves");
        shelves(helper, 5, 0);
        eq(helper, tier(helper), 10, "tier of 5 bookshelves");
        shelves(helper, 15, 0);
        eq(helper, tier(helper), 30, "tier of 15 bookshelves");
        // A block in the gap between table and shelf hides the shelf (Vanilla's transmitter rule).
        BlockPos first = EnchantingTableBlock.BOOKSHELF_OFFSETS.get(0);
        shelves(helper, 1, 1);
        eq(helper, EnchantingRules.shelfValue(helper.getLevel(), helper.absolutePos(TABLE), first), EnchantingRules.BLAZE_SHELF,
                "value of a blazewood shelf");
        helper.setBlock(TABLE.offset(first.getX() / 2, first.getY(), first.getZ() / 2), Blocks.STONE);
        eq(helper, EnchantingRules.shelfValue(helper.getLevel(), helper.absolutePos(TABLE), first), 0, "value of a shelf behind stone");
        helper.setBlock(TABLE.offset(first.getX() / 2, first.getY(), first.getZ() / 2), Blocks.AIR);
        shelves(helper, 10, 10);
        eq(helper, tier(helper), 33, "tier of 10 blazewood shelves (20 points) without floor");
        shelves(helper, 15, 5);
        eq(helper, tier(helper), 33, "tier of 5 blazewood + 10 bookshelves");
        shelves(helper, 15, 15);
        eq(helper, tier(helper), 40, "tier of 15 blazewood shelves without floor (owner N32)");
        shelves(helper, 15, 0);
        floor(helper, building("blazing_obsidian"));
        eq(helper, tier(helper), 40, "tier of 15 bookshelves on the floor");
        floor(helper, building("nihil_obsidian"));
        eq(helper, tier(helper), 40, "Nihil Obsidian is a floor of the Dark Enchanter too (owner 03.11.)");
        floor(helper, building("blazing_obsidian"));
        shelves(helper, 15, 8);
        eq(helper, tier(helper), 45, "tier of 8 blazewood + 7 bookshelves (23 points) on the floor");
        shelves(helper, 15, 15);
        eq(helper, tier(helper), 50, "tier of 15 blazewood shelves on the floor: both");
        helper.setBlock(TABLE.offset(2, -1, 2), Blocks.OBSIDIAN);
        eq(helper, tier(helper), 40, "tier of 15 blazewood shelves with one floor block missing");
        helper.setBlock(TABLE.offset(2, -1, 2), building("nihil_obsidian"));
        eq(helper, tier(helper), 50, "a mixed Blazing/Nihil floor counts at the Dark Enchanter");
        // The Astral Enchanter: Nihil Obsidian +20 (60), Blazing (or mixed) only half, +10 (50).
        helper.setBlock(TABLE, EnchantingRegistry.ASTRAL_ENCHANTER);
        eq(helper, astralTier(helper), 50, "Astral on a mixed floor: half");
        floor(helper, building("blazing_obsidian"));
        eq(helper, astralTier(helper), 50, "Astral on Blazing Obsidian: half (owner 03.11.)");
        floor(helper, building("nihil_obsidian"));
        eq(helper, astralTier(helper), 60, "Astral on Nihil Obsidian: 60");
        eq(helper, EnchantingRules.floorKind(helper.getLevel(), helper.absolutePos(TABLE)), EnchantingRules.FLOOR_NIHIL, "floor kind");
        helper.succeed();
    }

    /**
     * Made in the world (ENCH01 concept, owner 03.11.): the shipped upgrade file turns a Vanilla enchanting table into the Dark
     * Enchanter with 8 Netherite nuggets (one per 2 strikes, from a Netherite hammer, 35 durability a strike) and - only with
     * Simple Ores - the Dark into the Astral Enchanter with 8 Enderite nuggets from an Enderite hammer. SimpleLib runs the
     * entries (and the Vanilla axe way without a hammer mod).
     */
    public static void upgradesAreLoaded(GameTestHelper helper) {
        var dark = InWorldUpgrades.entryFor(Blocks.ENCHANTING_TABLE);
        helper.assertTrue(dark != null && dark.to() == EnchantingRegistry.DARK_ENCHANTER && dark.material() == item("simplelib", "netherite_nugget")
                && dark.materialCount() == 8 && dark.strikesPerMaterial() == 2 && dark.damagePerHit() == 35 && "netherite".equals(dark.minTool()),
                "enchanting table -> Dark Enchanter: " + dark);
        var astral = InWorldUpgrades.entryFor(EnchantingRegistry.DARK_ENCHANTER);
        if (ores()) {
            helper.assertTrue(astral != null && astral.to() == EnchantingRegistry.ASTRAL_ENCHANTER && astral.material() == item("simpleores", "enderite_nugget")
                    && astral.materialCount() == 8 && astral.strikesPerMaterial() == 2 && "enderite".equals(astral.minTool()),
                    "Dark -> Astral Enchanter: " + astral);
        } else {
            helper.assertTrue(astral == null, "without Simple Ores there is no Astral upgrade: " + astral);
        }
        helper.succeed();
    }

    /**
     * Twice as hard to break as the Vanilla table; WI26-1: the Dark Enchanter drops a Vanilla enchanting table, the 8 Netherite
     * nuggets it was made with and the stored lapis and blaze powder; the Astral Enchanter (with Simple Ores) the Dark Enchanter
     * and 8 Enderite nuggets.
     */
    public static void breakingDropsTableNuggetAndStock(GameTestHelper helper) {
        BlockPos abs = helper.absolutePos(TABLE);
        float vanilla = Blocks.ENCHANTING_TABLE.defaultBlockState().getDestroySpeed(helper.getLevel(), abs);
        float dark = EnchantingRegistry.DARK_ENCHANTER.defaultBlockState().getDestroySpeed(helper.getLevel(), abs);
        helper.assertTrue(dark == 2 * vanilla, "hardness " + dark + " is not twice the enchanting table's " + vanilla);
        helper.setBlock(TABLE.below(), Blocks.STONE);
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        table.setItem(EnchanterBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 10));
        table.setItem(EnchanterBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 20));
        helper.getLevel().destroyBlock(abs, true);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2.0));
        eq(helper, count(drops, Items.ENCHANTING_TABLE), 1, "enchanting tables dropped");
        eq(helper, count(drops, item("simplelib", "netherite_nugget")), 8, "Netherite nuggets dropped");
        eq(helper, count(drops, Items.LAPIS_LAZULI), 10, "lapis dropped");
        eq(helper, count(drops, Items.BLAZE_POWDER), 20, "blaze powder dropped");
        eq(helper, count(drops, EnchantingRegistry.DARK_ENCHANTER_ITEM), 0, "Dark Enchanters dropped");
        drops.forEach(ItemEntity::discard);
        if (ores()) {
            helper.setBlock(TABLE, EnchantingRegistry.ASTRAL_ENCHANTER);
            helper.getLevel().destroyBlock(abs, true);
            drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2.0));
            eq(helper, count(drops, EnchantingRegistry.DARK_ENCHANTER_ITEM), 1, "the Astral Enchanter drops the Dark Enchanter");
            eq(helper, count(drops, item("simpleores", "enderite_nugget")), 8, "Enderite nuggets dropped");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    private static int count(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static EnchanterMenu open(GameTestHelper helper, ServerPlayer player) {
        var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        EnchanterMenu menu = new EnchanterMenu(1, player.getInventory(), table,
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
        player.containerMenu = menu;
        return menu;
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        helper.runBeforeTestEnd(() -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    /**
     * Lapis and blaze powder go into the table's own slots (shift-click) and stay there when the menu closes; the item
     * to enchant goes back to the player. Only lapis and blaze powder fit, at most one stack each.
     */
    public static void stockStaysInTheTable(GameTestHelper helper) {
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        ServerPlayer player = survivor(helper);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 64));
        player.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 10));
        player.getInventory().setItem(2, new ItemStack(Items.BLAZE_POWDER, 30));
        player.getInventory().setItem(3, new ItemStack(Items.DIAMOND_SWORD));
        EnchanterMenu menu = open(helper, player);
        for (int i = 0; i < 4; i++) {
            int slot = slotOf(menu, player, i);
            menu.quickMoveStack(player, slot);
        }
        var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        eq(helper, table.getItem(EnchanterBlockEntity.LAPIS).getCount(), 64, "lapis in the table (one stack)");
        eq(helper, table.getItem(EnchanterBlockEntity.BLAZE).getCount(), 30, "blaze powder in the table");
        helper.assertTrue(menu.getSlot(EnchanterMenu.ITEM_SLOT).getItem().is(Items.DIAMOND_SWORD), "the sword is not in the item slot");
        helper.assertFalse(table.canPlaceItem(EnchanterBlockEntity.LAPIS, new ItemStack(Items.BLAZE_POWDER)), "blaze powder fits the lapis slot");
        helper.assertFalse(table.canTakeItemThroughFace(EnchanterBlockEntity.LAPIS, table.getItem(0), Direction.DOWN),
                "a hopper below could pull lapis out");
        menu.removed(player);
        player.containerMenu = player.inventoryMenu;
        eq(helper, table.getItem(EnchanterBlockEntity.LAPIS).getCount(), 64, "lapis after closing");
        eq(helper, table.getItem(EnchanterBlockEntity.BLAZE).getCount(), 30, "blaze powder after closing");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.DIAMOND_SWORD)), "the sword did not go back to the player");
        // Reopened: the stock is there again.
        EnchanterMenu again = open(helper, player);
        eq(helper, again.lapis(), 64, "lapis seen by a new menu");
        eq(helper, again.blazePowder(), 30, "blaze powder seen by a new menu");
        again.removed(player);
        helper.succeed();
    }

    private static int slotOf(EnchanterMenu menu, ServerPlayer player, int inventoryIndex) {
        for (int i = EnchanterMenu.PLAYER_START; i < menu.slots.size(); i++) {
            if (menu.getSlot(i).container == player.getInventory() && menu.getSlot(i).getContainerSlot() == inventoryIndex) return i;
        }
        throw new IllegalStateException("no menu slot for inventory slot " + inventoryIndex);
    }

    /**
     * With 15 bookshelves (tier 30) a pickaxe gets three fitting, compatible enchantments; a slider asked beyond the
     * budget stops at what the budget allows; enchanting applies exactly the chosen levels and uses up the levels, lapis
     * and twice the blaze powder the rules name, and gives the player a new enchantment seed.
     */
    public static void enchantingUsesUpWhatTheRulesSay(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        shelves(helper, 15, 0);
        var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        table.setItem(EnchanterBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 10));
        table.setItem(EnchanterBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 10));
        ServerPlayer player = survivor(helper);
        player.giveExperienceLevels(40);
        EnchanterMenu menu = open(helper, player);
        eq(helper, menu.tier(), 30, "tier seen by the menu");
        menu.getSlot(EnchanterMenu.ITEM_SLOT).set(new ItemStack(Items.DIAMOND_PICKAXE));
        int rows = 0;
        for (int row = 0; row < 3; row++) {
            Holder<Enchantment> holder = menu.enchantment(helper.getLevel(), row);
            if (holder == null) continue;
            rows++;
            helper.assertTrue(holder.value().isPrimaryItem(new ItemStack(Items.DIAMOND_PICKAXE)), holder + " does not fit a pickaxe");
            for (int other = 0; other < row; other++) {
                Holder<Enchantment> o = menu.enchantment(helper.getLevel(), other);
                helper.assertTrue(o == null || Enchantment.areCompatible(o, holder), o + " and " + holder + " offered together");
            }
        }
        eq(helper, rows, 3, "enchantments offered for a diamond pickaxe");
        // Every slider to its maximum, in order: what the budget does not allow is cut off.
        for (int row = 0; row < 3; row++) {
            menu.clickMenuButton(player, row * EnchanterMenu.LEVELS_PER_ROW + menu.maxLevel(row));
            helper.assertTrue(menu.chosen(row) <= menu.maxLevel(row), "slider beyond its maximum");
        }
        int spent = menu.price();
        helper.assertTrue(spent > 0 && spent <= 30, "price " + spent + " outside 1..30 at tier 30");
        int[] chosen = menu.chosenLevels();
        List<Holder<Enchantment>> offered = new java.util.ArrayList<>();
        for (int row = 0; row < 3; row++) offered.add(menu.enchantment(helper.getLevel(), row));
        int levels = spent;
        int seed = player.getEnchantmentSeed();
        int before = player.experienceLevel;
        helper.assertTrue(menu.clickMenuButton(player, EnchanterMenu.BUTTON_ENCHANT), "enchanting refused");
        ItemStack result = menu.getSlot(EnchanterMenu.ITEM_SLOT).getItem();
        for (int row = 0; row < 3; row++) {
            eq(helper, EnchantmentHelper.getItemEnchantmentLevel(offered.get(row), result), chosen[row], "level of " + offered.get(row));
        }
        eq(helper, total(result), sum(chosen), "enchantment levels on the pickaxe");
        eq(helper, player.experienceLevel, before - EnchantingRules.levelCost(levels), "only a few levels used up (N37: price = level needed)");
        eq(helper, table.getItem(EnchanterBlockEntity.LAPIS).getCount(), 10 - EnchantingRules.lapisCost(levels), "lapis left");
        eq(helper, table.getItem(EnchanterBlockEntity.BLAZE).getCount(), 10 - EnchantingRules.blazePowderCost(levels), "blaze powder left");
        helper.assertTrue(player.getEnchantmentSeed() != seed, "the enchantment seed did not change");
        eq(helper, menu.enchantId(0), -1, "offers for the already enchanted pickaxe");
        menu.removed(player);
        helper.succeed();
    }

    /**
     * BS25-22: a golden apple in the item slot becomes an Enchanted Golden Apple for 40 levels (really taken off), 3 lapis
     * and 3 blaze powder; without the levels or the stock nothing happens.
     */
    public static void goldenAppleBecomesEnchanted(GameTestHelper helper) {
        floor(helper, Blocks.STONE);
        helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
        var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
        table.setItem(EnchanterBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 5));
        table.setItem(EnchanterBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 2));
        ServerPlayer player = survivor(helper);
        player.giveExperienceLevels(45);
        EnchanterMenu menu = open(helper, player);
        menu.getSlot(EnchanterMenu.ITEM_SLOT).set(new ItemStack(Items.GOLDEN_APPLE));
        helper.assertTrue(menu.isApple() && menu.price() == 40 && menu.levelsTaken() == 40 && menu.lapisNeeded() == 3 && menu.blazeNeeded() == 3,
                "the apple offer: " + menu.price() + " levels, " + menu.lapisNeeded() + " lapis, " + menu.blazeNeeded() + " blaze powder");
        // Only 2 blaze powder: refused, nothing taken.
        helper.assertFalse(menu.clickMenuButton(player, EnchanterMenu.BUTTON_ENCHANT), "the apple was enchanted with 2 blaze powder");
        eq(helper, player.experienceLevel, 45, "levels after the refused click");
        helper.assertTrue(menu.getSlot(EnchanterMenu.ITEM_SLOT).getItem().is(Items.GOLDEN_APPLE), "the apple changed");
        table.setItem(EnchanterBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 4));
        helper.assertTrue(menu.clickMenuButton(player, EnchanterMenu.BUTTON_ENCHANT), "enchanting the apple was refused");
        helper.assertTrue(menu.getSlot(EnchanterMenu.ITEM_SLOT).getItem().is(Items.ENCHANTED_GOLDEN_APPLE), "no Enchanted Golden Apple");
        eq(helper, player.experienceLevel, 5, "levels left (45 - 40)");
        eq(helper, table.getItem(EnchanterBlockEntity.LAPIS).getCount(), 2, "lapis left");
        eq(helper, table.getItem(EnchanterBlockEntity.BLAZE).getCount(), 1, "blaze powder left");
        // Not enough levels: 5 < 40.
        menu.getSlot(EnchanterMenu.ITEM_SLOT).set(new ItemStack(Items.GOLDEN_APPLE));
        helper.assertFalse(menu.clickMenuButton(player, EnchanterMenu.BUTTON_ENCHANT), "the apple was enchanted without the levels");
        menu.removed(player);
        helper.succeed();
    }

    private static int total(ItemStack stack) {
        int sum = 0;
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) sum += entry.getIntValue();
        return sum;
    }

    private static int sum(int[] values) {
        int s = 0;
        for (int v : values) s += v;
        return s;
    }




    // ------------------------------------------------------------------ endgame levels (WP-22 / MOB27-4)

    /**
     * Endgame levels: Protection and Feather Falling reach V at the top tier only, never below tier 50 and never with the switch off;
     * unknown enchantments keep their Vanilla maximum; the hook clamps to V; the price of a level above the Vanilla maximum is the
     * plain share plus the endgame surcharge, and every price below it stays exactly as before.
     */
    public static void endgameLevelRules(GameTestHelper helper) {
        String protection = "minecraft:protection", fall = "minecraft:feather_falling";
        eq(helper, EnchantingRules.levelCap(protection, 4, 60, true), 5, "Protection V at tier 60 (Astral Enchanter)");
        eq(helper, EnchantingRules.levelCap(protection, 4, 50, true), 4, "tier 50 (the Dark Enchanter's top) stays at IV (ENCH01)");
        eq(helper, EnchantingRules.levelCap(fall, 4, 60, true), 5, "Feather Falling V at tier 60");
        eq(helper, EnchantingRules.levelCap(protection, 4, 59, true), 4, "tier 59 stays at the Vanilla maximum");
        eq(helper, EnchantingRules.levelCap(protection, 4, 40, true), 4, "tier 40 (all blazewood, no floor)");
        eq(helper, EnchantingRules.levelCap(protection, 4, 60, false), 4, "switch off");
        eq(helper, EnchantingRules.levelCap("minecraft:sharpness", 5, 60, true), 5, "Sharpness keeps V");
        eq(helper, EnchantingRules.levelCap("minecraft:unbreaking", 3, 60, true), 3, "unknown enchantments keep their maximum");
        eq(helper, EnchantingRules.levelCap("somemod:odd", 2, 60, true), 2, "foreign enchantments too");
        EnchantingRules.registerEndgame(Identifier.fromNamespaceAndPath("simpleenchanting", "test_only_endgame"), 99);
        eq(helper, EnchantingRules.levelCap("simpleenchanting:test_only_endgame", 1, 60, true), EnchantingRules.ENDGAME_MAX_LEVEL, "the hook never goes above V (VI adds nothing to a full set)");
        eq(helper, EnchantingRules.endgameLevels().get(protection), 5, "table: Protection");
        // Protection: value 12 at max IV. Levels up to IV cost what they cost; V costs the share 15 plus 6.
        eq(helper, EnchantingRules.levelPrice(4, 4, 12), 12, "Protection IV as before");
        eq(helper, EnchantingRules.levelPrice(4, 5, 4, 12), 12, "Protection IV with the endgame slider: the same");
        eq(helper, EnchantingRules.levelPrice(2, 5, 4, 12), 6, "Protection II: the same");
        eq(helper, EnchantingRules.levelPrice(5, 5, 4, 12), 21, "Protection V: share 15 + 6");
        eq(helper, EnchantingRules.levelPrice(0, 5, 4, 12), 0, "nothing");
        eq(helper, EnchantingRules.price(new int[] {5, 0, 0}, new int[] {5, 3, 3}, new int[] {4, 3, 3}, new int[] {12, 15, 12}), 17, "Protection V alone is scaled like any dear offer (N37)");
        int lastPrice = -1;
        for (int level = 0; level <= 5; level++) {
            int price = EnchantingRules.price(new int[] {level, 0, 0}, new int[] {5, 3, 3}, new int[] {4, 3, 3}, new int[] {12, 15, 12});
            helper.assertTrue(price >= lastPrice && (level == 0 || price > lastPrice), "every level costs more: " + level + " -> " + price);
            lastPrice = price;
        }
        eq(helper, EnchantingRules.maxAffordable(0, new int[3], new int[] {5, 3, 3}, new int[] {4, 3, 3}, new int[] {12, 15, 12}, EnchantingRules.budget(60)), 5, "tier 60 reaches V");
        eq(helper, EnchantingRules.maxAffordable(0, new int[3], new int[] {5, 3, 3}, new int[] {4, 3, 3}, new int[] {12, 15, 12}, EnchantingRules.budget(3)), 1, "tier 0 budget 3: only level I");
        // Vanilla's own maximum is untouched: the anvil can combine two IV books to IV only.
        Holder<Enchantment> prot = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        Holder<Enchantment> ff = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING);
        eq(helper, prot.value().getMaxLevel(), 4, "Vanilla max_level of Protection is not raised");
        eq(helper, ff.value().getMaxLevel(), 4, "Vanilla max_level of Feather Falling is not raised");
        eq(helper, EnchantingRules.levelCap(prot, 60), EnchantingConfig.endgameLevels ? 5 : 4, "holder cap at tier 60");
        eq(helper, EnchantingRules.levelCap(prot, 50), 4, "holder cap at tier 50");
        eq(helper, EnchantingRules.levelCap(prot, 30), 4, "holder cap at tier 30");
        helper.succeed();
    }

    /** Sets the seed the player's table offers are drawn from (the field is protected, no setter exists). */
    private static void setSeed(ServerPlayer player, int seed) {
        try {
            var field = net.minecraft.world.entity.player.Player.class.getDeclaredField("enchantmentSeed");
            field.setAccessible(true);
            field.setInt(player, seed);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** A seed for which the table offers {@code wanted} on {@code stack}. */
    private static int seedOffering(GameTestHelper helper, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> wanted) {
        for (int seed = 1; seed < 5000; seed++) {
            for (Holder<Enchantment> holder : EnchantingRules.choices(helper.getLevel().registryAccess(), stack, seed)) {
                if (holder.is(wanted)) return seed;
            }
        }
        throw new AssertionError("no seed offers " + wanted);
    }

    private static int rowOf(GameTestHelper helper, EnchanterMenu menu, net.minecraft.resources.ResourceKey<Enchantment> wanted) {
        for (int row = 0; row < 3; row++) {
            Holder<Enchantment> holder = menu.enchantment(helper.getLevel(), row);
            if (holder != null && holder.is(wanted)) return row;
        }
        throw new AssertionError(wanted + " is not offered");
    }

    /**
     * At the Astral Enchanter's top tier (15 blazewood shelves and a Nihil Obsidian floor) the Protection slider reaches V and the
     * result carries Protection V; on Blazing Obsidian (half: tier 50), at the Dark Enchanter (top 50) and with the switch off it
     * ends at IV.
     */
    public static void endgameLevelsAtTheTable(GameTestHelper helper) {
        boolean before = EnchantingConfig.endgameLevels;
        try {
            EnchantingConfig.endgameLevels = true;
            shelves(helper, 15, 15);
            floor(helper, building("blazing_obsidian"));
            helper.setBlock(TABLE, EnchantingRegistry.ASTRAL_ENCHANTER);
            var table = helper.getBlockEntity(TABLE, EnchanterBlockEntity.class);
            table.setItem(EnchanterBlockEntity.LAPIS, new ItemStack(Items.LAPIS_LAZULI, 10));
            table.setItem(EnchanterBlockEntity.BLAZE, new ItemStack(Items.BLAZE_POWDER, 10));
            ServerPlayer player = survivor(helper);
            player.giveExperienceLevels(60);
            ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            var protection = net.minecraft.world.item.enchantment.Enchantments.PROTECTION;
            // Astral on Blazing Obsidian: half the floor, tier 50, IV.
            setSeed(player, seedOffering(helper, chestplate, protection));
            EnchanterMenu low = open(helper, player);
            low.getSlot(EnchanterMenu.ITEM_SLOT).set(chestplate.copy());
            eq(helper, low.tier(), 50, "Astral on Blazing Obsidian");
            helper.assertTrue(low.stage() == EnchanterStage.ASTRAL, "the menu does not know the Astral stage");
            eq(helper, low.maxLevel(rowOf(helper, low, protection)), 4, "Protection ends at IV below the top tier");
            low.removed(player);
            // Astral on Nihil Obsidian: tier 60.
            floor(helper, building("nihil_obsidian"));
            setSeed(player, seedOffering(helper, chestplate, protection));
            EnchanterMenu menu = open(helper, player);
            menu.getSlot(EnchanterMenu.ITEM_SLOT).set(chestplate.copy());
            eq(helper, menu.tier(), 60, "top tier");
            eq(helper, menu.budget(), 60, "budget of the top tier");
            int row = rowOf(helper, menu, protection);
            eq(helper, menu.maxLevel(row), 5, "Protection V at the top tier");
            eq(helper, menu.baseMax(row), 4, "the Vanilla maximum is kept next to it");
            helper.assertTrue(menu.levelPrice(row, 5) > menu.levelPrice(row, 4), "V costs more than IV");
            helper.assertTrue(menu.clickMenuButton(player, row * EnchanterMenu.LEVELS_PER_ROW + 5), "slider click refused");
            eq(helper, menu.chosen(row), 5, "slider at V");
            // Read the offer before enchanting: the enchanted item draws a new offer.
            Holder<Enchantment> holder = menu.enchantment(helper.getLevel(), row);
            helper.assertTrue(menu.clickMenuButton(player, EnchanterMenu.BUTTON_ENCHANT), "enchanting refused");
            ItemStack result = menu.getSlot(EnchanterMenu.ITEM_SLOT).getItem();
            eq(helper, EnchantmentHelper.getItemEnchantmentLevel(holder, result), 5, "Protection V on the chestplate");
            menu.removed(player);
            // The Dark Enchanter on the same floor: tier 50, IV.
            helper.setBlock(TABLE, EnchantingRegistry.DARK_ENCHANTER);
            setSeed(player, seedOffering(helper, chestplate, protection));
            EnchanterMenu darkMenu = open(helper, player);
            darkMenu.getSlot(EnchanterMenu.ITEM_SLOT).set(chestplate.copy());
            eq(helper, darkMenu.tier(), 50, "Dark on Nihil Obsidian");
            eq(helper, darkMenu.maxLevel(rowOf(helper, darkMenu, protection)), 4, "the Dark Enchanter keeps the Vanilla maximum");
            darkMenu.removed(player);
            // Switch off: the Astral table offers IV only.
            helper.setBlock(TABLE, EnchantingRegistry.ASTRAL_ENCHANTER);
            EnchantingConfig.endgameLevels = false;
            setSeed(player, seedOffering(helper, chestplate, protection));
            EnchanterMenu off = open(helper, player);
            off.getSlot(EnchanterMenu.ITEM_SLOT).set(chestplate.copy());
            eq(helper, off.maxLevel(rowOf(helper, off, protection)), 4, "switch off: Vanilla maximum");
            off.removed(player);
        } finally {
            EnchantingConfig.endgameLevels = before;
        }
        helper.succeed();
    }

    private static void eq(GameTestHelper helper, int actual, int expected, String what) {
        helper.assertValueEqual(actual, expected, what);
    }
}
