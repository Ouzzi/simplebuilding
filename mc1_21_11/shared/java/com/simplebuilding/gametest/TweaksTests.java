package com.simplebuilding.gametest;

import com.mojang.authlib.GameProfile;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.CopperPressurePlateBlock;
import com.simplebuilding.tweaks.block.FilterPressurePlateBlock;
import com.simplebuilding.tweaks.block.PadOwnership;
import com.simplebuilding.tweaks.block.PadTiers;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.entity.ChunkLoaderBlockEntity;
import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.FilterPlateBlockEntity;
import com.simplebuilding.tweaks.block.entity.FlypadBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.command.TweaksCommands;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.EchoCompassItem;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import com.simplebuilding.tweaks.spawn.ElytraDamageRules;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import com.simplebuilding.tweaks.spawn.SpawnRules;
import com.simplebuilding.tweaks.spawn.SpawnSetup;
import com.simplebuilding.tweaks.xp.XpClumping;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests des aus Simple Tweaks uebernommenen Teils (docs/SIMPLETWEAKS-UEBERNAHME.md).
 *
 * <p>Die meisten Tests rufen die Logik der Block-Entities direkt und in einem Tick auf: die Pads
 * ticken nur jede halbe Sekunde, und Config-Schalter werden in demselben Aufruf gesetzt und wieder
 * zurueckgesetzt, damit parallel laufende Tests sie nie zu sehen bekommen.
 */
public final class TweaksTests {

    private TweaksTests() {
    }

    /** Ticks fuer die Tests, die echte Ticks abwarten (Kupferplatte, Teleporter). */
    public static final int COPPER_MAX_TICKS = 100;
    public static final int TELEPORTER_MAX_TICKS = 160;

    // =====================================================================================
    // Stufen und Rezepte
    // =====================================================================================

    /**
     * Die Pad-Bereiche wachsen mit jeder Stufe, und die neue Enderit-Stufe IV liegt zwischen Netherit
     * (III) und der Netherstern-Stufe (jetzt V): 47x47x95 zwischen 31x31x63 und 63x63x127.
     */
    public static void padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier(GameTestHelper helper) {
        int[] widths = new int[PadTiers.MAX];
        for (int tier = 1; tier <= PadTiers.MAX; tier++) {
            widths[tier - 1] = (int) (PadTiers.halfWidth(tier) * 2);
            if (tier > 1) {
                helper.assertTrue(PadTiers.halfWidth(tier) > PadTiers.halfWidth(tier - 1) && PadTiers.height(tier) > PadTiers.height(tier - 1),
                        "pad tier " + tier + " is not larger than tier " + (tier - 1));
            }
        }
        helper.assertValueEqual(List.of(widths[0], widths[1], widths[2], widths[3], widths[4]), List.of(5, 15, 31, 47, 63), "pad widths per tier");
        helper.assertValueEqual(List.of(PadTiers.height(1), PadTiers.height(2), PadTiers.height(3), PadTiers.height(4), PadTiers.height(5)),
                List.of(15, 31, 63, 95, 127), "pad heights per tier");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(TweaksBlocks.ENDERITE_ELYTRA_PAD.defaultBlockState()), 4, "tier of the enderite elytra pad");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(TweaksBlocks.FINE_ELYTRA_PAD.defaultBlockState()), 5, "tier of the fine elytra pad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(TweaksBlocks.ENDERITE_FLYPAD.defaultBlockState()), 4, "tier of the enderite flypad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(TweaksBlocks.STELLAR_FLYPAD.defaultBlockState()), 5, "tier of the stellar flypad");
        helper.assertFalse(PadTiers.hasEnderiteBonus(3), "the netherite tier already carries the enderite bonus");
        helper.assertTrue(PadTiers.hasEnderiteBonus(4) && PadTiers.hasEnderiteBonus(5), "the enderite bonus does not carry over to the higher tiers");
        TestCleanup.succeed(helper);
    }

    /**
     * Jede Enderit-Stufe entsteht im Schmiedetisch aus Enderit-Vorlage + Netherit-Stufe + Enderitbarren,
     * die Netherstern-Stufen jetzt aus der Enderit-Stufe - und nicht mehr direkt aus Netherit.
     */
    public static void enderiteTiersAreSmithedFromTheNetheriteTierAndTheNetherStarTiersFromEnderite(GameTestHelper helper) {
        Item template = ModItems.ENDERITE_UPGRADE_TEMPLATE;
        Item netherite = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
        Item ingot = ModItems.ENDERITE_INGOT;
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_ELYTRA_PAD, ingot, TweaksBlocks.ENDERITE_ELYTRA_PAD);
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_FLYPAD, ingot, TweaksBlocks.ENDERITE_FLYPAD);
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_PRESSURE_PLATE, ingot, TweaksBlocks.ENDERITE_PRESSURE_PLATE);
        expectSmithing(helper, template, TweaksBlocks.SPAWN_TELEPORTER_TIER_4, ingot, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);
        expectSmithing(helper, template, TweaksBlocks.CHUNK_LOADER, ingot, TweaksBlocks.ENDERITE_CHUNK_LOADER);
        expectSmithing(helper, template, TweaksBlocks.LAUNCHPAD, ingot, TweaksBlocks.ENDERITE_LAUNCHPAD);
        expectSmithing(helper, netherite, TweaksBlocks.ENDERITE_ELYTRA_PAD, Items.NETHER_STAR, TweaksBlocks.FINE_ELYTRA_PAD);
        expectSmithing(helper, netherite, TweaksBlocks.REINFORCED_ELYTRA_PAD, Items.NETHERITE_INGOT, TweaksBlocks.NETHERITE_ELYTRA_PAD);
        expectSmithing(helper, netherite, TweaksBlocks.FINE_ELYTRA_PAD, Items.NETHERITE_INGOT, TweaksBlocks.FLYPAD);
        expectSmithing(helper, netherite, TweaksBlocks.DIAMOND_PRESSURE_PLATE, Items.NETHERITE_INGOT, TweaksBlocks.NETHERITE_PRESSURE_PLATE);
        expectSmithing(helper, netherite, TweaksBlocks.COPPER_PRESSURE_PLATE, Items.NETHERITE_INGOT, TweaksBlocks.CHUNK_LOADER);
        // Der alte Weg (Netherit-Pad + Netherstern) fuehrt nicht mehr zum feinen Pad.
        Optional<RecipeHolder<SmithingRecipe>> oldWay = smithing(helper, netherite, TweaksBlocks.NETHERITE_ELYTRA_PAD, Items.NETHER_STAR);
        helper.assertTrue(oldWay.isEmpty() || !oldWay.get().value().assemble(smithingInput(netherite, TweaksBlocks.NETHERITE_ELYTRA_PAD, Items.NETHER_STAR), helper.getLevel().registryAccess()).is(TweaksBlocks.FINE_ELYTRA_PAD.asItem()),
                "the netherite elytra pad still becomes the fine pad directly, so the enderite tier can be skipped");
        TestCleanup.succeed(helper);
    }

    /** Stellares Flypad V: KKK / ESE / FFF mit F = Enderit-Flypad IV (vorher Netherit). */
    public static void theStellarFlypadIsCraftedFromEnderiteFlypads(GameTestHelper helper) {
        Item f = TweaksBlocks.ENDERITE_FLYPAD.asItem();
        CraftingInput grid = grid(Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY,
                Items.ENCHANTED_GOLDEN_APPLE, Items.NETHER_STAR, Items.ENCHANTED_GOLDEN_APPLE, f, f, f);
        expectCrafting(helper, grid, TweaksBlocks.STELLAR_FLYPAD.asItem(), "simplebuilding:stellar_flypad_crafting");
        Item n = TweaksBlocks.NETHERITE_FLYPAD.asItem();
        CraftingInput oldGrid = grid(Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY,
                Items.ENCHANTED_GOLDEN_APPLE, Items.NETHER_STAR, Items.ENCHANTED_GOLDEN_APPLE, n, n, n);
        helper.assertTrue(craftingResult(helper, oldGrid).isEmpty(), "netherite flypads still craft the stellar flypad");
        TestCleanup.succeed(helper);
    }

    /**
     * Echo-Kompass (Besitzer-Rezept): Bergungskompass in der Mitte, Enderit-Kern darueber,
     * Netherit-Druckplatten links und rechts.
     */
    public static void theEchoCompassIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndNetheritePlates(GameTestHelper helper) {
        Item p = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        CraftingInput grid = grid(null, ModItems.ENDERITE_CORE, null, p, Items.RECOVERY_COMPASS, p, null, null, null);
        expectCrafting(helper, grid, TweaksItems.ECHO_COMPASS, "simplebuilding:echo_compass");
        CraftingInput swapped = grid(null, Items.RECOVERY_COMPASS, null, p, ModItems.ENDERITE_CORE, p, null, null, null);
        helper.assertTrue(craftingResult(helper, swapped).isEmpty(), "core and compass swapped still craft an echo compass");
        CraftingInput diamondPlates = grid(null, ModItems.ENDERITE_CORE, null, TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem(),
                Items.RECOVERY_COMPASS, TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem(), null, null, null);
        helper.assertTrue(craftingResult(helper, diamondPlates).isEmpty(), "diamond plates are accepted instead of netherite plates");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Elytra-Pads und Spawn-Elytra
    // =====================================================================================

    /**
     * Ein Elytra-Pad legt einem Spieler mit leerem Brust-Slot eine Spawn-Elytra an, die NICHT vor
     * Fallschaden schuetzt (nur Spawn-Elytren tun das), und laedt sie mit voller Flugzeit.
     */
    public static void elytraPadsEquipAnUnsafeSpawnElytraInTheirArea(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, TweaksBlocks.ELYTRA_PAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(chest.is(TweaksItems.SPAWN_ELYTRA), "the elytra pad put " + chest + " into the chest slot instead of a spawn elytra");
        helper.assertValueEqual(chest.get(TweaksComponents.IS_SAFE_ELYTRA), false, "safe flag of a pad elytra");
        helper.assertValueEqual(chest.get(TweaksComponents.FLIGHT_TIME), SimpleTweaks.config().spawn.flightTimeSeconds * 20, "flight time of a fresh pad elytra");
        helper.assertValueEqual(chest.get(TweaksComponents.BOOST_LEVEL), 1.0f, "boost level of a fresh pad elytra");
        helper.assertFalse(ElytraDamageRules.wearsSafeElytra(player), "a pad elytra counts as a safe spawn elytra");

        // Wer eine Brustplatte traegt, behaelt sie.
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the elytra pad replaced a worn chestplate");
        TestCleanup.succeed(helper);
    }

    /**
     * Boosts laden bis Netherit nur in der 3x3-Saeule ueber dem Pad; ab der Enderit-Stufe im ganzen
     * Bereich (Zusatzfunktion der Stufe IV, die V behaelt).
     */
    public static void elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea(GameTestHelper helper) {
        BlockPos pad = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper, new Vec3(6.5, 2.0, 6.5));
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        helper.assertFalse(ElytraPadBlockEntity.isInBoostColumn(player, helper.absolutePos(pad)), "the probe player stands in the boost column");
        for (int tier = 3; tier <= 5; tier++) {
            ItemStack elytra = new ItemStack(TweaksItems.SPAWN_ELYTRA);
            elytra.set(TweaksComponents.BOOST_LEVEL, 0.0f);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            ElytraPadBlockEntity.applyTo(helper.getLevel(), helper.absolutePos(pad), tier, player, config);
            float boost = player.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(TweaksComponents.BOOST_LEVEL, 0.0f);
            if (tier < PadTiers.ENDERITE) {
                helper.assertValueEqual(boost, 0.0f, "boost level recharged outside the column by a tier " + tier + " pad");
            } else {
                helper.assertValueEqual(boost, 1.0f, "boost level recharged outside the column by a tier " + tier + " pad");
            }
        }
        TestCleanup.succeed(helper);
    }

    /** Boost: kostet 1/maxBoosts, braucht Gleitflug und Spawn-Elytra, schiebt in Blickrichtung. */
    public static void theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        ItemStack elytra = new ItemStack(TweaksItems.SPAWN_ELYTRA);
        elytra.set(TweaksComponents.BOOST_LEVEL, 1.0f);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        int maxBoosts = Math.max(1, SimpleTweaks.config().spawn.maxBoosts);

        TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
        helper.assertValueEqual(elytra.get(TweaksComponents.BOOST_LEVEL), 1.0f, "boost level after a boost request on foot");

        player.startFallFlying();
        player.setDeltaMovement(Vec3.ZERO);
        TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
        float expected = 1.0f - 1.0f / maxBoosts;
        helper.assertTrue(Math.abs(elytra.get(TweaksComponents.BOOST_LEVEL) - expected) < 0.0001f,
                "boost level after one boost: " + elytra.get(TweaksComponents.BOOST_LEVEL) + ", expected " + expected);
        helper.assertTrue(player.getDeltaMovement().length() > 0.1, "the boost did not move the player");
        for (int i = 0; i < maxBoosts + 2; i++) {
            TweaksNetwork.handleBoost(new ElytraBoostPayload(), player);
        }
        helper.assertValueEqual(elytra.get(TweaksComponents.BOOST_LEVEL), 0.0f, "boost level after draining every charge");
        player.stopFallFlying();
        TestCleanup.succeed(helper);
    }

    /**
     * Schadensschutz: sichere Spawn-Elytra verhindert Fall- und Kinetikschaden, eine Pad-Elytra nicht;
     * nach dem Enderit-Launchpad entfaellt genau ein Fallschaden.
     */
    public static void safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        ItemStack safe = new ItemStack(TweaksItems.SPAWN_ELYTRA);
        SpawnElytra.rechargeAsSpawnElytra(safe, SimpleTweaks.config().spawn);
        player.setItemSlot(EquipmentSlot.CHEST, safe);
        DamageSource wall = helper.getLevel().damageSources().flyIntoWall();
        DamageSource lava = helper.getLevel().damageSources().lava();
        helper.assertTrue(ElytraDamageRules.preventsFallDamage(player), "a safe spawn elytra lets fall damage through");
        helper.assertTrue(ElytraDamageRules.preventsDamage(player, wall), "a safe spawn elytra lets kinetic damage through");
        helper.assertFalse(ElytraDamageRules.preventsDamage(player, lava), "a safe spawn elytra blocks lava damage too");

        ItemStack unsafe = new ItemStack(TweaksItems.SPAWN_ELYTRA);
        unsafe.set(TweaksComponents.IS_SAFE_ELYTRA, false);
        player.setItemSlot(EquipmentSlot.CHEST, unsafe);
        boolean spawnOn = SimpleTweaks.config().spawn.giveElytraOnSpawn;
        try {
            SimpleTweaks.config().spawn.giveElytraOnSpawn = false;
            helper.assertFalse(ElytraDamageRules.preventsFallDamage(player), "a pad elytra prevents fall damage");
            helper.assertFalse(ElytraDamageRules.preventsDamage(player, wall), "a pad elytra prevents kinetic damage");

            LaunchpadBlockEntity.launch(player, LaunchpadBlockEntity.strengthFor(4), true);
            helper.assertTrue(LaunchSafety.isProtected(player), "an enderite launch does not protect the player");
            helper.assertTrue(ElytraDamageRules.preventsFallDamage(player), "fall damage after an enderite launch was not prevented");
            helper.assertFalse(ElytraDamageRules.preventsFallDamage(player), "the launch protection outlived its first landing");
            LaunchpadBlockEntity.launch(player, LaunchpadBlockEntity.strengthFor(4), false);
            helper.assertFalse(LaunchSafety.isProtected(player), "a normal launchpad protects against fall damage");
        } finally {
            SimpleTweaks.config().spawn.giveElytraOnSpawn = spawnOn;
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Timer und Aufraeumen einer Pad-Elytra laufen auch bei ausgeschalteter Spawn-Elytra (in Simple
     * Tweaks nicht: dort brach der Tick dann ab und Pad-Elytren liefen nie ab).
     */
    public static void padElytrasExpireEvenWithTheSpawnElytraSwitchedOff(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        boolean spawnOn = SimpleTweaks.config().spawn.giveElytraOnSpawn;
        try {
            SimpleTweaks.config().spawn.giveElytraOnSpawn = false;
            ItemStack elytra = new ItemStack(TweaksItems.SPAWN_ELYTRA);
            elytra.set(TweaksComponents.FLIGHT_TIME, 5);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            player.startFallFlying();
            SpawnElytra.tick(player, false);
            helper.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).get(TweaksComponents.FLIGHT_TIME), 4, "flight time after one gliding tick");
            player.getItemBySlot(EquipmentSlot.CHEST).set(TweaksComponents.FLIGHT_TIME, 1);
            SpawnElytra.tick(player, false);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "a pad elytra with no flight time left was not taken away");
            player.stopFallFlying();

            // Eine Spawn-Elytra im Inventar (statt im Brust-Slot) wird beim vollen Durchlauf entfernt.
            player.getInventory().setItem(5, new ItemStack(TweaksItems.SPAWN_ELYTRA));
            SpawnElytra.tick(player, true);
            helper.assertTrue(player.getInventory().getItem(5).isEmpty(), "a spawn elytra in the inventory was not cleaned up");
        } finally {
            SimpleTweaks.config().spawn.giveElytraOnSpawn = spawnOn;
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Flypads
    // =====================================================================================

    /** Flypads geben Kreativflug im Bereich; wer nicht mehr im Bereich ist, wird vorgemerkt und verliert ihn. */
    public static void flypadsGrantFlightInsideAndTakeItBackOutside(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, TweaksBlocks.FLYPAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().mayfly = false;
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertTrue(player.getAbilities().mayfly, "a player inside the flypad area did not get creative flight (player at "
                + player.position() + ", area " + PadTiers.flyArea(helper.absolutePos(pad), 1) + ", tracked " + be.flyingPlayers() + ", enabled " + SimpleTweaks.config().pads.enableFlypads + ", found "
                + helper.getLevel().getEntitiesOfClass(ServerPlayer.class, PadTiers.flyArea(helper.absolutePos(pad), 1), p -> true).size()
                + ", tier " + FlypadBlockEntity.tierOf(helper.getBlockState(pad)) + ")");
        helper.assertTrue(be.flyingPlayers().contains(player.getUUID()), "the flypad does not remember whom it let fly");
        helper.assertTrue(player.hasEffect(MobEffects.GLOWING), "flypad flyers are not highlighted");

        Vec3 far = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5)).add(0, PadTiers.height(1) + 5, 0);
        player.snapTo(far.x, far.y, far.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertFalse(be.flyingPlayers().contains(player.getUUID()), "the flypad still tracks a player who left its area");
        TestCleanup.succeed(helper);
    }

    /**
     * Beim Verlassen verliert ein Ueberlebensspieler den Flug; ab Stufe IV (Enderit) faengt ihn das
     * Sicherheitsnetz mit Sanftem Fall auf, auf Stufe III nicht.
     */
    public static void enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling(GameTestHelper helper) {
        ServerPlayer netherite = survivalLikePlayer(helper, new Vec3(1.5, 1.0, 1.5));
        netherite.getAbilities().mayfly = true;
        netherite.getAbilities().flying = true;
        FlypadBlockEntity.revoke(netherite, 3);
        helper.assertFalse(netherite.getAbilities().mayfly, "a survival player kept flight after leaving a flypad");
        helper.assertFalse(netherite.hasEffect(MobEffects.SLOW_FALLING), "a netherite flypad already has the safety net");

        ServerPlayer enderite = survivalLikePlayer(helper, new Vec3(3.5, 1.0, 1.5));
        enderite.getAbilities().mayfly = true;
        enderite.getAbilities().flying = true;
        FlypadBlockEntity.revoke(enderite, 4);
        helper.assertFalse(enderite.getAbilities().flying, "a survival player keeps flying after leaving an enderite flypad");
        helper.assertTrue(enderite.hasEffect(MobEffects.SLOW_FALLING), "the enderite flypad did not catch the falling player");

        ServerPlayer walker = survivalLikePlayer(helper, new Vec3(5.5, 1.0, 1.5));
        walker.getAbilities().mayfly = true;
        walker.getAbilities().flying = false;
        FlypadBlockEntity.revoke(walker, 5);
        helper.assertFalse(walker.hasEffect(MobEffects.SLOW_FALLING), "the safety net fires for players who were not flying");
        TestCleanup.succeed(helper);
    }

    /** Ausgeschaltete Flypads geben keinen Flug (Config pads.enableFlypads). */
    public static void switchedOffFlypadsGrantNoFlight(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, TweaksBlocks.FLYPAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().mayfly = false;
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        boolean enabled = SimpleTweaks.config().pads.enableFlypads;
        try {
            SimpleTweaks.config().pads.enableFlypads = false;
            FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        } finally {
            SimpleTweaks.config().pads.enableFlypads = enabled;
        }
        helper.assertFalse(player.getAbilities().mayfly, "a switched off flypad still granted flight");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Spawn-Teleporter
    // =====================================================================================

    /**
     * Wer 5 s still auf einem Spawn-Teleporter II steht, landet zwei Bloecke ueber Spawn 2; ein
     * Schritt dazwischen setzt die Zeit zurueck.
     */
    public static void spawnTeleportersSendStillPlayersToTheirSpawnPoint(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.SPAWN_TELEPORTER_TIER_2);
        BlockPos target = helper.absolutePos(new BlockPos(6, 1, 6));
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        int[] saved = {spawn.spawn2X, spawn.spawn2Y, spawn.spawn2Z};
        TweaksCommands.setTeleporterSpawn(2, target);
        TestCleanup.before(helper, () -> {
            spawn.spawn2X = saved[0];
            spawn.spawn2Y = saved[1];
            spawn.spawn2Z = saved[2];
        });
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 1.1, 2.5));
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 start = player.position();
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(2), 100, "standing time of the spawn teleporter");
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(5), 60, "standing time of the enderite spawn teleporter");
        helper.runAfterDelay(SpawnTeleporterBlockEntity.STANDARD_TICKS - 20, () ->
                helper.assertTrue(player.position().distanceTo(start) < 0.5, "the player was teleported before the countdown ended"));
        helper.succeedWhen(() -> {
            Vec3 expected = Vec3.atBottomCenterOf(target).add(0, 2.0, 0);
            helper.assertTrue(player.position().distanceTo(expected) < 1.5,
                    "the player is at " + player.position() + ", not two blocks above spawn 2 at " + expected);
        });
    }

    /** Ohne gesetztes Ziel geht es zum Weltspawn; Stufe V faellt ohne Wiedereinstiegspunkt auf Spawn 1 zurueck. */
    public static void spawnTeleporterTargetsFallBackToTheWorldSpawn(GameTestHelper helper) {
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        int savedY = spawn.spawn3Y;
        try {
            spawn.spawn3Y = -1000;
            helper.assertTrue(SpawnTeleporterBlockEntity.customTarget(3) == null, "an unset spawn 3 still has a target");
            spawn.spawn3Y = 70;
            helper.assertValueEqual(SpawnTeleporterBlockEntity.customTarget(3), new BlockPos(spawn.spawn3X, 70, spawn.spawn3Z), "target of spawn 3");
        } finally {
            spawn.spawn3Y = savedY;
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Druckplatten
    // =====================================================================================

    /** Kupferplatte: loest erst nach 1 s (unoxidiert) Stehen aus; die Stehzeit waechst mit der Oxidation. */
    public static void copperPlatesWaitLongerTheMoreTheyOxidized(GameTestHelper helper) {
        helper.assertValueEqual(List.of(
                CopperPressurePlateBlock.requiredTicks(WeatheringCopper.WeatherState.UNAFFECTED),
                CopperPressurePlateBlock.requiredTicks(WeatheringCopper.WeatherState.EXPOSED),
                CopperPressurePlateBlock.requiredTicks(WeatheringCopper.WeatherState.WEATHERED),
                CopperPressurePlateBlock.requiredTicks(WeatheringCopper.WeatherState.OXIDIZED)), List.of(20, 40, 60, 80), "standing ticks per oxidation stage");
        BlockPos plate = new BlockPos(3, 1, 3);
        helper.setBlock(plate, TweaksBlocks.COPPER_PRESSURE_PLATE);
        mockPlayer(helper, new Vec3(3.5, 1.05, 3.5));
        helper.runAfterDelay(10, () -> helper.assertFalse(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                "the copper plate powered after half a second"));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                "the copper plate never powered"));
    }

    /** Oxidation folgt der Kette, behaelt den Besitzer; die Axt kratzt eine Stufe ab. */
    public static void copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack(GameTestHelper helper) {
        List<Block> stages = CopperPressurePlateBlock.stages();
        for (int i = 0; i < stages.size(); i++) {
            CopperPressurePlateBlock block = (CopperPressurePlateBlock) stages.get(i);
            Optional<BlockState> next = block.getNext(block.defaultBlockState());
            if (i + 1 < stages.size()) {
                helper.assertTrue(next.isPresent() && next.get().is(stages.get(i + 1)), stages.get(i) + " does not oxidize into " + stages.get(i + 1));
            } else {
                helper.assertTrue(next.isEmpty(), "the oxidized copper plate still oxidizes further");
            }
        }
        BlockPos plate = new BlockPos(3, 1, 3);
        helper.setBlock(plate, TweaksBlocks.WEATHERED_COPPER_PRESSURE_PLATE);
        UUID owner = UUID.randomUUID();
        helper.getBlockEntity(plate, OwnedBlockEntity.class).setOwner(owner);
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(3.5, 1.0, 1.5));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        BlockPos abs = helper.absolutePos(plate);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        InteractionResult result = helper.getBlockState(plate).useItemOn(axe, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result.consumesAction(), "the axe did nothing on a weathered copper plate: " + result);
        helper.assertTrue(helper.getBlockState(plate).is(TweaksBlocks.EXPOSED_COPPER_PRESSURE_PLATE), "the axe scraped the plate to " + helper.getBlockState(plate));
        helper.assertValueEqual(helper.getBlockEntity(plate, OwnedBlockEntity.class).getOwner(), owner, "owner after scraping");
        helper.assertValueEqual(axe.getDamageValue(), 1, "axe damage after scraping");
        TestCleanup.succeed(helper);
    }

    /** Diamant-Druckplatte: nur Spieler, keine Mobs oder Gegenstaende. */
    public static void diamondPressurePlatesReactToPlayersOnly(GameTestHelper helper) {
        BlockPos plate = new BlockPos(2, 1, 2);
        helper.setBlock(plate, TweaksBlocks.DIAMOND_PRESSURE_PLATE);
        helper.spawn(EntityType.ZOMBIE, plate);
        helper.spawnItem(Items.DIAMOND, new Vec3(2.5, 1.1, 2.5));
        BlockPos other = new BlockPos(5, 1, 5);
        helper.setBlock(other, TweaksBlocks.DIAMOND_PRESSURE_PLATE);
        mockPlayer(helper, new Vec3(5.5, 1.05, 5.5));
        net.minecraft.world.level.block.state.properties.BooleanProperty powered = net.minecraft.world.level.block.PressurePlateBlock.POWERED;
        // Mock-Spieler bewegen sich nicht selbst (die Bewegung kommt sonst vom Client), loesen also
        // Vanillas entityInside nicht aus - die Signalfrage wird deshalb direkt gestellt. Zombie und
        // Gegenstand bewegen sich echt und liegen die ganze Zeit auf der ersten Platte.
        com.simplebuilding.tweaks.block.DiamondPressurePlateBlock block =
                (com.simplebuilding.tweaks.block.DiamondPressurePlateBlock) TweaksBlocks.DIAMOND_PRESSURE_PLATE;
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(helper.getBlockState(plate).getValue(powered), "the diamond plate reacted to a zombie or an item");
            helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(plate)), 0, "signal of the plate under a zombie and an item");
            helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(other)), 15, "signal of the plate under a player");
            TestCleanup.succeed(helper);
        });
    }

    /** Netherit-Druckplatte: ohne Fass jeder Spieler, mit Fass nur wer einen Gegenstand daraus traegt. */
    public static void netheritePlatesAdmitOnlyHoldersOfBarrelItems(GameTestHelper helper) {
        FilterPressurePlateBlock plate = (FilterPressurePlateBlock) TweaksBlocks.NETHERITE_PRESSURE_PLATE;
        Player holder = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        holder.getInventory().setItem(3, new ItemStack(Items.EMERALD));
        helper.assertTrue(plate.admits(stranger, null, null), "without a barrel the netherite plate refuses a player");
        BarrelBlockEntity barrel = barrel(helper, new BlockPos(1, 1, 1), new ItemStack(Items.EMERALD));
        helper.assertTrue(plate.admits(holder, barrel, null), "a player carrying the whitelisted emerald is refused");
        helper.assertFalse(plate.admits(stranger, barrel, null), "a player without the whitelisted item is admitted");
        TestCleanup.succeed(helper);
    }

    /**
     * Enderit-Druckplatte: der Besitzer immer; ohne Fass sonst niemand; mit Fass jeder, dessen Name
     * auf einem Namensschild im Fass steht (oder der einen Whitelist-Gegenstand traegt).
     */
    public static void enderitePlatesLockToTheirOwnerAndNamedTags(GameTestHelper helper) {
        FilterPressurePlateBlock plate = (FilterPressurePlateBlock) TweaksBlocks.ENDERITE_PRESSURE_PLATE;
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, TweaksBlocks.ENDERITE_PRESSURE_PLATE);
        FilterPlateBlockEntity be = helper.getBlockEntity(pos, FilterPlateBlockEntity.class);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player guest = helper.makeMockPlayer(GameType.SURVIVAL);
        be.setOwner(owner.getUUID());
        helper.assertTrue(plate.admits(owner, null, be), "the enderite plate refuses its owner");
        helper.assertFalse(plate.admits(guest, null, be), "without a barrel the enderite plate admits a stranger");
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal(guest.getName().getString()));
        BarrelBlockEntity barrel = barrel(helper, new BlockPos(1, 1, 1), tag);
        helper.assertTrue(plate.admits(guest, barrel, be), "a player named on a name tag in the barrel is refused");
        guest.getInventory().setItem(0, new ItemStack(Items.NAME_TAG));
        ItemStack otherTag = new ItemStack(Items.NAME_TAG);
        otherTag.set(DataComponents.CUSTOM_NAME, Component.literal("somebody_else"));
        BarrelBlockEntity otherBarrel = barrel(helper, new BlockPos(1, 1, 5), otherTag);
        helper.assertFalse(plate.admits(guest, otherBarrel, be), "carrying any name tag passes a named-tag lock");
        TestCleanup.succeed(helper);
    }

    /** Besitzer bauen Platten schnell ab, Fremde sehr langsam; Kreativ bleibt Vanilla. */
    public static void ownersBreakTheirPadsFastAndStrangersSlowly(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        BlockPos plate = new BlockPos(5, 1, 5);
        helper.setBlock(pad, TweaksBlocks.ELYTRA_PAD);
        helper.setBlock(plate, TweaksBlocks.COPPER_PRESSURE_PLATE);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.getBlockEntity(pad, OwnedBlockEntity.class).setOwner(owner.getUUID());
        helper.getBlockEntity(plate, OwnedBlockEntity.class).setOwner(owner.getUUID());
        ServerLevel level = helper.getLevel();
        helper.assertValueEqual(helper.getBlockState(pad).getDestroyProgress(owner, level, helper.absolutePos(pad)), PadOwnership.OWNER_PAD, "owner progress on a pad");
        helper.assertValueEqual(helper.getBlockState(pad).getDestroyProgress(stranger, level, helper.absolutePos(pad)), PadOwnership.STRANGER_PAD, "stranger progress on a pad");
        helper.assertValueEqual(helper.getBlockState(plate).getDestroyProgress(owner, level, helper.absolutePos(plate)), PadOwnership.OWNER_PLATE, "owner progress on a copper plate");
        helper.assertValueEqual(helper.getBlockState(plate).getDestroyProgress(stranger, level, helper.absolutePos(plate)), PadOwnership.STRANGER_PLATE, "stranger progress on a copper plate");
        helper.assertTrue(PadOwnership.OWNER_PAD > PadOwnership.STRANGER_PAD * 10, "owners are not much faster than strangers");
        TestCleanup.succeed(helper);
    }

    /** Wer setzt, ist Besitzer (Setzen ueber das BlockItem, wie ein Spieler). */
    public static void placingPadsMakesThePlacerTheOwner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        helper.setBlock(pos, TweaksBlocks.SPAWN_TELEPORTER);
        helper.getBlockState(pos).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), player,
                new ItemStack(TweaksBlocks.SPAWN_TELEPORTER));
        helper.assertValueEqual(helper.getBlockEntity(pos, OwnedBlockEntity.class).getOwner(), player.getUUID(), "owner of a freshly placed teleporter");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Chunk-Loader, Launchpad
    // =====================================================================================

    /** Chunk-Loader erzwingt seinen Chunk, der Enderit-Loader 3x3; beim Abbau werden sie frei. */
    public static void chunkLoadersForceTheirChunksAndReleaseThemWhenBroken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, TweaksBlocks.ENDERITE_CHUNK_LOADER);
        BlockPos abs = helper.absolutePos(pos);
        int cx = abs.getX() >> 4;
        int cz = abs.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.asLong(cx + dx, cz + dz)),
                        "the enderite chunk loader does not force chunk " + (cx + dx) + "," + (cz + dz));
            }
        }
        helper.setBlock(pos, Blocks.AIR);
        helper.assertFalse(level.getForceLoadedChunks().contains(ChunkPos.asLong(cx + 1, cz + 1)), "a broken enderite chunk loader keeps its chunks forced");
        helper.assertFalse(level.getForceLoadedChunks().contains(ChunkPos.asLong(cx, cz)), "a broken chunk loader keeps its chunk forced");
        TestCleanup.succeed(helper);
    }

    /** Launchpads fassen 16 Windkugeln, das Enderit-Launchpad 32; mehr nimmt keiner. */
    public static void launchpadsHoldSixteenWindChargesAndTheEnderiteOneThirtyTwo(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        int[] limits = {16, 32};
        Block[] pads = {TweaksBlocks.LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD};
        for (int i = 0; i < 2; i++) {
            BlockPos pos = new BlockPos(3 + i * 2, 1, 3);
            helper.setBlock(pos, pads[i]);
            BlockPos abs = helper.absolutePos(pos);
            ItemStack charges = new ItemStack(Items.WIND_CHARGE, 64);
            player.setItemInHand(InteractionHand.MAIN_HAND, charges);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
            for (int n = 0; n < 40; n++) {
                helper.getBlockState(pos).useItemOn(charges, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            }
            helper.assertValueEqual(helper.getBlockEntity(pos, LaunchpadBlockEntity.class).getCharges(), limits[i], "charges stored in " + pads[i]);
            helper.assertValueEqual(charges.getCount(), 64 - limits[i], "wind charges spent on " + pads[i]);
        }
        helper.assertTrue(LaunchpadBlockEntity.strengthFor(32) > LaunchpadBlockEntity.strengthFor(16), "more charges do not launch higher");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Echo-Kompass
    // =====================================================================================

    /**
     * Echo-Kompass: Rechtsklick auf einen Leitstein verknuepft; Benutzen teleportiert ueber den
     * Leitstein, verbraucht eine Enderperle, der Kompass bleibt und nimmt 1 Haltbarkeit.
     */
    public static void theEchoCompassLinksToTheLodestoneAndTeleportsForOnePearl(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        BlockPos abs = helper.absolutePos(lodestone);
        compass.getItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false)));
        helper.assertValueEqual(EchoCompassItem.target(compass), GlobalPos.of(helper.getLevel().dimension(), abs), "linked target of the echo compass");

        helper.assertFalse(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass teleported without an ender pearl");
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        helper.assertTrue(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass refused to teleport with a pearl");
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "the player landed at " + player.position() + " instead of on the lodestone");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 1, "ender pearls left after one jump");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == compass, "the echo compass was used up");
        helper.assertValueEqual(compass.getDamageValue(), 1, "echo compass damage after one jump");
        helper.assertFalse(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass ignored its cooldown");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 1, "pearls spent by a jump refused for cooldown");
        TestCleanup.succeed(helper);
    }

    /**
     * Unbreaking wirkt auf den Echo-Kompass (Simple-Tweaks-Bug: das Datenpaket zog die Haltbarkeit
     * direkt ab). Mit Unbreaking 255 bleibt der Schaden praktisch aus; der Kompass liegt im Tag
     * enchantable/durability, also kann man ihn auch verzaubern.
     */
    public static void unbreakingProtectsTheEchoCompass(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 255);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 16));
        int jumps = 0;
        for (int i = 0; i < 8; i++) {
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(compass));
            if (EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass)) {
                jumps++;
            }
        }
        helper.assertValueEqual(jumps, 8, "jumps made");
        helper.assertTrue(compass.getDamageValue() <= 1, "unbreaking 255 still let the echo compass take " + compass.getDamageValue() + " damage in 8 jumps");
        helper.assertTrue(new ItemStack(TweaksItems.ECHO_COMPASS).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE),
                "the echo compass is missing from enchantable/durability, so unbreaking cannot be put on it");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Spawn, Dimensionen, XP, Stapel, Befehle, Config
    // =====================================================================================

    /** Erstes Betreten: Teleporter und Elytra-Pad in der Config-Menge, genau einmal (Tag wie Simple Tweaks). */
    public static void theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getInventory().clearContent();
        player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
        int amount = Math.max(0, Math.min(64, SimpleTweaks.config().spawn.spawnTeleporterCount));
        SpawnSetup.onPlayerJoin(player);
        helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), amount, "spawn teleporters given on the first join");
        helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.ELYTRA_PAD.asItem()), amount, "elytra pads given on the first join");
        helper.assertTrue(player.getTags().contains("simpletweaks.first_join"), "the first-join tag is not the one Simple Tweaks used");
        SpawnSetup.onPlayerJoin(player);
        helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), amount, "spawn teleporters after a second join");
        TestCleanup.succeed(helper);
    }

    /** Nether und End lassen sich per Config sperren; offen ist die Voreinstellung. */
    public static void theNetherAndTheEndCanBeLockedByConfig(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(nether != null && end != null, "the test server has no nether or end");
        TweaksConfig.Dimensions dims = SimpleTweaks.config().dimensions;
        helper.assertTrue(new TweaksConfig().dimensions.allowNether && new TweaksConfig().dimensions.allowEnd, "the dimensions are locked by default");
        boolean nOn = dims.allowNether;
        boolean eOn = dims.allowEnd;
        try {
            dims.allowNether = true;
            dims.allowEnd = true;
            helper.assertFalse(SpawnRules.blocksDimensionChange(player, nether), "an open nether is blocked");
            dims.allowNether = false;
            helper.assertTrue(SpawnRules.blocksDimensionChange(player, nether), "a locked nether lets the player in");
            helper.assertFalse(SpawnRules.blocksDimensionChange(player, end), "locking the nether also locks the end");
            dims.allowEnd = false;
            helper.assertTrue(SpawnRules.blocksDimensionChange(player, end), "a locked end lets the player in");
            helper.assertFalse(SpawnRules.blocksDimensionChange(player, helper.getLevel()), "staying in the overworld counts as a dimension change");
        } finally {
            dims.allowNether = nOn;
            dims.allowEnd = eOn;
        }
        TestCleanup.succeed(helper);
    }

    /** XP-Kugeln verklumpen ohne XP zu verlieren - auch wenn Vanilla schon gleichwertige Kugeln zusammengelegt hat. */
    public static void xpOrbsClumpWithoutLosingExperience(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 spot = helper.absoluteVec(new Vec3(3.5, 1.5, 3.5));
        ExperienceOrb a = new ExperienceOrb(level, spot.x, spot.y, spot.z, 7);
        ExperienceOrb b = new ExperienceOrb(level, spot.x + 0.5, spot.y, spot.z, 5);
        ExperienceOrb c = new ExperienceOrb(level, spot.x, spot.y, spot.z + 0.5, 3);
        ((com.simplebuilding.tweaks.mixin.ExperienceOrbAccessor) b).simplebuilding$setCount(3);
        level.addFreshEntity(a);
        level.addFreshEntity(b);
        level.addFreshEntity(c);
        int swallowed = XpClumping.clump(a);
        helper.assertValueEqual(swallowed, 2, "orbs swallowed");
        helper.assertValueEqual(a.getValue(), 7 + 5 * 3 + 3, "value of the clumped orb");
        helper.assertValueEqual(((com.simplebuilding.tweaks.mixin.ExperienceOrbAccessor) a).simplebuilding$getCount(), 1, "count of the clumped orb");
        helper.assertFalse(b.isAlive() || c.isAlive(), "swallowed orbs are still in the world");
        a.discard();
        TestCleanup.succeed(helper);
    }

    /** Raketen-Stapelgroesse aus der Config (nur kleiner als Vanilla); andere Items unberuehrt. */
    public static void theRocketStackSizeFollowsTheConfig(GameTestHelper helper) {
        int saved = SimpleTweaks.config().balancing.rocketStackSize;
        try {
            SimpleTweaks.config().balancing.rocketStackSize = 16;
            helper.assertValueEqual(new ItemStack(Items.FIREWORK_ROCKET).getMaxStackSize(), 16, "rocket stack size with the limit at 16");
            helper.assertValueEqual(new ItemStack(Items.ARROW).getMaxStackSize(), 64, "arrow stack size with the rocket limit at 16");
            SimpleTweaks.config().balancing.rocketStackSize = 64;
            helper.assertValueEqual(new ItemStack(Items.FIREWORK_ROCKET).getMaxStackSize(), 64, "rocket stack size with the limit at 64");
        } finally {
            SimpleTweaks.config().balancing.rocketStackSize = saved;
        }
        TestCleanup.succeed(helper);
    }

    /** /killboats: standard nur Boote ohne Kiste, empty dazu leere Kistenboote, all alle unbesetzten. */
    public static void killBoatsRemovesBoatsByMode(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds();
        helper.spawn(EntityType.OAK_BOAT, new BlockPos(1, 2, 1));
        helper.spawn(EntityType.OAK_CHEST_BOAT, new BlockPos(4, 2, 1));
        var full = helper.spawn(EntityType.OAK_CHEST_BOAT, new BlockPos(1, 2, 5));
        full.setItem(0, new ItemStack(Items.DIRT));
        helper.assertValueEqual(TweaksCommands.killBoats(level, box, "standard"), 1, "boats removed in standard mode");
        helper.assertValueEqual(TweaksCommands.killBoats(level, box, "empty"), 1, "boats removed in empty mode");
        helper.assertValueEqual(TweaksCommands.killBoats(level, box, "all"), 1, "boats removed in all mode");
        helper.assertTrue(level.getEntitiesOfClass(AbstractBoat.class, box).isEmpty(), "boats left after /killboats all");
        TestCleanup.succeed(helper);
    }

    /** Die Tweaks-Config behaelt Namen und Voreinstellungen (sie werden so gespeichert). */
    public static void tweaksConfigKeepsItsNamesAndDefaults(GameTestHelper helper) {
        Set<String> found = new TreeSet<>();
        TweaksConfig defaults = new TweaksConfig();
        for (Field group : TweaksConfig.class.getFields()) {
            try {
                Object value = group.get(defaults);
                for (Field field : group.getType().getFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        found.add(group.getName() + "." + field.getName() + "=" + field.get(value));
                    }
                }
            } catch (IllegalAccessException e) {
                helper.fail("cannot read " + group.getName() + ": " + e);
            }
        }
        Set<String> expected = new TreeSet<>(List.of(
                "pads.enableChunkLoaders=true", "pads.enableElytraPads=true", "pads.enableFlypads=true",
                "pads.enableSpawnTeleporters=true", "pads.enableLaunchpads=true", "pads.enableTimedCopperPlates=true",
                "pads.enableFilterPlates=true", "balancing.rocketStackSize=64", "dimensions.allowNether=true",
                "dimensions.allowEnd=true", "spawn.forceExactSpawn=false", "spawn.disableFallDamageInSpawn=true",
                "spawn.useCustomWorldSpawn=false", "spawn.xCoordSpawnPoint=0", "spawn.yCoordSpawnPoint=-1",
                "spawn.zCoordSpawnPoint=0", "spawn.spawnTeleporterCount=1", "spawn.giveElytraOnSpawn=false",
                "spawn.spawnElytraRadius=25", "spawn.useWorldSpawnAsCenter=false", "spawn.customSpawnElytraX=0",
                "spawn.customSpawnElytraZ=0", "spawn.flightTimeSeconds=300", "spawn.maxBoosts=3", "spawn.boostStrength=0.6",
                "spawn.spawn1X=0", "spawn.spawn1Y=-1000", "spawn.spawn1Z=0", "spawn.spawn2X=0", "spawn.spawn2Y=-1000",
                "spawn.spawn2Z=0", "spawn.spawn3X=0", "spawn.spawn3Y=-1000", "spawn.spawn3Z=0", "spawn.spawn4X=0",
                "spawn.spawn4Y=-1000", "spawn.spawn4Z=0", "commands.enableKillBoatsCommand=true",
                "commands.enableKillCartsCommand=false", "optimization.enableXpClumps=true", "optimization.scaleXpOrbs=true",
                "laserPointer.enable=true", "laserPointer.color=16711680", "laserPointer.scale=0.25", "laserPointer.range=512",
                "laserPointer.showLine=false"));
        helper.assertValueEqual(found, expected, "tweaks config options with their defaults");
        helper.assertTrue(Simplebuilding.getConfig().tweaks == SimpleTweaks.config(), "SimpleTweaks.config() is not the live tweaks section");
        TestCleanup.succeed(helper);
    }

    /** Die Config-Befehle unter /simplebuilding tweaks schreiben die Config (hier: Flypads aus und wieder an). */
    public static void theTweaksCommandsWriteTheConfig(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        boolean flypads = SimpleTweaks.config().pads.enableFlypads;
        int radius = SimpleTweaks.config().spawn.spawnElytraRadius;
        try {
            players.op(player.nameAndId());
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            var source = player.createCommandSourceStack().withSuppressedOutput();
            dispatcher.execute("simplebuilding tweaks pads flypads false", source);
            helper.assertFalse(SimpleTweaks.config().pads.enableFlypads, "the flypad switch command did not switch flypads off");
            dispatcher.execute("simplebuilding tweaks spawn elytra radius 40", source);
            helper.assertValueEqual(SimpleTweaks.config().spawn.spawnElytraRadius, 40, "spawn elytra radius after the command");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("tweaks command failed: " + e.getMessage());
        } finally {
            SimpleTweaks.config().pads.enableFlypads = flypads;
            SimpleTweaks.config().spawn.spawnElytraRadius = radius;
            // Die Befehle speichern die Config auf Platte; den alten Stand ebenfalls speichern,
            // sonst startet der naechste Lauf mit ausgeschalteten Flypads.
            SimpleTweaks.saveConfig();
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        TestCleanup.before(helper, () -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    /**
     * Ein Spieler, der wie im Ueberlebensmodus baut (instabuild aus). Der In-Level-Mock bleibt fuer
     * isCreative() kreativ, darum fragt der Flypad-Code instabuild; eine Verbindung hat er, anders als
     * ein losgeloester Mock, sodass onUpdateAbilities nicht ins Leere sendet.
     */
    private static ServerPlayer survivalLikePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = mockPlayer(helper, relative);
        player.getAbilities().instabuild = false;
        return player;
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos, ItemStack content) {
        helper.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity barrel = helper.getBlockEntity(pos, BarrelBlockEntity.class);
        barrel.setItem(0, content);
        return barrel;
    }

    private static SmithingRecipeInput smithingInput(Item template, net.minecraft.world.level.ItemLike base, Item addition) {
        return new SmithingRecipeInput(new ItemStack(template), new ItemStack(base), new ItemStack(addition));
    }

    private static Optional<RecipeHolder<SmithingRecipe>> smithing(GameTestHelper helper, Item template, net.minecraft.world.level.ItemLike base, Item addition) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, smithingInput(template, base, addition), helper.getLevel());
    }

    private static void expectSmithing(GameTestHelper helper, Item template, net.minecraft.world.level.ItemLike base, Item addition, net.minecraft.world.level.ItemLike result) {
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, template, base, addition);
        helper.assertTrue(match.isPresent(), "no smithing recipe turns " + base + " with " + addition + " into " + result);
        ItemStack out = match.get().value().assemble(smithingInput(template, base, addition), helper.getLevel().registryAccess());
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }

    private static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) {
            stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static Optional<ItemStack> craftingResult(GameTestHelper helper, CraftingInput grid) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(holder -> holder.value().assemble(grid, helper.getLevel().registryAccess()));
    }

    private static void expectCrafting(GameTestHelper helper, CraftingInput grid, Item result, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the documented pattern for " + result + " matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched for " + result);
        helper.assertTrue(match.get().value().assemble(grid, helper.getLevel().registryAccess()).is(result), "the recipe for " + result + " made something else");
    }
}
