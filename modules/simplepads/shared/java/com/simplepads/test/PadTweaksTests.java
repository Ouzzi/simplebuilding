package com.simplepads.test;

import com.mojang.authlib.GameProfile;
import com.simplepads.SimplePads;
import com.simplepads.PadsConfig;
import com.simplepads.block.CopperPressurePlateBlock;
import com.simplepads.block.FilterPressurePlateBlock;
import com.simplepads.block.PadOwnership;
import com.simplepads.block.PadTiers;
import com.simplepads.block.PadsBlocks;
import com.simplepads.block.entity.ChunkLoaderBlockEntity;
import com.simplepads.block.entity.ElytraPadBlockEntity;
import com.simplepads.block.entity.FilterPlateBlockEntity;
import com.simplepads.block.entity.FlypadBlockEntity;
import com.simplepads.block.entity.LaunchpadBlockEntity;
import com.simplepads.block.entity.OwnedBlockEntity;
import com.simplepads.block.entity.SpawnTeleporterBlockEntity;
import com.simplepads.component.PadsComponents;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import com.simplepads.item.PadsItems;
import com.simplepads.network.ElytraBoostPayload;
import com.simplepads.network.PadsNetwork;
import com.simplepads.spawn.ElytraDamageRules;
import com.simplepads.spawn.LaunchSafety;
import com.simplepads.spawn.SpawnElytra;
import com.simplepads.spawn.SpawnSetup;
import com.simplepads.PadsContent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.storage.LevelData;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import com.google.gson.JsonObject;
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
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Spieltests des aus Simple Tweaks uebernommenen Teils (docs/SIMPLETWEAKS-UEBERNAHME.md).
 *
 * <p>Die meisten Tests rufen die Logik der Block-Entities direkt und in einem Tick auf: die Pads
 * ticken nur jede halbe Sekunde, und Config-Schalter werden in demselben Aufruf gesetzt und wieder
 * zurueckgesetzt, damit parallel laufende Tests sie nie zu sehen bekommen.
 */
public final class PadTweaksTests {

    private PadTweaksTests() {
    }

    /** Ticks fuer die Tests, die echte Ticks abwarten (Kupferplatte, Teleporter). */
    public static final int COPPER_MAX_TICKS = 100;
    public static final int TELEPORTER_MAX_TICKS = 160;

    // =====================================================================================
    // Stufen und Rezepte
    // =====================================================================================

    /**
     * Die Pad-Bereiche wachsen mit jeder Stufe; seit 2026-10-07 hat das Elytra-Pad drei Stufen: 5x5x5,
     * 32x32x32, 128x128x192 (nur die Endstufe ist Enderit und traegt den Boost-Vorteil).
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
        helper.assertValueEqual(List.of(PadTiers.width(1), PadTiers.width(2), PadTiers.width(3)),
                List.of(5, 32, 128), "elytra pad widths per tier");
        helper.assertValueEqual(List.of(PadTiers.height(1), PadTiers.height(2), PadTiers.height(3)),
                List.of(5, 32, 192), "pad heights per tier");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(PadsBlocks.ENDERITE_ELYTRA_PAD.defaultBlockState()), 3, "tier of the enderite elytra pad");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(PadsBlocks.FINE_ELYTRA_PAD.defaultBlockState()), 5, "tier of the fine elytra pad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(PadsBlocks.REINFORCED_FLYPAD.defaultBlockState()), 2, "tier of the reinforced flypad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(PadsBlocks.STELLAR_FLYPAD.defaultBlockState()), 3, "tier of the stellar flypad");
        helper.assertFalse(PadTiers.hasEnderiteBonus(2), "the netherite tier already carries the enderite bonus");
        helper.assertTrue(PadTiers.hasEnderiteBonus(3), "the enderite tier does not carry the bonus");
        helper.succeed();
    }


    /**
     * Stellares Flypad III (Besitzer 2026-09-27): zwei Verstaerkte Flypads II im Schmiedetisch
     * (Enderit-Vorlage, das zweite als Zutat); das alte Werkbank-Rezept KKK / ESE / FFF gibt es nicht mehr.
     */
    public static void theStellarFlypadIsSmithedFromTwoReinforcedFlypads(GameTestHelper helper) {
        expectSmithing(helper, com.simplepads.util.PadRefs.enderiteTemplate(), PadsBlocks.REINFORCED_FLYPAD, PadsBlocks.REINFORCED_FLYPAD.asItem(), PadsBlocks.STELLAR_FLYPAD);
        Optional<RecipeHolder<SmithingRecipe>> one = smithing(helper, com.simplepads.util.PadRefs.enderiteTemplate(), PadsBlocks.FLYPAD, PadsBlocks.REINFORCED_FLYPAD.asItem());
        helper.assertTrue(one.isEmpty(), "a flypad I and a flypad II already smith something");
        Item f = PadsBlocks.ENDERITE_FLYPAD.asItem();
        CraftingInput oldGrid = grid(Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY,
                Items.ENCHANTED_GOLDEN_APPLE, Items.NETHER_STAR, Items.ENCHANTED_GOLDEN_APPLE, f, f, f);
        helper.assertTrue(craftingResult(helper, oldGrid).isEmpty(), "the old crafting recipe still makes a stellar flypad");
        helper.succeed();
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
        helper.setBlock(pad, PadsBlocks.ELYTRA_PAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(chest.is(PadsItems.SPAWN_ELYTRA), "the elytra pad put " + chest + " into the chest slot instead of a spawn elytra");
        helper.assertValueEqual(chest.get(PadsComponents.IS_SAFE_ELYTRA), false, "safe flag of a pad elytra");
        helper.assertValueEqual(chest.get(PadsComponents.FLIGHT_TIME), SimplePads.config().spawn.flightTimeSeconds * 20, "flight time of a fresh pad elytra");
        helper.assertValueEqual(chest.get(PadsComponents.BOOST_LEVEL), 1.0f, "boost level of a fresh pad elytra");
        helper.assertFalse(ElytraDamageRules.wearsSafeElytra(player), "a pad elytra counts as a safe spawn elytra");

        // Wer eine Brustplatte traegt, behaelt sie.
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "the elytra pad replaced a worn chestplate");
        helper.succeed();
    }

    /**
     * Boosts laden bis Netherit nur in der 3x3-Saeule ueber dem Pad; ab der Enderit-Stufe im ganzen
     * Bereich (Zusatzfunktion der Stufe IV, die V behaelt).
     */
    public static void elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea(GameTestHelper helper) {
        BlockPos pad = new BlockPos(1, 1, 1);
        ServerPlayer player = mockPlayer(helper, new Vec3(6.5, 2.0, 6.5));
        PadsConfig.Spawn config = SimplePads.config().spawn;
        helper.assertFalse(ElytraPadBlockEntity.isInBoostColumn(player, helper.absolutePos(pad)), "the probe player stands in the boost column");
        for (int tier = 3; tier <= 5; tier++) {
            ItemStack elytra = new ItemStack(PadsItems.SPAWN_ELYTRA);
            elytra.set(PadsComponents.BOOST_LEVEL, 0.0f);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            ElytraPadBlockEntity.applyTo(helper.getLevel(), helper.absolutePos(pad), tier, player, config);
            float boost = player.getItemBySlot(EquipmentSlot.CHEST).getOrDefault(PadsComponents.BOOST_LEVEL, 0.0f);
            if (tier < PadTiers.ENDERITE) {
                helper.assertValueEqual(boost, 0.0f, "boost level recharged outside the column by a tier " + tier + " pad");
            } else {
                helper.assertValueEqual(boost, 1.0f, "boost level recharged outside the column by a tier " + tier + " pad");
            }
        }
        helper.succeed();
    }

    /** Boost: kostet 1/maxBoosts, braucht Gleitflug und Spawn-Elytra, schiebt in Blickrichtung. */
    public static void theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        ItemStack elytra = new ItemStack(PadsItems.SPAWN_ELYTRA);
        elytra.set(PadsComponents.BOOST_LEVEL, 1.0f);
        player.setItemSlot(EquipmentSlot.CHEST, elytra);
        int maxBoosts = Math.max(1, SimplePads.config().spawn.maxBoosts);

        PadsNetwork.handleBoost(new ElytraBoostPayload(), player);
        helper.assertValueEqual(elytra.get(PadsComponents.BOOST_LEVEL), 1.0f, "boost level after a boost request on foot");

        player.startFallFlying();
        player.setDeltaMovement(Vec3.ZERO);
        PadsNetwork.handleBoost(new ElytraBoostPayload(), player);
        float expected = 1.0f - 1.0f / maxBoosts;
        helper.assertTrue(Math.abs(elytra.get(PadsComponents.BOOST_LEVEL) - expected) < 0.0001f,
                "boost level after one boost: " + elytra.get(PadsComponents.BOOST_LEVEL) + ", expected " + expected);
        helper.assertTrue(player.getDeltaMovement().length() > 0.1, "the boost did not move the player");
        for (int i = 0; i < maxBoosts + 2; i++) {
            PadsNetwork.handleBoost(new ElytraBoostPayload(), player);
        }
        helper.assertValueEqual(elytra.get(PadsComponents.BOOST_LEVEL), 0.0f, "boost level after draining every charge");
        player.stopFallFlying();
        helper.succeed();
    }

    /**
     * Schadensschutz: sichere Spawn-Elytra verhindert Fall- und Kinetikschaden, eine Pad-Elytra nicht;
     * nach dem Enderit-Launchpad entfaellt genau ein Fallschaden.
     */
    public static void safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        ItemStack safe = new ItemStack(PadsItems.SPAWN_ELYTRA);
        SpawnElytra.rechargeAsSpawnElytra(safe, SimplePads.config().spawn);
        player.setItemSlot(EquipmentSlot.CHEST, safe);
        DamageSource wall = helper.getLevel().damageSources().flyIntoWall();
        DamageSource lava = helper.getLevel().damageSources().lava();
        helper.assertTrue(ElytraDamageRules.preventsFallDamage(player), "a safe spawn elytra lets fall damage through");
        helper.assertTrue(ElytraDamageRules.preventsDamage(player, wall), "a safe spawn elytra lets kinetic damage through");
        helper.assertFalse(ElytraDamageRules.preventsDamage(player, lava), "a safe spawn elytra blocks lava damage too");

        ItemStack unsafe = new ItemStack(PadsItems.SPAWN_ELYTRA);
        unsafe.set(PadsComponents.IS_SAFE_ELYTRA, false);
        player.setItemSlot(EquipmentSlot.CHEST, unsafe);
        boolean spawnOn = SimplePads.config().spawn.giveElytraOnSpawn;
        try {
            SimplePads.config().spawn.giveElytraOnSpawn = false;
            helper.assertFalse(ElytraDamageRules.preventsFallDamage(player), "a pad elytra prevents fall damage");
            helper.assertFalse(ElytraDamageRules.preventsDamage(player, wall), "a pad elytra prevents kinetic damage");

            LaunchpadBlockEntity.launch(player, LaunchpadBlockEntity.strengthFor(4), true);
            helper.assertTrue(LaunchSafety.isProtected(player), "an enderite launch does not protect the player");
            helper.assertTrue(ElytraDamageRules.preventsFallDamage(player), "fall damage after an enderite launch was not prevented");
            helper.assertFalse(ElytraDamageRules.preventsFallDamage(player), "the launch protection outlived its first landing");
            LaunchpadBlockEntity.launch(player, LaunchpadBlockEntity.strengthFor(4), false);
            helper.assertFalse(LaunchSafety.isProtected(player), "a normal launchpad protects against fall damage");
        } finally {
            SimplePads.config().spawn.giveElytraOnSpawn = spawnOn;
        }
        helper.succeed();
    }

    /**
     * Timer und Aufraeumen einer Pad-Elytra laufen auch bei ausgeschalteter Spawn-Elytra (in Simple
     * Tweaks nicht: dort brach der Tick dann ab und Pad-Elytren liefen nie ab).
     */
    public static void padElytrasExpireEvenWithTheSpawnElytraSwitchedOff(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        boolean spawnOn = SimplePads.config().spawn.giveElytraOnSpawn;
        try {
            SimplePads.config().spawn.giveElytraOnSpawn = false;
            ItemStack elytra = new ItemStack(PadsItems.SPAWN_ELYTRA);
            elytra.set(PadsComponents.FLIGHT_TIME, 5);
            player.setItemSlot(EquipmentSlot.CHEST, elytra);
            player.startFallFlying();
            SpawnElytra.tick(player, false);
            helper.assertValueEqual(player.getItemBySlot(EquipmentSlot.CHEST).get(PadsComponents.FLIGHT_TIME), 4, "flight time after one gliding tick");
            player.getItemBySlot(EquipmentSlot.CHEST).set(PadsComponents.FLIGHT_TIME, 1);
            SpawnElytra.tick(player, false);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "a pad elytra with no flight time left was not taken away");
            player.stopFallFlying();

            // Eine Spawn-Elytra im Inventar (statt im Brust-Slot) wird beim vollen Durchlauf entfernt.
            player.getInventory().setItem(5, new ItemStack(PadsItems.SPAWN_ELYTRA));
            SpawnElytra.tick(player, true);
            helper.assertTrue(player.getInventory().getItem(5).isEmpty(), "a spawn elytra in the inventory was not cleaned up");
        } finally {
            SimplePads.config().spawn.giveElytraOnSpawn = spawnOn;
        }
        helper.succeed();
    }

    // =====================================================================================
    // Flypads
    // =====================================================================================

    /** Flypads geben Kreativflug im Bereich; wer nicht mehr im Bereich ist, wird vorgemerkt und verliert ihn. */
    public static void flypadsGrantFlightInsideAndTakeItBackOutside(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, PadsBlocks.FLYPAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().mayfly = false;
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertTrue(player.getAbilities().mayfly, "a player inside the flypad area did not get creative flight (player at "
                + player.position() + ", area " + PadTiers.flyArea(helper.absolutePos(pad), 1) + ", tracked " + be.flyingPlayers() + ", enabled " + SimplePads.config().pads.enableFlypads + ", found "
                + helper.getLevel().getEntitiesOfClass(ServerPlayer.class, PadTiers.flyArea(helper.absolutePos(pad), 1), p -> true).size()
                + ", tier " + FlypadBlockEntity.tierOf(helper.getBlockState(pad)) + ")");
        helper.assertTrue(be.flyingPlayers().contains(player.getUUID()), "the flypad does not remember whom it let fly");
        helper.assertTrue(player.hasEffect(MobEffects.GLOWING), "flypad flyers are not highlighted");

        Vec3 far = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5)).add(0, PadTiers.flyHeight(1) + 5, 0);
        player.snapTo(far.x, far.y, far.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertFalse(be.flyingPlayers().contains(player.getUUID()), "the flypad still tracks a player who left its area");
        helper.succeed();
    }

    /**
     * Beim Verlassen verliert ein Ueberlebensspieler den Flug; seit 2026-09-27 sind alle drei Flypad-
     * Stufen aus Enderit und fangen ihn mit dem Sicherheitsnetz (Sanfter Fall) auf - schon Stufe I.
     */
    public static void enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling(GameTestHelper helper) {
        ServerPlayer first = survivalLikePlayer(helper, new Vec3(1.5, 1.0, 1.5));
        first.getAbilities().mayfly = true;
        first.getAbilities().flying = true;
        FlypadBlockEntity.revoke(first, 1);
        helper.assertFalse(first.getAbilities().mayfly, "a survival player kept flight after leaving a flypad");
        helper.assertTrue(first.hasEffect(MobEffects.SLOW_FALLING), "flypad I does not catch the falling player");

        ServerPlayer enderite = survivalLikePlayer(helper, new Vec3(3.5, 1.0, 1.5));
        enderite.getAbilities().mayfly = true;
        enderite.getAbilities().flying = true;
        FlypadBlockEntity.revoke(enderite, 3);
        helper.assertFalse(enderite.getAbilities().flying, "a survival player keeps flying after leaving a stellar flypad");
        helper.assertTrue(enderite.hasEffect(MobEffects.SLOW_FALLING), "the stellar flypad did not catch the falling player");

        ServerPlayer walker = survivalLikePlayer(helper, new Vec3(5.5, 1.0, 1.5));
        walker.getAbilities().mayfly = true;
        walker.getAbilities().flying = false;
        FlypadBlockEntity.revoke(walker, 3);
        helper.assertFalse(walker.hasEffect(MobEffects.SLOW_FALLING), "the safety net fires for players who were not flying");
        helper.succeed();
    }

    /** Ausgeschaltete Flypads geben keinen Flug (Config pads.enableFlypads). */
    public static void switchedOffFlypadsGrantNoFlight(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, PadsBlocks.FLYPAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().mayfly = false;
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        boolean enabled = SimplePads.config().pads.enableFlypads;
        try {
            SimplePads.config().pads.enableFlypads = false;
            FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        } finally {
            SimplePads.config().pads.enableFlypads = enabled;
        }
        helper.assertFalse(player.getAbilities().mayfly, "a switched off flypad still granted flight");
        helper.succeed();
    }

    // =====================================================================================
    // Spawn-Teleporter
    // =====================================================================================

    /**
     * Wer still auf einem Spawn-Teleporter steht, landet zwei Bloecke ueber dem Spawn-Ziel; ein Schritt
     * dazwischen setzt die Zeit zurueck. Drei Stufen seit 2026-09-28, nur die Wartezeit unterscheidet sie
     * (50/20/5 s); gefahren wird Stufe III (5 s), der Mock-Spieler hat keinen Wiedereinstiegspunkt, also
     * gilt auch fuer sie das Spawn-Ziel.
     */
    public static void spawnTeleportersSendStillPlayersToTheirSpawnPoint(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, PadsBlocks.ENDERITE_SPAWN_TELEPORTER);
        BlockPos target = helper.absolutePos(new BlockPos(6, 1, 6));
        PadsConfig.Spawn spawn = SimplePads.config().spawn;
        int[] saved = {spawn.spawn1X, spawn.spawn1Y, spawn.spawn1Z};
        com.simplepads.command.PadsCommands.setTeleporterSpawn(SimplePads.localConfig(), target);
        helper.runBeforeTestEnd(() -> {
            spawn.spawn1X = saved[0];
            spawn.spawn1Y = saved[1];
            spawn.spawn1Z = saved[2];
        });
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 1.1, 2.5));
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 start = player.position();
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(1), 1000, "standing time of the spawn teleporter I");
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(2), 400, "standing time of the spawn teleporter II");
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(3), 100, "standing time of the spawn teleporter III");
        helper.startSequence()
                .thenExecuteAfter(SpawnTeleporterBlockEntity.ENDERITE_TICKS - 20, () ->
                        helper.assertTrue(player.position().distanceTo(start) < 0.5, "the player was teleported before the countdown ended"))
                .thenWaitUntil(() -> {
                    Vec3 expected = Vec3.atBottomCenterOf(target).add(0, 2.0, 0);
                    helper.assertTrue(player.position().distanceTo(expected) < 1.5,
                            "the player is at " + player.position() + ", not two blocks above the spawn target at " + expected);
                })
                .thenSucceed();
    }

    /** Ohne gesetztes Ziel geht es zum Weltspawn; ein per Befehl gesetztes Ziel gilt fuer alle Stufen. */
    public static void spawnTeleporterTargetsFallBackToTheWorldSpawn(GameTestHelper helper) {
        PadsConfig.Spawn spawn = SimplePads.config().spawn;
        int savedY = spawn.spawn1Y;
        try {
            spawn.spawn1Y = -1000;
            helper.assertTrue(SpawnTeleporterBlockEntity.customTarget() == null, "an unset spawn target still has a target");
            spawn.spawn1Y = 70;
            helper.assertValueEqual(SpawnTeleporterBlockEntity.customTarget(), new BlockPos(spawn.spawn1X, 70, spawn.spawn1Z), "the spawn target");
        } finally {
            spawn.spawn1Y = savedY;
        }
        helper.succeed();
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
        helper.setBlock(plate, PadsBlocks.COPPER_PRESSURE_PLATE);
        mockPlayer(helper, new Vec3(3.5, 1.05, 3.5));
        helper.startSequence()
                .thenExecuteAfter(10, () -> helper.assertFalse(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate powered after half a second"))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate never powered"))
                .thenSucceed();
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
        helper.setBlock(plate, PadsBlocks.WEATHERED_COPPER_PRESSURE_PLATE);
        UUID owner = UUID.randomUUID();
        helper.getBlockEntity(plate, OwnedBlockEntity.class).setOwner(owner);
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(3.5, 1.0, 1.5));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        BlockPos abs = helper.absolutePos(plate);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false);
        InteractionResult result = helper.getBlockState(plate).useItemOn(axe, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result.consumesAction(), "the axe did nothing on a weathered copper plate: " + result);
        helper.assertTrue(helper.getBlockState(plate).is(PadsBlocks.EXPOSED_COPPER_PRESSURE_PLATE), "the axe scraped the plate to " + helper.getBlockState(plate));
        helper.assertValueEqual(helper.getBlockEntity(plate, OwnedBlockEntity.class).getOwner(), owner, "owner after scraping");
        helper.assertValueEqual(axe.getDamageValue(), 1, "axe damage after scraping");
        helper.succeed();
    }

    /** Diamant-Druckplatte: nur Spieler, keine Mobs oder Gegenstaende. */
    public static void diamondPressurePlatesReactToPlayersOnly(GameTestHelper helper) {
        BlockPos plate = new BlockPos(2, 1, 2);
        helper.setBlock(plate, PadsBlocks.DIAMOND_PRESSURE_PLATE);
        helper.spawn(EntityTypes.ZOMBIE, plate);
        helper.spawnItem(Items.DIAMOND, new Vec3(2.5, 1.1, 2.5));
        BlockPos other = new BlockPos(5, 1, 5);
        helper.setBlock(other, PadsBlocks.DIAMOND_PRESSURE_PLATE);
        mockPlayer(helper, new Vec3(5.5, 1.05, 5.5));
        net.minecraft.world.level.block.state.properties.BooleanProperty powered = net.minecraft.world.level.block.PressurePlateBlock.POWERED;
        // Mock-Spieler bewegen sich nicht selbst (die Bewegung kommt sonst vom Client), loesen also
        // Vanillas entityInside nicht aus - die Signalfrage wird deshalb direkt gestellt. Zombie und
        // Gegenstand bewegen sich echt und liegen die ganze Zeit auf der ersten Platte.
        com.simplepads.block.DiamondPressurePlateBlock block =
                (com.simplepads.block.DiamondPressurePlateBlock) PadsBlocks.DIAMOND_PRESSURE_PLATE;
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertFalse(helper.getBlockState(plate).getValue(powered), "the diamond plate reacted to a zombie or an item");
                    helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(plate)), 0, "signal of the plate under a zombie and an item");
                    helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(other)), 15, "signal of the plate under a player");
                })
                .thenSucceed();
    }

    /** Netherit-Druckplatte: ohne Fass jeder Spieler, mit Fass nur wer einen Gegenstand daraus traegt. */
    public static void netheritePlatesAdmitOnlyHoldersOfBarrelItems(GameTestHelper helper) {
        FilterPressurePlateBlock plate = (FilterPressurePlateBlock) PadsBlocks.NETHERITE_PRESSURE_PLATE;
        Player holder = helper.makeMockPlayer(GameType.SURVIVAL);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        holder.getInventory().setItem(3, new ItemStack(Items.EMERALD));
        helper.assertTrue(plate.admits(stranger, null, null), "without a barrel the netherite plate refuses a player");
        BarrelBlockEntity barrel = barrel(helper, new BlockPos(1, 1, 1), new ItemStack(Items.EMERALD));
        helper.assertTrue(plate.admits(holder, barrel, null), "a player carrying the whitelisted emerald is refused");
        helper.assertFalse(plate.admits(stranger, barrel, null), "a player without the whitelisted item is admitted");
        helper.succeed();
    }

    /**
     * Enderit-Druckplatte: der Besitzer immer; ohne Fass sonst niemand; mit Fass jeder, dessen Name
     * auf einem Namensschild im Fass steht (oder der einen Whitelist-Gegenstand traegt).
     */
    public static void enderitePlatesLockToTheirOwnerAndNamedTags(GameTestHelper helper) {
        FilterPressurePlateBlock plate = (FilterPressurePlateBlock) PadsBlocks.ENDERITE_PRESSURE_PLATE;
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, PadsBlocks.ENDERITE_PRESSURE_PLATE);
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
        helper.succeed();
    }

    /** Besitzer bauen Platten schnell ab, Fremde sehr langsam; Kreativ bleibt Vanilla. */
    public static void ownersBreakTheirPadsFastAndStrangersSlowly(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        BlockPos plate = new BlockPos(5, 1, 5);
        helper.setBlock(pad, PadsBlocks.ELYTRA_PAD);
        helper.setBlock(plate, PadsBlocks.COPPER_PRESSURE_PLATE);
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
        helper.succeed();
    }

    /** Wer setzt, ist Besitzer (Setzen ueber das BlockItem, wie ein Spieler). */
    public static void placingPadsMakesThePlacerTheOwner(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        helper.setBlock(pos, PadsBlocks.SPAWN_TELEPORTER);
        helper.getBlockState(pos).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), player,
                new ItemStack(PadsBlocks.SPAWN_TELEPORTER));
        helper.assertValueEqual(helper.getBlockEntity(pos, OwnedBlockEntity.class).getOwner(), player.getUUID(), "owner of a freshly placed teleporter");
        helper.succeed();
    }

    // =====================================================================================
    // Chunk-Loader, Launchpad
    // =====================================================================================

    /**
     * Chunk-Loader erzwingt seinen Chunk, der Enderit-Loader 3x3; beim Abbau gibt er genau die Chunks
     * frei, die ER erzwungen hat - fremde Erzwingungen (hier die der Spieltest-Umgebung) bleiben.
     */
    public static void chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(3, 1, 3);
        BlockPos abs = helper.absolutePos(pos);
        int cx = abs.getX() >> 4;
        int cz = abs.getZ() >> 4;
        Set<Long> foreign = new java.util.HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getForceLoadedChunks().contains(ChunkPos.pack(cx + dx, cz + dz))) {
                    foreign.add(ChunkLoaderBlockEntity.key(cx + dx, cz + dz));
                }
            }
        }
        helper.setBlock(pos, PadsBlocks.ENDERITE_CHUNK_LOADER);
        ChunkLoaderBlockEntity be = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
        be.update(level);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.pack(cx + dx, cz + dz)),
                        "the enderite chunk loader does not force chunk " + (cx + dx) + "," + (cz + dz));
            }
        }
        helper.assertValueEqual(be.ownForced().size(), 9 - foreign.size(), "chunks the enderite loader claims as its own");
        helper.assertTrue(java.util.Collections.disjoint(be.ownForced(), foreign), "the loader claims chunks that were forced before it");
        Set<Long> own = new java.util.HashSet<>(be.ownForced());
        helper.setBlock(pos, Blocks.AIR);
        for (long key : own) {
            helper.assertFalse(level.getForceLoadedChunks().contains(ChunkPos.pack((int) key, (int) (key >> 32))), "a broken chunk loader keeps its own chunk forced");
        }
        for (long key : foreign) {
            helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.pack((int) key, (int) (key >> 32))), "breaking a chunk loader released a chunk someone else had forced");
        }
        helper.assertValueEqual(ChunkLoaderBlockEntity.tierOf(PadsBlocks.CHUNK_LOADER.defaultBlockState()), 1, "tier of the plain chunk loader");
        helper.succeed();
    }


    private static JsonObject langFile(GameTestHelper helper, String code) {
        try (java.io.InputStream in = PadTweaksTests.class.getResourceAsStream("/assets/simplepads/lang/" + code + ".json")) {
            helper.assertTrue(in != null, "no " + code + ".json on the classpath");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + code + ".json", e);
        }
    }








    // =====================================================================================
    // Spawn, Dimensionen, XP, Stapel, Befehle, Config
    // =====================================================================================

    /**
     * Erstes Betreten: Teleporter und Elytra-Pad je in ihrer eigenen Config-Menge (beide Standard 0),
     * genau einmal (Tag wie Simple Tweaks), nicht fuer eine abgeschaltete Familie; ein alter
     * {@code spawnTeleporterCount} wird uebernommen.
     */
    public static void theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers(GameTestHelper helper) {
        PadsConfig.Spawn fresh = new PadsConfig().spawn;
        helper.assertValueEqual(fresh.firstJoinTeleporterCount, 0, "default spawn teleporters on the first join");
        helper.assertValueEqual(fresh.firstJoinElytraPadCount, 0, "default elytra pads on the first join");
        PadsConfig.Spawn spawn = SimplePads.config().spawn;
        PadsConfig.Pads families = SimplePads.config().pads;
        int teleporters = spawn.firstJoinTeleporterCount;
        int pads = spawn.firstJoinElytraPadCount;
        boolean teleportersOn = families.enableSpawnTeleporters;
        boolean elytraPadsOn = families.enableElytraPads;
        try {
            families.enableSpawnTeleporters = true;
            families.enableElytraPads = true;
            ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
            player.getInventory().clearContent();
            player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
            spawn.firstJoinTeleporterCount = 0;
            spawn.firstJoinElytraPadCount = 0;
            SpawnSetup.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.SPAWN_TELEPORTER.asItem()), 0, "spawn teleporters given with the default config");
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.ELYTRA_PAD.asItem()), 0, "elytra pads given with the default config");
            helper.assertTrue(player.entityTags().contains("simpletweaks.first_join"), "a first join with nothing to give did not set the first-join tag");

            player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
            spawn.firstJoinTeleporterCount = 2;
            spawn.firstJoinElytraPadCount = 3;
            SpawnSetup.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.SPAWN_TELEPORTER.asItem()), 2, "spawn teleporters given on the first join");
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.ELYTRA_PAD.asItem()), 3, "elytra pads given on the first join");
            helper.assertTrue(player.entityTags().contains("simpletweaks.first_join"), "the first-join tag is not the one Simple Tweaks used");
            SpawnSetup.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.SPAWN_TELEPORTER.asItem()), 2, "spawn teleporters after a second join");
            helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.ELYTRA_PAD.asItem()), 3, "elytra pads after a second join");

            // Abgeschaltete Familie: dieses Geschenk faellt weg, das andere bleibt.
            for (boolean teleportersEnabled : new boolean[] {false, true}) {
                player.getInventory().clearContent();
                player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
                families.enableSpawnTeleporters = teleportersEnabled;
                families.enableElytraPads = !teleportersEnabled;
                SpawnSetup.onPlayerJoin(player);
                helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.SPAWN_TELEPORTER.asItem()), teleportersEnabled ? 2 : 0,
                        "spawn teleporters given with spawn teleporters " + (teleportersEnabled ? "enabled" : "disabled"));
                helper.assertValueEqual(player.getInventory().countItem(PadsBlocks.ELYTRA_PAD.asItem()), teleportersEnabled ? 0 : 3,
                        "elytra pads given with elytra pads " + (teleportersEnabled ? "disabled" : "enabled"));
            }
            families.enableSpawnTeleporters = true;
            families.enableElytraPads = true;

            // Alte Config-Datei: ein geaenderter Wert gilt fuer beide, der alte Standard 1 wird 0.
            PadsConfig.Spawn legacy = new PadsConfig().spawn;
            legacy.spawnTeleporterCount = 5;
            helper.assertTrue(legacy.migrateLegacyFirstJoinCount(), "an old spawnTeleporterCount was not seen");
            helper.assertValueEqual(legacy.firstJoinTeleporterCount, 5, "teleporters taken over from an old spawnTeleporterCount");
            helper.assertValueEqual(legacy.firstJoinElytraPadCount, 5, "elytra pads taken over from an old spawnTeleporterCount");
            helper.assertTrue(legacy.spawnTeleporterCount == null, "the old key is kept and would be saved again");
            PadsConfig.Spawn oldDefault = new PadsConfig().spawn;
            oldDefault.spawnTeleporterCount = 1;
            oldDefault.migrateLegacyFirstJoinCount();
            helper.assertValueEqual(oldDefault.firstJoinTeleporterCount + oldDefault.firstJoinElytraPadCount, 0,
                    "the old default 1 still hands out first-join gifts");
            helper.assertFalse(new PadsConfig().spawn.migrateLegacyFirstJoinCount(), "a new config claims to carry an old key");
        } finally {
            spawn.firstJoinTeleporterCount = teleporters;
            spawn.firstJoinElytraPadCount = pads;
            families.enableSpawnTeleporters = teleportersOn;
            families.enableElytraPads = elytraPadsOn;
        }
        helper.succeed();
    }







    // =====================================================================================
    // Audit 2026-09-26 (#3, #4, #5, #16, #17, #32-#35, #40, #51)
    // =====================================================================================

    /**
     * #3: Eine Spawn-Elytra verlaesst den Brust-Slot nicht als Gegenstand - fallen gelassen wird der
     * Stapel leer (nichts fuer Trichter), Container-Slots und Buendel nehmen sie nicht, und im
     * Inventar oder am Cursor verschwindet sie noch im selben Tick (frueher nur jede Sekunde; jede
     * Luecke war bei sofort nachgelegter Elytra eine geschenkte echte).
     */
    public static void spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot(GameTestHelper helper) {
        BlockPos pad = new BlockPos(3, 1, 3);
        helper.setBlock(pad, PadsBlocks.ELYTRA_PAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(elytra.is(PadsItems.SPAWN_ELYTRA), "the elytra pad gave no spawn elytra to test with");

        Vec3 spot = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        ItemEntity dropped = new ItemEntity(helper.getLevel(), spot.x, spot.y, spot.z, elytra.copy());
        helper.assertTrue(dropped.getItem().isEmpty(), "a dropped spawn elytra lies on the ground as " + dropped.getItem());

        BarrelBlockEntity barrel = barrel(helper, new BlockPos(1, 1, 5), ItemStack.EMPTY);
        helper.assertFalse(new Slot(barrel, 0, 0, 0).mayPlace(elytra.copy()), "a barrel slot accepts the spawn elytra");
        helper.assertTrue(new Slot(player.getInventory(), 9, 0, 0).mayPlace(elytra.copy()), "the own inventory refuses the spawn elytra, so it cannot even be taken off");
        helper.assertFalse(elytra.getItem().canFitInsideContainerItems(), "bundles and shulker boxes take the spawn elytra");

        player.getInventory().setItem(5, elytra.copy());
        player.containerMenu.setCarried(elytra.copy());
        SpawnElytra.tick(player, false);
        helper.assertTrue(player.getInventory().getItem(5).isEmpty(), "a spawn elytra in the inventory survives the tick");
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "a spawn elytra on the cursor survives the tick");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(PadsItems.SPAWN_ELYTRA), "the worn spawn elytra was cleaned up too");
        helper.succeed();
    }

    /**
     * #4: Der Spawnbereich gilt nur in der Weltspawn-Dimension (vorher gab der Nether bei 0,0 freie
     * Elytren und Fallschutz), und Fallschutz und Elytra-Bereich sind dasselbe Quadrat (vorher war der
     * Fallschutz ein Kreis, die Ecken gaben Elytren ohne Schutz).
     */
    public static void theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has no nether");
        ServerPlayer inNether = new ServerPlayer(helper.getLevel().getServer(), nether,
                new GameProfile(UUID.randomUUID(), "nether_probe"), ClientInformation.createDefault());
        PadsConfig.Spawn spawn = SimplePads.config().spawn;
        boolean give = spawn.giveElytraOnSpawn;
        boolean noFall = spawn.disableFallDamageInSpawn;
        boolean worldCenter = spawn.useWorldSpawnAsCenter;
        int radius = spawn.spawnElytraRadius;
        int cx = spawn.customSpawnElytraX;
        int cz = spawn.customSpawnElytraZ;
        try {
            spawn.giveElytraOnSpawn = true;
            spawn.disableFallDamageInSpawn = true;
            spawn.useWorldSpawnAsCenter = false;
            spawn.spawnElytraRadius = 5;
            // Mitte so, dass der Spieler in der Ecke des Quadrats steht (4,5 je Achse, ausserhalb des Kreises).
            spawn.customSpawnElytraX = player.getBlockX() - 4;
            spawn.customSpawnElytraZ = player.getBlockZ() - 4;
            helper.assertTrue(SpawnElytra.insideSpawn(player), "the corner of the square does not count as spawn area");
            helper.assertTrue(ElytraDamageRules.inSpawnArea(player), "fall protection is missing in the corner where the spawn elytra is handed out");
            helper.assertTrue(ElytraDamageRules.preventsFallDamage(player), "fall damage is not prevented in the corner of the spawn area");

            inNether.snapTo(spawn.customSpawnElytraX + 0.5, 64.0, spawn.customSpawnElytraZ + 0.5, 0.0F, 0.0F);
            helper.assertFalse(SpawnElytra.insideSpawn(inNether), "the spawn area coordinates also count in the nether");
            helper.assertFalse(ElytraDamageRules.preventsFallDamage(inNether), "the nether below the spawn protects against fall damage");
        } finally {
            spawn.giveElytraOnSpawn = give;
            spawn.disableFallDamageInSpawn = noFall;
            spawn.useWorldSpawnAsCenter = worldCenter;
            spawn.spawnElytraRadius = radius;
            spawn.customSpawnElytraX = cx;
            spawn.customSpawnElytraZ = cz;
        }
        helper.succeed();
    }

    /**
     * #5 und #32: Ein per {@code /setblock}/{@code /fill} (Flag 256, ohne preRemoveSideEffects)
     * ersetzter Loader gibt seine Chunks trotzdem frei; ueberlappen sich zwei Loader, uebernimmt der
     * verbliebene die gemeinsamen Chunks, statt dass sie freikommen. Weit weg von der Teststruktur,
     * damit keine fremde Erzwingung der Spieltest-Umgebung die Chunks schon haelt.
     */
    public static void chunkLoadersReleaseOnSetblockAndHandOverSharedChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos enderite = helper.absolutePos(new BlockPos(1, 1, 1)).offset(4096, 0, 4096);
        BlockPos plain = enderite.offset(1, 0, 1);
        long plainChunk = ChunkLoaderBlockEntity.key(plain.getX() >> 4, plain.getZ() >> 4);
        helper.assertFalse(isForced(level, plainChunk), "the far test chunk is already forced by someone else");
        try {
            level.setBlock(enderite, PadsBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity big = (ChunkLoaderBlockEntity) level.getBlockEntity(enderite);
            big.update(level);
            Set<Long> bigOwn = new java.util.HashSet<>(big.ownForced());
            helper.assertTrue(bigOwn.contains(plainChunk), "the enderite loader does not own its own chunk");

            level.setBlock(plain, PadsBlocks.CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity small = (ChunkLoaderBlockEntity) level.getBlockEntity(plain);
            small.update(level);
            helper.assertTrue(small.ownForced().isEmpty(), "the second loader claims a chunk the first one forced");

            level.setBlock(enderite, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(isForced(level, plainChunk), "breaking one of two overlapping loaders released the chunk the other still covers");
            helper.assertTrue(small.ownForced().contains(plainChunk), "the remaining loader did not take over the shared chunk");
            for (long key : bigOwn) {
                if (key != plainChunk) {
                    helper.assertFalse(isForced(level, key), "a chunk only the broken loader covered stays forced");
                }
            }

            // /setblock und /fill: Flag 256 ueberspringt preRemoveSideEffects.
            level.setBlock(plain, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            helper.assertFalse(isForced(level, plainChunk), "a loader replaced by /setblock keeps its chunk forced forever");
        } finally {
            level.setBlock(enderite, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(plain, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    /**
     * #32: Zwei ueberlappende Flypads - faellt eines weg, fliegt der Spieler weiter, solange das andere
     * ihn abdeckt; und ein Pad nimmt nur Flug zurueck, den ein Flypad gab (nicht den eines anderen Mods).
     */
    public static void overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 1, 1);
        BlockPos second = new BlockPos(5, 1, 5);
        helper.setBlock(first, PadsBlocks.FLYPAD);
        helper.setBlock(second, PadsBlocks.FLYPAD);
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(3.5, 3.0, 3.5));
        player.getAbilities().mayfly = false;
        player.removeTag(FlypadBlockEntity.FLIGHT_TAG);
        FlypadBlockEntity a = helper.getBlockEntity(first, FlypadBlockEntity.class);
        FlypadBlockEntity b = helper.getBlockEntity(second, FlypadBlockEntity.class);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(first), helper.getBlockState(first), a);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(second), helper.getBlockState(second), b);
        helper.assertTrue(player.getAbilities().mayfly, "the flypads did not grant flight");
        helper.assertTrue(a.flyingPlayers().contains(player.getUUID()) && b.flyingPlayers().contains(player.getUUID()), "not both flypads track the player");

        helper.setBlock(first, Blocks.AIR);
        helper.assertTrue(player.getAbilities().mayfly, "breaking one flypad took the flight although the other still covers the player");

        Vec3 far = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5)).add(0, PadTiers.flyHeight(1) + 5, 0);
        player.snapTo(far.x, far.y, far.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(second), helper.getBlockState(second), b);
        helper.assertFalse(player.getAbilities().mayfly, "leaving the last flypad did not take the flight");

        // Flug aus anderer Quelle (kein Flypad-Tag): das Pad laesst ihn in Ruhe.
        player.getAbilities().mayfly = true;
        Vec3 inside = helper.absoluteVec(new Vec3(5.5, 3.0, 5.5));
        player.snapTo(inside.x, inside.y, inside.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(second), helper.getBlockState(second), b);
        player.snapTo(far.x, far.y, far.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(second), helper.getBlockState(second), b);
        helper.assertTrue(player.getAbilities().mayfly, "a flypad took flight it never granted");
        helper.succeed();
    }











    private static final int SHORT_LASER_RANGE = 2;


    private static final net.minecraft.world.entity.EntityType<? extends Mob> PIG_TYPE = EntityTypes.PIG;














    private static net.minecraft.world.inventory.AnvilMenu recharge(GameTestHelper helper, ServerPlayer player, ItemStack lens, Item material, int count) {
        net.minecraft.world.inventory.AnvilMenu menu = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.INPUT_SLOT).set(lens);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(material, count));
        return menu;
    }

    private static void takeResult(GameTestHelper helper, ServerPlayer player, net.minecraft.world.inventory.AnvilMenu menu) {
        Slot result = menu.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT);
        helper.assertTrue(result.mayPickup(player), "a level-0 survival player may not take the free recharge");
        result.onTake(player, result.getItem());
    }


    /** Blick waagerecht nach Norden (-Z); Lebewesen blicken ueber den Kopf, darum auch yHeadRot. */
    private static void faceNorth(ServerPlayer player) {
        player.setXRot(0.0f);
        player.setYRot(180.0f);
        player.setYHeadRot(180.0f);
    }

    /** Wo der Strahl eine Blockseite trifft (Mitte der Seite, absolut). */
    private static Vec3 beamPoint(GameTestHelper helper, BlockPos relative, Direction face) {
        return Vec3.atCenterOf(helper.absolutePos(relative)).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
    }




    /**
     * #51: Flugzeit und Boosts sind gedeckelt (vorher lief {@code flightTimeSeconds * 20} ueber), und
     * ein abgebautes Launchpad laesst seine Windkugeln fallen.
     */
    public static void flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges(GameTestHelper helper) {
        PadsConfig.Spawn huge = new PadsConfig().spawn;
        huge.flightTimeSeconds = Integer.MAX_VALUE;
        huge.maxBoosts = Integer.MAX_VALUE;
        helper.assertValueEqual(huge.flightTicks(), PadsConfig.Spawn.MAX_FLIGHT_SECONDS * 20, "flight ticks of an oversized flight time");
        helper.assertValueEqual(huge.boostCount(), PadsConfig.Spawn.MAX_BOOSTS, "boosts of an oversized boost count");

        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, PadsBlocks.LAUNCHPAD);
        LaunchpadBlockEntity pad = helper.getBlockEntity(pos, LaunchpadBlockEntity.class);
        for (int n = 0; n < 5; n++) {
            pad.addCharge(16);
        }
        helper.getLevel().destroyBlock(helper.absolutePos(pos), false);
        int charges = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (item.getItem().is(Items.WIND_CHARGE)) {
                charges += item.getItem().getCount();
                item.discard();
            }
        }
        helper.assertValueEqual(charges, 5, "wind charges dropped by the broken launchpad");
        helper.succeed();
    }



    /**
     * N10: {@code /setblock} eines Loader-Typs auf einen anderen laesst dort wieder einen Loader
     * stehen, entfernt aber die alte Block-Entity - sie gibt ihre Chunks trotzdem frei (vorher
     * erzwungen fuer immer, der neue Loader kennt sie nicht).
     */
    public static void aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1)).offset(8192, 0, 8192);
        long chunk = ChunkLoaderBlockEntity.key(pos.getX() >> 4, pos.getZ() >> 4);
        helper.assertFalse(isForced(level, chunk), "the far test chunk is already forced by someone else");
        try {
            level.setBlock(pos, PadsBlocks.CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity plain = (ChunkLoaderBlockEntity) level.getBlockEntity(pos);
            plain.update(level);
            helper.assertTrue(plain.ownForced().contains(chunk) && isForced(level, chunk), "the loader did not force its own chunk");

            // /setblock: Flag 256 ueberspringt preRemoveSideEffects; der neue Loader hat noch nicht getickt.
            level.setBlock(pos, PadsBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            helper.assertTrue(plain.isRemoved() && level.getBlockEntity(pos) != plain, "setblock kept the old loader's block entity");
            helper.assertTrue(plain.ownForced().isEmpty(), "the replaced loader still claims its chunks");
            helper.assertFalse(isForced(level, chunk), "a loader replaced by another loader type through /setblock keeps its chunk forced");
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.succeed();
    }



    /**
     * N16: Ein Kreativspieler behaelt beim Verlassen eines Flypads den Flug, verliert aber den
     * Flypad-Tag - sonst naehme ein Pad spaeter Flug zurueck, den ein anderer Mod gab.
     */
    public static void creativePlayersLoseTheStaleFlypadFlightTag(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = true;
        player.getAbilities().mayfly = true;
        player.addTag(FlypadBlockEntity.FLIGHT_TAG);
        FlypadBlockEntity.revoke(player, 1);
        helper.assertTrue(player.getAbilities().mayfly, "a flypad took a creative player's flight");
        helper.assertFalse(player.entityTags().contains(FlypadBlockEntity.FLIGHT_TAG), "a creative player kept the flypad flight tag");
        helper.succeed();
    }

    /**
     * Audit b-audsb31-a (simplepads-P1-1): flypad flight that no pad covers any more (logout while flying, restart, pad broken
     * meanwhile) is taken by the sweep; a player inside a switched-on flypad keeps it.
     */
    public static void staleFlypadFlightIsSweptAway(GameTestHelper helper) {
        ServerPlayer stale = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        stale.getAbilities().instabuild = false;
        stale.getAbilities().mayfly = true;
        stale.addTag(FlypadBlockEntity.FLIGHT_TAG);
        BlockPos pad = new BlockPos(5, 1, 5);
        helper.setBlock(pad, PadsBlocks.FLYPAD);
        ServerPlayer covered = mockPlayer(helper, new Vec3(5.5, 3.0, 5.5));
        covered.getAbilities().instabuild = false;
        covered.getAbilities().mayfly = false;
        FlypadBlockEntity be = helper.getBlockEntity(pad, FlypadBlockEntity.class);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertTrue(covered.getAbilities().mayfly && covered.entityTags().contains(FlypadBlockEntity.FLIGHT_TAG), "the flypad did not let the covered player fly");
        FlypadBlockEntity.sweepStaleFlight(helper.getLevel().getServer());
        helper.assertFalse(stale.getAbilities().mayfly, "a player with stale flypad flight kept creative flight");
        helper.assertFalse(stale.entityTags().contains(FlypadBlockEntity.FLIGHT_TAG), "the stale flight tag stayed");
        helper.assertTrue(covered.getAbilities().mayfly, "the sweep took the flight of a player over a switched-on flypad");
        helper.succeed();
    }

    private static boolean isForced(ServerLevel level, long key) {
        return level.getForceLoadedChunks().contains(ChunkPos.pack((int) key, (int) (key >> 32)));
    }


    /**
     * Audit 2026-09-26 #42: jede wasserloggbare Tweaks-Platte (Spawn-Teleporter, Elytra-Pad, Launchpad
     * je Stufe, Diamant-Druckplatte) wird ueber den echten Setz-Weg in eine Wasserquelle gesetzt:
     * {@code waterlogged=true}, die Position meldet Wasserquelle, beim Abbau faellt das Item und das
     * Wasser bleibt stehen. Trocken gesetzt bleibt sie trocken und hinterlaesst Luft.
     */
    public static void padsPlacedInWaterAreWaterloggedAndLeaveTheWaterBehind(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        BlockPos wet = new BlockPos(4, 1, 4);
        BlockPos dry = new BlockPos(6, 1, 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(wet.offset(dx, -1, dz), Blocks.STONE);
                if (dx != 0 || dz != 0) {
                    helper.setBlock(wet.offset(dx, 0, dz), Blocks.GLASS);
                }
            }
        }
        helper.setBlock(dry.below(), Blocks.STONE);

        List<Block> pads = new ArrayList<>();
        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (block.getClass().getPackageName().startsWith("com.simplepads.block")
                    && block.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
                pads.add(block);
            }
        }
        for (Block expected : List.of(PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.ENDERITE_SPAWN_TELEPORTER,
                PadsBlocks.ELYTRA_PAD, PadsBlocks.FINE_ELYTRA_PAD, PadsBlocks.LAUNCHPAD,
                PadsBlocks.ENDERITE_LAUNCHPAD, PadsBlocks.DIAMOND_PRESSURE_PLATE)) {
            helper.assertTrue(pads.contains(expected), expected + " is not waterloggable any more");
        }

        for (Block pad : pads) {
            helper.setBlock(wet, Blocks.WATER);
            placeByHand(helper, player, pad, wet);
            BlockState placed = helper.getBlockState(wet);
            helper.assertTrue(placed.is(pad), pad + " was not placed into the water: " + placed);
            helper.assertTrue(placed.getValue(BlockStateProperties.WATERLOGGED), pad + " placed into water is not waterlogged");
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(wet)).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER),
                    pad + " placed into water does not hold a water source");
            helper.getLevel().destroyBlock(helper.absolutePos(wet), true);
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(wet)).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER),
                    "breaking a waterlogged " + pad + " did not leave its water behind");
            helper.assertValueEqual(droppedCount(helper, pad.asItem()), 1, pad + " items dropped from the waterlogged block");

            placeByHand(helper, player, pad, dry);
            BlockState dryState = helper.getBlockState(dry);
            helper.assertTrue(dryState.is(pad) && !dryState.getValue(BlockStateProperties.WATERLOGGED), pad + " placed on dry ground: " + dryState);
            helper.getLevel().destroyBlock(helper.absolutePos(dry), true);
            helper.assertTrue(helper.getBlockState(dry).isAir(), "breaking a dry " + pad + " left " + helper.getBlockState(dry));
            helper.assertValueEqual(droppedCount(helper, pad.asItem()), 1, pad + " items dropped from the dry block");
        }
        helper.setBlock(wet, Blocks.AIR);
        helper.succeed();
    }

    /** Setzt {@code block} wie ein Spieler von oben auf den Block unter {@code relative}. */
    private static void placeByHand(GameTestHelper helper, ServerPlayer player, Block block, BlockPos relative) {
        BlockPos floor = helper.absolutePos(relative.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(block));
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
    }

    /** Zaehlt und entfernt die fallengelassenen {@code item}-Stapel im Testraum. */
    private static int droppedCount(GameTestHelper helper, net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())) {
            if (drop.getItem().is(item)) {
                count += drop.getItem().getCount();
                drop.discard();
            }
        }
        return count;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
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
        ItemStack out = match.get().value().assemble(smithingInput(template, base, addition));
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }


    /**
     * {@code ticks} Benutz-Ticks wie Vanillas LivingEntity#updatingUsingItem: onUseTick, Zaehler
     * herunter, bei 0 completeUsingItem. Per Reflexion, weil der Mock-Spieler hier nicht getickt wird.
     */
    private static void tickUse(ServerPlayer player, int ticks) {
        try {
            java.lang.reflect.Method update = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("updateUsingItem", ItemStack.class);
            update.setAccessible(true);
            for (int i = 0; i < ticks && player.isUsingItem(); i++) {
                update.invoke(player, player.getUseItem());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("LivingEntity#updateUsingItem not reachable", e);
        }
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
                .map(holder -> holder.value().assemble(grid));
    }

    private static void expectCrafting(GameTestHelper helper, CraftingInput grid, Item result, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the documented pattern for " + result + " matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched for " + result);
        helper.assertTrue(match.get().value().assemble(grid).is(result), "the recipe for " + result + " made something else");
    }
}
