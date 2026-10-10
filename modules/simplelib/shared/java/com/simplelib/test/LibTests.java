package com.simplelib.test;

import com.google.gson.JsonObject;
import com.simplelib.config.LibConfig;
import com.simplelib.crucible.CrucibleBlankBlock;
import com.simplelib.crucible.CrucibleBlock;
import com.simplelib.crucible.CrucibleBlockEntity;
import com.simplelib.crucible.CrucibleTier;
import com.simplelib.crucible.Heat;
import com.simplelib.crucible.HeatLevel;
import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibComponents;
import com.simplelib.warm.Warm;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of SimpleLib. One body per id; the Fabric adapter ({@code ModuleGameTest}) and the
 * NeoForge/Forge registrations use the same names ({@code module_game_test_<name>}). Crucible ticks
 * are driven directly (no waiting), so every test finishes in its first tick.
 */
public final class LibTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("creative_tab_routing", LibTests::creativeTabRouting);
        ALL.put("config_bounds", LibTests::configBounds);
        ALL.put("tier_layout", LibTests::tierLayout);
        ALL.put("heat_sources", LibTests::heatSources);
        ALL.put("heat_two_below", LibTests::heatTwoBelow);
        ALL.put("cooks_into_slot_below", LibTests::cooksIntoSlotBelow);
        ALL.put("medium_heat_blocks_smelting", LibTests::mediumHeatBlocksSmelting);
        ALL.put("reserved_slot_blocks_and_keeps_progress", LibTests::reservedSlotBlocks);
        ALL.put("result_not_recooked", LibTests::resultNotRecooked);
        ALL.put("warms_food_and_eats_faster", LibTests::warmsFood);
        ALL.put("afterglow_keeps_heat", LibTests::afterglow);
        ALL.put("hopper_rules", LibTests::hopperRules);
        ALL.put("axe_builds_iron_crucible", LibTests::axeBuild);
        ALL.put("break_drops_contents", LibTests::breakDrops);
        ALL.put("axe_upgrade_keeps_contents", LibTests::axeUpgrade);
        ALL.put("warm_stacks_by_mean", LibTests::warmStacksByMean);
        ALL.put("warm_bundle_insulates", LibTests::warmBundleInsulates);
        ALL.put("village_kitchen_in_pools", LibTests::villageKitchen);
        ALL.put("barrel_attach_and_results_first", LibTests::barrelAttach);
        ALL.put("barrel_attached_later_takes_over_reservations", LibTests::barrelAttachedLater);
        ALL.put("client_menu_shows_the_raised_stack_limit", LibTests::clientMenuStackLimit);
        ALL.put("axe_click_reaches_axe_not_menu", LibTests::axeClickReachesAxe);
        ALL.put("axe_upgrades_cauldron", LibTests::axeUpgradesCauldron);
        ALL.put("reinforced_cauldron_holds_buckets", LibTests::reinforcedCauldronBuckets);
        ALL.put("reinforced_cauldron_inherits_vanilla", LibTests::reinforcedCauldronInheritsVanilla);
        ALL.put("axe_upgrades_barrel_to_netherite", LibTests::axeUpgradesBarrelToNetherite);
        ALL.put("crucible_burn_damage_follows_config", LibTests::crucibleBurnDamageFollowsConfig);
        ALL.put("loose_barrel_menu_keeps_the_raised_limit", LibTests::looseBarrelMenu);
        ALL.put("hoppers_fill_raised_slots", LibTests::hoppersFillRaisedSlots);
        ALL.put("reinforced_crucible_loads_old_nine_slots", LibTests::reinforcedCrucibleLoadsOldNineSlots);
    }

    // ------------------------------------------------------------ helpers

    private static CrucibleBlockEntity crucible(GameTestHelper h, BlockPos rel, BlockState below, CrucibleBlock block) {
        h.setBlock(rel.below(), below);
        h.setBlock(rel, block.defaultBlockState());
        CrucibleBlockEntity be = (CrucibleBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        h.assertTrue(be != null, "no crucible block entity");
        return be;
    }

    private static void run(GameTestHelper h, CrucibleBlockEntity be, int ticks) {
        ServerLevel level = h.getLevel();
        for (int i = 0; i < ticks; i++) {
            CrucibleBlockEntity.serverTick(level, be.getBlockPos(), level.getBlockState(be.getBlockPos()), be);
        }
    }

    private static String debug(CrucibleBlockEntity be) {
        StringBuilder sb = new StringBuilder("[heat=" + be.heat() + " mul=" + be.heatMultiplier());
        for (int i = 0; i < be.getContainerSize(); i++) {
            sb.append(" | ").append(i).append(':').append(be.getItem(i)).append(" st=").append(be.slotState(i))
                    .append(" p=").append(be.percent(i)).append(" t=").append(be.target(i)).append(be.isResult(i) ? " R" : "");
        }
        return sb.append(']').toString();
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    // ------------------------------------------------------------ tests

    private static void creativeTabRouting(GameTestHelper h) {
        var vanilla = net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS;
        var sb = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,
                com.simplelib.registry.LibRegistry.SB_FUNCTIONAL_TAB);
        var other = net.minecraft.world.item.CreativeModeTabs.COMBAT;
        h.assertTrue(com.simplelib.registry.LibRegistry.wantsStacks(vanilla, true), "Vanilla functional tab gets no crucibles");
        h.assertTrue(!com.simplelib.registry.LibRegistry.wantsStacks(vanilla, false), "Vanilla tab filled although switched off");
        h.assertTrue(com.simplelib.registry.LibRegistry.wantsStacks(sb, false), "SimpleBuilding's tab skipped");
        h.assertTrue(!com.simplelib.registry.LibRegistry.wantsStacks(other, true), "an unrelated tab gets crucibles");
        h.assertTrue(!com.simplelib.registry.LibRegistry.tabStacks().isEmpty(), "nothing to route");
        h.succeed();
    }

    private static void configBounds(GameTestHelper h) {
        JsonObject json = new JsonObject();
        json.addProperty("reinforcedSpeed", 999);
        json.addProperty("factorMedium", -3.0);
        json.addProperty("afterglowEnderite", 10_000);
        json.addProperty("warmDurationTicks", 5);
        json.addProperty("eatSpeedBonus", 0.9);
        json.addProperty("crucibleBurnDamage", 50.0);
        LibConfig.apply(json);
        check(h, LibConfig.reinforcedSpeed == LibConfig.MAX_SPEED, "speed not clamped");
        check(h, LibConfig.factorMedium == LibConfig.FACTOR_MIN, "factor not clamped");
        check(h, LibConfig.afterglowSeconds[3] == LibConfig.AFTERGLOW_MAX_SECONDS, "afterglow not clamped");
        check(h, LibConfig.warmDurationTicks == LibConfig.WARM_DURATION_MIN, "warm duration not clamped");
        check(h, LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_MAX, "eat bonus not clamped");
        check(h, LibConfig.crucibleBurnDamage == LibConfig.BURN_DAMAGE_MAX, "crucible burn damage not clamped");
        LibConfig.reset();
        check(h, LibConfig.factorHigh == 0.75 && LibConfig.afterglowSeconds[0] == 2, "defaults wrong");
        check(h, LibConfig.crucibleBurnDamage == 1.0, "crucible burn damage default is not magma's 1");
        h.succeed();
    }

    /** Owner N11 P6: a high-heat crucible burns like magma for crucibleBurnDamage; 0 switches the burn off. */
    private static void crucibleBurnDamageFollowsConfig(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        run(h, be, 1);
        check(h, be.heat().atLeast(HeatLevel.HIGH), "lava cauldron below: high heat");
        ServerLevel level = h.getLevel();
        BlockPos pos = be.getBlockPos();
        BlockState state = level.getBlockState(pos);
        try {
            var pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 3, 1));
            float before = pig.getHealth();
            state.getBlock().stepOn(level, pos, state, pig);
            check(h, Math.abs(before - pig.getHealth() - 1.0F) < 1.0E-4, "default burn is 1 like magma: " + pig.getHealth());
            var spared = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 3, 1));
            LibConfig.crucibleBurnDamage = 0;
            float full = spared.getHealth();
            state.getBlock().stepOn(level, pos, state, spared);
            check(h, spared.getHealth() == full, "crucibleBurnDamage 0 still burns");
        } finally {
            LibConfig.crucibleBurnDamage = LibConfig.BURN_DAMAGE_DEFAULT;
        }
        h.succeed();
    }

    private static void tierLayout(GameTestHelper h) {
        check(h, CrucibleTier.IRON.slots() == 6 && CrucibleTier.REINFORCED.slots() == 12
                && CrucibleTier.NETHERITE.slots() == 18 && CrucibleTier.ENDERITE.slots() == 27, "slot counts (owner F4, reinforced 12 since N30)");
        check(h, CrucibleTier.REINFORCED.below(6) == 9 && CrucibleTier.REINFORCED.below(9) == -1, "reinforced fourth row below");
        check(h, CrucibleTier.IRON.below(0) == 3 && CrucibleTier.IRON.below(3) == -1, "iron below");
        check(h, CrucibleTier.NETHERITE.below(6) == -1 && CrucibleTier.NETHERITE.below(9) == 12, "second grid below");
        check(h, CrucibleTier.ENDERITE.stackMultiplier() == 2 && CrucibleTier.NETHERITE.stackMultiplier() == 1, "stack multiplier");
        h.succeed();
    }

    /**
     * N30: the reinforced crucible grew from 9 (3x3) to 12 slots (3x4). A crucible saved with 9 slots (its arrays 9
     * long) loads with every stack, reservation and progress in the same place; the fourth row starts empty.
     */
    private static void reinforcedCrucibleLoadsOldNineSlots(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity old = crucible(h, rel, Blocks.STONE.defaultBlockState(), LibBlocks.REINFORCED_CRUCIBLE);
        check(h, old.getContainerSize() == 12, "reinforced crucible has 12 slots, has " + old.getContainerSize());
        for (int i = 0; i < 9; i++) old.setItem(i, new ItemStack(Items.COBBLESTONE, i + 1));
        net.minecraft.nbt.CompoundTag tag = old.saveCustomOnly(h.getLevel().registryAccess());
        // The old save: arrays of 9 (Progress, Targets, Results), one reservation 2 -> 5.
        int[] targets = new int[9];
        java.util.Arrays.fill(targets, -1);
        targets[2] = 5;
        tag.putIntArray("Progress", new int[] {0, 0, 7, 0, 0, 0, 0, 0, 0});
        tag.putIntArray("Targets", targets);
        tag.putIntArray("Results", new int[9]);
        BlockPos rel2 = new BlockPos(3, 2, 1);
        CrucibleBlockEntity loaded = crucible(h, rel2, Blocks.STONE.defaultBlockState(), LibBlocks.REINFORCED_CRUCIBLE);
        loaded.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        for (int i = 0; i < 9; i++) {
            check(h, loaded.getItem(i).is(Items.COBBLESTONE) && loaded.getItem(i).getCount() == i + 1,
                    "old slot " + i + " kept its stack: " + debug(loaded));
        }
        for (int i = 9; i < 12; i++) check(h, loaded.getItem(i).isEmpty(), "new fourth row starts empty: " + debug(loaded));
        check(h, loaded.target(2) == 5 && loaded.target(9) == -1, "reservation kept, new slots free: " + debug(loaded));
        old.clearContent();
        loaded.clearContent();
        h.succeed();
    }

    private static void heatSources(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        check(h, Heat.of(level, pos, Blocks.CAMPFIRE.defaultBlockState()) == HeatLevel.MEDIUM, "campfire medium");
        check(h, Heat.of(level, pos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)) == HeatLevel.NONE, "unlit campfire");
        check(h, Heat.of(level, pos, Blocks.SOUL_CAMPFIRE.defaultBlockState()) == HeatLevel.MEDIUM, "soul campfire medium (owner 25)");
        check(h, Heat.of(level, pos, Blocks.MAGMA_BLOCK.defaultBlockState()) == HeatLevel.MEDIUM, "magma medium");
        check(h, Heat.of(level, pos, Blocks.LAVA.defaultBlockState()) == HeatLevel.HIGH, "lava source high");
        check(h, Heat.of(level, pos, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 3)) == HeatLevel.MEDIUM, "flowing lava one lower (F30)");
        check(h, Heat.of(level, pos, Blocks.LAVA_CAULDRON.defaultBlockState()) == HeatLevel.HIGH, "lava cauldron like source (F30)");
        check(h, Heat.of(level, pos, Blocks.TORCH.defaultBlockState()) == HeatLevel.NONE, "torch gives no heat (owner 51)");
        check(h, Heat.of(level, pos, Blocks.FIRE.defaultBlockState()) == HeatLevel.NONE, "fire gives no heat (owner 51)");
        h.succeed();
    }

    private static void heatTwoBelow(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 3, 1);
        h.setBlock(rel.below(2), Blocks.LAVA_CAULDRON);
        h.setBlock(rel.below(), Blocks.HOPPER);
        h.setBlock(rel, LibBlocks.IRON_CRUCIBLE);
        Heat.Reading reading = Heat.at(h.getLevel(), h.absolutePos(rel));
        check(h, reading.level() == HeatLevel.HIGH && Math.abs(reading.multiplier() - 0.9) < 1e-9, "hopper in between, 10 % slower (F26)");
        h.setBlock(rel.below(), Blocks.STONE);
        check(h, Heat.at(h.getLevel(), h.absolutePos(rel)).level() == HeatLevel.NONE, "full block blocks the heat");
        h.succeed();
    }

    private static void cooksIntoSlotBelow(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        be.setItem(1, new ItemStack(Items.SAND));
        // 26.3 cooking recipes all take 200 ticks: / 0.75 (high) = 267 ticks per item at iron speed.
        run(h, be, 270);
        check(h, be.getItem(3).is(Items.IRON_INGOT) && be.getItem(3).getCount() == 1, "first ingot in the slot below (F13) ");
        check(h, be.isResult(3), "result locked");
        run(h, be, 270);
        check(h, be.getItem(3).getCount() == 2 && be.getItem(0).isEmpty(), "second ingot merged");
        check(h, be.getItem(4).is(Items.GLASS), "sand smelted in parallel into its own slot below");
        check(h, be.storedExperience() > 0, "experience stored");
        h.succeed();
    }

    private static void mediumHeatBlocksSmelting(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.CAMPFIRE.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        be.setItem(1, new ItemStack(Items.BEEF));
        // Beef: smoking 200 ticks / 0.5 (medium) = 400 ticks.
        run(h, be, 405);
        check(h, be.slotState(0) == CrucibleBlockEntity.COLD, "raw iron needs high heat (blue)");
        check(h, be.getItem(4).is(Items.COOKED_BEEF), "beef cooked on a campfire");
        check(h, Warm.isWarm(be.getItem(4), h.getLevel()), "cooked food comes out warm");
        h.succeed();
    }

    private static void reservedSlotBlocks(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        run(h, be, 20);
        check(h, be.target(0) == 3 && be.ghost(3).is(Items.IRON_INGOT), "reserved below with ghost");
        int before = be.percent(0);
        be.setItem(3, new ItemStack(Items.DIRT));
        run(h, be, 200);
        check(h, be.slotState(0) == CrucibleBlockEntity.BLOCKED, "foreign item blocks (F18)");
        check(h, be.percent(0) == before && be.getItem(0).is(Items.RAW_IRON), "progress kept");
        be.setItem(3, ItemStack.EMPTY);
        run(h, be, 260);
        check(h, be.getItem(3).is(Items.IRON_INGOT), "continues after taking it out");
        h.succeed();
    }

    private static void resultNotRecooked(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.COBBLESTONE));
        run(h, be, 600);
        check(h, be.getItem(3).is(Items.STONE), "stone stays stone, never smooth stone (F14)");
        h.succeed();
    }

    private static void warmsFood(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.CAMPFIRE.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(2, new ItemStack(Items.COOKED_PORKCHOP, 5));
        run(h, be, 210);
        ItemStack warm = be.getItem(2);
        check(h, warm.getCount() == 5 && warm.has(LibComponents.WARM), "whole stack warmed in its slot");
        check(h, be.isResult(2), "warm food is a result");
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        int cold = new ItemStack(Items.COOKED_PORKCHOP).getUseDuration(player);
        int fast = warm.getUseDuration(player);
        check(h, fast == Warm.fasterUse(cold) && fast < cold, "warm food is eaten 15 % faster");
        long until = warm.get(LibComponents.WARM).until();
        run(h, be, 40);
        check(h, be.getItem(2).get(LibComponents.WARM).until() >= until, "does not cool inside the crucible (owner 39)");
        h.succeed();
    }

    private static void afterglow(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        run(h, be, 2);
        check(h, be.heat() == HeatLevel.HIGH, "heated");
        h.setBlock(rel.below(), Blocks.STONE);
        be.markHeatDirty();
        run(h, be, 20);
        check(h, be.heat() == HeatLevel.HIGH, "afterglow keeps the heat (iron: 2 s)");
        run(h, be, 30);
        check(h, be.heat() == HeatLevel.NONE, "cold after the afterglow");
        h.succeed();
    }

    private static void hopperRules(GameTestHelper h) {
        CrucibleBlockEntity be = crucible(h, new BlockPos(1, 2, 1), Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        check(h, be.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.UP), "insert cookable from above");
        check(h, !be.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.DOWN), "never from below");
        check(h, !be.canPlaceItemThroughFace(0, new ItemStack(Items.DIRT), Direction.UP), "only cookable items");
        be.setItem(0, new ItemStack(Items.RAW_IRON));
        run(h, be, 270);
        check(h, be.canTakeItemThroughFace(3, be.getItem(3), Direction.DOWN), "results go out below");
        check(h, !be.canTakeItemThroughFace(0, be.getItem(0), Direction.DOWN) || be.getItem(0).isEmpty(), "inputs stay");
        check(h, !be.canPlaceItemThroughFace(3, new ItemStack(Items.IRON_INGOT), Direction.UP), "no insert into results");
        h.succeed();
    }

    private static void axeBuild(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1);
        h.setBlock(rel, Blocks.IRON_BLOCK);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, 4));
        ItemStack[] axes = {new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.STONE_AXE), new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.GOLDEN_AXE), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.NETHERITE_AXE)};
        check(h, !CrucibleBlankBlock.fitsNext(0, new ItemStack(Items.IRON_INGOT)), "first strikes need walls");
        for (int i = 0; i < 4; i++) {
            check(h, CrucibleBlankBlock.strike(h.getLevel(), abs, player, axes[i], 1), "wall strike " + (i + 1));
        }
        check(h, h.getLevel().getBlockState(abs).getValue(CrucibleBlankBlock.STAGE) == 4, "four walls");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 2));
        check(h, CrucibleBlankBlock.strike(h.getLevel(), abs, player, axes[4], 1), "handle 1");
        check(h, CrucibleBlankBlock.strike(h.getLevel(), abs, player, axes[5], 1), "handle 2");
        check(h, h.getLevel().getBlockState(abs).is(LibBlocks.IRON_CRUCIBLE), "iron crucible built");
        check(h, player.getOffhandItem().isEmpty() && axes[0].getDamageValue() == 1, "parts used, axe damaged");
        h.succeed();
    }

    private static void breakDrops(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.STONE.defaultBlockState(), LibBlocks.REINFORCED_CRUCIBLE);
        be.setItem(5, new ItemStack(Items.RAW_GOLD, 7));
        check(h, be.drops().size() == 1 && be.drops().get(0).getCount() == 7, "drops contents");
        h.getLevel().destroyBlock(h.absolutePos(rel), true);
        h.assertItemEntityPresent(Items.RAW_GOLD, rel, 2.0);
        h.succeed();
    }

    private static void axeUpgrade(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.STONE.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(4, new ItemStack(Items.RAW_COPPER, 3));
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 2));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleUpgrades.STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(LibBlocks.REINFORCED_CRUCIBLE), "upgraded to reinforced (owner 50: 2 diamonds, 10 strikes)");
        CrucibleBlockEntity fresh = (CrucibleBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, fresh != null && fresh.getContainerSize() == 12 && fresh.getItem(4).is(Items.RAW_COPPER) && fresh.getItem(4).getCount() == 3,
                "contents kept in the same slot");
        check(h, player.getOffhandItem().isEmpty(), "two diamonds used");
        h.succeed();
    }

    private static void warmStacksByMean(GameTestHelper h) {
        long now = h.getLevel().getGameTime();
        com.simplelib.warm.WarmMerge.tick(now);
        ItemStack warm = new ItemStack(Items.COOKED_BEEF, 2);
        warm.set(LibComponents.WARM, new Warm(now + 1000));
        ItemStack cold = new ItemStack(Items.COOKED_BEEF, 2);
        check(h, ItemStack.isSameItemSameComponents(warm, cold), "warm and cold food stack (owner: mean warmth)");
        warm.grow(cold.getCount());
        check(h, Warm.remaining(warm, now) == 500, "mean of 2 x 1000 and 2 x 0 ticks is 500 (owner 40: cold counts 0), got " + Warm.remaining(warm, now));
        ItemStack other = new ItemStack(Items.COOKED_PORKCHOP);
        check(h, !ItemStack.isSameItemSameComponents(warm, other), "different food never stacks");
        h.succeed();
    }

    private static void warmBundleInsulates(GameTestHelper h) {
        long now = h.getLevel().getGameTime();
        com.simplelib.warm.WarmMerge.tick(now);
        ItemStack warm = new ItemStack(Items.BAKED_POTATO, 3);
        warm.set(LibComponents.WARM, new Warm(now + 1000));
        net.minecraft.world.item.component.BundleContents.Mutable bundle =
                new net.minecraft.world.item.component.BundleContents.Mutable();
        check(h, bundle.tryInsert(warm) == 3 && warm.isEmpty(), "inserted");
        ItemStack out = bundle.removeOne();
        check(h, out.getCount() == 3 && Warm.remaining(out, now) == 1000 && !out.get(LibComponents.WARM).insulated(),
                "back out with its remaining time and normal cooling");
        ItemStack probe = new ItemStack(Items.BAKED_POTATO);
        probe.set(LibComponents.WARM, new Warm(now + 1000));
        Warm.insulate(probe, now);
        check(h, Warm.remaining(probe, now) == 4000, "inside a bundle it cools four times slower (12 000 -> 48 000 ticks)");
        h.succeed();
    }

    private static void villageKitchen(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        com.simplelib.village.VillageKitchen.inject(access);
        var pools = access.lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        for (String type : com.simplelib.village.VillageKitchen.TYPES) {
            var pool = pools.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("village/" + type + "/houses"));
            check(h, pool != null, "pool " + type);
            long copies = ((com.simplelib.mixin.TemplatePoolAccessor) pool).simplelib$templates().stream()
                    .filter(e -> e.toString().contains("simplelib:village/" + type + "/field_kitchen")).count();
            check(h, copies == LibConfig.villageKitchenWeight, type + ": kitchen added once with weight " + LibConfig.villageKitchenWeight + ", found " + copies);
            check(h, h.getLevel().getStructureTemplateManager().get(com.simplelib.SimpleLib.id("village/" + type + "/field_kitchen")).isPresent(),
                    type + ": structure file loads");
        }
        h.succeed();
    }

    private static void barrelAttach(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        BlockPos barrelRel = rel.east();
        h.setBlock(barrelRel, LibBlocks.COPPER_BARREL);
        BlockPos barrelAbs = h.absolutePos(barrelRel);
        var barrel = (com.simplelib.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(barrelAbs);
        check(h, barrel.getContainerSize() == 27, "on its own a copper barrel has 27 slots (owner 58)");
        barrel.setItem(20, new ItemStack(Items.APPLE, 3));
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        check(h, com.simplelib.crucible.CrucibleBarrelBlock.crackStage(1) == 0 && com.simplelib.crucible.CrucibleBarrelBlock.crackStage(5) == 7,
                "cracks grow with the strikes (owner addition 11)");
        for (int i = 0; i < com.simplelib.crucible.CrucibleBarrelBlock.ATTACH_STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleBarrelBlock.attachStrike(h.getLevel(), barrelAbs, player, axe, 1), "attach strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        var state = h.getLevel().getBlockState(barrelAbs);
        check(h, state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED)
                && state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.FACING) == Direction.WEST, "attached, facing the crucible");
        // Owner N12c: attached, the barrel offers as many slots as the crucible (iron: 6); the rest stays stored, hidden.
        check(h, h.getLevel().getBlockEntity(barrelAbs) == barrel && barrel.getContainerSize() == 6, "attached to the iron crucible: 6 slots");
        check(h, h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(barrelAbs).inflate(2), e -> e.getItem().is(Items.APPLE)).isEmpty(), "nothing dropped when attaching");
        var menu = new com.simplelib.crucible.CrucibleMenu(0, player.getInventory(), be);
        check(h, menu.slots.size() == 5 * 6 + 72, "menu: 6 barrel fields for the 6-slot crucible, got " + (menu.slots.size() - 72 - 24));
        be.markHeatDirty();
        be.setItem(0, new ItemStack(Items.RAW_GOLD));
        run(h, be, 270);
        check(h, barrel.getItem(0).is(Items.GOLD_INGOT), "result went into the barrel first (owner wish)");
        check(h, be.getItem(3).isEmpty(), "not into the slot below");
        menu.slots.get(menu.barrelStart() + 5).set(new ItemStack(Items.STICK));
        check(h, barrel.getItem(5).is(Items.STICK), "the 6th field is the barrel's slot 6");
        check(h, menu.slots.get(menu.barrelStart() + 5).container.getContainerSize() == 6 && barrel.getContainerSize() == 6,
                "the menu reaches no barrel slot beyond 6");
        h.setBlock(rel, Blocks.AIR);
        var after = h.getLevel().getBlockState(barrelAbs);
        check(h, !after.getValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED) && barrel.getContainerSize() == 27,
                "crucible gone: a normal barrel again");
        check(h, barrel.getItem(0).is(Items.GOLD_INGOT), "contents stay in the barrel");
        check(h, barrel.getItem(20).is(Items.APPLE) && barrel.getItem(20).getCount() == 3, "the hidden slot 21 is back with its apples");
        h.succeed();
    }

    /**
     * Owner N15: a loose barrel opens a chest menu whose slots keep the barrel's limit on both sides - the Enderite
     * mirror takes 128, a shift-click of two full stacks fills one slot, picking up gives a normal stack, and a
     * copper barrel stays at 64.
     */
    private static void looseBarrelMenu(GameTestHelper h) {
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        var tier = com.simplelib.crucible.BarrelTier.ENDERITE;
        var client = com.simplelib.crucible.BarrelMenu.client(tier, 0, player.getInventory());
        check(h, client.slots.get(0).getMaxStackSize(cobble) == 128 && client.getRowCount() == 6,
                "enderite barrel on the client: 6 rows of 128, got " + client.getRowCount() + " rows of " + client.slots.get(0).getMaxStackSize(cobble));
        var barrel = com.simplelib.api.StackLimits.mirror(tier.slots(), tier::stackMultiplier);
        var menu = new com.simplelib.crucible.BarrelMenu(tier, 0, player.getInventory(), barrel);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        int hotbar = tier.slots() + 27;
        menu.quickMoveStack(player, hotbar);
        menu.quickMoveStack(player, hotbar + 1);
        check(h, barrel.getItem(0).getCount() == 128 && barrel.getItem(1).isEmpty(),
                "shift-click fills one enderite slot to 128, got " + barrel.getItem(0).getCount() + "+" + barrel.getItem(1).getCount());
        var taken = menu.slots.get(0).tryRemove(128, Integer.MAX_VALUE, player);
        check(h, taken.isPresent() && taken.get().getCount() == 64 && barrel.getItem(0).getCount() == 64,
                "picking up takes one normal stack, got " + taken.map(ItemStack::getCount).orElse(0));
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, LibBlocks.COPPER_BARREL);
        var copper = (com.simplelib.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
        var copperMenu = new com.simplelib.crucible.BarrelMenu(copper.tier(), 0, player.getInventory(), copper);
        check(h, copperMenu.slots.get(0).getMaxStackSize(cobble) == 64 && copperMenu.getRowCount() == 3,
                "copper barrel: 3 rows of 64, got " + copperMenu.getRowCount() + " rows of " + copperMenu.slots.get(0).getMaxStackSize(cobble));
        h.succeed();
    }

    /**
     * Owner N15: hoppers top raised slots up to their container's limit - alone and as a double chest - while a
     * Vanilla chest keeps Vanilla's 64.
     */
    private static void hoppersFillRaisedSlots(GameTestHelper h) {
        var raised = com.simplelib.api.StackLimits.mirror(9, () -> 2);
        raised.setItem(0, new ItemStack(Items.COBBLESTONE, 127));
        ItemStack rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, raised, new ItemStack(Items.COBBLESTONE, 2), Direction.UP);
        check(h, raised.getItem(0).getCount() == 128 && raised.getItem(1).getCount() == 1 && rest.isEmpty(),
                "a x2 slot at 127 plus 2: " + raised.getItem(0).getCount() + "+" + raised.getItem(1).getCount() + " rest " + rest.getCount());
        var first = com.simplelib.api.StackLimits.mirror(1, () -> 2);
        var second = com.simplelib.api.StackLimits.mirror(1, () -> 2);
        first.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        second.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        var both = new net.minecraft.world.CompoundContainer(first, second);
        rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, both, new ItemStack(Items.COBBLESTONE, 1), Direction.UP);
        check(h, rest.isEmpty() && first.getItem(0).getCount() == 65, "a double x2 container of 64s is not full, got " + first.getItem(0).getCount());
        var vanilla = new net.minecraft.world.SimpleContainer(1);
        vanilla.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, vanilla, new ItemStack(Items.COBBLESTONE, 1), Direction.UP);
        check(h, rest.getCount() == 1 && vanilla.getItem(0).getCount() == 64, "a Vanilla container stays at 64");
        h.succeed();
    }

    /** Owner N15: the client's menu mirror allows the same raised stacks as the server (Enderite: 128, not 64). */
    private static void clientMenuStackLimit(GameTestHelper h) {
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        var enderite = com.simplelib.crucible.CrucibleMenu.client(com.simplelib.crucible.CrucibleTier.ENDERITE, 0, player.getInventory());
        check(h, enderite.slots.get(0).getMaxStackSize(cobble) == 128, "enderite crucible slot on the client: 128, got " + enderite.slots.get(0).getMaxStackSize(cobble));
        var iron = com.simplelib.crucible.CrucibleMenu.client(com.simplelib.crucible.CrucibleTier.IRON, 0, player.getInventory());
        check(h, iron.slots.get(0).getMaxStackSize(cobble) == 64, "iron crucible slot on the client: 64");
        check(h, com.simplelib.api.StackLimits.max(new ItemStack(Items.IRON_SWORD), 2) == 1, "unstackable items stay single");
        h.succeed();
    }

    /** Owner N16: items already cooking when the barrel is attached send their results to the barrel too. */
    private static void barrelAttachedLater(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 2, 2);
        CrucibleBlockEntity be = crucible(h, rel, Blocks.LAVA_CAULDRON.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        be.setItem(0, new ItemStack(Items.RAW_GOLD, 2));
        be.setItem(1, new ItemStack(Items.RAW_IRON));
        run(h, be, 20);
        check(h, be.target(0) >= 0 && be.target(0) < CrucibleBlockEntity.BARREL, "without a barrel the gold reserves a crucible slot, got " + be.target(0));
        BlockPos barrelAbs = h.absolutePos(rel.east());
        h.setBlock(rel.east(), LibBlocks.COPPER_BARREL);
        var barrel = (com.simplelib.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(barrelAbs);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleBarrelBlock.ATTACH_STRIKES; i++) {
            com.simplelib.crucible.CrucibleBarrelBlock.attachStrike(h.getLevel(), barrelAbs, player, axe, 1);
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        run(h, be, 1);
        check(h, be.target(0) >= CrucibleBlockEntity.BARREL && be.target(1) >= CrucibleBlockEntity.BARREL,
                "after attaching, both running jobs reserve barrel slots, got " + be.target(0) + "/" + be.target(1));
        for (int s = 2; s < 6; s++) check(h, be.ghost(s).isEmpty(), "no crucible slot keeps a stale reservation: " + s);
        run(h, be, 700);
        int gold = 0, iron = 0;
        for (int s = 0; s < barrel.getContainerSize(); s++) {
            if (barrel.getItem(s).is(Items.GOLD_INGOT)) gold += barrel.getItem(s).getCount();
            if (barrel.getItem(s).is(Items.IRON_INGOT)) iron += barrel.getItem(s).getCount();
        }
        check(h, gold == 2 && iron == 1, "all results went into the barrel, gold " + gold + ", iron " + iron);
        for (int s = 0; s < 6; s++) check(h, be.getItem(s).isEmpty(), "crucible slot " + s + " is empty afterwards");
        h.succeed();
    }

    /** Principle 5a: with the axe way on, an axe + material click reaches the axe instead of opening the menu. */
    private static void axeClickReachesAxe(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        crucible(h, rel, Blocks.STONE.defaultBlockState(), LibBlocks.IRON_CRUCIBLE);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        var state = h.getLevel().getBlockState(abs);
        check(h, !com.simplelib.api.SimpleLibApi.toolWants(state, h.getLevel(), abs, player, net.minecraft.world.InteractionHand.MAIN_HAND),
                "an axe alone opens the menu");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 2));
        boolean expected = com.simplelib.api.SimpleLibApi.axeWaysEnabled();
        check(h, com.simplelib.api.SimpleLibApi.toolWants(state, h.getLevel(), abs, player, net.minecraft.world.InteractionHand.MAIN_HAND) == expected,
                "axe + 2 diamonds reaches the axe exactly when the axe way is on (" + expected + ")");
        h.succeed();
    }

    /** Owner 56 B: without SimpleBuilding a cauldron becomes reinforced with an axe and 8 diamonds (doubled 2026-10-06). */
    private static void axeUpgradesCauldron(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, Blocks.CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, com.simplelib.crucible.CrucibleUpgrades.CAULDRON_COST));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleUpgrades.CAULDRON_STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(LibBlocks.REINFORCED_CAULDRON), "reinforced cauldron built");
        check(h, player.getOffhandItem().isEmpty(), "eight diamonds used");
        h.succeed();
    }

    /** The reinforced cauldron takes and gives lava/water by bucket; lava in it heats a crucible like a lava source (owner F30). */
    private static void reinforcedCauldronBuckets(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, LibBlocks.REINFORCED_CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        check(h, "lava".equals(com.simplelib.api.SimpleLibApi.cauldronContent(h.getLevel().getBlockState(abs))), "lava poured in");
        check(h, player.getMainHandItem().is(Items.BUCKET), "empty bucket back");
        check(h, Heat.at(h.getLevel(), abs.above()).level() == HeatLevel.HIGH, "lava in the cauldron heats high");
        h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        check(h, player.getMainHandItem().is(Items.LAVA_BUCKET), "lava taken back");
        h.succeed();
    }

    /** Owner addition 11: the reinforced cauldron runs the Vanilla cauldron interactions (bottles, levels) and stays reinforced. */
    private static void reinforcedCauldronInheritsVanilla(GameTestHelper h) {
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, LibBlocks.REINFORCED_CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        var level = h.getLevel();
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs), Direction.UP, abs, false);
        java.util.function.Consumer<ItemStack> use = stack -> {
            player.setItemInHand(hand, stack);
            level.getBlockState(abs).useItemOn(player.getMainHandItem(), level, player, hand, hit);
        };
        use.accept(new ItemStack(Items.WATER_BUCKET));
        var state = level.getBlockState(abs);
        check(h, state.is(LibBlocks.REINFORCED_CAULDRON) && "water".equals(com.simplelib.api.SimpleLibApi.cauldronContent(state))
                && state.getValue(com.simplelib.cauldron.ReinforcedCauldronBlock.LEVEL) == 3, "water bucket: full water, still reinforced");
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        state = level.getBlockState(abs);
        check(h, player.getMainHandItem().is(Items.POTION), "bottle filled with water");
        check(h, state.is(LibBlocks.REINFORCED_CAULDRON) && state.getValue(com.simplelib.cauldron.ReinforcedCauldronBlock.LEVEL) == 2, "level 3 -> 2");
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        use.accept(new ItemStack(Items.GLASS_BOTTLE));
        state = level.getBlockState(abs);
        check(h, state.is(LibBlocks.REINFORCED_CAULDRON) && "empty".equals(com.simplelib.api.SimpleLibApi.cauldronContent(state)),
                "three bottles empty it, it stays reinforced");
        use.accept(new ItemStack(Items.POWDER_SNOW_BUCKET));
        check(h, "powder_snow".equals(com.simplelib.api.SimpleLibApi.cauldronContent(level.getBlockState(abs))), "powder snow bucket");
        use.accept(new ItemStack(Items.BUCKET));
        check(h, player.getMainHandItem().is(Items.POWDER_SNOW_BUCKET)
                && "empty".equals(com.simplelib.api.SimpleLibApi.cauldronContent(level.getBlockState(abs))), "powder snow taken back");
        h.succeed();
    }

    /** Owner addition 11: reinforced barrel -> netherite barrel (axe, 1 netherite ingot, ten strikes), 45 slots, contents kept. */
    private static void axeUpgradesBarrelToNetherite(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, LibBlocks.REINFORCED_BARREL);
        BlockPos abs = h.absolutePos(rel);
        ((net.minecraft.world.Container) h.getLevel().getBlockEntity(abs)).setItem(30, new ItemStack(Items.COAL, 7));
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.NETHERITE_INGOT));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleUpgrades.STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(LibBlocks.NETHERITE_BARREL), "netherite barrel built");
        var barrel = (com.simplelib.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(abs);
        check(h, barrel.getContainerSize() == 45 && barrel.getItem(30).is(Items.COAL) && barrel.getItem(30).getCount() == 7, "45 slots, contents kept");
        check(h, player.getOffhandItem().isEmpty(), "netherite ingot used");
        h.succeed();
    }

    private LibTests() {}
}
