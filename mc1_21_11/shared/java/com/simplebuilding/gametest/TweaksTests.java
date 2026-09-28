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
import com.simplebuilding.tweaks.item.LaserBeam;
import com.simplebuilding.tweaks.item.LaserPointerItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import com.simplebuilding.tweaks.item.TweaksItems;
import com.simplebuilding.tweaks.network.ElytraBoostPayload;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import com.simplebuilding.tweaks.spawn.ElytraDamageRules;
import com.simplebuilding.tweaks.spawn.LaunchSafety;
import com.simplebuilding.tweaks.spawn.SpawnElytra;
import com.simplebuilding.tweaks.spawn.SpawnRules;
import com.simplebuilding.tweaks.spawn.SpawnSetup;
import com.simplebuilding.tweaks.xp.XpClumping;
import com.simplebuilding.tweaks.TweaksContent;
import com.simplebuilding.tweaks.network.LaserPayload;
import com.simplebuilding.tweaks.network.TweaksConfigPayload;
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
        helper.assertValueEqual(List.of(PadTiers.width(1), PadTiers.width(2), PadTiers.width(3), PadTiers.width(4), PadTiers.width(5)),
                List.of(1, 5, 16, 32, 128), "elytra pad widths per tier");
        helper.assertValueEqual(List.of(PadTiers.height(1), PadTiers.height(2), PadTiers.height(3), PadTiers.height(4), PadTiers.height(5)),
                List.of(15, 31, 63, 95, 127), "pad heights per tier");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(TweaksBlocks.ENDERITE_ELYTRA_PAD.defaultBlockState()), 4, "tier of the enderite elytra pad");
        helper.assertValueEqual(ElytraPadBlockEntity.tierOf(TweaksBlocks.FINE_ELYTRA_PAD.defaultBlockState()), 5, "tier of the fine elytra pad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(TweaksBlocks.REINFORCED_FLYPAD.defaultBlockState()), 2, "tier of the reinforced flypad");
        helper.assertValueEqual(FlypadBlockEntity.tierOf(TweaksBlocks.STELLAR_FLYPAD.defaultBlockState()), 3, "tier of the stellar flypad");
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
        // Seit 2026-09-27 zahlen die Aufwertungen mit der Druckplatte des Zielmaterials (TweaksTierTests).
        Item enderitePlate = TweaksBlocks.ENDERITE_PRESSURE_PLATE.asItem();
        Item netheritePlate = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_ELYTRA_PAD, enderitePlate, TweaksBlocks.ENDERITE_ELYTRA_PAD);
        expectSmithing(helper, template, TweaksBlocks.FLYPAD, enderitePlate, TweaksBlocks.REINFORCED_FLYPAD);
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_PRESSURE_PLATE, ingot, TweaksBlocks.ENDERITE_PRESSURE_PLATE);
        expectSmithing(helper, template, TweaksBlocks.SPAWN_TELEPORTER_TIER_4, enderitePlate, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_CHUNK_LOADER, enderitePlate, TweaksBlocks.ENDERITE_CHUNK_LOADER);
        expectSmithing(helper, template, TweaksBlocks.NETHERITE_LAUNCHPAD, enderitePlate, TweaksBlocks.ENDERITE_LAUNCHPAD);
        expectSmithing(helper, netherite, TweaksBlocks.ENDERITE_ELYTRA_PAD, Items.NETHER_STAR, TweaksBlocks.FINE_ELYTRA_PAD);
        expectSmithing(helper, netherite, TweaksBlocks.REINFORCED_ELYTRA_PAD, netheritePlate, TweaksBlocks.NETHERITE_ELYTRA_PAD);
        expectSmithing(helper, template, TweaksBlocks.ENDERITE_PRESSURE_PLATE, ModItems.ENDERITE_CORE, TweaksBlocks.FLYPAD);
        expectSmithing(helper, netherite, TweaksBlocks.DIAMOND_PRESSURE_PLATE, Items.NETHERITE_INGOT, TweaksBlocks.NETHERITE_PRESSURE_PLATE);
        expectSmithing(helper, netherite, TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.DIAMOND_PRESSURE_PLATE.asItem(), TweaksBlocks.CHUNK_LOADER);
        // Der alte Weg (Netherit-Pad + Netherstern) fuehrt nicht mehr zum feinen Pad.
        Optional<RecipeHolder<SmithingRecipe>> oldWay = smithing(helper, netherite, TweaksBlocks.NETHERITE_ELYTRA_PAD, Items.NETHER_STAR);
        helper.assertTrue(oldWay.isEmpty() || !oldWay.get().value().assemble(smithingInput(netherite, TweaksBlocks.NETHERITE_ELYTRA_PAD, Items.NETHER_STAR), helper.getLevel().registryAccess()).is(TweaksBlocks.FINE_ELYTRA_PAD.asItem()),
                "the netherite elytra pad still becomes the fine pad directly, so the enderite tier can be skipped");
        TestCleanup.succeed(helper);
    }

    /**
     * Stellares Flypad III (Besitzer 2026-09-27): zwei Verstaerkte Flypads II im Schmiedetisch
     * (Enderit-Vorlage, das zweite als Zutat); das alte Werkbank-Rezept KKK / ESE / FFF gibt es nicht mehr.
     */
    public static void theStellarFlypadIsSmithedFromTwoReinforcedFlypads(GameTestHelper helper) {
        expectSmithing(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.REINFORCED_FLYPAD.asItem(), TweaksBlocks.STELLAR_FLYPAD);
        Optional<RecipeHolder<SmithingRecipe>> one = smithing(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD.asItem());
        helper.assertTrue(one.isEmpty(), "a flypad I and a flypad II already smith something");
        Item f = TweaksBlocks.ENDERITE_FLYPAD.asItem();
        CraftingInput oldGrid = grid(Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY, Items.OMINOUS_TRIAL_KEY,
                Items.ENCHANTED_GOLDEN_APPLE, Items.NETHER_STAR, Items.ENCHANTED_GOLDEN_APPLE, f, f, f);
        helper.assertTrue(craftingResult(helper, oldGrid).isEmpty(), "the old crafting recipe still makes a stellar flypad");
        TestCleanup.succeed(helper);
    }

    /**
     * Echolot / Echo Sounder (Id echo_compass; Besitzer-Rezept 2026-09-27): Bergungskompass in der
     * Mitte, Enderit-Kern unten mittig, sieben Enderit-Nuggets aussen herum - seit der zweiten Runde
     * auch oben mittig ("NNN" / "NRN" / "NEN").
     */
    public static void theEchoSounderIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndSevenEnderiteNuggets(GameTestHelper helper) {
        Item n = ModItems.ENDERITE_NUGGET;
        CraftingInput grid = grid(n, n, n, n, Items.RECOVERY_COMPASS, n, n, ModItems.ENDERITE_CORE, n);
        expectCrafting(helper, grid, TweaksItems.ECHO_COMPASS, "simplebuilding:echo_compass");
        CraftingInput swapped = grid(n, n, n, n, ModItems.ENDERITE_CORE, n, n, Items.RECOVERY_COMPASS, n);
        helper.assertTrue(craftingResult(helper, swapped).isEmpty(), "core and compass swapped still craft an echo sounder");
        CraftingInput topEmpty = grid(n, null, n, n, Items.RECOVERY_COMPASS, n, n, ModItems.ENDERITE_CORE, n);
        helper.assertTrue(craftingResult(helper, topEmpty).isEmpty(), "the old six nugget recipe with the top middle empty still crafts an echo sounder");
        Item p = TweaksBlocks.NETHERITE_PRESSURE_PLATE.asItem();
        CraftingInput oldRecipe = grid(null, ModItems.ENDERITE_CORE, null, p, Items.RECOVERY_COMPASS, p, null, null, null);
        helper.assertTrue(craftingResult(helper, oldRecipe).isEmpty(), "the old netherite plate recipe still crafts an echo compass");
        TestCleanup.succeed(helper);
    }

    /**
     * Geschwindigkeitsmesser (Besitzer 2026-09-27/28): Quarz in den oberen Ecken um den
     * Amethystsplitter, Kupfernuggets links und rechts vom Kompass und unten links/rechts, unten
     * mittig ein Kupfer-Baukern ("QAQ" / "NCN" / "NKN").
     */
    public static void theVelocityGaugeIsCraftedWithQuartzCornersCopperNuggetsAndTheCopperCore(GameTestHelper helper) {
        Item q = Items.QUARTZ;
        Item n = Items.COPPER_NUGGET;
        Item o = Items.COPPER_INGOT;
        CraftingInput grid = grid(q, Items.AMETHYST_SHARD, q, n, Items.COMPASS, n, n, ModItems.COPPER_CORE, n);
        expectCrafting(helper, grid, ModItems.VELOCITY_GAUGE, "simplebuilding:velocity-gauge");
        CraftingInput ingots = grid(q, Items.AMETHYST_SHARD, q, o, Items.COMPASS, o, null, ModItems.COPPER_CORE, null);
        helper.assertTrue(craftingResult(helper, ingots).isEmpty(), "the previous copper ingot recipe still crafts a velocity gauge");
        CraftingInput oldRecipe = grid(null, Items.AMETHYST_SHARD, null, o, Items.COMPASS, o, q, q, q);
        helper.assertTrue(craftingResult(helper, oldRecipe).isEmpty(), "the old quartz row recipe still crafts a velocity gauge");
        CraftingInput bottomEmpty = grid(q, Items.AMETHYST_SHARD, q, n, Items.COMPASS, n, null, ModItems.COPPER_CORE, null);
        helper.assertTrue(craftingResult(helper, bottomEmpty).isEmpty(), "the gauge crafts without the two bottom copper nuggets");
        CraftingInput ironCore = grid(q, Items.AMETHYST_SHARD, q, n, Items.COMPASS, n, n, ModItems.IRON_CORE, n);
        helper.assertTrue(craftingResult(helper, ironCore).isEmpty(), "an iron core is accepted instead of the copper core");
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

        Vec3 far = helper.absoluteVec(new Vec3(3.5, 3.0, 3.5)).add(0, PadTiers.flyHeight(1) + 5, 0);
        player.snapTo(far.x, far.y, far.z);
        FlypadBlockEntity.update(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad), be);
        helper.assertFalse(be.flyingPlayers().contains(player.getUUID()), "the flypad still tracks a player who left its area");
        TestCleanup.succeed(helper);
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
        helper.startSequence()
                .thenExecuteAfter(SpawnTeleporterBlockEntity.STANDARD_TICKS - 20, () ->
                        helper.assertTrue(player.position().distanceTo(start) < 0.5, "the player was teleported before the countdown ended"))
                .thenWaitUntil(() -> {
                    Vec3 expected = Vec3.atBottomCenterOf(target).add(0, 2.0, 0);
                    helper.assertTrue(player.position().distanceTo(expected) < 1.5,
                            "the player is at " + player.position() + ", not two blocks above spawn 2 at " + expected);
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
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
        helper.startSequence()
                .thenExecuteAfter(10, () -> helper.assertFalse(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate powered after half a second"))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(plate).getValue(CopperPressurePlateBlock.POWERED),
                        "the copper plate never powered"))
                .thenExecute(() -> TestCleanup.run(helper))
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
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertFalse(helper.getBlockState(plate).getValue(powered), "the diamond plate reacted to a zombie or an item");
                    helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(plate)), 0, "signal of the plate under a zombie and an item");
                    helper.assertValueEqual(block.signalAt(helper.getLevel(), helper.absolutePos(other)), 15, "signal of the plate under a player");
                })
                .thenExecute(() -> TestCleanup.run(helper))
                .thenSucceed();
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
                if (level.getForceLoadedChunks().contains(ChunkPos.asLong(cx + dx, cz + dz))) {
                    foreign.add(ChunkLoaderBlockEntity.key(cx + dx, cz + dz));
                }
            }
        }
        helper.setBlock(pos, TweaksBlocks.ENDERITE_CHUNK_LOADER);
        ChunkLoaderBlockEntity be = helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
        be.update(level);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.asLong(cx + dx, cz + dz)),
                        "the enderite chunk loader does not force chunk " + (cx + dx) + "," + (cz + dz));
            }
        }
        helper.assertValueEqual(be.ownForced().size(), 9 - foreign.size(), "chunks the enderite loader claims as its own");
        helper.assertTrue(java.util.Collections.disjoint(be.ownForced(), foreign), "the loader claims chunks that were forced before it");
        Set<Long> own = new java.util.HashSet<>(be.ownForced());
        helper.setBlock(pos, Blocks.AIR);
        for (long key : own) {
            helper.assertFalse(level.getForceLoadedChunks().contains(ChunkPos.asLong((int) key, (int) (key >> 32))), "a broken chunk loader keeps its own chunk forced");
        }
        for (long key : foreign) {
            helper.assertTrue(level.getForceLoadedChunks().contains(ChunkPos.asLong((int) key, (int) (key >> 32))), "breaking a chunk loader released a chunk someone else had forced");
        }
        helper.assertValueEqual(ChunkLoaderBlockEntity.tierOf(TweaksBlocks.CHUNK_LOADER.defaultBlockState()), 1, "tier of the plain chunk loader");
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Echolot / Echo Sounder (Registry-Id echo_compass)
    // =====================================================================================

    /**
     * Umbenennung (Besitzer 2026-09-27): das Item heisst "Echo Sounder" / "Echolot", die Registry-Id
     * bleibt {@code echo_compass} (alte Welten behalten ihre Items), und die Texte sprechen nicht mehr
     * vom Kompass oder von einer Enderperle. Gelesen werden die Sprachdateien aus dem Mod-Jar.
     */
    public static void theEchoSounderKeepsItsIdButIsNamedEchoSounder(GameTestHelper helper) {
        helper.assertValueEqual(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(TweaksItems.ECHO_COMPASS).toString(),
                "simplebuilding:echo_compass", "registry id of the echo sounder");
        JsonObject en = langFile(helper, "en_us");
        JsonObject de = langFile(helper, "de_de");
        helper.assertValueEqual(en.get("item.simplebuilding.echo_compass").getAsString(), "Echo Sounder", "english item name");
        helper.assertValueEqual(de.get("item.simplebuilding.echo_compass").getAsString(), "Echolot", "german item name");
        for (JsonObject lang : List.of(en, de)) {
            helper.assertFalse(lang.has("message.simplebuilding.echo_compass.no_pearl"), "the no pearl message is still translated");
            for (String key : List.of("message.simplebuilding.echo_compass.unlinked", "jei.simplebuilding.info.echo_compass",
                    "simplebuilding.testcentre.tweaks.echo")) {
                String text = lang.get(key).getAsString();
                helper.assertFalse(text.contains("Echo Compass") || text.contains("Echo-Kompass"), key + " still names the echo compass: " + text);
            }
            String info = lang.get("jei.simplebuilding.info.echo_compass").getAsString();
            helper.assertFalse(info.contains("for one ender pearl") || info.contains("für eine Enderperle"), "the JEI page still asks for an ender pearl: " + info);
        }
        TestCleanup.succeed(helper);
    }

    private static JsonObject langFile(GameTestHelper helper, String code) {
        try (java.io.InputStream in = TweaksTests.class.getResourceAsStream("/assets/simplebuilding/lang/" + code + ".json")) {
            helper.assertTrue(in != null, "no " + code + ".json on the classpath");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + code + ".json", e);
        }
    }

    /**
     * Echolot: Rechtsklick auf einen Leitstein verknuepft; der Sprung teleportiert ueber den
     * Leitstein - ohne Enderperle (Besitzer 2026-09-27), auch mit Perlen im Inventar wird keine
     * verbraucht -, das Echolot bleibt, ist danach aber leer (voller Schaden).
     */
    public static void theEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl(GameTestHelper helper) {
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

        helper.assertFalse(player.getInventory().hasAnyOf(java.util.Set.of(Items.ENDER_PEARL)), "the mock player starts with an ender pearl, so this proves nothing");
        helper.assertTrue(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder refused to teleport without an ender pearl");
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "the player landed at " + player.position() + " instead of on the lodestone");
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == compass, "the echo compass was used up");
        helper.assertValueEqual(compass.getDamageValue(), EchoCompassItem.MAX_DAMAGE, "echo compass damage after one jump");
        helper.assertFalse(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass ignored its cooldown");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "pearls spent by a jump refused for cooldown");
        TestCleanup.succeed(helper);
    }

    /**
     * Unbreaking wirkt auf den Echo-Kompass wie auf jedes Werkzeug (Simple-Tweaks-Bug: das Datenpaket
     * zog die Haltbarkeit direkt ab): ein Sprung leert ihn mit Unbreaking III nur zu etwa einem Viertel
     * (im Mittel 375 von 1500, Streuung ~17), leer ist er trotzdem - er muss wieder aufgeladen werden.
     * Der Kompass liegt im Tag enchantable/durability, also kann man ihn auch verzaubern.
     */
    public static void unbreakingLowersHowMuchTheJumpEmptiesTheEchoCompass(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linkedEchoCompass(helper, lodestone);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 4));
        helper.assertTrue(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass refused to jump");
        int damage = compass.getDamageValue();
        helper.assertTrue(damage > 150 && damage < 600,
                "unbreaking III emptied the echo compass by " + damage + " of " + EchoCompassItem.MAX_DAMAGE + " instead of about a quarter");
        helper.assertTrue(EchoCompassItem.isCracked(compass), "the echo compass is still charged after a jump with unbreaking");
        helper.assertTrue(new ItemStack(TweaksItems.ECHO_COMPASS).is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE),
                "the echo compass is missing from enchantable/durability, so unbreaking and mending cannot be put on it");
        TestCleanup.succeed(helper);
    }

    /**
     * Aufladen (Besitzer 2026-09-27): Benutzen startet eine Ladung von 3 s (60 Ticks); wer vorher
     * loslaesst, springt nicht und verliert nichts - keine Perle, kein Schaden, keine Abklingzeit.
     */
    public static void theEchoCompassChargesForThreeSecondsAndReleasingEarlyCostsNothing(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linkedEchoCompass(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        Vec3 start = player.position();
        helper.assertValueEqual(compass.getUseDuration(player), EchoCompassItem.CHARGE_TICKS, "charge ticks of a charged echo compass");
        helper.assertValueEqual(EchoCompassItem.CHARGE_TICKS, 60, "charge ticks (3 seconds)");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "using the echo compass did not start a charge");
        helper.assertTrue(player.isUsingItem(), "the echo compass does not charge");
        tickUse(player, EchoCompassItem.CHARGE_TICKS - 1);
        helper.assertTrue(player.isUsingItem(), "the charge ended before 3 seconds");
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the player jumped before the charge was full");
        player.releaseUsingItem();
        helper.assertFalse(player.isUsingItem(), "releasing did not stop the charge");
        tickUse(player, 5);
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "an early release still jumped");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls left after an early release");
        helper.assertValueEqual(compass.getDamageValue(), 0, "echo compass damage after an early release");
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "an early release put the echo compass on cooldown");
        TestCleanup.succeed(helper);
    }

    /**
     * Die volle Ladung springt: danach ist der Kompass leer (Schaden 1500), zeigt keinen Glanz mehr -
     * auch nicht mit Mending -, und die naechste Ladung dauert doppelt so lange.
     */
    public static void aFullChargeJumpsAndLeavesTheEchoCompassEmpty(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linkedEchoCompass(helper, lodestone);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        helper.assertTrue(compass.hasFoil(), "a charged echo compass has no glint");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "using the echo compass did not start a charge");
        tickUse(player, EchoCompassItem.CHARGE_TICKS);
        BlockPos abs = helper.absolutePos(lodestone);
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "a full charge left the player at " + player.position());
        helper.assertFalse(player.isUsingItem(), "the charge did not end with the jump");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND) == compass && !compass.isEmpty(), "the first jump destroyed the echo compass");
        helper.assertValueEqual(compass.getDamageValue(), EchoCompassItem.MAX_DAMAGE, "echo compass damage after the first jump");
        helper.assertValueEqual(compass.getMaxDamage(), 1500, "repair points of an empty echo compass");
        helper.assertTrue(EchoCompassItem.isCracked(compass), "the empty echo compass is not cracked");
        helper.assertFalse(compass.hasFoil(), "the empty echo compass still has a glint");
        helper.assertValueEqual(compass.getUseDuration(player), EchoCompassItem.CRACKED_CHARGE_TICKS, "charge ticks of an empty echo compass");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls spent by the jump (none are needed any more)");
        TestCleanup.succeed(helper);
    }

    /**
     * Wieder aufladen: erst nach allen 1500 Reparaturpunkten (Mending: 2 je XP-Punkt, also 750 XP) ist
     * der Kompass wieder normal benutzbar und glaenzt; 1498 Punkte reichen nicht. Am Amboss repariert
     * eine Echoscherbe.
     */
    public static void theEchoCompassIsOnlyChargedAgainAfterFifteenHundredRepairPoints(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
        compass.setDamageValue(EchoCompassItem.MAX_DAMAGE);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        Vec3 at = player.position();
        player.takeXpDelay = 0;
        new ExperienceOrb(helper.getLevel(), at.x, at.y, at.z, 749).playerTouch(player);
        helper.assertValueEqual(compass.getDamageValue(), 2, "damage left after 749 XP of mending");
        helper.assertTrue(EchoCompassItem.isCracked(compass), "the echo compass counts as repaired with 2 points missing");
        helper.assertFalse(compass.hasFoil(), "the not fully repaired echo compass has a glint");
        helper.assertValueEqual(compass.getUseDuration(player), EchoCompassItem.CRACKED_CHARGE_TICKS, "charge ticks with 2 points missing");
        player.takeXpDelay = 0;
        new ExperienceOrb(helper.getLevel(), at.x, at.y, at.z, 1).playerTouch(player);
        helper.assertValueEqual(compass.getDamageValue(), 0, "damage after 750 XP of mending");
        helper.assertFalse(EchoCompassItem.isCracked(compass), "the fully repaired echo compass is still cracked");
        helper.assertTrue(compass.hasFoil(), "the fully repaired echo compass has no glint");
        helper.assertValueEqual(compass.getUseDuration(player), EchoCompassItem.CHARGE_TICKS, "charge ticks after the full repair");
        helper.assertTrue(compass.isValidRepairItem(new ItemStack(Items.ECHO_SHARD)), "echo shards do not repair the echo compass at the anvil");
        TestCleanup.succeed(helper);
    }

    /**
     * Den nicht voll reparierten Kompass zu benutzen provoziert den Bruch: die Ladung dauert doppelt so
     * lange (6 s - nach 3 s passiert noch nichts), der Sprung gelingt, danach ist der Kompass zerstoert.
     */
    public static void aCrackedEchoCompassChargesTwiceAsLongAndShattersAfterTheJump(GameTestHelper helper) {
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linkedEchoCompass(helper, lodestone);
        compass.setDamageValue(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        Vec3 start = player.position();
        helper.assertValueEqual(EchoCompassItem.CRACKED_CHARGE_TICKS, 2 * EchoCompassItem.CHARGE_TICKS, "cracked charge ticks");
        helper.assertTrue(compass.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the cracked echo compass does not charge");
        tickUse(player, EchoCompassItem.CHARGE_TICKS);
        helper.assertTrue(player.isUsingItem(), "the cracked echo compass stopped charging after 3 seconds");
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the cracked echo compass jumped after 3 seconds");
        tickUse(player, EchoCompassItem.CHARGE_TICKS - 1);
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "the cracked echo compass jumped a tick early");
        tickUse(player, 1);
        BlockPos abs = helper.absolutePos(lodestone);
        helper.assertTrue(player.position().distanceTo(Vec3.atBottomCenterOf(abs.above())) < 0.1, "the cracked echo compass left the player at " + player.position());
        helper.assertTrue(compass.isEmpty(), "the cracked echo compass survived its jump");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "something is left in the hand after the echo compass shattered");
        helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls spent by the shattering jump (none are needed any more)");
        TestCleanup.succeed(helper);
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
        TweaksConfig.Spawn fresh = new TweaksConfig().spawn;
        helper.assertValueEqual(fresh.firstJoinTeleporterCount, 0, "default spawn teleporters on the first join");
        helper.assertValueEqual(fresh.firstJoinElytraPadCount, 0, "default elytra pads on the first join");
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        TweaksConfig.Pads families = SimpleTweaks.config().pads;
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
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), 0, "spawn teleporters given with the default config");
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.ELYTRA_PAD.asItem()), 0, "elytra pads given with the default config");
            helper.assertTrue(player.getTags().contains("simpletweaks.first_join"), "a first join with nothing to give did not set the first-join tag");

            player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
            spawn.firstJoinTeleporterCount = 2;
            spawn.firstJoinElytraPadCount = 3;
            SpawnSetup.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), 2, "spawn teleporters given on the first join");
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.ELYTRA_PAD.asItem()), 3, "elytra pads given on the first join");
            helper.assertTrue(player.getTags().contains("simpletweaks.first_join"), "the first-join tag is not the one Simple Tweaks used");
            SpawnSetup.onPlayerJoin(player);
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), 2, "spawn teleporters after a second join");
            helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.ELYTRA_PAD.asItem()), 3, "elytra pads after a second join");

            // Abgeschaltete Familie: dieses Geschenk faellt weg, das andere bleibt.
            for (boolean teleportersEnabled : new boolean[] {false, true}) {
                player.getInventory().clearContent();
                player.removeTag(SpawnSetup.FIRST_JOIN_TAG);
                families.enableSpawnTeleporters = teleportersEnabled;
                families.enableElytraPads = !teleportersEnabled;
                SpawnSetup.onPlayerJoin(player);
                helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.SPAWN_TELEPORTER.asItem()), teleportersEnabled ? 2 : 0,
                        "spawn teleporters given with spawn teleporters " + (teleportersEnabled ? "enabled" : "disabled"));
                helper.assertValueEqual(player.getInventory().countItem(TweaksBlocks.ELYTRA_PAD.asItem()), teleportersEnabled ? 0 : 3,
                        "elytra pads given with elytra pads " + (teleportersEnabled ? "disabled" : "enabled"));
            }
            families.enableSpawnTeleporters = true;
            families.enableElytraPads = true;

            // Alte Config-Datei: ein geaenderter Wert gilt fuer beide, der alte Standard 1 wird 0.
            TweaksConfig.Spawn legacy = new TweaksConfig().spawn;
            legacy.spawnTeleporterCount = 5;
            helper.assertTrue(legacy.migrateLegacyFirstJoinCount(), "an old spawnTeleporterCount was not seen");
            helper.assertValueEqual(legacy.firstJoinTeleporterCount, 5, "teleporters taken over from an old spawnTeleporterCount");
            helper.assertValueEqual(legacy.firstJoinElytraPadCount, 5, "elytra pads taken over from an old spawnTeleporterCount");
            helper.assertTrue(legacy.spawnTeleporterCount == null, "the old key is kept and would be saved again");
            TweaksConfig.Spawn oldDefault = new TweaksConfig().spawn;
            oldDefault.spawnTeleporterCount = 1;
            oldDefault.migrateLegacyFirstJoinCount();
            helper.assertValueEqual(oldDefault.firstJoinTeleporterCount + oldDefault.firstJoinElytraPadCount, 0,
                    "the old default 1 still hands out first-join gifts");
            helper.assertFalse(new TweaksConfig().spawn.migrateLegacyFirstJoinCount(), "a new config claims to carry an old key");
        } finally {
            spawn.firstJoinTeleporterCount = teleporters;
            spawn.firstJoinElytraPadCount = pads;
            families.enableSpawnTeleporters = teleportersOn;
            families.enableElytraPads = elytraPadsOn;
        }
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
                "spawn.zCoordSpawnPoint=0", "spawn.firstJoinTeleporterCount=0", "spawn.firstJoinElytraPadCount=0",
                "spawn.spawnTeleporterCount=null", "spawn.giveElytraOnSpawn=false",
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
        helper.setBlock(pad, TweaksBlocks.ELYTRA_PAD);
        ServerPlayer player = mockPlayer(helper, new Vec3(3.5, 2.0, 3.5));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ElytraPadBlockEntity.applyArea(helper.getLevel(), helper.absolutePos(pad), helper.getBlockState(pad));
        ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertTrue(elytra.is(TweaksItems.SPAWN_ELYTRA), "the elytra pad gave no spawn elytra to test with");

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
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(TweaksItems.SPAWN_ELYTRA), "the worn spawn elytra was cleaned up too");
        TestCleanup.succeed(helper);
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
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
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
        TestCleanup.succeed(helper);
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
            level.setBlock(enderite, TweaksBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity big = (ChunkLoaderBlockEntity) level.getBlockEntity(enderite);
            big.update(level);
            Set<Long> bigOwn = new java.util.HashSet<>(big.ownForced());
            helper.assertTrue(bigOwn.contains(plainChunk), "the enderite loader does not own its own chunk");

            level.setBlock(plain, TweaksBlocks.CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
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
        TestCleanup.succeed(helper);
    }

    /**
     * #32: Zwei ueberlappende Flypads - faellt eines weg, fliegt der Spieler weiter, solange das andere
     * ihn abdeckt; und ein Pad nimmt nur Flug zurueck, den ein Flypad gab (nicht den eines anderen Mods).
     */
    public static void overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 1, 1);
        BlockPos second = new BlockPos(5, 1, 5);
        helper.setBlock(first, TweaksBlocks.FLYPAD);
        helper.setBlock(second, TweaksBlocks.FLYPAD);
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
        TestCleanup.succeed(helper);
    }

    /**
     * #17: Das Laser-Relay ersetzt die UUID im Paket durch die des Absenders, schickt nichts ohne
     * Laserpointer in Benutzung oder mit abgeschaltetem Laser, nur an Spieler in der Naehe und
     * hoechstens {@link TweaksNetwork#LASER_PACKETS_PER_SECOND} Pakete je Sekunde.
     */
    public static void theLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers(GameTestHelper helper) {
        ServerPlayer sender = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ServerPlayer near = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        ServerPlayer far = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        far.snapTo(far.getX() + 600, far.getY(), far.getZ());
        Vec3 dotPos = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        UUID spoofed = UUID.randomUUID();
        LaserPayload payload = new LaserPayload(spoofed, (float) dotPos.x, (float) dotPos.y, (float) dotPos.z, true);
        List<ServerPlayer> receivers = new ArrayList<>();
        List<LaserPayload> relayed = new ArrayList<>();
        TweaksNetwork.PlayerSender capture = (player, sent) -> {
            receivers.add(player);
            relayed.add((LaserPayload) sent);
        };

        helper.assertValueEqual(TweaksNetwork.relayLaser(payload, sender, capture), 0, "players reached without a laser pointer in use");
        sender.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TweaksItems.LASER_POINTER));
        sender.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(sender.isUsingItem(), "the probe player does not use the laser pointer");

        boolean enabled = SimpleTweaks.config().laserPointer.enable;
        try {
            SimpleTweaks.config().laserPointer.enable = false;
            helper.assertValueEqual(TweaksNetwork.relayLaser(payload, sender, capture), 0, "players reached with the laser switched off on the server");
        } finally {
            SimpleTweaks.config().laserPointer.enable = enabled;
        }
        helper.assertTrue(relayed.isEmpty(), "something was relayed before the checks passed");

        TweaksNetwork.relayLaser(payload, sender, capture);
        helper.assertTrue(receivers.contains(near), "the nearby player did not get the laser dot");
        helper.assertFalse(receivers.contains(far), "a player 600 blocks away got the laser dot");
        helper.assertFalse(receivers.contains(sender), "the sender got its own dot back");
        helper.assertValueEqual(relayed.get(0).player(), sender.getUUID(), "player id in the relayed packet");

        Vec3 outOfRange = sender.getEyePosition().add(0, 0, SimpleTweaks.config().laserPointer.range + 50);
        relayed.clear();
        TweaksNetwork.relayLaser(new LaserPayload(sender.getUUID(), (float) outOfRange.x, (float) outOfRange.y, (float) outOfRange.z, true), sender, capture);
        helper.assertTrue(relayed.isEmpty(), "a dot beyond the laser range was relayed");

        int accepted = 0;
        for (int i = 0; i < 40; i++) {
            if (TweaksNetwork.relayLaser(payload, sender, capture) > 0) {
                accepted++;
            }
        }
        helper.assertTrue(accepted <= TweaksNetwork.LASER_PACKETS_PER_SECOND, accepted + " laser packets relayed within one tick");
        sender.stopUsingItem();
        TestCleanup.succeed(helper);
    }

    /**
     * #16: Beim Einloggen und nach jedem Tweaks-Befehl bekommt der Client die clientrelevanten Werte
     * (Raketen-Stapel, Boosts, Laser); der Server-Thread selbst liest nie die gemeldeten Werte.
     */
    public static void theClientRelevantTweaksValuesAreSentAtLoginAndOnEveryChange(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        int rockets = SimpleTweaks.config().balancing.rocketStackSize;
        TweaksNetwork.PlayerSender original = TweaksNetwork.playerSender();
        List<TweaksConfigPayload> sent = new ArrayList<>();
        try {
            TweaksNetwork.setPlayerSender((to, payload) -> {
                if (to == player && payload instanceof TweaksConfigPayload config) {
                    sent.add(config);
                }
            });
            TweaksContent.onPlayerJoin(player);
            helper.assertValueEqual(sent.size(), 1, "config packets sent at login");
            helper.assertValueEqual(sent.get(0).values(), SimpleTweaks.localValues(), "values sent at login");

            players.op(player.nameAndId());
            helper.getLevel().getServer().getCommands().getDispatcher()
                    .execute("simplebuilding tweaks balancing rocketStackSize 16", player.createCommandSourceStack().withSuppressedOutput());
            helper.assertValueEqual(sent.size(), 2, "config packets sent after the command");
            helper.assertValueEqual(sent.get(1).rocketStackSize(), 16, "rocket stack size sent after the command");
            helper.assertValueEqual(sent.get(1).maxBoosts(), SimpleTweaks.config().spawn.boostCount(), "boosts sent after the command");

            SimpleTweaks.setServerValues(new SimpleTweaks.ServerValues(3, 1, false, 1));
            helper.assertValueEqual(new ItemStack(Items.FIREWORK_ROCKET).getMaxStackSize(), 16, "server-side rocket stack size while client values are stored");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("tweaks command failed: " + e.getMessage());
        } finally {
            SimpleTweaks.setServerValues(null);
            TweaksNetwork.setPlayerSender(original);
            SimpleTweaks.config().balancing.rocketStackSize = rockets;
            SimpleTweaks.saveConfig();
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        TestCleanup.succeed(helper);
    }

    /**
     * #33: Scheitert der Sprung (gesperrte Dimension), kostet der Echo-Kompass nichts - keine Perle,
     * keine Haltbarkeit, keine Abklingzeit.
     */
    public static void aBlockedEchoCompassJumpCostsNothing(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has no nether");
        BlockPos lodestone = new BlockPos(0, 100, 0);
        BlockState before = nether.getBlockState(lodestone);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(Level.NETHER, lodestone)), true));
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        player.getInventory().setItem(8, new ItemStack(Items.ENDER_PEARL, 2));
        boolean allowNether = SimpleTweaks.config().dimensions.allowNether;
        try {
            nether.setBlock(lodestone, Blocks.LODESTONE.defaultBlockState(), Block.UPDATE_ALL);
            SimpleTweaks.config().dimensions.allowNether = false;
            helper.assertFalse(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo compass reports a jump into a locked nether");
            helper.assertTrue(player.level() == helper.getLevel(), "the player got into the locked nether");
            helper.assertValueEqual(player.getInventory().getItem(8).getCount(), 2, "ender pearls left after a blocked jump");
            helper.assertValueEqual(compass.getDamageValue(), 0, "echo compass damage after a blocked jump");
            helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "a blocked jump put the echo compass on cooldown");
        } finally {
            SimpleTweaks.config().dimensions.allowNether = allowNether;
            nether.setBlock(lodestone, before, Block.UPDATE_ALL);
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Amethystlinse (Id weiter laser_pointer): Redstone/Amethyst/Redstone, Eisen/Eisen-Baukern/Eisen,
     * drei Eisen - kein Glas mehr. Die Haltbarkeit ist die Ladung.
     */
    public static void theAmethystLensIsCraftedAroundAnIronCore(GameTestHelper helper) {
        Item i = Items.IRON_INGOT;
        Item r = Items.REDSTONE;
        CraftingInput grid = grid(r, Items.AMETHYST_SHARD, r, i, ModItems.IRON_CORE, i, i, i, i);
        expectCrafting(helper, grid, TweaksItems.LASER_POINTER, "simplebuilding:laser_pointer");
        Optional<ItemStack> oldPattern = craftingResult(helper, grid(null, Items.AMETHYST_SHARD, null, i, Items.GLASS, i, i, r, i));
        helper.assertTrue(oldPattern.isEmpty() || !oldPattern.get().is(TweaksItems.LASER_POINTER), "the old glass pattern still makes the lens");
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        helper.assertTrue(lens.isDamageableItem() && lens.getMaxDamage() == LaserPointerItem.MAX_CHARGE,
                "the lens has no charge of " + LaserPointerItem.MAX_CHARGE + " (max damage " + lens.getMaxDamage() + ")");
        helper.assertTrue(LaserPointerItem.CHARGE_PER_REDSTONE * 64 == LaserPointerItem.MAX_CHARGE, "64 redstone are not exactly one full charge");
        TestCleanup.succeed(helper);
    }

    /** Eis wird Wasser, Packeis Eis, Blaueis Packeis; Schneeschicht und Schneeblock schmelzen weg - erst nach der Verweildauer. */
    public static void theLensBeamMeltsIceAndSnow(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        BlockPos target = new BlockPos(2, 2, 2);
        helper.setBlock(target.below(), Blocks.STONE);
        Block[][] steps = {
                {Blocks.ICE, Blocks.WATER}, {Blocks.PACKED_ICE, Blocks.ICE}, {Blocks.BLUE_ICE, Blocks.PACKED_ICE},
                {Blocks.SNOW_BLOCK, Blocks.AIR}};
        for (Block[] step : steps) {
            helper.setBlock(target, step[0]);
            beam(helper, player, lens, target, Direction.UP, LaserBeam.MELT_TICKS - 1);
            helper.assertTrue(helper.getBlockState(target).is(step[0]), step[0] + " melted before the dwell time");
            beam(helper, player, lens, target, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(target).is(step[1]), step[0] + " became " + helper.getBlockState(target) + " instead of " + step[1]);
            helper.setBlock(target, Blocks.AIR);
        }
        helper.setBlock(target, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 3));
        beam(helper, player, lens, target, Direction.UP, LaserBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(target).isAir(), "a snow layer did not melt away");
        TestCleanup.succeed(helper);
    }

    /** Brennbares (Bretter oben, Stamm seitlich) faengt erst nach der Verweildauer Feuer, auf der angestrahlten Seite; Stein nie. */
    public static void theLensBeamIgnitesFlammableBlocksOnlyAfterDwelling(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(1, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            beam(helper, player, lens, planks, Direction.UP, LaserBeam.IGNITE_TICKS - 1);
            helper.assertTrue(helper.getBlockState(planks.above()).isAir(), "the planks caught fire before the dwell time");
            beam(helper, player, lens, planks, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(planks.above()).is(Blocks.FIRE), "the planks did not catch fire on top after the dwell time");
            helper.setBlock(planks.above(), Blocks.AIR);

            BlockPos wool = new BlockPos(3, 2, 2);
            helper.setBlock(wool, Blocks.OAK_LOG);
            beam(helper, player, lens, wool, Direction.SOUTH, LaserBeam.IGNITE_TICKS);
            helper.assertTrue(helper.getBlockState(wool.south()).is(Blocks.FIRE), "the log did not catch fire on the beamed side");
            helper.setBlock(wool.south(), Blocks.AIR);

            BlockPos stone = new BlockPos(2, 2, 4);
            helper.setBlock(stone, Blocks.STONE);
            beam(helper, player, lens, stone, Direction.UP, LaserBeam.IGNITE_TICKS * 3);
            helper.assertTrue(helper.getBlockState(stone.above()).isAir(), "stone caught fire from the beam");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        TestCleanup.succeed(helper);
    }

    /** Seelensand bekommt oben Seelenfeuer; Lagerfeuer, Seelenlagerfeuer und Kerzen gehen an. */
    public static void theLensBeamLightsSoulFireCampfiresAndCandles(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        BlockPos soul = new BlockPos(1, 2, 1);
        helper.setBlock(soul, Blocks.SOUL_SAND);
        beam(helper, player, lens, soul, Direction.UP, LaserBeam.SOUL_FIRE_TICKS - 1);
        helper.assertTrue(helper.getBlockState(soul.above()).isAir(), "soul fire appeared before the dwell time");
        beam(helper, player, lens, soul, Direction.UP, 1);
        helper.assertTrue(helper.getBlockState(soul.above()).is(Blocks.SOUL_FIRE), "the soul sand got " + helper.getBlockState(soul.above()) + " instead of soul fire");
        helper.setBlock(soul.above(), Blocks.AIR);

        BlockPos floor = new BlockPos(3, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        BlockState[] unlit = {
                Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false),
                Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false),
                Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, false)};
        for (BlockState state : unlit) {
            helper.setBlock(floor.above(), state);
            beam(helper, player, lens, floor.above(), Direction.UP, LaserBeam.LIGHT_TICKS);
            helper.assertTrue(helper.getBlockState(floor.above()).getValue(BlockStateProperties.LIT), state.getBlock() + " was not lit by the beam");
        }
        helper.setBlock(floor.above(), Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    /**
     * Anders als ein Feuerzeug fuellt der Strahl nie einen leeren Portalrahmen (Bretter hinter dem
     * Rahmen, Feuerplatz im Rahmen) - auch nicht, seit er TNT zuenden darf.
     */
    public static void theLensBeamNeverLightsNetherPortals(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(5.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            // Rahmen in der X-Ebene z = 3: innen x 1..2, y 2..4.
            int z = 3;
            for (int x = 1; x <= 2; x++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(x, 5, z), Blocks.OBSIDIAN);
            }
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(0, y, z), Blocks.OBSIDIAN);
                helper.setBlock(new BlockPos(3, y, z), Blocks.OBSIDIAN);
            }
            BlockPos planks = new BlockPos(1, 2, z - 1);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            BlockPos inside = new BlockPos(1, 2, z);
            helper.assertTrue(net.minecraft.world.level.portal.PortalShape.findEmptyPortalShape(helper.getLevel(), helper.absolutePos(inside), Direction.Axis.X).isPresent(),
                    "the test frame is no valid portal frame, so this test proves nothing");
            beam(helper, player, lens, planks, Direction.SOUTH, LaserBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(inside).isAir(), "the beam put " + helper.getBlockState(inside) + " into an empty portal frame");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        TestCleanup.succeed(helper);
    }

    /**
     * TNT (Besitzer 2026-09-27): nach derselben Verweildauer wie beim Anzuenden wird es gezuendet -
     * der Block wird zu gezuendetem TNT mit dem Spieler als Zuender, die Ladung sinkt um die
     * Wirkungskosten. Nicht im Abenteuermodus, nicht mit der Spielregel tnt_explodes aus (dann kostet
     * es auch nichts).
     */
    public static void theLensBeamPrimesTntAfterDwellingButRespectsTheRules(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(5.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        var rules = helper.getLevel().getGameRules();
        boolean tntExplodes = rules.get(GameRules.TNT_EXPLODES);
        try {
            rules.set(GameRules.TNT_EXPLODES, true, helper.getLevel().getServer());
            BlockPos tnt = new BlockPos(3, 2, 2);
            helper.setBlock(tnt.below(), Blocks.STONE);
            helper.setBlock(tnt, Blocks.TNT);
            beam(helper, player, lens, tnt, Direction.UP, LaserBeam.IGNITE_TICKS - 1);
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the TNT was primed before the dwell time");
            beam(helper, player, lens, tnt, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(tnt).isAir(), "the beam did not prime the TNT after the dwell time: " + helper.getBlockState(tnt));
            List<PrimedTnt> primed = helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(tnt)).inflate(1.0));
            helper.assertValueEqual(primed.size(), 1, "primed TNT entities after the beam");
            PrimedTnt fuse = primed.get(0);
            helper.assertTrue(fuse.getOwner() == player, "the primed TNT does not name the beaming player as its igniter: " + fuse.getOwner());
            fuse.discard();
            helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.EFFECT_COST, "charge spent on priming the TNT");

            helper.setBlock(tnt, Blocks.TNT);
            player.getAbilities().mayBuild = false;
            beam(helper, player, lens, tnt, Direction.UP, LaserBeam.IGNITE_TICKS * 2);
            player.getAbilities().mayBuild = true;
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the beam primed TNT for a player who may not build");

            rules.set(GameRules.TNT_EXPLODES, false, helper.getLevel().getServer());
            beam(helper, player, lens, tnt, Direction.UP, LaserBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(tnt).is(Blocks.TNT), "the beam primed TNT with tnt_explodes switched off");
            helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.EFFECT_COST, "charge spent while TNT was not allowed to explode");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(tnt)).inflate(1.0)).isEmpty(),
                    "primed TNT appeared although the block stayed");
            helper.setBlock(tnt, Blocks.AIR);
        } finally {
            rules.set(GameRules.TNT_EXPLODES, tntExplodes, helper.getLevel().getServer());
            for (PrimedTnt left : helper.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(helper.absolutePos(new BlockPos(3, 2, 2))).inflate(2.0))) {
                left.discard();
            }
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Jede Nutzung kostet Ladung (Besitzer 2026-09-27): schon der erste Tick zieht
     * {@link LaserPointerItem#BEAM_COST} ab - frueher erst nach 20 Ticks, kurzes Antippen war gratis -,
     * danach je angefangene Sekunde, auch wenn der Strahl ins Leere zeigt oder auf einem Block ohne
     * Wirkung ruht. Die Reichweite wird dafuer auf {@value #SHORT_LASER_RANGE} Bloecke gesenkt, damit
     * "ins Leere" nicht von dem abhaengt, was ueber der Teststruktur steht.
     */
    public static void theLensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper helper) {
        int range = SimpleTweaks.config().laserPointer.range;
        try {
            SimpleTweaks.config().laserPointer.range = SHORT_LASER_RANGE;
            lensDrainsChargeEvenWhenItPointsIntoTheAir(helper);
        } finally {
            SimpleTweaks.config().laserPointer.range = range;
        }
    }

    private static final int SHORT_LASER_RANGE = 2;

    /**
     * Messen mit der Amethystlinse (Besitzer 2026-09-28): ohne Verzauberung schreibt die Linse nie
     * eine Messung; mit Berührung des Konstrukteurs steht die letzte Messung
     * ({@link com.simplebuilding.component.LensMeasurement}: Entfernung Auge -> Trefferpunkt auf
     * 0,1 gerundet, Hoehenunterschied, Zielblock) schon nach dem ersten Tick in der Linse und wird
     * beim Loslassen auf das neue Ziel aktualisiert.
     */
    public static void theLensRecordsTheLastMeasurementOnlyWithConstructorsTouch(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        faceNorth(player);
        BlockPos stone = new BlockPos(2, 3, 3);
        BlockPos nearer = new BlockPos(2, 3, 4);
        helper.setBlock(stone, Blocks.STONE);
        helper.assertTrue(player.pick(8.0, 1.0f, false) instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                "the player does not look at the stone block");
        double stoneDistance = player.pick(8.0, 1.0f, false).getLocation().distanceTo(player.getEyePosition());
        var component = com.simplebuilding.component.ModDataComponentTypes.LENS_MEASUREMENT;

        ItemStack plain = new ItemStack(TweaksItems.LASER_POINTER);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        helper.assertTrue(plain.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the plain lens could not be used");
        tickUse(player, 21);
        player.releaseUsingItem();
        helper.assertFalse(LaserPointerItem.measures(plain, helper.getLevel()), "a plain lens counts as measuring");
        helper.assertTrue(!plain.has(component), "a lens without Constructor's Touch stored a measurement: " + plain.get(component));

        ItemStack touched = new ItemStack(TweaksItems.LASER_POINTER);
        touched.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(com.simplebuilding.enchantment.ModEnchantments.CONSTRUCTORS_TOUCH), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, touched);
        helper.assertTrue(LaserPointerItem.measures(touched, helper.getLevel()), "a lens with Constructor's Touch does not count as measuring");
        helper.assertTrue(touched.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the enchanted lens could not be used");
        tickUse(player, 1);
        com.simplebuilding.component.LensMeasurement first = touched.get(component);
        helper.assertTrue(first != null, "the enchanted lens stored no measurement after the first tick");
        helper.assertTrue(Math.abs(first.distance() - com.simplebuilding.component.LensMeasurement.round(stoneDistance)) < 0.001f,
                "measured " + first.distance() + " blocks to the stone instead of " + stoneDistance);
        helper.assertValueEqual(first.heightDifference(), 1, "height difference to the stone one block above the feet");
        helper.assertValueEqual(first.target(), Blocks.STONE.getDescriptionId(), "measured target");

        // Neues Ziel einen Block naeher: das Loslassen misst noch einmal.
        helper.setBlock(nearer, Blocks.COBBLESTONE);
        player.releaseUsingItem();
        com.simplebuilding.component.LensMeasurement last = touched.get(component);
        helper.assertTrue(last != null && last.target().equals(Blocks.COBBLESTONE.getDescriptionId()),
                "releasing the lens did not record the new target: " + last);
        helper.assertTrue(Math.abs(last.distance() - com.simplebuilding.component.LensMeasurement.round(stoneDistance - 1.0)) < 0.001f,
                "measured " + last.distance() + " blocks to the nearer block instead of " + (stoneDistance - 1.0));
        helper.setBlock(nearer, Blocks.AIR);
        helper.setBlock(stone, Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    private static final net.minecraft.world.entity.EntityType<? extends Mob> PIG_TYPE = EntityType.PIG;

    private static void lensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        player.setXRot(-90.0f);
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        player.setItemInHand(InteractionHand.MAIN_HAND, lens);
        helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false).getType() == HitResult.Type.MISS,
                "something blocks the view straight up within " + SHORT_LASER_RANGE + " blocks, so the beam does not point into the air");
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(), "the lens could not be used");
        tickUse(player, 1);
        helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.BEAM_COST, "charge after one tick of pointing into the air");
        tickUse(player, 19);
        helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.BEAM_COST, "charge after the first second of pointing into the air");
        tickUse(player, 1);
        helper.assertValueEqual(lens.getDamageValue(), 2 * LaserPointerItem.BEAM_COST, "charge at the start of the second second");
        player.releaseUsingItem();

        // Kurzes Antippen: jede neue Nutzung kostet sofort.
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used again");
        tickUse(player, 3);
        player.releaseUsingItem();
        helper.assertValueEqual(lens.getDamageValue(), 3 * LaserPointerItem.BEAM_COST, "charge after a three tick tap");

        // Auf einem Block ohne Wirkung (Stein) kostet es genauso.
        BlockPos stone = new BlockPos(2, 3, 3);
        helper.setBlock(stone, Blocks.STONE);
        faceNorth(player);
        helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false) instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                "the player does not look at the stone block");
        helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used a third time");
        tickUse(player, 1);
        player.releaseUsingItem();
        helper.assertValueEqual(lens.getDamageValue(), 4 * LaserPointerItem.BEAM_COST, "charge after pointing at a stone block");
        helper.setBlock(stone, Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    /**
     * Verweildauer und Abstand (Besitzer 2026-09-27): bis 5 Bloecke die Basis, bei 10 Bloecken etwa
     * eine Sekunde mehr, bei 200 Bloecken rund 20 s (Anzuenden, Basis 3 s), stetig wachsend. In der
     * Welt schmilzt Eis aus 40 und aus 200 Bloecken erst nach der verlaengerten Verweildauer - auch
     * jenseits der alten Grenze von 24 Bloecken.
     */
    public static void theLensDwellTimeGrowsModeratelyWithDistance(GameTestHelper helper) {
        int base = LaserBeam.IGNITE_TICKS;
        helper.assertValueEqual(LaserBeam.dwellTicks(base, 1.0), base, "ignite dwell ticks right in front");
        helper.assertValueEqual(LaserBeam.dwellTicks(base, 5.0), base, "ignite dwell ticks at 5 blocks");
        int ten = LaserBeam.dwellTicks(base, 10.0);
        helper.assertTrue(ten >= base + 14 && ten <= base + 26, "10 blocks should add about one second to the 3 s ignite dwell, got " + ten + " ticks");
        int far = LaserBeam.dwellTicks(base, 200.0);
        helper.assertTrue(far >= 380 && far <= 420, "200 blocks should take about 20 s to ignite, got " + far + " ticks");
        int last = base;
        for (int d = 0; d <= 512; d += 4) {
            int ticks = LaserBeam.dwellTicks(base, d);
            helper.assertTrue(ticks >= last, "the dwell time shrinks between " + (d - 4) + " and " + d + " blocks");
            last = ticks;
        }

        BlockPos ice = new BlockPos(2, 2, 2);
        helper.setBlock(ice.below(), Blocks.STONE);
        for (double away : new double[]{40.0, 200.0}) {
            helper.setBlock(ice, Blocks.ICE);
            ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5 + away));
            ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
            int needed = LaserBeam.dwellTicks(LaserBeam.MELT_TICKS, player.getEyePosition().distanceTo(beamPoint(helper, ice, Direction.UP)));
            helper.assertTrue(needed > LaserBeam.MELT_TICKS, "no longer dwell at " + away + " blocks: " + needed);
            beam(helper, player, lens, ice, Direction.UP, needed - 1);
            helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "the ice melted before the " + needed + " tick dwell at " + away + " blocks");
            beam(helper, player, lens, ice, Direction.UP, 1);
            helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "the ice did not melt after the " + needed + " tick dwell at " + away + " blocks");
        }
        helper.setBlock(ice, Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    /**
     * Lebewesen fangen Feuer (Besitzer 2026-09-27), brauchen dafuer aber doppelt so lange wie ein
     * brennbarer Block im selben Abstand; das kostet die Wirkungsladung. Im Wasser nicht.
     */
    public static void theLensSetsLivingEntitiesOnFireTakingTwiceAsLong(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        Mob pig = helper.spawn(PIG_TYPE, new BlockPos(2, 2, 2));
        pig.setNoAi(true);
        try {
            EntityHitResult hit = new EntityHitResult(pig, pig.position().add(0.0, 0.4, 0.0));
            int block = LaserBeam.dwellTicks(LaserBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            beamEntity(player, lens, hit, block);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "the pig caught fire after the dwell time of a block, not twice that");
            beamEntity(player, lens, hit, block - 1);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "the pig caught fire a tick before twice the block dwell time");
            helper.assertValueEqual(lens.getDamageValue(), 0, "charge spent before the pig caught fire");
            beamEntity(player, lens, hit, 1);
            helper.assertTrue(pig.getRemainingFireTicks() > 0, "the pig did not catch fire after twice the block dwell time (" + (2 * block) + " ticks)");
            helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.EFFECT_COST, "charge spent on setting the pig on fire");

            pig.clearFire();
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
            beamEntity(player, lens, hit, 4 * block);
            helper.assertFalse(pig.getRemainingFireTicks() > 0, "a pig standing in water caught fire");
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        } finally {
            pig.discard();
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Spieler nur mit PvP (Besitzer 2026-09-27): mit der Spielregel pvp an brennt ein anderer
     * Spieler nach der doppelten Verweildauer, mit pvp aus nie; ein Spieler im Kreativmodus
     * (unverwundbar) nie.
     */
    public static void theLensOnlyIgnitesPlayersWhenPvpAllowsIt(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ServerPlayer target = mockPlayer(helper, new Vec3(2.5, 2.0, 2.5));
        target.getAbilities().invulnerable = false;
        target.getAbilities().instabuild = false;
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        var rules = helper.getLevel().getGameRules();
        boolean pvp = rules.get(GameRules.PVP);
        try {
            EntityHitResult hit = new EntityHitResult(target, target.position().add(0.0, 1.0, 0.0));
            int needed = LaserBeam.ENTITY_DWELL_FACTOR * LaserBeam.dwellTicks(LaserBeam.IGNITE_TICKS, player.getEyePosition().distanceTo(hit.getLocation()));
            rules.set(GameRules.PVP, true, helper.getLevel().getServer());
            beamEntity(player, lens, hit, needed);
            helper.assertTrue(target.getRemainingFireTicks() > 0, "with pvp on the other player did not catch fire, so the checks below prove nothing");
            target.clearFire();

            rules.set(GameRules.PVP, false, helper.getLevel().getServer());
            beamEntity(player, lens, hit, needed * 2);
            helper.assertFalse(target.getRemainingFireTicks() > 0, "the beam set a player on fire with pvp switched off");

            rules.set(GameRules.PVP, true, helper.getLevel().getServer());
            target.getAbilities().invulnerable = true;
            beamEntity(player, lens, hit, needed * 2);
            helper.assertFalse(target.getRemainingFireTicks() > 0, "the beam set an invulnerable (creative) player on fire");
        } finally {
            rules.set(GameRules.PVP, pvp, helper.getLevel().getServer());
            target.clearFire();
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Klaenge am Trefferpunkt (Besitzer 2026-09-27): ein leises Summen, sobald der Strahl irgendetwas
     * trifft, hoechstens einmal je {@link LaserBeam#HUM_PERIOD} Ticks, nicht ins Leere; waehrend Eis
     * schmilzt ein Zischen, waehrend Brennbares heiss wird ein Knistern, je hoechstens einmal je
     * {@link LaserBeam#HEAT_SOUND_PERIOD} Ticks. Gezaehlt ueber den Test-Haken der Linse.
     */
    public static void theLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating(GameTestHelper helper) {
        java.util.Map<LaserBeam.Sound, Integer> heard = new java.util.EnumMap<>(LaserBeam.Sound.class);
        int range = SimpleTweaks.config().laserPointer.range;
        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        LaserBeam.setSoundHook((sound, at) -> heard.merge(sound, 1, Integer::sum));
        try {
            SimpleTweaks.config().laserPointer.range = SHORT_LASER_RANGE;
            ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
            ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
            player.setItemInHand(InteractionHand.MAIN_HAND, lens);
            BlockPos stone = new BlockPos(2, 3, 3);
            helper.setBlock(stone, Blocks.STONE);
            faceNorth(player);
            helper.assertTrue(player.pick(SHORT_LASER_RANGE, 1.0f, false) instanceof BlockHitResult hit
                            && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(helper.absolutePos(stone)),
                    "the player does not look at the stone block");
            helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction() && player.isUsingItem(), "the lens could not be used");
            tickUse(player, 1);
            helper.assertValueEqual(lens.getDamageValue(), LaserPointerItem.BEAM_COST, "charge after the first tick (did the use tick run?)");
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.HUM, 0), 1, "hums after the first tick on stone");
            tickUse(player, LaserBeam.HUM_PERIOD - 1);
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.HUM, 0), 1, "hums within the first hum period (rate limit)");
            tickUse(player, 1);
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.HUM, 0), 2, "hums at the start of the second hum period");
            helper.assertTrue(!heard.containsKey(LaserBeam.Sound.SIZZLE) && !heard.containsKey(LaserBeam.Sound.CRACKLE),
                    "stone, which the beam cannot change, sizzled or crackled: " + heard);
            player.releaseUsingItem();
            player.setXRot(-90.0f);
            helper.assertTrue(lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the lens could not be used again");
            tickUse(player, LaserBeam.HUM_PERIOD * 2);
            player.releaseUsingItem();
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.HUM, 0), 2, "hums while pointing into the air");
            helper.setBlock(stone, Blocks.AIR);

            heard.clear();
            BlockPos ice = new BlockPos(1, 2, 2);
            helper.setBlock(ice, Blocks.ICE);
            int meltTicks = LaserBeam.MELT_TICKS - 1;
            beam(helper, player, lens, ice, Direction.UP, meltTicks);
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.SIZZLE, 0), (meltTicks - 1) / LaserBeam.HEAT_SOUND_PERIOD + 1, "sizzles while the ice heats up");
            helper.assertFalse(heard.containsKey(LaserBeam.Sound.CRACKLE), "melting ice crackled like fire");
            helper.setBlock(ice, Blocks.AIR);

            heard.clear();
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, -1, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(3, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            int igniteTicks = LaserBeam.IGNITE_TICKS - 1;
            beam(helper, player, lens, planks, Direction.UP, igniteTicks);
            helper.assertValueEqual(heard.getOrDefault(LaserBeam.Sound.CRACKLE, 0), (igniteTicks - 1) / LaserBeam.HEAT_SOUND_PERIOD + 1, "crackles while the planks heat up");
            helper.assertFalse(heard.containsKey(LaserBeam.Sound.SIZZLE), "heating planks sizzled like ice");
            helper.setBlock(planks, Blocks.AIR);
        } finally {
            LaserBeam.setSoundHook(null);
            SimpleTweaks.config().laserPointer.range = range;
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        TestCleanup.succeed(helper);
    }

    /** Die Zusatzwirkung: ein nasser Schwamm trocknet nach laengerem Strahlen. */
    public static void theLensBeamDriesWetSponges(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        BlockPos sponge = new BlockPos(2, 2, 2);
        helper.setBlock(sponge, Blocks.WET_SPONGE);
        beam(helper, player, lens, sponge, Direction.NORTH, LaserBeam.DRY_TICKS - 1);
        helper.assertTrue(helper.getBlockState(sponge).is(Blocks.WET_SPONGE), "the sponge dried before the dwell time");
        beam(helper, player, lens, sponge, Direction.NORTH, 1);
        helper.assertTrue(helper.getBlockState(sponge).is(Blocks.SPONGE), "the wet sponge did not dry");
        TestCleanup.succeed(helper);
    }

    /** Im Abenteuermodus wirkt der Strahl nicht; ohne Feuerausbreitung (Spielregel 0) zuendet er kein Brennbares. */
    public static void theLensBeamRespectsAdventureModeAndTheFireSpreadRule(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 2.0, 5.5));
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        BlockPos ice = new BlockPos(1, 2, 2);
        helper.setBlock(ice, Blocks.ICE);
        player.getAbilities().mayBuild = false;
        beam(helper, player, lens, ice, Direction.UP, LaserBeam.MELT_TICKS * 2);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "the beam melted ice for a player who may not build");
        player.getAbilities().mayBuild = true;

        var rules = helper.getLevel().getGameRules();
        int radius = rules.get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        try {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, helper.getLevel().getServer());
            BlockPos planks = new BlockPos(3, 2, 2);
            helper.setBlock(planks, Blocks.OAK_PLANKS);
            beam(helper, player, lens, planks, Direction.UP, LaserBeam.IGNITE_TICKS * 2);
            helper.assertTrue(helper.getBlockState(planks.above()).isAir(), "the beam lit planks with fire spread switched off");
        } finally {
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, radius, helper.getLevel().getServer());
        }
        TestCleanup.succeed(helper);
    }

    /**
     * Die Ladung sinkt (Strahlen je Sekunde, jede Wirkung mehr), aber die Linse zerbricht nie: leer
     * bleibt sie im Inventar, laesst sich nicht mehr benutzen und wirkt nicht mehr. Kreativ kostet nichts.
     */
    public static void theLensChargeRunsDownButTheLensNeverBreaks(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        LaserPointerItem item = (LaserPointerItem) TweaksItems.LASER_POINTER;
        ItemStack lens = new ItemStack(TweaksItems.LASER_POINTER);
        item.onUseTick(helper.getLevel(), player, lens, item.getUseDuration(lens, player) - 20);
        helper.assertTrue(lens.getDamageValue() == LaserPointerItem.BEAM_COST, "a second of beaming cost " + lens.getDamageValue() + " charge");

        lens.setDamageValue(LaserPointerItem.MAX_CHARGE - 2);
        BlockPos ice = new BlockPos(2, 2, 2);
        helper.setBlock(ice, Blocks.ICE);
        beam(helper, player, lens, ice, Direction.UP, LaserBeam.MELT_TICKS);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.WATER), "the last bit of charge did not melt the ice");
        helper.assertTrue(!lens.isEmpty() && lens.is(TweaksItems.LASER_POINTER) && lens.getDamageValue() == LaserPointerItem.MAX_CHARGE,
                "the lens broke or overshot instead of ending empty: " + lens + " damage " + lens.getDamageValue());
        helper.assertTrue(LaserPointerItem.isEmpty(lens), "a fully used lens does not count as empty");

        helper.setBlock(ice, Blocks.ICE);
        beam(helper, player, lens, ice, Direction.UP, LaserBeam.MELT_TICKS * 2);
        helper.assertTrue(helper.getBlockState(ice).is(Blocks.ICE), "an empty lens still melted ice");
        player.setItemInHand(InteractionHand.MAIN_HAND, lens);
        InteractionResult result = lens.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.isUsingItem(), "an empty lens could still be used (" + result + ")");
        LaserPointerItem.drain(player, lens, 50);
        helper.assertTrue(lens.getDamageValue() == LaserPointerItem.MAX_CHARGE && !lens.isEmpty(), "draining an empty lens broke it");

        ServerPlayer creative = mockPlayer(helper, new Vec3(4.5, 2.0, 5.5));
        ItemStack free = new ItemStack(TweaksItems.LASER_POINTER);
        LaserPointerItem.drain(creative, free, 50);
        helper.assertTrue(free.getDamageValue() == 0, "beaming cost charge in creative");
        helper.setBlock(ice, Blocks.AIR);
        TestCleanup.succeed(helper);
    }

    /** Aufladen im Amboss: Redstone, 0 Stufen, 64 Staub = voll, nur der noetige Teil eines Stapels wird verbraucht. */
    public static void anvilRechargeWithRedstoneCostsNoLevels(GameTestHelper helper) {
        ServerPlayer player = survivalLikePlayer(helper, new Vec3(2.5, 2.0, 5.5));
        player.experienceLevel = 0;

        ItemStack empty = new ItemStack(TweaksItems.LASER_POINTER);
        empty.setDamageValue(LaserPointerItem.MAX_CHARGE);
        net.minecraft.world.inventory.AnvilMenu full = recharge(helper, player, empty, 64);
        ItemStack out = full.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(out.is(TweaksItems.LASER_POINTER) && out.getDamageValue() == 0, "64 redstone did not fully charge an empty lens: " + out + " damage " + out.getDamageValue());
        helper.assertTrue(full.getCost() == 0, "recharging costs " + full.getCost() + " levels");
        takeResult(helper, player, full);
        helper.assertTrue(full.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).getItem().isEmpty(), "a full recharge left redstone behind");
        helper.assertTrue(player.experienceLevel == 0, "recharging changed the player's level");

        ItemStack partial = new ItemStack(TweaksItems.LASER_POINTER);
        partial.setDamageValue(25);
        net.minecraft.world.inventory.AnvilMenu topUp = recharge(helper, player, partial, 64);
        helper.assertTrue(topUp.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().getDamageValue() == 0, "a top-up did not fill the lens");
        takeResult(helper, player, topUp);
        int left = topUp.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).getItem().getCount();
        helper.assertTrue(left == 61, "topping up 25 charge used " + (64 - left) + " redstone instead of 3");

        ItemStack drained = new ItemStack(TweaksItems.LASER_POINTER);
        drained.setDamageValue(LaserPointerItem.MAX_CHARGE);
        net.minecraft.world.inventory.AnvilMenu some = recharge(helper, player, drained, 10);
        int damage = some.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().getDamageValue();
        helper.assertTrue(damage == LaserPointerItem.MAX_CHARGE - 10 * LaserPointerItem.CHARGE_PER_REDSTONE, "10 redstone charged to damage " + damage);

        ItemStack alreadyFull = new ItemStack(TweaksItems.LASER_POINTER);
        net.minecraft.world.inventory.AnvilMenu none = recharge(helper, player, alreadyFull, 5);
        helper.assertTrue(none.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT).getItem().isEmpty(), "the anvil offers to recharge a full lens");
        TestCleanup.succeed(helper);
    }

    private static net.minecraft.world.inventory.AnvilMenu recharge(GameTestHelper helper, ServerPlayer player, ItemStack lens, int redstone) {
        net.minecraft.world.inventory.AnvilMenu menu = new net.minecraft.world.inventory.AnvilMenu(1, player.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.INPUT_SLOT).set(lens);
        menu.getSlot(net.minecraft.world.inventory.AnvilMenu.ADDITIONAL_SLOT).set(new ItemStack(Items.REDSTONE, redstone));
        return menu;
    }

    private static void takeResult(GameTestHelper helper, ServerPlayer player, net.minecraft.world.inventory.AnvilMenu menu) {
        Slot result = menu.getSlot(net.minecraft.world.inventory.AnvilMenu.RESULT_SLOT);
        helper.assertTrue(result.mayPickup(player), "a level-0 survival player may not take the free recharge");
        result.onTake(player, result.getItem());
    }

    /** Laesst den Strahl {@code ticks} Mal auf die Mitte der Seite {@code face} von {@code relative} fallen. */
    private static void beam(GameTestHelper helper, ServerPlayer player, ItemStack lens, BlockPos relative, Direction face, int ticks) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(beamPoint(helper, relative, face), face, pos, false);
        for (int i = 0; i < ticks; i++) {
            LaserBeam.beamAt(player, lens, hit);
        }
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

    private static void beamEntity(ServerPlayer player, ItemStack lens, EntityHitResult hit, int ticks) {
        for (int i = 0; i < ticks; i++) {
            LaserBeam.beamAtEntity(player, lens, hit);
        }
    }

    /** #35: {@code /simplebuilding tweaks worldspawn set} setzt den Weltspawn sofort, nicht erst nach einem Neustart. */
    public static void theWorldSpawnCommandTakesEffectImmediately(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        var players = helper.getLevel().getServer().getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        LevelData.RespawnData savedSpawn = overworld.getRespawnData();
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        boolean custom = spawn.useCustomWorldSpawn;
        int[] saved = {spawn.xCoordSpawnPoint, spawn.yCoordSpawnPoint, spawn.zCoordSpawnPoint};
        // y fest, denn der Befehl nimmt nur -1..320 (die Testwelt liegt teils tiefer).
        BlockPos target = new BlockPos(savedSpawn.pos().getX() + 3, 70, savedSpawn.pos().getZ() - 2);
        try {
            players.op(player.nameAndId());
            helper.getLevel().getServer().getCommands().getDispatcher().execute(
                    "simplebuilding tweaks worldspawn set " + target.getX() + " " + target.getY() + " " + target.getZ(),
                    player.createCommandSourceStack().withSuppressedOutput());
            helper.assertValueEqual(overworld.getRespawnData().pos(), target, "world spawn right after the command");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("worldspawn command failed: " + e.getMessage());
        } finally {
            spawn.useCustomWorldSpawn = custom;
            spawn.xCoordSpawnPoint = saved[0];
            spawn.yCoordSpawnPoint = saved[1];
            spawn.zCoordSpawnPoint = saved[2];
            SimpleTweaks.saveConfig();
            overworld.setRespawnData(savedSpawn);
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        TestCleanup.succeed(helper);
    }

    /**
     * #40 (gegengeprueft, kein Fehler): {@code /killboats all} nimmt volle Kistenboote mit, der Inhalt
     * faellt aber heraus - {@code discard()} laesst ihn ueber AbstractChestBoat#remove fallen. Pinnt das.
     */
    public static void killBoatsAllDropsTheContentsOfChestBoats(GameTestHelper helper) {
        AABB box = helper.getBounds();
        var full = helper.spawn(EntityType.OAK_CHEST_BOAT, new BlockPos(2, 2, 2));
        full.setItem(0, new ItemStack(Items.DIAMOND, 7));
        helper.assertValueEqual(TweaksCommands.killBoats(helper.getLevel(), box, "all"), 1, "boats removed in all mode");
        int diamonds = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.getItem().is(Items.DIAMOND)) {
                diamonds += item.getItem().getCount();
                item.discard();
            }
        }
        helper.assertValueEqual(diamonds, 7, "diamonds dropped from the removed chest boat");
        TestCleanup.succeed(helper);
    }

    /**
     * #51: Flugzeit und Boosts sind gedeckelt (vorher lief {@code flightTimeSeconds * 20} ueber), und
     * ein abgebautes Launchpad laesst seine Windkugeln fallen.
     */
    public static void flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges(GameTestHelper helper) {
        TweaksConfig.Spawn huge = new TweaksConfig().spawn;
        huge.flightTimeSeconds = Integer.MAX_VALUE;
        huge.maxBoosts = Integer.MAX_VALUE;
        helper.assertValueEqual(huge.flightTicks(), TweaksConfig.Spawn.MAX_FLIGHT_SECONDS * 20, "flight ticks of an oversized flight time");
        helper.assertValueEqual(huge.boostCount(), TweaksConfig.Spawn.MAX_BOOSTS, "boosts of an oversized boost count");

        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, TweaksBlocks.LAUNCHPAD);
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
        TestCleanup.succeed(helper);
    }

    // =====================================================================================
    // Welle 19 (#35 Bett-Mitte, #51 Rest, N10, N11, N12, N16)
    // =====================================================================================

    /**
     * #35: forceExactSpawn stellt den Spieler auf die Mitte seines Betts. Die Bettposition kommt aus
     * der Respawn-Config - Vanilla liefert als Ziel die Aufstehposition neben dem Bett, und der
     * alte Code fragte, ob DORT ein Bett steht (fast nie).
     */
    public static void forcedExactRespawnPutsThePlayerOnTheBedCentre(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos foot = new BlockPos(2, 1, 2);
        BlockPos head = foot.east();
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.EAST);
        helper.setBlock(foot, bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        helper.setBlock(head, bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        BlockPos absHead = helper.absolutePos(head);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), absHead, 0.0F, 0.0F), false), false);
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        boolean exact = spawn.forceExactSpawn;
        try {
            Vec3 centre = new Vec3(absHead.getX() + 0.5, absHead.getY() + 0.5625, absHead.getZ() + 0.5);
            spawn.forceExactSpawn = true;
            // Vanillas Ziel: die Aufstehposition neben dem Bett - dort steht kein Bett.
            var beside = new net.minecraft.world.level.portal.TeleportTransition(level, Vec3.atBottomCenterOf(helper.absolutePos(head.north())), Vec3.ZERO,
                    0.0F, 0.0F, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
            var fromBeside = SpawnRules.exactRespawn(player, beside, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
            helper.assertTrue(fromBeside != null, "forceExactSpawn left a respawn beside the bed alone");
            helper.assertValueEqual(fromBeside.position(), centre, "respawn position from beside the bed with forceExactSpawn");
            // Der ganze Weg ueber ServerPlayer#findRespawnPositionAndUseSpawnBlock und das Mixin.
            var adjusted = player.findRespawnPositionAndUseSpawnBlock(false, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
            helper.assertTrue(adjusted.newLevel() == level, "the exact respawn left the bed's dimension");
            helper.assertValueEqual(adjusted.position(), centre, "respawn position with forceExactSpawn");
        } finally {
            spawn.forceExactSpawn = exact;
        }
        TestCleanup.succeed(helper);
    }

    /**
     * #51: Die Dimensionssperre haelt Portale und Gegenstaende auf, nicht einen Operator per
     * {@code /execute in ... run tp}; und wer im Portal steht, bekommt die Sperr-Meldung nicht jeden
     * Tick, sondern hoechstens alle {@link SpawnRules#LOCK_MESSAGE_COOLDOWN_TICKS} Ticks.
     */
    public static void commandTeleportsPassTheDimensionLockAndTheLockMessageWaits(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        var server = helper.getLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "the test server has no nether");
        TweaksConfig.Dimensions dims = SimpleTweaks.config().dimensions;
        boolean netherOn = dims.allowNether;
        Vec3 home = player.position();
        String key = "message.simplebuilding.dimension.nether_disabled";
        try {
            dims.allowNether = false;
            var portalTrip = new net.minecraft.world.level.portal.TeleportTransition(nether, new Vec3(home.x, 120.0, home.z), Vec3.ZERO, 0.0F, 0.0F,
                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
            helper.assertTrue(player.teleport(portalTrip) == null && player.level() == helper.getLevel(), "a locked nether let a portal traveller in");

            SpawnRules.forget(player);
            helper.assertTrue(SpawnRules.lockMessage(player, nether, key), "the first lock message was held back");
            helper.assertFalse(SpawnRules.lockMessage(player, nether, key), "the lock message repeats every tick while the player stands in the portal");

            // Konsole (volle Rechte), wie ein Operator: /execute as ... at @s in the_nether run tp @s ~ 120 ~
            server.getCommands().getDispatcher().execute("execute as " + player.getStringUUID() + " at @s in minecraft:the_nether run tp @s ~ 120 ~",
                    server.createCommandSourceStack().withSuppressedOutput());
            helper.assertTrue(player.level().dimension() == Level.NETHER, "the dimension lock stopped an operator's /execute in ... run tp");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("teleport command failed: " + e.getMessage());
        } finally {
            dims.allowNether = netherOn;
            if (player.level() != helper.getLevel()) {
                player.teleportTo(helper.getLevel(), home.x, home.y, home.z, Set.of(), 0.0F, 0.0F, false);
            }
            SpawnRules.forget(player);
        }
        TestCleanup.succeed(helper);
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
            level.setBlock(pos, TweaksBlocks.CHUNK_LOADER.defaultBlockState(), Block.UPDATE_ALL);
            ChunkLoaderBlockEntity plain = (ChunkLoaderBlockEntity) level.getBlockEntity(pos);
            plain.update(level);
            helper.assertTrue(plain.ownForced().contains(chunk) && isForced(level, chunk), "the loader did not force its own chunk");

            // /setblock: Flag 256 ueberspringt preRemoveSideEffects; der neue Loader hat noch nicht getickt.
            level.setBlock(pos, TweaksBlocks.ENDERITE_CHUNK_LOADER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
            helper.assertTrue(plain.isRemoved() && level.getBlockEntity(pos) != plain, "setblock kept the old loader's block entity");
            helper.assertTrue(plain.ownForced().isEmpty(), "the replaced loader still claims its chunks");
            helper.assertFalse(isForced(level, chunk), "a loader replaced by another loader type through /setblock keeps its chunk forced");
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        TestCleanup.succeed(helper);
    }

    /**
     * N11: Nur die Weltspawn-Befehle setzen den eigenen Weltspawn neu; jeder andere Tweaks-Befehl
     * ueberschrieb vorher ein zwischenzeitliches {@code /setworldspawn}.
     */
    public static void onlyWorldSpawnCommandsReapplyTheCustomWorldSpawn(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        var server = helper.getLevel().getServer();
        var players = server.getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        ServerLevel overworld = server.overworld();
        LevelData.RespawnData savedSpawn = overworld.getRespawnData();
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        boolean custom = spawn.useCustomWorldSpawn;
        int[] saved = {spawn.xCoordSpawnPoint, spawn.yCoordSpawnPoint, spawn.zCoordSpawnPoint};
        boolean killBoats = SimpleTweaks.config().commands.enableKillBoatsCommand;
        BlockPos configured = new BlockPos(savedSpawn.pos().getX() + 3, 70, savedSpawn.pos().getZ() - 2);
        BlockPos byVanilla = configured.offset(5, 0, 5);
        var dispatcher = server.getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withSuppressedOutput();
        try {
            players.op(player.nameAndId());
            spawn.useCustomWorldSpawn = true;
            spawn.xCoordSpawnPoint = configured.getX();
            spawn.yCoordSpawnPoint = configured.getY();
            spawn.zCoordSpawnPoint = configured.getZ();
            // wie /setworldspawn nach dem Einrichten des eigenen Weltspawns
            overworld.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, byVanilla, 0.0F, 0.0F));
            dispatcher.execute("simplebuilding tweaks commands enableKillBoats " + killBoats, source);
            helper.assertValueEqual(overworld.getRespawnData().pos(), byVanilla, "world spawn after an unrelated tweaks command");
            dispatcher.execute("simplebuilding tweaks worldspawn custom true", source);
            helper.assertValueEqual(overworld.getRespawnData().pos(), configured, "world spawn after worldspawn custom true");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            helper.fail("tweaks command failed: " + e.getMessage());
        } finally {
            spawn.useCustomWorldSpawn = custom;
            spawn.xCoordSpawnPoint = saved[0];
            spawn.yCoordSpawnPoint = saved[1];
            spawn.zCoordSpawnPoint = saved[2];
            SimpleTweaks.config().commands.enableKillBoatsCommand = killBoats;
            SimpleTweaks.saveConfig();
            overworld.setRespawnData(savedSpawn);
            if (!wasOp) {
                players.deop(player.nameAndId());
            }
        }
        TestCleanup.succeed(helper);
    }

    /**
     * N12: Traegt die geladene Datei noch den alten Geschenk-Schluessel, wird die Config gleich nach
     * der Migration einmal geschrieben (sobald der Loader seinen Speicherer setzt) - sonst lief die
     * Migration bei jedem Laden erneut.
     */
    public static void theFirstJoinKeyMigrationSavesTheConfigOnce(GameTestHelper helper) {
        Runnable installed = SimpleTweaks.configSaver();
        int[] saves = {0};
        Runnable counting = () -> saves[0]++;
        try {
            SimpleTweaks.setConfigSaver(counting);
            helper.assertValueEqual(saves[0], 0, "saves before any migration");
            com.simplebuilding.config.SimplebuildingConfig loaded = new com.simplebuilding.config.SimplebuildingConfig();
            loaded.tweaks.spawn.spawnTeleporterCount = 5;
            loaded.validatePostLoad();
            helper.assertTrue(loaded.tweaks.spawn.spawnTeleporterCount == null, "the old key survived the migration");
            helper.assertTrue(SimpleTweaks.configSaveRequested(), "the migration did not ask for a save");
            SimpleTweaks.setConfigSaver(counting);
            helper.assertValueEqual(saves[0], 1, "saves once the loader sets its saver after the migration");
            SimpleTweaks.setConfigSaver(counting);
            new com.simplebuilding.config.SimplebuildingConfig().validatePostLoad();
            SimpleTweaks.setConfigSaver(counting);
            helper.assertValueEqual(saves[0], 1, "saves after a load without the old key");
        } finally {
            SimpleTweaks.setConfigSaver(installed);
        }
        TestCleanup.succeed(helper);
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
        helper.assertFalse(player.getTags().contains(FlypadBlockEntity.FLIGHT_TAG), "a creative player kept the flypad flight tag");
        TestCleanup.succeed(helper);
    }

    private static boolean isForced(ServerLevel level, long key) {
        return level.getForceLoadedChunks().contains(ChunkPos.asLong((int) key, (int) (key >> 32)));
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

    /** Ein mit dem Leitstein an {@code lodestone} (relativ) verknuepfter Echo-Kompass. */
    private static ItemStack linkedEchoCompass(GameTestHelper helper, BlockPos lodestone) {
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
        return compass;
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
                .map(holder -> holder.value().assemble(grid, helper.getLevel().registryAccess()));
    }

    private static void expectCrafting(GameTestHelper helper, CraftingInput grid, Item result, String recipeId) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the documented pattern for " + result + " matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), recipeId, "recipe matched for " + result);
        helper.assertTrue(match.get().value().assemble(grid, helper.getLevel().registryAccess()).is(result), "the recipe for " + result + " made something else");
    }
}
