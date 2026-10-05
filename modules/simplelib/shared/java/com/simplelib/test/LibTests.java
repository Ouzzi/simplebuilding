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
        ALL.put("axe_click_reaches_axe_not_menu", LibTests::axeClickReachesAxe);
        ALL.put("axe_upgrades_cauldron", LibTests::axeUpgradesCauldron);
        ALL.put("reinforced_cauldron_holds_buckets", LibTests::reinforcedCauldronBuckets);
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

    private static void configBounds(GameTestHelper h) {
        JsonObject json = new JsonObject();
        json.addProperty("reinforcedSpeed", 999);
        json.addProperty("factorMedium", -3.0);
        json.addProperty("afterglowEnderite", 10_000);
        json.addProperty("warmDurationTicks", 5);
        json.addProperty("eatSpeedBonus", 0.9);
        LibConfig.apply(json);
        check(h, LibConfig.reinforcedSpeed == LibConfig.MAX_SPEED, "speed not clamped");
        check(h, LibConfig.factorMedium == LibConfig.FACTOR_MIN, "factor not clamped");
        check(h, LibConfig.afterglowSeconds[3] == LibConfig.AFTERGLOW_MAX_SECONDS, "afterglow not clamped");
        check(h, LibConfig.warmDurationTicks == LibConfig.WARM_DURATION_MIN, "warm duration not clamped");
        check(h, LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_MAX, "eat bonus not clamped");
        LibConfig.reset();
        check(h, LibConfig.factorHigh == 0.75 && LibConfig.afterglowSeconds[0] == 2, "defaults wrong");
        h.succeed();
    }

    private static void tierLayout(GameTestHelper h) {
        check(h, CrucibleTier.IRON.slots() == 6 && CrucibleTier.REINFORCED.slots() == 9
                && CrucibleTier.NETHERITE.slots() == 18 && CrucibleTier.ENDERITE.slots() == 27, "slot counts (owner F4)");
        check(h, CrucibleTier.IRON.below(0) == 3 && CrucibleTier.IRON.below(3) == -1, "iron below");
        check(h, CrucibleTier.NETHERITE.below(6) == -1 && CrucibleTier.NETHERITE.below(9) == 12, "second grid below");
        check(h, CrucibleTier.ENDERITE.stackMultiplier() == 2 && CrucibleTier.NETHERITE.stackMultiplier() == 1, "stack multiplier");
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
        check(h, fresh != null && fresh.getContainerSize() == 9 && fresh.getItem(4).is(Items.RAW_COPPER) && fresh.getItem(4).getCount() == 3,
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
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleBarrelBlock.ATTACH_STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleBarrelBlock.attachStrike(h.getLevel(), barrelAbs, player, axe, 1), "attach strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        var state = h.getLevel().getBlockState(barrelAbs);
        check(h, state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.ATTACHED)
                && state.getValue(com.simplelib.crucible.CrucibleBarrelBlock.FACING) == Direction.WEST, "attached, facing the crucible");
        var barrel = (com.simplelib.crucible.CrucibleBarrelBlockEntity) h.getLevel().getBlockEntity(barrelAbs);
        check(h, barrel.getContainerSize() == 27, "on its own a copper barrel has 27 slots (owner 58)");
        be.markHeatDirty();
        be.setItem(0, new ItemStack(Items.RAW_GOLD));
        run(h, be, 270);
        check(h, barrel.getItem(0).is(Items.GOLD_INGOT), "result went into the barrel first (owner wish)");
        check(h, be.getItem(3).isEmpty(), "not into the slot below");
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

    /** Owner 56 B: without SimpleBuilding a cauldron becomes reinforced with an axe and 4 diamonds. */
    private static void axeUpgradesCauldron(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        h.setBlock(rel, Blocks.CAULDRON);
        BlockPos abs = h.absolutePos(rel);
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 4));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        for (int i = 0; i < com.simplelib.crucible.CrucibleUpgrades.CAULDRON_STRIKES; i++) {
            check(h, com.simplelib.crucible.CrucibleUpgrades.strike(h.getLevel(), abs, player, axe), "strike " + (i + 1));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(axe));
        }
        check(h, h.getLevel().getBlockState(abs).is(LibBlocks.REINFORCED_CAULDRON), "reinforced cauldron built");
        check(h, player.getOffhandItem().isEmpty(), "four diamonds used");
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

    private LibTests() {}
}
