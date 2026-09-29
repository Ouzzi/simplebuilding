package com.simplebuilding.gametest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.OreDetectorItem;
import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.recipe.RecipeFilter;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksClientHooks;
import com.simplebuilding.tweaks.block.PadOwnership;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderRegistry;
import com.simplebuilding.tweaks.item.LaserBeam;
import com.simplebuilding.tweaks.network.TweaksConfigPayload;
import com.simplebuilding.util.AirJumpGuard;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.TradePrices;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviour of the "Server & Modpack Tuning" tab (owner decisions 2026-09-28): a switch that is off
 * really switches its feature off, every speed and range is clamped, and on a client the server's
 * values win over the client's own file.
 *
 * <p>Every test changes the live config and puts it back in a {@code finally} before it returns -
 * the bodies run synchronously on the server thread, so no other test ever sees a changed value.
 */
public final class ServerTuningTests {

    private ServerTuningTests() {
    }

    /**
     * The client reads the server's tab, not its own file: {@link ServerTuning#resolve} with the
     * client flag returns what the last {@code TweaksConfigPayload} carried, the server side keeps
     * its own values, and the payload carries the tab intact through its codec. A reader that goes
     * through {@link ServerTuning#get()} (the pad break time) follows the synced value on the client
     * thread.
     *
     * <p>What breaks it: dropping the JSON from the payload or its codec, {@code resolve} ignoring
     * the synced values, or a client-side reader that goes back to the local config.
     */
    public static void theServerValueWinsOverTheClientFile(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean airJump = config.server.features.airJump;
        int padSeconds = config.server.pads.strangerPadBreakSeconds;
        BooleanSupplier hook = TweaksClientHooks.clientThreadHook();
        try {
            ServerTuningConfig server = new ServerTuningConfig();
            server.features.airJump = false;
            server.pads.strangerPadBreakSeconds = 5;
            SimpleTweaks.ServerValues values = new SimpleTweaks.ServerValues(64, 3, true, 512, 100, ServerTuning.toJson(server));

            // the codec carries the whole tab
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            TweaksConfigPayload.CODEC.encode(buf, TweaksConfigPayload.of(values));
            TweaksConfigPayload decoded = TweaksConfigPayload.CODEC.decode(buf);
            helper.assertTrue(decoded.values().equals(values), "the tuning tab did not survive the payload codec");

            config.server.features.airJump = true;
            config.server.pads.strangerPadBreakSeconds = 60;
            SimpleTweaks.setServerValues(decoded.values());

            helper.assertTrue(!ServerTuning.resolve(true).features.airJump,
                    "on the client the own air jump switch (on) won over the server's (off)");
            helper.assertTrue(ServerTuning.resolve(true).pads.strangerPadBreakSeconds == 5,
                    "on the client the pad break time is not the server's 5 s");
            helper.assertTrue(ServerTuning.resolve(false).features.airJump,
                    "the server side read the synced client copy instead of its own file");

            TweaksClientHooks.setClientThread(() -> true);
            float clientProgress = PadOwnership.strangerProgress(PadOwnership.STRANGER_PAD);
            TweaksClientHooks.setClientThread(hook);
            helper.assertTrue(Math.abs(clientProgress - 1.0f / 100.0f) < 1.0e-6f,
                    "the client computed a stranger's break progress of " + clientProgress + " instead of the server's 1/100 per tick");
            helper.assertTrue(Math.abs(PadOwnership.strangerProgress(PadOwnership.STRANGER_PAD) - 1.0f / 1200.0f) < 1.0e-6f,
                    "the server thread did not keep its own 60 s pad break time");

            SimpleTweaks.setServerValues(new SimpleTweaks.ServerValues(64, 3, true, 512, 100));
            helper.assertTrue(ServerTuning.resolve(true).features.airJump,
                    "without a synced tab the client did not fall back to its own file");
        } finally {
            TweaksClientHooks.setClientThread(hook);
            SimpleTweaks.setServerValues(null);
            config.server.features.airJump = airJump;
            config.server.pads.strangerPadBreakSeconds = padSeconds;
        }
        helper.succeed();
    }

    /**
     * Every speed and range has an upper bound that keeps vanilla intact, and nonsense is pulled
     * back into range when the file loads or the command writes (both run {@code validatePostLoad}).
     */
    public static void everySpeedAndRangeOptionIsClamped(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        String saved = ServerTuning.toJson(config.server);
        try {
            ServerTuningConfig s = config.server;
            s.machines.enderiteHopperSpeed = 100;
            s.machines.reinforcedFurnaceSpeed = -3;
            s.oreDetector.rangeMultiplier = 10.0;
            s.oreDetector.scanIntervalTicks = 1;
            s.loot.globalLootMultiplier = -2.0;
            s.loot.tradePriceMultiplier = Double.NaN;
            s.tools.stoneChiselCooldownTicks = 0;
            s.tools.sledgehammerUpgradeSeconds = 500;
            s.blueprint.maxBlocksPerTick = 999_999;
            s.pads.strangerPadBreakSeconds = 0;
            s.charges.lensMaxCharge = 1;
            s.trimStrengths.fireProtection = 5.0;
            s.dimensionLocks.flypadBlockedDimensions = null;
            config.validatePostLoad();

            List<String> problems = new ArrayList<>();
            check(problems, s.machines.enderiteHopperSpeed == ServerTuning.MAX_MACHINE_SPEED, "hopper speed " + s.machines.enderiteHopperSpeed);
            check(problems, ServerTuning.hopperCooldown(3) == 1, "hopper cooldown " + ServerTuning.hopperCooldown(3));
            check(problems, s.machines.reinforcedFurnaceSpeed == 1, "furnace speed " + s.machines.reinforcedFurnaceSpeed);
            check(problems, ServerTuning.furnaceExtraTicks(1) == 0, "furnace extra ticks " + ServerTuning.furnaceExtraTicks(1));
            check(problems, s.oreDetector.rangeMultiplier == ServerTuning.MAX_DETECTOR_RANGE, "detector range " + s.oreDetector.rangeMultiplier);
            check(problems, s.oreDetector.scanIntervalTicks == ServerTuning.MIN_SCAN_INTERVAL, "scan interval " + s.oreDetector.scanIntervalTicks);
            check(problems, s.loot.globalLootMultiplier == 0.0, "loot multiplier " + s.loot.globalLootMultiplier);
            check(problems, s.loot.tradePriceMultiplier == 1.0, "a NaN price multiplier became " + s.loot.tradePriceMultiplier);
            check(problems, s.tools.stoneChiselCooldownTicks == ServerTuning.MIN_CHISEL_COOLDOWN, "chisel cooldown " + s.tools.stoneChiselCooldownTicks);
            check(problems, SledgehammerUpgrades.blows() == ServerTuning.MAX_UPGRADE_SECONDS, "hammer blows " + SledgehammerUpgrades.blows());
            check(problems, s.blueprint.maxBlocksPerTick == ServerTuning.MAX_BLUEPRINT_BLOCKS_PER_TICK, "blueprint speed " + s.blueprint.maxBlocksPerTick);
            check(problems, s.pads.strangerPadBreakSeconds == 1, "pad break seconds " + s.pads.strangerPadBreakSeconds);
            check(problems, s.charges.lensMaxCharge == ServerTuning.MIN_LENS, "lens charge " + s.charges.lensMaxCharge);
            check(problems, s.trimStrengths.fireProtection == ServerTuning.MAX_TRIM_STRENGTH, "fire protection strength " + s.trimStrengths.fireProtection);
            check(problems, ServerTuning.trimStrength("fire_protection") == (float) ServerTuning.MAX_TRIM_STRENGTH,
                    "trim strength read as " + ServerTuning.trimStrength("fire_protection"));
            check(problems, "".equals(s.dimensionLocks.flypadBlockedDimensions), "a missing dimension list stayed null");
            helper.assertTrue(problems.isEmpty(), "values escaped their bounds: " + problems);
        } finally {
            config.server = ServerTuning.copyOf(saved);
            config.validatePostLoad();
        }
        helper.succeed();
    }

    /**
     * {@code server.features.airJump} off: the server guard refuses an air jump although the boots
     * would allow it and the client asked; on again, the same jump goes through.
     */
    public static void theAirJumpSwitchRefusesAirJumps(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean original = config.server.features.airJump;
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 4.0, 3.5));
        try {
            player.setOnGround(false);
            config.server.features.airJump = false;
            helper.assertTrue(!AirJumpGuard.tryUse(player, 1), "the server accepted an air jump with the server switch off");
            config.server.features.airJump = true;
            helper.assertTrue(AirJumpGuard.tryUse(player, 1), "the server refused an air jump with the server switch on");
        } finally {
            config.server.features.airJump = original;
            removePlayer(helper, player);
        }
        helper.succeed();
    }

    /**
     * Chunk loaders (owner decision: default on): a loader whose owner is offline releases its chunks
     * and shows up idle in the registry the admin command lists; with its owner online it loads;
     * with the rule switched off it loads for an offline owner; in a blocked dimension it never loads.
     */
    public static void chunkLoadersIdleWhileTheirOwnerIsOffline(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean rule = config.server.chunkLoaders.requireOwnerOnline;
        String blocked = config.server.dimensionLocks.chunkLoaderBlockedDimensions;
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos pos = helper.absolutePos(rel);
        ServerPlayer owner = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        try {
            config.server.chunkLoaders.requireOwnerOnline = true;
            config.server.dimensionLocks.chunkLoaderBlockedDimensions = "";
            helper.setBlock(rel, TweaksBlocks.CHUNK_LOADER);
            ChunkLoaderBlockEntity loader = (ChunkLoaderBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(loader != null, "no chunk loader block entity");

            loader.setOwner(UUID.randomUUID());
            loader.update(level);
            // The gametest area is force-loaded already, so "forced by this loader" is the loader's own
            // record plus the registry flag, not the ticket list.
            helper.assertTrue(!loader.mayRun(level) && loader.ownForced().isEmpty(),
                    "a loader with an offline owner may run or forced " + loader.ownForced().size() + " chunks");
            ChunkLoaderRegistry.Entry idle = ChunkLoaderRegistry.find(level.getServer(), level.dimension(), pos);
            helper.assertTrue(idle != null && !idle.active(), "the registry does not list the loader as idle: " + idle);

            config.server.chunkLoaders.requireOwnerOnline = false;
            loader.update(level);
            helper.assertTrue(loader.mayRun(level), "with the owner rule off the loader of an offline owner may not run");
            helper.assertTrue(ChunkLoaderRegistry.find(level.getServer(), level.dimension(), pos).active(),
                    "the registry still lists the running loader as idle");

            config.server.chunkLoaders.requireOwnerOnline = true;
            loader.update(level);
            helper.assertTrue(loader.ownForced().isEmpty(), "switching the owner rule back on did not release the chunks");

            loader.setOwner(owner.getUUID());
            loader.update(level);
            helper.assertTrue(loader.mayRun(level), "a loader whose owner is online may not run");

            config.server.dimensionLocks.chunkLoaderBlockedDimensions = level.dimension().identifier().toString();
            loader.update(level);
            helper.assertTrue(loader.ownForced().isEmpty() && !loader.mayRun(level),
                    "a loader in a blocked dimension still holds " + loader.ownForced().size() + " chunks");
        } finally {
            config.server.chunkLoaders.requireOwnerOnline = rule;
            config.server.dimensionLocks.chunkLoaderBlockedDimensions = blocked;
            helper.setBlock(rel, Blocks.AIR);
            removePlayer(helper, owner);
        }
        helper.assertTrue(ChunkLoaderRegistry.find(level.getServer(), level.dimension(), pos) == null,
                "breaking the loader left its registry entry behind");
        helper.succeed();
    }

    /**
     * A switched-off feature loses its recipes: {@link RecipeFilter} names them, and a recipe table
     * built while the switch is off (every table goes through the private {@code RecipeMap}
     * constructor, so this is what {@code /reload} builds) no longer contains them - unrelated
     * recipes stay. With the switch on, nothing is filtered.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void disabledFeaturesLoseTheirRecipes(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean backpack = config.server.features.backpack;
        Identifier backpackId = Identifier.fromNamespaceAndPath("simplebuilding", "backpack");
        Identifier octantId = Identifier.fromNamespaceAndPath("simplebuilding", "octant");
        try {
            helper.assertTrue(!RecipeFilter.removes(backpackId), "the backpack recipe is filtered with every switch on");
            Optional<RecipeHolder<?>> backpackRecipe = helper.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, backpackId));
            Optional<RecipeHolder<?>> octantRecipe = helper.getLevel().getServer().getRecipeManager()
                    .byKey(ResourceKey.create(Registries.RECIPE, octantId));
            helper.assertTrue(backpackRecipe.isPresent() && octantRecipe.isPresent(),
                    "the live recipe table lacks the backpack or octant recipe with every switch on");

            config.server.features.backpack = false;
            helper.assertTrue(RecipeFilter.removes(backpackId), "the backpack switch is off but its recipe is not filtered");
            helper.assertTrue(RecipeFilter.removes(Identifier.fromNamespaceAndPath("simplebuilding", "netherite_backpack_smithing")),
                    "the backpack upgrade recipe survives the switch");
            helper.assertTrue(!RecipeFilter.removes(octantId), "the octant recipe is filtered by the backpack switch");

            Multimap<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.of(
                    backpackRecipe.get().value().getType(), backpackRecipe.get(),
                    octantRecipe.get().value().getType(), octantRecipe.get());
            Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.of(
                    backpackRecipe.get().id(), backpackRecipe.get(), octantRecipe.get().id(), octantRecipe.get());
            Constructor<RecipeMap> constructor = RecipeMap.class.getDeclaredConstructor(Multimap.class, Map.class);
            constructor.setAccessible(true);
            RecipeMap filtered = constructor.newInstance(byType, byKey);
            helper.assertTrue(filtered.values().size() == 1 && filtered.values().iterator().next().id().identifier().equals(octantId),
                    "a recipe table built with the backpack switch off holds " + filtered.values().size()
                            + " recipes instead of only the octant");
        } catch (ReflectiveOperationException e) {
            helper.fail("could not build a recipe table: " + e);
        } finally {
            config.server.features.backpack = backpack;
        }
        helper.succeed();
    }

    /**
     * The global loot multiplier scales every mod pool (2 = twice the pools on both editor paths,
     * 0 = none), a structure switch keeps the mod out of that structure's chests only, and the
     * charged-creeper heads stay whatever the tab says.
     */
    public static void theLootMultiplierAndStructureSwitchesShapeTheModLoot(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        double multiplier = config.server.loot.globalLootMultiplier;
        boolean endCity = config.server.loot.endCityLoot;
        try {
            config.server.loot.globalLootMultiplier = 1.0;
            config.server.loot.endCityLoot = true;
            Recorder base = record(helper, BuiltInLootTables.END_CITY_TREASURE);
            helper.assertTrue(base.builders > 0 && base.built > 0, "the end city got no mod pools with the defaults");

            config.server.loot.globalLootMultiplier = 2.0;
            Recorder doubled = record(helper, BuiltInLootTables.END_CITY_TREASURE);
            helper.assertTrue(doubled.builders == 2 * base.builders && doubled.built == 2 * base.built,
                    "a loot multiplier of 2 gave " + doubled.builders + "/" + doubled.built + " pools instead of "
                            + (2 * base.builders) + "/" + (2 * base.built));

            config.server.loot.globalLootMultiplier = 0.0;
            helper.assertTrue(record(helper, BuiltInLootTables.END_CITY_TREASURE).total() == 0,
                    "a loot multiplier of 0 still added mod pools");
            helper.assertTrue(record(helper, BuiltInLootTables.CHARGED_CREEPER).total() > 0,
                    "the loot multiplier also took the charged creeper heads");

            config.server.loot.globalLootMultiplier = 1.0;
            config.server.loot.endCityLoot = false;
            helper.assertTrue(record(helper, BuiltInLootTables.END_CITY_TREASURE).total() == 0,
                    "the end city switch is off but the mod still adds pools there");
            helper.assertTrue(record(helper, BuiltInLootTables.STRONGHOLD_LIBRARY).total() > 0,
                    "the end city switch also emptied the stronghold library");
        } finally {
            config.server.loot.globalLootMultiplier = multiplier;
            config.server.loot.endCityLoot = endCity;
        }
        helper.succeed();
    }

    /**
     * The laser switch for creatures: off, the beam may not set a living target on fire, on it may.
     */
    public static void theLaserSwitchesStopWhatTheBeamIgnites(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        boolean entities = config.server.laser.igniteEntities;
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        LivingEntity pig = spawnPig(helper, new BlockPos(4, 1, 4));
        try {
            config.server.laser.igniteEntities = true;
            helper.assertTrue(LaserBeam.canIgnite(player, pig), "the beam may not ignite a dry pig with the switch on");
            config.server.laser.igniteEntities = false;
            helper.assertTrue(!LaserBeam.canIgnite(player, pig), "the beam may ignite a pig with the creature switch off");
        } finally {
            config.server.laser.igniteEntities = entities;
            pig.discard();
            removePlayer(helper, player);
        }
        helper.succeed();
    }

    /**
     * The numbers reach the code that uses them: hopper and furnace tiers, chisel cooldowns, hammer
     * upgrade length and damage, ore detector range and interval, stranger break time and trade prices.
     */
    public static void tuningValuesReachTheToolsAndMachines(GameTestHelper helper) {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        String saved = ServerTuning.toJson(config.server);
        try {
            List<String> problems = new ArrayList<>();
            // defaults = the old fixed behaviour
            check(problems, ServerTuning.hopperCooldown(1) == 4 && ServerTuning.hopperCooldown(2) == 2 && ServerTuning.hopperCooldown(3) == 1,
                    "default hopper cooldowns");
            check(problems, ServerTuning.furnaceExtraTicks(1) == 1 && ServerTuning.furnaceExtraTicks(2) == 3 && ServerTuning.furnaceExtraTicks(3) == 7,
                    "default furnace extra ticks");
            check(problems, ModItems.IRON_CHISEL.effectiveCooldownTicks() == 25 && ModItems.NETHERITE_SPATULA.effectiveCooldownTicks() == 5,
                    "default chisel cooldowns");
            check(problems, SledgehammerUpgrades.upgradeTicks() == SledgehammerUpgrades.UPGRADE_TICKS, "default upgrade length");
            check(problems, OreDetectorItem.OreClass.COMMON.range(false) == 24 && OreDetectorItem.scanInterval(null) == 20,
                    "default detector range/interval");
            check(problems, ServerTuning.upgradeDamagePerHit(false, true) == SledgehammerUpgrades.ENDERITE_DAMAGE_PER_HIT, "default enderite damage");

            ServerTuningConfig s = config.server;
            s.machines.reinforcedHopperSpeed = 8;
            s.machines.enderiteFurnaceSpeed = 2;
            s.tools.ironChiselCooldownTicks = 40;
            s.tools.sledgehammerUpgradeSeconds = 3;
            s.tools.enderiteUpgradeDamagePerHit = 1;
            s.oreDetector.rangeMultiplier = 0.5;
            s.oreDetector.scanIntervalTicks = 40;
            s.pads.strangerPlateBreakSeconds = 30;
            s.loot.tradePriceMultiplier = 2.0;
            check(problems, ServerTuning.hopperCooldown(1) == 1, "reinforced hopper at speed 8: " + ServerTuning.hopperCooldown(1));
            check(problems, ServerTuning.furnaceExtraTicks(3) == 1, "enderite furnace at speed 2: " + ServerTuning.furnaceExtraTicks(3));
            check(problems, ModItems.IRON_CHISEL.effectiveCooldownTicks() == 40 && ModItems.IRON_SPATULA.effectiveCooldownTicks() == 40,
                    "iron chisel/spatula cooldown: " + ModItems.IRON_CHISEL.effectiveCooldownTicks());
            check(problems, SledgehammerUpgrades.upgradeTicks() == 60 && SledgehammerUpgrades.blows() == 3,
                    "upgrade length " + SledgehammerUpgrades.upgradeTicks());
            check(problems, ServerTuning.upgradeDamagePerHit(false, true) == 1, "enderite damage per blow");
            check(problems, OreDetectorItem.OreClass.COMMON.range(false) == 12, "detector range at 0.5: " + OreDetectorItem.OreClass.COMMON.range(false));
            check(problems, OreDetectorItem.scanInterval(null) == 40 && OreDetectorItem.scanInterval(net.minecraft.world.entity.EquipmentSlot.OFFHAND) == 80,
                    "detector interval");
            check(problems, Math.abs(PadOwnership.strangerProgress(PadOwnership.STRANGER_PLATE) - 1.0f / 600.0f) < 1.0e-6f,
                    "plate break progress " + PadOwnership.strangerProgress(PadOwnership.STRANGER_PLATE));
            check(problems, TradePrices.scale(new ItemCost(Items.EMERALD, 10)).count() == 20, "trade price at 2x");
            check(problems, TradePrices.scale(new ItemCost(Items.EMERALD, 40)).count() == 64, "a doubled price above a stack");
            check(problems, TradePrices.scale(new ItemCost(Items.EMERALD, 1), 0.25).count() == 1, "a quartered price below 1");
            helper.assertTrue(problems.isEmpty(), "tuning values did not reach their readers: " + problems);
        } finally {
            config.server = ServerTuning.copyOf(saved);
            config.validatePostLoad();
        }
        helper.succeed();
    }

    // =====================================================================================

    private static void check(List<String> problems, boolean ok, String what) {
        if (!ok) {
            problems.add(what);
        }
    }

    /** Counts the pools the mod hands over, keeping the two editor paths apart. */
    private static final class Recorder implements ModLootTableModifications.Editor {
        int builders;
        int built;

        @Override
        public void addPool(LootPool.Builder pool) {
            pool.build();
            builders++;
        }

        @Override
        public void addBuiltPool(LootPool pool) {
            built++;
        }

        int total() {
            return builders + built;
        }
    }

    private static Recorder record(GameTestHelper helper, ResourceKey<LootTable> table) {
        Recorder recorder = new Recorder();
        ModLootTableModifications.apply(table, recorder, helper.getLevel().registryAccess());
        return recorder;
    }

    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static LivingEntity spawnPig(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(net.minecraft.world.entity.EntityType.PIG, pos);
    }
}
