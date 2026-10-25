package com.simplegadgets.test;

import com.simplegadgets.GadgetEnchantments;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.gauge.VelocityGaugeItem;
import com.simplelib.api.IdMoves;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Gadgets (OW24-2 G1): registration, the sub-mod switch, the migration of SimpleBuilding's old ids (item, recipe,
 * unlock; a stack and a chest of an old world), the Gauge's recipes with and without SimpleBuilding, its auto-walk rules
 * and its altimeter. The Gauge bodies moved from SimpleBuilding's {@code TweaksTests}. Loader-neutral; the Fabric adapter
 * registers {@link #ALL} as module_game_test_<name> on {@code simplegadgets:empty}; every test runs standalone and with
 * SimpleBuilding (parts that need SimpleBuilding's items or enchantments check for them).
 */
public final class GadgetsTests {
    /** Test name -> body, in catalogue order (the Fabric adapter names them module_game_test_<name>). */
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    private static final Identifier COPPER_CORE = Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "copper_core");

    static {
        ALL.put("registered", GadgetsTests::registered);
        ALL.put("sub_mod_switch", GadgetsTests::subModSwitch);
        ALL.put("migration_from_simplebuilding", GadgetsTests::migrationFromSimplebuilding);
        ALL.put("gauge_recipe", GadgetsTests::gaugeRecipe);
        ALL.put("gauge_autowalk_keeps_walking_under_non_pausing_screens", GadgetsTests::gaugeAutowalkKeepsWalkingUnderNonPausingScreens);
        ALL.put("gauge_autowalk_follows_paths_and_rails_around_corners", GadgetsTests::gaugeAutowalkFollowsPathsAndRailsAroundCorners);
        ALL.put("gauge_altimeter_reads_the_ground_and_range_reaches_deeper", GadgetsTests::gaugeAltimeterReadsTheGroundAndRangeReachesDeeper);
        ALL.put("rotator_log_axis_cycles_through_all_three_axes_and_ignores_sneaking", RotatorTests::logAxisCyclesThroughAllThreeAxesAndIgnoresSneaking);
        ALL.put("rotator_rim_is_the_outer_eighth_of_every_face_and_nowhere_inside", RotatorTests::rimIsTheOuterEighthOfEveryFaceAndNowhereInside);
        ALL.put("rotator_facing_blocks_turn_one_quarter_around_the_clicked_axis_or_jump_to_its_start", RotatorTests::facingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart);
        ALL.put("rotator_rim_aims_facing_blocks_at_the_rim_its_opposite_or_the_next_valid_value", RotatorTests::rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue);
        ALL.put("rotator_sixteen_step_blocks_step_once_in_the_middle_and_four_times_at_the_rim", RotatorTests::sixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim);
        ALL.put("rotator_charge_runs_down_but_the_rotator_never_breaks", RotatorTests::chargeRunsDownButTheRotatorNeverBreaks);
        ALL.put("rotator_anvil_recharges_with_sixteen_ender_pearls_for_no_levels", RotatorTests::anvilRechargesWithSixteenEnderPearlsForNoLevels);
        ALL.put("rotator_anvil_takes_unbreaking_but_refuses_mending", RotatorTests::anvilTakesUnbreakingButRefusesMending);
        ALL.put("rotator_a_turn_queues_the_ender_echo_shortly_after_the_ratchet", RotatorTests::aTurnQueuesTheEnderEchoShortlyAfterTheRatchet);
        ALL.put("rotator_crafting_takes_an_iron_core_four_iron_and_an_ender_pearl_in_that_shape", RotatorTests::craftingTakesAnIronCoreFourIronAndAnEnderPearlInThatShape);
        ALL.put("rotator_swap_replaces_the_block_gives_the_drops_and_uses_the_offhand_block", RotatorTests::swapReplacesTheBlockGivesTheDropsAndUsesTheOffhandBlock);
        ALL.put("rotator_swap_windup_grows_with_hardness_and_shrinks_with_efficiency", RotatorTests::swapWindupGrowsWithHardnessAndShrinksWithEfficiency);
        ALL.put("rotator_swap_windup_starts_on_click_and_only_finishes_on_the_same_aimed_block", RotatorTests::swapWindupStartsOnClickAndOnlyFinishesOnTheSameAimedBlock);
        ALL.put("rotator_swap_allows_stone_tool_blocks_and_keeps_valuables_and_lists_decide", RotatorTests::swapAllowsStoneToolBlocksAndKeepsValuablesAndListsDecide);
        ALL.put("rotator_swap_refusals_change_nothing_and_creative_swaps_for_free", RotatorTests::swapRefusalsChangeNothingAndCreativeSwapsForFree);
        ALL.put("discover_recipe_unlocks", com.simplegadgets.discover.GadgetsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplegadgets.discover.GadgetsDiscover::behaviourHints);
        ALL.put("guide_book", com.simplegadgets.guide.GadgetsGuide::gameTest);
    }

    private GadgetsTests() {
    }

    /** SimpleBuilding is loaded (its copper core exists) - loader-neutral, no import. */
    private static boolean withSimpleBuilding() {
        return BuiltInRegistries.ITEM.containsKey(COPPER_CORE);
    }

    public static void registered(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleGadgets.MOVED_ITEMS) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleGadgets.id(path))) problems.add("item " + path);
        }
        if (!(GadgetsRegistry.VELOCITY_GAUGE instanceof VelocityGaugeItem)
                || !BuiltInRegistries.ITEM.getKey(GadgetsRegistry.VELOCITY_GAUGE).equals(SimpleGadgets.id("velocity_gauge"))) {
            problems.add("gauge item " + GadgetsRegistry.VELOCITY_GAUGE);
        }
        if (new ItemStack(GadgetsRegistry.VELOCITY_GAUGE).getMaxStackSize() != 1) problems.add("gauge stacks");
        if (!(GadgetsRegistry.ROTATOR instanceof com.simplegadgets.rotator.RotatorItem)
                || !BuiltInRegistries.ITEM.getKey(GadgetsRegistry.ROTATOR).equals(SimpleGadgets.id("rotator"))) {
            problems.add("rotator item " + GadgetsRegistry.ROTATOR);
        }
        ItemStack transposer = new ItemStack(GadgetsRegistry.ROTATOR);
        if (transposer.getMaxStackSize() != 1 || transposer.getMaxDamage() != com.simplegadgets.config.GadgetsConfig.rotatorMaxCharge
                || !(GadgetsRegistry.ROTATOR instanceof com.simplelib.api.AnvilRechargeable)) {
            problems.add("transposer stack/charge/recharge");
        }
        if (!BuiltInRegistries.ITEM.containsKey(SimpleGadgets.id("guide_book"))) problems.add("guide book");
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleGadgets.id("gadgets"))) problems.add("tab");
        if (GadgetsRegistry.tabStacks().size() != SimpleGadgets.MOVED_ITEMS.size() + 1) problems.add("tab stacks " + GadgetsRegistry.tabStacks().size());
        helper.assertTrue(problems.isEmpty(), "registration: " + problems);
        helper.succeed();
    }

    public static void subModSwitch(GameTestHelper helper) {
        com.simplebuilding.framework.api.SubMods.set(SimpleGadgets.MOD_ID, false);
        try {
            helper.assertFalse(SimpleGadgets.on(), "switched off");
            helper.assertTrue(GadgetsRegistry.tabStacks().isEmpty() && GadgetsRegistry.gadgetStacks().isEmpty(), "no tab stacks while off");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleGadgets.id("velocity_gauge")), "registrations stay");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleGadgets.MOD_ID, true);
        }
        helper.assertTrue(SimpleGadgets.on() && !GadgetsRegistry.tabStacks().isEmpty(), "back on");
        helper.succeed();
    }

    /**
     * Old worlds: SimpleBuilding's ids resolve to the module's (item, recipe, recipe unlock), a stack saved by SimpleBuilding
     * loads as the Gauge and saves the new id, and so does a stack inside a saved chest (the container path of a chunk).
     */
    public static void migrationFromSimplebuilding(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        ServerLevel level = helper.getLevel();
        for (String path : SimpleGadgets.MOVED_ITEMS) {
            if (BuiltInRegistries.ITEM.getValue(SimpleGadgets.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleGadgets.id(path))) problems.add("item " + path);
            if (!SimpleGadgets.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleGadgets.old(path)))) problems.add("recipe " + path);
            String advancement = "recipes/tools/" + path;
            if (!SimpleGadgets.id(advancement).equals(IdMoves.advancement(SimpleGadgets.old(advancement)))) problems.add("advancement " + path);
            String unlock = withSimpleBuilding() ? advancement : advancement + "_without_simplebuilding";
            if (level.getServer().getAdvancements().get(SimpleGadgets.id(unlock)) == null) problems.add("unlock missing " + unlock);
        }
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        // A stack of an old inventory, with a component (custom name), as SimpleBuilding saved it.
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", "simplebuilding:velocity_gauge");
        stackTag.putInt("count", 1);
        CompoundTag components = new CompoundTag();
        components.putString("minecraft:custom_name", "Old Gauge");
        stackTag.put("components", components);
        ItemStack stack = ItemStack.CODEC.parse(ops, stackTag).result().orElse(ItemStack.EMPTY);
        if (!stack.is(GadgetsRegistry.VELOCITY_GAUGE) || !stack.getHoverName().getString().equals("Old Gauge")) {
            problems.add("old stack loads as " + stack + " (" + stack.getHoverName().getString() + ")");
        } else {
            String saved = ItemStack.CODEC.encodeStart(ops, stack).result().map(t -> ((CompoundTag) t).getStringOr("id", "")).orElse("");
            if (!saved.equals("simplegadgets:velocity_gauge")) problems.add("old stack saves as " + saved);
        }
        // A chest of an old world: its block entity data holds the old id in "Items".
        BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        if (helper.getBlockEntity(chestPos, ChestBlockEntity.class) instanceof ChestBlockEntity chest) {
            CompoundTag chestTag = new CompoundTag();
            ListTag items = new ListTag();
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) 4);
            entry.putString("id", "simplebuilding:velocity_gauge");
            entry.putInt("count", 1);
            items.add(entry);
            chestTag.put("Items", items);
            chest.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                    level.registryAccess(), chestTag));
            if (!chest.getItem(4).is(GadgetsRegistry.VELOCITY_GAUGE)) {
                problems.add("old chest slot loads as " + chest.getItem(4));
            } else if (!chest.saveCustomOnly(level.registryAccess()).toString().contains("simplegadgets:velocity_gauge")) {
                problems.add("old chest saves " + chest.saveCustomOnly(level.registryAccess()));
            }
        } else {
            problems.add("no chest block entity");
        }
        // The Transposer (G2): an old stack keeps its charge (damage component) and a saved chest slot loads it.
        CompoundTag rotatorTag = new CompoundTag();
        rotatorTag.putString("id", "simplebuilding:rotator");
        rotatorTag.putInt("count", 1);
        CompoundTag rotatorComponents = new CompoundTag();
        rotatorComponents.putInt("minecraft:damage", 7);
        rotatorTag.put("components", rotatorComponents);
        ItemStack rotator = ItemStack.CODEC.parse(ops, rotatorTag).result().orElse(ItemStack.EMPTY);
        if (!rotator.is(GadgetsRegistry.ROTATOR) || rotator.getDamageValue() != 7) {
            problems.add("old Transposer loads as " + rotator + " damage " + rotator.getDamageValue());
        } else if (!ItemStack.CODEC.encodeStart(ops, rotator).result().map(t -> ((CompoundTag) t).getStringOr("id", "")).orElse("").equals("simplegadgets:rotator")) {
            problems.add("old Transposer saves with the old id");
        }
        BlockPos rotatorChest = new BlockPos(3, 1, 1);
        helper.setBlock(rotatorChest, Blocks.CHEST);
        if (helper.getBlockEntity(rotatorChest, ChestBlockEntity.class) instanceof ChestBlockEntity chest) {
            CompoundTag chestTag = new CompoundTag();
            ListTag items = new ListTag();
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) 2);
            entry.putString("id", "simplebuilding:rotator");
            entry.putInt("count", 1);
            items.add(entry);
            chestTag.put("Items", items);
            chest.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,
                    level.registryAccess(), chestTag));
            if (!chest.getItem(2).is(GadgetsRegistry.ROTATOR)) problems.add("old chest slot loads the Transposer as " + chest.getItem(2));
        } else {
            problems.add("no chest block entity for the Transposer");
        }
        helper.assertTrue(problems.isEmpty(), "migration: " + problems);
        helper.succeed();
    }

    private static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        return CraftingInput.of(3, 3, stacks);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> match(GameTestHelper helper, CraftingInput grid) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
    }

    /**
     * Gauge (26.3 layout "NAN" / "NCN" / "NKN" around a clock): with SimpleBuilding K is its Copper Core
     * ({@code simplegadgets:velocity_gauge}); alone a copper ingot takes its place
     * ({@code simplegadgets:velocity_gauge_without_simplebuilding}), and only one of the two is loaded.
     */
    public static void gaugeRecipe(GameTestHelper helper) {
        Item n = Items.COPPER_NUGGET;
        boolean sb = withSimpleBuilding();
        Item k = sb ? BuiltInRegistries.ITEM.getValue(COPPER_CORE) : Items.COPPER_INGOT;
        CraftingInput grid = grid(n, Items.AMETHYST_SHARD, n, n, Items.CLOCK, n, n, k, n);
        var recipe = match(helper, grid);
        helper.assertTrue(recipe.isPresent(), "the documented Gauge pattern matches no recipe");
        String expected = sb ? "simplegadgets:velocity_gauge" : "simplegadgets:velocity_gauge_without_simplebuilding";
        helper.assertValueEqual(recipe.get().id().identifier().toString(), expected, "recipe matched for the Gauge");
        helper.assertTrue(recipe.get().value().assemble(grid).is(GadgetsRegistry.VELOCITY_GAUGE), "the recipe made something else");
        if (sb) {
            helper.assertTrue(match(helper, grid(n, Items.AMETHYST_SHARD, n, n, Items.CLOCK, n, n, Items.COPPER_INGOT, n)).isEmpty(),
                    "with SimpleBuilding the copper ingot fallback still crafts a Gauge");
        }
        helper.assertTrue(match(helper, grid(Items.QUARTZ, Items.AMETHYST_SHARD, Items.QUARTZ, n, Items.COMPASS, n, n, k, n)).isEmpty(),
                "the 2026-09-28 quartz corner recipe still crafts a Gauge");
        helper.succeed();
    }

    /**
     * Auto-walk under screens (owner 2026-10-02): no screen and non-pausing screens (inventory, chat, chest) keep it going, the
     * pause menu lets go. It ends only when the main-hand slot changes or holds no Gauge - a re-sent copy does not end it.
     */
    public static void gaugeAutowalkKeepsWalkingUnderNonPausingScreens(GameTestHelper helper) {
        helper.assertTrue(VelocityGaugeItem.autowalkContinuesUnder(false, false), "no screen stops the autowalk");
        helper.assertTrue(VelocityGaugeItem.autowalkContinuesUnder(true, false), "an inventory or chat screen stops the autowalk");
        helper.assertFalse(VelocityGaugeItem.autowalkContinuesUnder(true, true), "the pause menu keeps the autowalk running");
        ItemStack gauge = new ItemStack(GadgetsRegistry.VELOCITY_GAUGE);
        helper.assertTrue(VelocityGaugeItem.autowalkKeepsGauge(3, 3, gauge.copy()), "a re-synced copy of the gauge stops the autowalk");
        helper.assertFalse(VelocityGaugeItem.autowalkKeepsGauge(4, 3, gauge), "switching the hotbar slot keeps the autowalk running");
        helper.assertFalse(VelocityGaugeItem.autowalkKeepsGauge(3, 3, new ItemStack(Items.COMPASS)), "another item in the main hand keeps the autowalk running");
        helper.succeed();
    }

    /**
     * Auto-walk with Constructor's Touch (owner 2026-10-01): where a dirt path or rail ends straight ahead and goes on on exactly
     * one side, it turns that way; straight on, both sides or no way underfoot do not turn.
     */
    public static void gaugeAutowalkFollowsPathsAndRailsAroundCorners(GameTestHelper helper) {
        Direction north = Direction.NORTH; // absolute world directions: the test structure may be rotated
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        for (var way : List.of(Blocks.DIRT_PATH, Blocks.RAIL)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(abs.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
                    level.setBlockAndUpdate(abs.offset(dx, 0, dz), Blocks.STONE.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(abs, way.defaultBlockState());
            level.setBlockAndUpdate(abs.east(), way.defaultBlockState());
            helper.assertValueEqual(VelocityGaugeItem.followTurn(level, abs, north), Direction.EAST, way + ": the way turning east is not followed");
            level.setBlockAndUpdate(abs.west(), way.defaultBlockState());
            helper.assertTrue(VelocityGaugeItem.followTurn(level, abs, north) == null, way + ": a fork turned the view");
            level.setBlockAndUpdate(abs.west(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(abs.north(), way.defaultBlockState());
            helper.assertTrue(VelocityGaugeItem.followTurn(level, abs, north) == null, way + ": a way that goes on straight ahead turned the view");
            level.setBlockAndUpdate(abs, Blocks.STONE.defaultBlockState());
            helper.assertTrue(VelocityGaugeItem.followTurn(level, abs, north) == null, way + ": no way underfoot turned the view");
        }
        helper.succeed();
    }

    /**
     * Altimeter (2026-09-29): distance from the feet to the top of the first solid block below, at most as deep as its range -
     * 24 blocks, 16 more per level of Range, never over 64. Liquid is no ground. The predicted fall damage follows Vanilla's
     * formula. With SimpleBuilding, Range fits the Gauge at the anvil but adds no block reach there.
     */
    public static void gaugeAltimeterReadsTheGroundAndRangeReachesDeeper(GameTestHelper helper) {
        helper.assertValueEqual(VelocityGaugeItem.altimeterRange(0), 24, "altimeter range without Range");
        helper.assertValueEqual(VelocityGaugeItem.altimeterRange(1), 40, "altimeter range with Range I");
        helper.assertValueEqual(VelocityGaugeItem.altimeterRange(3), 64, "altimeter range with Range III");
        helper.assertValueEqual(VelocityGaugeItem.altimeterRange(5), 64, "altimeter range with Range V (cap)");
        ServerLevel level = helper.getLevel();
        ItemStack gauge = new ItemStack(GadgetsRegistry.VELOCITY_GAUGE);
        helper.assertValueEqual(VelocityGaugeItem.altimeterRange(gauge, level), 24, "plain Gauge range");
        var range = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(GadgetEnchantments.RANGE);
        helper.assertTrue(range.isPresent() == withSimpleBuilding(), "SimpleBuilding's Range is " + (range.isPresent() ? "there without" : "missing with") + " SimpleBuilding");
        if (range.isPresent()) {
            helper.assertTrue(range.get().value().isSupportedItem(gauge), "Range cannot be put on the Gauge at an anvil");
            gauge.enchant(range.get(), 2);
            helper.assertValueEqual(VelocityGaugeItem.altimeterRange(gauge, level), 56, "Gauge with Range II");
            boolean[] reach = {false};
            net.minecraft.world.item.enchantment.EnchantmentHelper.forEachModifier(gauge, net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    (attribute, modifier) -> reach[0] |= modifier.id().getNamespace().equals(SimpleGadgets.OLD_NAMESPACE)
                            && modifier.id().getPath().startsWith("enchantment.range"));
            helper.assertTrue(!reach[0], "Range on the Gauge adds block interaction range");
            var touch = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(GadgetEnchantments.CONSTRUCTORS_TOUCH);
            helper.assertTrue(touch.isPresent() && touch.get().value().isSupportedItem(gauge), "Constructor's Touch cannot be put on the Gauge");
        }
        // Ground: stone at y 1 (top 2), measured from feet at y 8.25 -> 6.25 blocks.
        BlockPos column = new BlockPos(3, 1, 3);
        helper.setBlock(column, Blocks.STONE);
        Vec3 feet = helper.absoluteVec(new Vec3(3.5, 8.25, 3.5));
        double height = VelocityGaugeItem.heightAboveGround(level, feet.x, feet.y, feet.z, 24);
        helper.assertTrue(Math.abs(height - 6.25) < 1.0E-6, "height above stone is " + height + " instead of 6.25");
        helper.setBlock(column.above(2), Blocks.WATER);
        height = VelocityGaugeItem.heightAboveGround(level, feet.x, feet.y, feet.z, 24);
        helper.assertTrue(Math.abs(height - 6.25) < 1.0E-6, "water counted as ground: " + height);
        helper.setBlock(column.above(2), Blocks.STONE_SLAB);
        height = VelocityGaugeItem.heightAboveGround(level, feet.x, feet.y, feet.z, 24);
        helper.assertTrue(Math.abs(height - 4.75) < 1.0E-6, "height above a bottom slab is " + height + " instead of 4.75");
        height = VelocityGaugeItem.heightAboveGround(level, feet.x, feet.y, feet.z, 4);
        helper.assertTrue(height < 0.0, "ground 4.75 below was found with a 4 block altimeter: " + height);
        helper.setBlock(column.above(2), Blocks.AIR);
        helper.setBlock(column, Blocks.AIR);
        // Fall damage: safe fall distance 3, factor 1 - 10 blocks in all = 7 half hearts.
        helper.assertValueEqual(VelocityGaugeItem.impactDamage(4.0, 6.0, 3.0, 1.0), 7, "impact after 4 + 6 blocks");
        helper.assertValueEqual(VelocityGaugeItem.impactDamage(1.0, 1.5, 3.0, 1.0), 0, "a 2.5 block fall hurts");
        helper.assertValueEqual(VelocityGaugeItem.impactDamage(10.0, 0.0, 3.0, 0.5), 3, "impact with half fall damage");
        helper.assertTrue(VelocityGaugeItem.needleFraction(0.0) == 0.0 && VelocityGaugeItem.needleFraction(50.0) == 1.0
                        && VelocityGaugeItem.needleFraction(500.0) == 1.0 && Math.abs(VelocityGaugeItem.needleFraction(12.5) - 0.5) < 1.0E-9,
                "the needle scale is not the square-root scale of the HUD");
        helper.succeed();
    }
}
