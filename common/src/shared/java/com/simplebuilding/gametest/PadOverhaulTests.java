package com.simplebuilding.gametest;

import com.mojang.authlib.GameProfile;
import com.simplebuilding.items.CreativeTabLayout;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.LaunchpadBlock;
import com.simplebuilding.tweaks.block.LegacyTierBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.tweaks.block.TweaksFamilies;
import com.simplebuilding.tweaks.block.entity.ElytraPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.OwnedBlockEntity;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.SpawnTeleporterBlockEntity;
import com.simplebuilding.tweaks.easter.EasterEggs;
import com.simplebuilding.tweaks.item.EchoCompassItem;
import com.simplebuilding.tweaks.item.TweaksItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Die Pad-Ueberarbeitung des Besitzers vom 2026-09-28 (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2.5):
 * Spawn-Teleporter mit drei Stufen (50/20/5 s, alte Stufen III/IV in Welt und Inventar umgebaut),
 * Stufe I jeder Pad-Familie im Schmiedetisch aus Familienplatte + Freischalt-Zutat, keine
 * Bildschirmtexte bei Pads und Geraeten, Echolot mit vierfacher Abklingzeit ohne Doppelverknuepfung.
 * Dazu die Entscheidungen vom 2026-09-29: Teleporter zum eigenen Spawn (Redstone: Weltspawn), Echolot
 * mit Sperre nach jedem Versuch, Vorschlaghammer ohne Aktionsleisten-Hinweis, sichtbare Pad-Zustaende.
 * Fabric-Adapter {@code PadOverhaulGameTest}, Test-IDs {@code simplebuilding:pad_overhaul_game_test_*}.
 */
public final class PadOverhaulTests {
    /** Budget fuer den Bildschirmtext-Test (Teleporter III: 5 s Stehen plus Abbruch davor). */
    public static final int NO_TEXT_MAX_TICKS = 300;
    /** Budget fuer den Wartezeit-Test (Stufe I darf nach Stufe III' 5 s noch nicht springen). */
    public static final int WAIT_MAX_TICKS = 200;
    /** Budget fuer den Trichter-Test (vier Windkugeln, eine je 8 Ticks, plus Luft). */
    public static final int HOPPER_MAX_TICKS = 200;
    /** Budget fuer den Teleporter-Ziel-Test (Stufe III: 5 s Stehen plus Luft). */
    public static final int DESTINATION_MAX_TICKS = 260;
    /** Budget fuer den Zustands-Test (Elytra-Pad je halbe Sekunde, Trank-Pad, Luft). */
    public static final int STATE_MAX_TICKS = 200;

    private PadOverhaulTests() {
    }

    // =====================================================================================
    // Spawn-Teleporter: drei Stufen
    // =====================================================================================

    /**
     * Drei Stufen, die sich nur in der Wartezeit unterscheiden: I 50 s, II 20 s, III 5 s (die letzte
     * Easter-Stufe die Haelfte). Echt gefahren: auf Stufe I und II steht ein Spieler so lange wie Stufe
     * III braucht und noch 20 Ticks mehr - er bleibt, der Spieler auf Stufe III ist dann laengst am
     * Spawn-Ziel. Stufe I und II springen zum selben Ziel wie III ohne Wiedereinstiegspunkt.
     */
    public static void spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds(GameTestHelper helper) {
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(1), 50 * 20, "wait of spawn teleporter I");
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(2), 20 * 20, "wait of spawn teleporter II");
        helper.assertValueEqual(SpawnTeleporterBlockEntity.requiredTicks(3), 5 * 20, "wait of spawn teleporter III");
        helper.assertValueEqual(TweaksFamilies.tiers(TweaksFamilies.Family.SPAWN_TELEPORTER),
                List.of(TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER),
                "tiers of the spawn teleporter family");
        helper.assertValueEqual(EasterEggs.stageCount(TweaksFamilies.Family.SPAWN_TELEPORTER), 3, "easter stages of the spawn teleporter");

        // Kein eigenes Spawn-Ziel: die Config ist global, und parallel laufende Tests setzen es auch
        // (TweaksTests). Geprueft wird darum "weggesprungen" und "alle Stufen landen am selben Ort".
        Block[] tiers = {TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER};
        List<ServerPlayer> players = new ArrayList<>();
        List<Vec3> starts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BlockPos pad = new BlockPos(1 + 2 * i, 1, 1);
            helper.setBlock(pad, tiers[i]);
            ServerPlayer player = mockPlayer(helper, new Vec3(1.5 + 2 * i, 1.1, 1.5));
            player.setDeltaMovement(Vec3.ZERO);
            players.add(player);
            starts.add(player.position());
        }
        helper.startSequence()
                .thenExecuteAfter(SpawnTeleporterBlockEntity.ENDERITE_TICKS + 20, () -> {
                    helper.assertTrue(players.get(2).position().distanceTo(starts.get(2)) > 2.0,
                            "spawn teleporter III did not send its player away within 5 s: " + players.get(2).position());
                    for (int i = 0; i < 2; i++) {
                        helper.assertTrue(players.get(i).position().distanceTo(starts.get(i)) < 0.5,
                                "spawn teleporter " + (i + 1) + " already jumped after " + (SpawnTeleporterBlockEntity.ENDERITE_TICKS + 20) + " ticks");
                    }
                    // Dasselbe Ziel fuer jede Stufe: I, II und III (ohne Wiedereinstiegspunkt) im selben Tick springen lassen.
                    ServerPlayer third = mockPlayer(helper, new Vec3(5.5, 1.1, 5.5));
                    SpawnTeleporterBlockEntity.teleport(helper.getLevel(), players.get(0), 1);
                    SpawnTeleporterBlockEntity.teleport(helper.getLevel(), players.get(1), 2);
                    SpawnTeleporterBlockEntity.teleport(helper.getLevel(), third, 3);
                    for (ServerPlayer other : List.of(players.get(1), third)) {
                        helper.assertTrue(other.position().distanceTo(players.get(0).position()) < 0.01,
                                "the tiers jump to different places: " + players.get(0).position() + " and " + other.position());
                    }
                    helper.assertTrue(players.get(0).position().distanceTo(starts.get(0)) > 2.0, "spawn teleporter I did not send its player away");
                })
                .thenSucceed();
    }

    /**
     * Welt-Upgrade (fuenf Stufen -> drei): ein alter Teleporter III wird beim ersten Tick Stufe II, ein
     * alter IV Stufe III (wie das alte V, das seine Id behaelt) - Besitzer bleibt, eine Easter-Stufe wird
     * auf die neue Stufe umgerechnet (alt IV mit Easter-Stufe 4 ist danach die letzte Stufe 3, doppelt so
     * schnell). Im Inventar tauschen sich die Items samt Anzahl und Easter-Stufe; die alten Stufen stehen
     * nicht im Kreativ-Tab, sind in JEI ausgeblendet und gehoeren zu keiner Familie.
     */
    public static void oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper helper) {
        Block[] old = {TweaksBlocks.SPAWN_TELEPORTER_TIER_3, TweaksBlocks.SPAWN_TELEPORTER_TIER_4};
        Block[] now = {TweaksBlocks.SPAWN_TELEPORTER_TIER_2, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER};
        int[] oldStage = {3, 4};
        int[] newStage = {2, 3};
        UUID owner = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 2; i++) {
            BlockPos pos = new BlockPos(2 + i * 3, 1, 3);
            helper.setBlock(pos, old[i]);
            SpawnTeleporterBlockEntity be = helper.getBlockEntity(pos, SpawnTeleporterBlockEntity.class);
            be.setOwner(owner);
            be.setEasterStage(oldStage[i]);
            SpawnTeleporterBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), be);
            helper.assertTrue(helper.getBlockState(pos).is(now[i]), old[i] + " became " + helper.getBlockState(pos).getBlock() + " instead of " + now[i]);
            OwnedBlockEntity fresh = helper.getBlockEntity(pos, OwnedBlockEntity.class);
            helper.assertTrue(owner.equals(fresh.getOwner()), "the migrated " + now[i] + " lost its owner");
            helper.assertValueEqual(fresh.easterStage(), newStage[i], "easter stage of the migrated " + now[i]);
            helper.assertTrue(((LegacyTierBlock) old[i]).target() == now[i], old[i] + " names the wrong new tier");
        }
        helper.assertTrue(EasterEggs.isBoosted(level, helper.absolutePos(new BlockPos(5, 1, 3))),
                "the old easter IV did not become the final (doubled) easter stage of tier III");

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        for (int i = 0; i < 2; i++) {
            ItemStack stack = new ItemStack(old[i], 1);
            if (i == 1) {
                stack.set(EasterEggs.EASTER_STAGE, 4);
            }
            player.getInventory().setItem(10 + i, stack);
            stack.getItem().inventoryTick(stack, level, player, null);
            ItemStack after = player.getInventory().getItem(10 + i);
            helper.assertTrue(after.is(now[i].asItem()) && after.getCount() == 1, "an old " + old[i] + " stack in the inventory became " + after);
            if (i == 1) {
                helper.assertValueEqual(EasterEggs.stageOf(after), 3, "easter stage of the swapped old spawn teleporter IV");
            }
        }
        TagKey<Item> hidden = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "hidden_from_recipe_viewers"));
        for (Block legacy : old) {
            helper.assertTrue(TweaksBlocks.legacy().contains(legacy), legacy + " is not listed as a legacy block");
            helper.assertTrue(new ItemStack(legacy).is(hidden), legacy + " is not hidden from recipe viewers");
            for (CreativeTabLayout.Row row : TweaksItems.functionalRows()) {
                for (ItemStack stack : row.stacks()) {
                    helper.assertTrue(!stack.is(legacy.asItem()), legacy + " is still in the creative tab row " + row.name());
                }
            }
        }
        helper.succeed();
    }

    // =====================================================================================
    // Rezepte: Stufe I jeder Familie im Schmiedetisch
    // =====================================================================================

    /**
     * Stufe I jeder Pad-Familie ist ein Schmiederezept: Vorlage + Druckplatte der Familie (Basis) +
     * Freischalt-Zutat, in Erz-Reihenfolge - Chunk-Loader: Kupfer-Druckplatte + Kupferkern, Launchpad:
     * schwere Waegeplatte + Eisenkern, Spawn-Teleporter: leichte Waegeplatte + Endermankopf, Elytra-Pad:
     * Diamant-Druckplatte + Elytra (vorher Elytra ohne dritte Zutat - kaputt), Trank-Pad:
     * Netherit-Druckplatte + Lohenkopf (vorher Werkbank), Flypad: Enderit-Vorlage + Enderit-Druckplatte +
     * Enderit-Kern. Jede beliebige Vorlage geht (hier ein Besatz und die Netherit-Aufwertung). Der
     * Einstieg in die Easter-Kette nimmt dieselbe Freischalt-Zutat.
     */
    public static void tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem(GameTestHelper helper) {
        record Entry(TweaksFamilies.Family family, ItemLike plate, Item unlock, Block result) {
        }
        List<Entry> entries = List.of(
                new Entry(TweaksFamilies.Family.CHUNK_LOADER, TweaksBlocks.COPPER_PRESSURE_PLATE, ModItems.COPPER_CORE, TweaksBlocks.CHUNK_LOADER),
                new Entry(TweaksFamilies.Family.LAUNCHPAD, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, ModItems.IRON_CORE, TweaksBlocks.LAUNCHPAD),
                new Entry(TweaksFamilies.Family.SPAWN_TELEPORTER, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, TweaksItems.ENDERMAN_HEAD, TweaksBlocks.SPAWN_TELEPORTER),
                new Entry(TweaksFamilies.Family.ELYTRA_PAD, TweaksBlocks.DIAMOND_PRESSURE_PLATE, Items.ELYTRA, TweaksBlocks.ELYTRA_PAD),
                new Entry(TweaksFamilies.Family.POTION_PAD, TweaksBlocks.NETHERITE_PRESSURE_PLATE, TweaksItems.BLAZE_HEAD, TweaksBlocks.POTION_PAD));
        for (Entry entry : entries) {
            for (Item template : List.of(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                expect(helper, template, entry.plate(), entry.unlock(), entry.result());
            }
            expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, entry.plate(), null);
            helper.assertValueEqual(EasterEggs.steps(entry.family()).get(0).addition(), entry.unlock(),
                    "unlock item of the easter entry of " + entry.family());
        }
        expect(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE, ModItems.ENDERITE_CORE, TweaksBlocks.FLYPAD);
        // Die alten Wege sind weg: Elytra ohne Zutat, Diamant-Druckplatte als Zutat von Launchpad/Chunk-Loader,
        // Diamantblock/Netheritbarren beim Teleporter.
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.ELYTRA, null);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, TweaksBlocks.DIAMOND_PRESSURE_PLATE);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, TweaksBlocks.COPPER_PRESSURE_PLATE, TweaksBlocks.DIAMOND_PRESSURE_PLATE);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.DIAMOND_BLOCK);
        expectNothing(helper, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.NETHERITE_INGOT);
        helper.succeed();
    }

    // =====================================================================================
    // Keine Bildschirmtexte
    // =====================================================================================

    /**
     * Pads und Geraete schreiben nichts auf den Bildschirm (Besitzer 2026-09-28: das Spiel soll sich wie
     * Vanilla anfuehlen): ein Spieler, der jede Aktionsleisten- und Chatmeldung mitschreibt
     * ({@link #textRecordingPlayer}), wartet auf einem Spawn-Teleporter III, tritt zur Seite (Abbruch),
     * wartet erneut bis zum Sprung; bekommt eine Elytra vom Elytra-Pad; laedt ein Launchpad voll, drueber
     * hinaus, steht leer und geladen darauf; klickt ein Trank-Pad an; benutzt ein unverknuepftes Echolot
     * und eines ohne Leitstein; klickt mit einem gesperrten Oktanten. Danach steht keine einzige Meldung
     * im Protokoll, und die Sprachdateien kennen die entfernten Meldungen nicht mehr.
     */
    public static void padsAndGadgetsWriteNoTextOnTheScreen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Component> texts = new ArrayList<>();
        TweaksConfig.Spawn spawn = SimpleTweaks.config().spawn;
        BlockPos teleporter = new BlockPos(1, 1, 1);
        helper.setBlock(teleporter, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);
        BlockPos launch = new BlockPos(5, 1, 1);
        helper.setBlock(launch, TweaksBlocks.LAUNCHPAD);
        BlockPos emptyLaunch = new BlockPos(5, 1, 3);
        helper.setBlock(emptyLaunch, TweaksBlocks.LAUNCHPAD);
        ServerPlayer player = textRecordingPlayer(helper, new Vec3(1.5, 1.1, 1.5), texts);
        // Steht die ganze Zeit auf einem leeren Launchpad (der Block klickt dann wie ein leerer Werfer).
        ServerPlayer emptyStander = textRecordingPlayer(helper, Vec3.atBottomCenterOf(emptyLaunch).add(0, 0.1, 0), texts);
        emptyStander.setDeltaMovement(Vec3.ZERO);
        ServerPlayer stander = textRecordingPlayer(helper, new Vec3(7.5, 1.0, 3.5), texts);
        // Beitrittsmeldungen und Startgeschenke gehoeren nicht zu den Pads.
        texts.clear();

        // Sofort pruefbar: Elytra-Pad, Launchpad, Trank-Pad, Echolot, Oktant.
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ElytraPadBlockEntity.applyTo(level, helper.absolutePos(teleporter), 1, player, spawn);
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(TweaksItems.SPAWN_ELYTRA), "the elytra pad handed out no elytra");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        BlockPos launchAbs = helper.absolutePos(launch);
        LaunchpadBlock pad = (LaunchpadBlock) TweaksBlocks.LAUNCHPAD;
        ItemStack charges = new ItemStack(Items.WIND_CHARGE, 64);
        pad.deposit(charges, level, launchAbs, player, true);
        InteractionResult full = pad.deposit(charges, level, launchAbs, player, false);
        helper.assertTrue(full == InteractionResult.FAIL, "a full launchpad took another wind charge");
        helper.getBlockState(launch).useWithoutItem(level, player, hit(helper, launch));

        BlockPos potion = new BlockPos(5, 1, 5);
        helper.setBlock(potion, TweaksBlocks.POTION_PAD);
        helper.getBlockState(potion).useWithoutItem(level, player, hit(helper, potion));
        helper.getBlockEntity(potion, PotionPadBlockEntity.class);

        ItemStack unlinked = new ItemStack(TweaksItems.ECHO_COMPASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, unlinked);
        unlinked.getItem().use(level, player, InteractionHand.MAIN_HAND);
        ItemStack missing = new ItemStack(TweaksItems.ECHO_COMPASS);
        missing.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(level.dimension(), helper.absolutePos(new BlockPos(3, 1, 7)))), false));
        player.setItemInHand(InteractionHand.MAIN_HAND, missing);
        missing.getItem().use(level, player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();

        ItemStack octant = new ItemStack(ModItems.OCTANT);
        CompoundTag locked = new CompoundTag();
        locked.putBoolean("Locked", true);
        octant.set(DataComponents.CUSTOM_DATA, CustomData.of(locked));
        player.setItemInHand(InteractionHand.MAIN_HAND, octant);
        octant.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, new BlockPos(3, 0, 3))));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(texts.isEmpty(), "pads and gadgets wrote on the screen: " + texts);

        LaunchpadBlockEntity launchpad = helper.getBlockEntity(launch, LaunchpadBlockEntity.class);
        int stored = launchpad.getCharges();
        helper.assertTrue(stored > 0, "the launchpad was not charged for the countdown part");

        Vec3 start = player.position();
        helper.startSequence()
                // Teleporter: 40 Ticks stehen, dann ein Schritt (Abbruch nach hoerbarer Wartezeit) ...
                .thenExecuteAfter(40, () -> player.snapTo(start.x + 0.3, start.y, start.z, 0.0F, 0.0F))
                .thenExecuteAfter(1, () -> {
                    // Geladenes Launchpad: draufstellen, 45 Ticks Countdown (unter den 60 bis zum Start), wieder runter.
                    Vec3 on = helper.absoluteVec(Vec3.atBottomCenterOf(launch).add(0, 0.1, 0));
                    stander.snapTo(on.x, on.y, on.z, 0.0F, 0.0F);
                    stander.setDeltaMovement(Vec3.ZERO);
                    for (int t = 0; t < 45; t++) {
                        LaunchpadBlockEntity.tick(level, launchAbs, helper.getBlockState(launch), launchpad);
                    }
                    Vec3 off = helper.absoluteVec(new Vec3(7.5, 1.0, 5.5));
                    stander.snapTo(off.x, off.y, off.z, 0.0F, 0.0F);
                    helper.assertTrue(launchpad.getCharges() == stored, "the launchpad launched during the countdown part");
                })
                // ... dann bis zum Sprung stehen bleiben.
                .thenWaitUntil(() -> helper.assertTrue(player.position().distanceTo(start) > 2.0,
                        "the spawn teleporter did not send the player away"))
                .thenExecute(() -> {
                    helper.assertTrue(texts.isEmpty(), "pads and gadgets wrote on the screen: " + texts);
                    com.google.gson.JsonObject[] langs = {lang(helper, "en_us"), lang(helper, "de_de")};
                    for (com.google.gson.JsonObject lang : langs) {
                        for (String key : List.of("message.simplebuilding.spawn_teleporter.countdown",
                                "message.simplebuilding.spawn_teleporter.cancelled", "message.simplebuilding.spawn_teleporter.welcome",
                                "message.simplebuilding.elytra_pad.equipped", "message.simplebuilding.launchpad.charges",
                                "message.simplebuilding.launchpad.countdown", "message.simplebuilding.potion_pad.stored",
                                "message.simplebuilding.echo_sounder.unlinked", "message.simplebuilding.spawn_elytra.expired")) {
                            helper.assertFalse(lang.has(key), "the removed on-screen text " + key + " is still translated");
                        }
                    }
                })
                .thenSucceed();
    }

    // =====================================================================================
    // Echolot
    // =====================================================================================

    /**
     * Die Abklingzeit nach einem Sprung ist viermal so lang wie frueher: 480 statt 120 Ticks (24 s).
     * Nach 121 Ticks ist das Echolot noch gesperrt, nach 480 wieder frei.
     */
    public static void theEchoSounderCooldownIsFourTimesLonger(GameTestHelper helper) {
        helper.assertValueEqual(EchoCompassItem.COOLDOWN_TICKS, 4 * 120, "cooldown of the echo sounder");
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.getAbilities().instabuild = false;
        ItemStack compass = linked(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        helper.assertTrue(EchoCompassItem.teleport(player, InteractionHand.MAIN_HAND, compass), "the echo sounder did not jump");
        for (int t = 0; t < 121; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "the echo sounder is off cooldown after the old 6 s");
        for (int t = 121; t < EchoCompassItem.COOLDOWN_TICKS; t++) {
            player.getCooldowns().tick();
        }
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "the echo sounder is still on cooldown after 24 s");
        helper.succeed();
    }

    /**
     * Rechtsklick auf einen Leitstein verknuepft nur, wenn das Echolot nicht schon mit genau diesem
     * Leitstein verknuepft ist: der zweite Klick tut nichts (FAIL, keine Blindheit, kein Aufladen), ein
     * anderer Leitstein verknuepft neu.
     */
    public static void theEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo(GameTestHelper helper) {
        BlockPos first = new BlockPos(6, 1, 6);
        BlockPos second = new BlockPos(2, 1, 6);
        helper.setBlock(first, Blocks.LODESTONE);
        helper.setBlock(second, Blocks.LODESTONE);
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        GlobalPos firstPos = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(first));

        InteractionResult link = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, first)));
        helper.assertTrue(link.consumesAction() && firstPos.equals(EchoCompassItem.target(compass)), "the first click did not link: " + link);
        helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "the first link had no effect");
        player.removeAllEffects();

        InteractionResult again = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, first)));
        helper.assertTrue(again == InteractionResult.FAIL, "clicking the linked lodestone again answered " + again + " instead of FAIL");
        helper.assertFalse(player.hasEffect(MobEffects.BLINDNESS), "clicking the linked lodestone again replayed the link effects");
        helper.assertFalse(player.isUsingItem(), "clicking the linked lodestone again started charging");
        helper.assertValueEqual(EchoCompassItem.target(compass), firstPos, "target after clicking the same lodestone");

        InteractionResult other = compass.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, second)));
        helper.assertTrue(other.consumesAction(), "a different lodestone did not link: " + other);
        helper.assertValueEqual(EchoCompassItem.target(compass), GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(second)),
                "target after clicking another lodestone");
        helper.succeed();
    }

    // ---- owner decisions 2026-09-28 (begin)

    /**
     * Redstone switches pads off and a comparator reads them (owner, 2026-09-28): a powered launchpad
     * keeps its charges although a player stands on it for longer than the countdown, the same pad
     * unpowered launches; a powered flypad gives no flight and takes back the flight it gave. The
     * launchpad's comparator signal is its fill level (empty 0, half 8, full 15), the flypad's the
     * number of players it serves.
     *
     * <p>What breaks it: the redstone check going missing from a pad's tick, or a comparator signal
     * that does not follow the charges or the players.
     */
    public static void redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pad = new BlockPos(1, 1, 1);
        helper.setBlock(pad, TweaksBlocks.LAUNCHPAD);
        BlockPos abs = helper.absolutePos(pad);
        LaunchpadBlockEntity launchpad = helper.getBlockEntity(pad, LaunchpadBlockEntity.class);

        helper.assertTrue(level.getBlockState(abs).hasAnalogOutputSignal(), "a launchpad has no comparator output");
        helper.assertValueEqual(padSignal(helper, pad), 0, "comparator signal of an empty launchpad");
        launchpad.addCharges(2, 4);
        helper.assertValueEqual(padSignal(helper, pad), 8, "comparator signal of a launchpad I with 2 of 4 charges");
        launchpad.addCharges(2, 4);
        helper.assertValueEqual(padSignal(helper, pad), 15, "comparator signal of a full launchpad I");

        // --- powered: a player stands on it for longer than the countdown, and nothing happens ---
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.1, 1.5));
        helper.setBlock(pad.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, abs),
                "a launchpad next to a redstone block does not count as switched off");
        for (int tick = 0; tick <= LaunchpadBlockEntity.LAUNCH_TICKS + 5; tick++) {
            LaunchpadBlockEntity.tick(level, abs, level.getBlockState(abs), launchpad);
        }
        helper.assertValueEqual(launchpad.getCharges(), 4, "charges of a powered launchpad after a full countdown with a player on it");

        // --- unpowered: the same countdown launches ---
        helper.setBlock(pad.east(), Blocks.AIR);
        for (int tick = 0; tick <= LaunchpadBlockEntity.LAUNCH_TICKS + 5; tick++) {
            LaunchpadBlockEntity.tick(level, abs, level.getBlockState(abs), launchpad);
        }
        helper.assertValueEqual(launchpad.getCharges(), 0, "charges of the unpowered launchpad after the countdown - it should have launched");
        helper.assertValueEqual(padSignal(helper, pad), 0, "comparator signal of the launchpad after the launch");
        player.setDeltaMovement(Vec3.ZERO);

        // --- flypad: powered means no flight ---
        BlockPos fly = new BlockPos(4, 1, 4);
        helper.setBlock(fly, TweaksBlocks.FLYPAD);
        BlockPos flyAbs = helper.absolutePos(fly);
        com.simplebuilding.tweaks.block.entity.FlypadBlockEntity flypad =
                helper.getBlockEntity(fly, com.simplebuilding.tweaks.block.entity.FlypadBlockEntity.class);
        Vec3 onFlypad = helper.absoluteVec(new Vec3(4.5, 1.3, 4.5));
        player.snapTo(onFlypad.x, onFlypad.y, onFlypad.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        helper.setBlock(fly.east(), Blocks.REDSTONE_BLOCK);
        com.simplebuilding.tweaks.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
        helper.assertFalse(player.getAbilities().mayfly, "a powered flypad gave flight");
        helper.assertValueEqual(padSignal(helper, fly), 0, "comparator signal of a powered flypad");

        helper.setBlock(fly.east(), Blocks.AIR);
        com.simplebuilding.tweaks.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
        helper.assertTrue(player.getAbilities().mayfly, "the unpowered flypad gave no flight");
        // At least one: the flypad's area is wide, a neighbouring test's player may stand in it too.
        helper.assertTrue(padSignal(helper, fly) >= 1, "comparator signal of a flypad serving a player: " + padSignal(helper, fly));

        helper.setBlock(fly.east(), Blocks.REDSTONE_BLOCK);
        com.simplebuilding.tweaks.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
        helper.assertFalse(player.getAbilities().mayfly, "powering the flypad did not take back the flight it gave");

        helper.succeed();
    }

    /**
     * Hoppers fill the launchpad with wind charges and with nothing else (owner, 2026-09-28): a
     * hopper holding dirt and six wind charges above a launchpad I loads four charges, keeps the two
     * that no longer fit and never gives up the dirt.
     *
     * <p>What breaks it: the launchpad no longer being a container, a filter that lets anything in,
     * or a pad that takes more than its capacity.
     */
    public static void hoppersFillOnlyWindChargesIntoTheLaunchpad(GameTestHelper helper) {
        BlockPos pad = new BlockPos(1, 1, 1);
        BlockPos hopperPos = pad.above();
        helper.setBlock(pad, TweaksBlocks.LAUNCHPAD);
        helper.setBlock(hopperPos, Blocks.HOPPER);
        LaunchpadBlockEntity launchpad = helper.getBlockEntity(pad, LaunchpadBlockEntity.class);
        net.minecraft.world.level.block.entity.HopperBlockEntity hopper =
                helper.getBlockEntity(hopperPos, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.DIRT));
        hopper.setItem(1, new ItemStack(Items.WIND_CHARGE, 6));

        helper.succeedWhen(() -> {
            helper.assertValueEqual(launchpad.getCharges(), 4, "charges the hopper loaded into the launchpad I");
            helper.assertValueEqual(hopper.getItem(1).getCount(), 2, "wind charges left in the hopper once the pad is full");
            helper.assertTrue(hopper.getItem(0).is(Items.DIRT), "the hopper gave the dirt away");
        });
    }

    /** The comparator signal of the pad at {@code pad}, as a comparator facing away from it would read it. */
    private static int padSignal(GameTestHelper helper, BlockPos pad) {
        BlockPos abs = helper.absolutePos(pad);
        return helper.getLevel().getBlockState(abs).getAnalogOutputSignal(helper.getLevel(), abs, Direction.EAST);
    }

    // ---- owner decisions 2026-09-28 (end)

    // ---- owner decisions 2026-09-29 (begin)

    /**
     * The spawn teleporter takes a player to his own spawn and, powered by redstone, to the world spawn
     * (owner, 2026-09-29): a player whose respawn point is a bed lands next to that bed, whatever the
     * tier; a player without one lands at the spawn target. A redstone block next to the pad switches it
     * to the world spawn instead of switching it off - driven for real on tier III (5 s), the bed owner
     * charges it (the pad shows {@code active=true} meanwhile) and ends up away from his bed, and neither
     * way writes a line on the screen. The two destinations have different arrival sounds.
     *
     * <p>What breaks it: the teleporter going back to the world spawn for everyone, redstone switching it
     * off again, or the active state not following the charging player.
     */
    public static void theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Component> texts = new ArrayList<>();
        BlockPos bedFoot = new BlockPos(6, 1, 6);
        BlockPos bedHead = bedFoot.north();
        helper.setBlock(bedFoot, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
        helper.setBlock(bedHead, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        Vec3 bed = helper.absoluteVec(Vec3.atCenterOf(bedHead));
        ServerPlayer.RespawnConfig bedSpawn = new ServerPlayer.RespawnConfig(
                net.minecraft.world.level.storage.LevelData.RespawnData.of(level.dimension(), helper.absolutePos(bedHead), 0.0F, 0.0F), false);

        // --- own spawn, every tier: next to the bed ---
        for (int tier = 1; tier <= 3; tier++) {
            ServerPlayer sleeper = mockPlayer(helper, new Vec3(1.5, 1.1, 1.5));
            sleeper.setRespawnPosition(bedSpawn, false);
            helper.assertTrue(SpawnTeleporterBlockEntity.ownSpawn(sleeper) != null, "a player with a bed has no own spawn");
            SpawnTeleporterBlockEntity.teleport(level, sleeper, tier);
            helper.assertTrue(sleeper.position().distanceTo(bed) < 3.0,
                    "spawn teleporter " + tier + " did not take a player with a bed to his bed: " + sleeper.position() + " vs " + bed);
        }
        // --- no own spawn: the spawn target, the same as the world spawn mode ---
        ServerPlayer homeless = mockPlayer(helper, new Vec3(1.5, 1.1, 3.5));
        helper.assertTrue(SpawnTeleporterBlockEntity.ownSpawn(homeless) == null, "a player without a bed has an own spawn");
        SpawnTeleporterBlockEntity.SpawnTarget spawnTarget = SpawnTeleporterBlockEntity.spawnTarget(level);
        SpawnTeleporterBlockEntity.teleport(level, homeless, 1);
        helper.assertTrue(homeless.position().distanceTo(spawnTarget.position()) < 0.5,
                "a player without a bed did not land at the spawn target: " + homeless.position() + " vs " + spawnTarget.position());
        // --- world spawn mode: even the bed owner lands at the spawn target ---
        ServerPlayer forced = mockPlayer(helper, new Vec3(1.5, 1.1, 5.5));
        forced.setRespawnPosition(bedSpawn, false);
        SpawnTeleporterBlockEntity.SpawnTarget sameTick = SpawnTeleporterBlockEntity.spawnTarget(level);
        SpawnTeleporterBlockEntity.teleport(level, forced, 3, SpawnTeleporterBlockEntity.Destination.WORLD_SPAWN);
        helper.assertTrue(forced.position().distanceTo(sameTick.position()) < 0.5,
                "the world spawn mode did not take the bed owner to the spawn target: " + forced.position() + " vs " + sameTick.position());
        helper.assertFalse(SpawnTeleporterBlockEntity.arrivalSound(SpawnTeleporterBlockEntity.Destination.OWN_SPAWN)
                        == SpawnTeleporterBlockEntity.arrivalSound(SpawnTeleporterBlockEntity.Destination.WORLD_SPAWN),
                "own spawn and world spawn arrive with the same sound");

        // --- the real thing: tier III with a redstone block next to it ---
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);
        BlockPos padAbs = helper.absolutePos(pad);
        helper.assertValueEqual(SpawnTeleporterBlockEntity.destinationAt(level, padAbs), SpawnTeleporterBlockEntity.Destination.OWN_SPAWN,
                "destination of an unpowered spawn teleporter");
        helper.setBlock(pad.west(), Blocks.REDSTONE_BLOCK);
        helper.assertValueEqual(SpawnTeleporterBlockEntity.destinationAt(level, padAbs), SpawnTeleporterBlockEntity.Destination.WORLD_SPAWN,
                "destination of a powered spawn teleporter");
        helper.assertFalse(com.simplebuilding.tweaks.block.PadBlock.isDisabledByRedstone(level, padAbs),
                "redstone switches the spawn teleporter off instead of choosing the world spawn");
        ServerPlayer rider = textRecordingPlayer(helper, new Vec3(2.5, 1.1, 2.5), texts);
        rider.setRespawnPosition(bedSpawn, false);
        rider.setDeltaMovement(Vec3.ZERO);
        texts.clear();
        Vec3 start = rider.position();
        helper.startSequence()
                .thenExecuteAfter(20, () -> helper.assertTrue(helper.getBlockState(pad).getValue(com.simplebuilding.tweaks.block.SpawnTeleporterBlock.ACTIVE),
                        "the spawn teleporter does not show that someone is charging it"))
                .thenWaitUntil(() -> helper.assertTrue(rider.position().distanceTo(start) > 2.0, "the powered spawn teleporter did not send the player away"))
                .thenExecute(() -> {
                    helper.assertTrue(rider.position().distanceTo(bed) > 3.0,
                            "the powered spawn teleporter took the player to his bed instead of the world spawn: " + rider.position());
                    helper.assertTrue(texts.isEmpty(), "the spawn teleporter wrote on the screen: " + texts);
                })
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(pad).getValue(com.simplebuilding.tweaks.block.SpawnTeleporterBlock.ACTIVE),
                        "the spawn teleporter still shows a charge after the player left"))
                .thenSucceed();
    }

    /**
     * The echo sounder is for deliberate use (owner, 2026-09-29): linking it and every attempt that does
     * not jump lock it for 1 s right next to the lodestone up to 5 s from 1000 blocks away or in another
     * dimension, shown as the item cooldown. Driven: an attempt released early near its lodestone locks
     * it for exactly {@link EchoCompassItem#attemptLockTicks} (on cooldown one tick before, free after); an
     * attempt on a missing lodestone 2000 blocks away locks it for the full 100 ticks. The failure click
     * sounds only once while the use key is held (repeats within {@value EchoCompassItem#HELD_GAP_TICKS}
     * ticks stay silent), and none of it writes a line on the screen.
     *
     * <p>What breaks it: the lock going missing from a path, a lock that ignores the distance, or the
     * click rattling along with the held key.
     */
    public static void theEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Component> texts = new ArrayList<>();
        helper.assertValueEqual(EchoCompassItem.ATTEMPT_LOCK_TICKS, 5 * 20, "longest attempt lock of the echo sounder");
        helper.assertValueEqual(SimpleTweaks.config().balancing.echoSounderAttemptLockTicks, 100, "configured attempt lock");
        BlockPos lodestone = new BlockPos(6, 1, 6);
        helper.setBlock(lodestone, Blocks.LODESTONE);
        ServerPlayer player = textRecordingPlayer(helper, new Vec3(1.5, 1.0, 1.5), texts);
        player.getAbilities().instabuild = false;
        texts.clear();

        // Distance: 1 s right next to it, 5 s far away and in another dimension, nothing unlinked.
        BlockPos abs = helper.absolutePos(lodestone);
        GlobalPos near = GlobalPos.of(level.dimension(), abs);
        GlobalPos far = GlobalPos.of(level.dimension(), abs.offset(2000, 0, 0));
        GlobalPos halfway = GlobalPos.of(level.dimension(), BlockPos.containing(player.position()).offset(500, 0, 0));
        GlobalPos nether = GlobalPos.of(net.minecraft.world.level.Level.NETHER, abs);
        int nearLock = EchoCompassItem.attemptLockTicks(player, near);
        helper.assertTrue(nearLock >= 20 && nearLock <= 22, "attempt lock next to the lodestone: " + nearLock);
        helper.assertValueEqual(EchoCompassItem.attemptLockTicks(player, far), 100, "attempt lock 2000 blocks away");
        int half = EchoCompassItem.attemptLockTicks(player, halfway);
        helper.assertTrue(half >= 58 && half <= 62, "attempt lock 500 blocks away: " + half);
        helper.assertValueEqual(EchoCompassItem.attemptLockTicks(player, nether), 100, "attempt lock into another dimension");
        helper.assertValueEqual(EchoCompassItem.attemptLockTicks(player, null), 0, "attempt lock of an unlinked echo sounder");

        // Released early near its lodestone: locked for exactly the near lock.
        ItemStack compass = linked(helper, lodestone);
        player.setItemInHand(InteractionHand.MAIN_HAND, compass);
        InteractionResult charge = compass.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(charge.consumesAction() && player.isUsingItem(), "the linked echo sounder did not start charging: " + charge);
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "starting to charge already locked the echo sounder");
        player.releaseUsingItem();
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "releasing early did not lock the echo sounder");
        int lock = EchoCompassItem.attemptLockTicks(player, EchoCompassItem.target(compass));
        for (int t = 1; t < lock; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(compass), "the attempt lock ended before " + lock + " ticks");
        player.getCooldowns().tick();
        helper.assertFalse(player.getCooldowns().isOnCooldown(compass), "the attempt lock lasts longer than " + lock + " ticks");

        // A missing lodestone 2000 blocks away: refused, locked for the full 5 s.
        ItemStack missing = new ItemStack(TweaksItems.ECHO_COMPASS);
        missing.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(far), false));
        player.setItemInHand(InteractionHand.MAIN_HAND, missing);
        InteractionResult refused = missing.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(refused == InteractionResult.FAIL && !player.isUsingItem(), "an echo sounder without its lodestone charged: " + refused);
        for (int t = 1; t < EchoCompassItem.ATTEMPT_LOCK_TICKS; t++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(player.getCooldowns().isOnCooldown(missing), "the lock after a missing lodestone ended before 5 s");
        player.getCooldowns().tick();
        helper.assertFalse(player.getCooldowns().isOnCooldown(missing), "the lock after a missing lodestone lasts longer than 5 s");

        // An unlinked echo sounder only clicks, it does not lock (the cooldown group is the item, so this
        // is checked while no lock runs).
        ItemStack unlinked = new ItemStack(TweaksItems.ECHO_COMPASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, unlinked);
        unlinked.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertFalse(player.getCooldowns().isOnCooldown(unlinked), "an unlinked echo sounder locked itself");

        // Linking locks it for a moment too.
        ItemStack fresh = new ItemStack(TweaksItems.ECHO_COMPASS);
        player.setItemInHand(InteractionHand.MAIN_HAND, fresh);
        InteractionResult link = fresh.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, lodestone)));
        helper.assertTrue(link.consumesAction() && player.getCooldowns().isOnCooldown(fresh), "linking did not lock the echo sounder: " + link);

        // Held use key: the failure click sounds once, repeats inside the held gap stay silent (the refused
        // attempt above already clicked in this tick, so the first press is checked after a pause).
        helper.startSequence()
                .thenExecuteAfter(EchoCompassItem.HELD_GAP_TICKS + 2, () -> {
                    helper.assertTrue(EchoCompassItem.failCue(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f),
                            "the first failed click after a pause made no sound");
                    helper.assertFalse(EchoCompassItem.failCue(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f),
                            "a repeated click of the held use key sounded again");
                })
                .thenExecuteAfter(4, () -> helper.assertFalse(EchoCompassItem.failCue(player,
                        net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f), "the held use key repeating after 4 ticks sounded again"))
                .thenExecuteAfter(EchoCompassItem.HELD_GAP_TICKS + 2, () -> {
                    helper.assertTrue(EchoCompassItem.failCue(player, net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, 0.6f, 0.8f),
                            "a new press after letting go made no sound");
                    helper.assertTrue(texts.isEmpty(), "the echo sounder wrote on the screen: " + texts);
                })
                .thenSucceed();
    }

    /**
     * Sledgehammer upgrades show in the hand, not on the screen (owner, 2026-09-29): with a Diamond
     * Sledgehammer and a Netherite Nugget the machine under the crosshair would be upgradable - the
     * nugget and the hammer tilt ({@code showsUpgradeHint}); with the wrong nugget (Enderite) nothing
     * tilts, the right-click falls through to the machine as before and writes no line on the screen, and
     * the removed action bar texts are gone from both language files.
     *
     * <p>What breaks it: the old action bar hint coming back, or the tilt answering for the wrong nugget.
     */
    public static void aWrongNuggetOnTheSledgehammerWritesNothingAndDoesNotTilt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Component> texts = new ArrayList<>();
        BlockPos hopper = new BlockPos(3, 1, 3);
        helper.setBlock(hopper, com.simplebuilding.blocks.ModBlocks.REINFORCED_HOPPER);
        ServerPlayer player = textRecordingPlayer(helper, new Vec3(3.5, 1.0, 1.5), texts);
        texts.clear();
        BlockPos abs = helper.absolutePos(hopper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER));

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.NETHERITE_NUGGET));
        helper.assertTrue(com.simplebuilding.util.SledgehammerUpgrades.showsUpgradeHint(level, abs, player),
                "the fitting nugget does not tilt in front of an upgradable machine");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ENDERITE_NUGGET));
        helper.assertFalse(com.simplebuilding.util.SledgehammerUpgrades.showsUpgradeHint(level, abs, player),
                "the wrong nugget tilts as if it could upgrade the machine");
        InteractionResult begun = com.simplebuilding.util.SledgehammerUpgrades.tryBegin(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, hopper)));
        helper.assertTrue(begun == null, "the wrong nugget started an upgrade: " + begun);
        helper.assertFalse(com.simplebuilding.util.SledgehammerUpgrades.shouldSkipBlockUse(helper.getBlockState(hopper), level, abs,
                player, InteractionHand.MAIN_HAND), "the wrong nugget kept the machine from opening");
        helper.assertTrue(texts.isEmpty(), "the sledgehammer wrote on the screen: " + texts);
        for (com.google.gson.JsonObject lang : new com.google.gson.JsonObject[]{lang(helper, "en_us"), lang(helper, "de_de")}) {
            for (String key : List.of("message.simplebuilding.smithing.wrong_nugget", "message.simplebuilding.smithing.hammer_too_weak",
                    "message.simplebuilding.smithing.piston_busy", "message.simplebuilding.smithing.double_chest",
                    "message.simplebuilding.echo_sounder.dimension_locked")) {
                helper.assertFalse(lang.has(key), "the removed on-screen text " + key + " is still translated");
            }
        }
        helper.succeed();
    }

    /**
     * Every pad family shows whether it is working (owner, 2026-09-29): an Elytra Pad turns
     * {@code active} while it serves a player and back off when he leaves, and stays off under a redstone
     * signal; a Potion Pad is {@code active} only while it holds a potion, is off cooldown and unpowered.
     * The blockstate files point every active state at its own model with its own texture, and pressed
     * plates at their glowing texture.
     *
     * <p>What breaks it: a state that no longer follows the pad, or a blockstate without the active model.
     */
    public static void everyPadFamilyShowsWhetherItIsWorking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos elytra = new BlockPos(1, 1, 1);
        helper.setBlock(elytra, TweaksBlocks.ELYTRA_PAD);
        BlockPos potion = new BlockPos(5, 1, 5);
        helper.setBlock(potion, TweaksBlocks.POTION_PAD);
        PotionPadBlockEntity potionPad = helper.getBlockEntity(potion, PotionPadBlockEntity.class);
        net.minecraft.world.level.block.state.properties.BooleanProperty elytraActive = com.simplebuilding.tweaks.block.ElytraPadBlock.ACTIVE;
        net.minecraft.world.level.block.state.properties.BooleanProperty potionActive = com.simplebuilding.tweaks.block.PotionPadBlock.ACTIVE;
        helper.assertFalse(helper.getBlockState(elytra).getValue(elytraActive), "a fresh elytra pad is active");
        helper.assertFalse(helper.getBlockState(potion).getValue(potionActive), "a fresh potion pad is active");

        // Blockstates and models (resources of this line).
        for (String id : List.of("elytra_pad", "reinforced_elytra_pad", "netherite_elytra_pad", "enderite_elytra_pad", "fine_elytra_pad",
                "spawn_teleporter", "spawn_teleporter_tier_2", "enderite_spawn_teleporter", "potion_pad", "reinforced_potion_pad", "infused_potion_pad")) {
            String blockstate = resource(helper, "/assets/simplebuilding/blockstates/" + id + ".json");
            helper.assertTrue(blockstate.contains("simplebuilding:block/" + id + "_active"), "the blockstate of " + id + " has no active model");
            String model = resource(helper, "/assets/simplebuilding/models/block/" + id + "_active.json");
            helper.assertTrue(model.contains("simplebuilding:block/" + id + "_active"), "the active model of " + id + " does not use its active texture");
            helper.assertTrue(PadOverhaulTests.class.getResource("/assets/simplebuilding/textures/block/" + id + "_active.png") != null,
                    "no active texture for " + id);
        }
        for (String id : List.of("diamond_pressure_plate", "netherite_pressure_plate", "enderite_pressure_plate", "copper_pressure_plate",
                "exposed_copper_pressure_plate", "weathered_copper_pressure_plate", "oxidized_copper_pressure_plate")) {
            String model = resource(helper, "/assets/simplebuilding/models/block/" + id + "_down.json");
            helper.assertTrue(model.contains("simplebuilding:block/" + id + "_active"), "the pressed " + id + " does not glow");
            helper.assertTrue(PadOverhaulTests.class.getResource("/assets/simplebuilding/textures/block/" + id + "_active.png") != null,
                    "no glowing texture for the pressed " + id);
        }

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 2.0, 1.5));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(elytra).getValue(elytraActive),
                        "the elytra pad does not show that it serves a player"))
                .thenExecute(() -> {
                    Vec3 away = helper.absoluteVec(new Vec3(7.5, 1.0, 1.5));
                    player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
                })
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(elytra).getValue(elytraActive),
                        "the elytra pad still shows a player who left"))
                .thenExecute(() -> {
                    helper.setBlock(elytra.east(), Blocks.REDSTONE_BLOCK);
                    Vec3 back = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
                    player.snapTo(back.x, back.y, back.z, 0.0F, 0.0F);
                    // A potion arrives on the potion pad.
                    com.simplebuilding.tweaks.block.PotionPadBlock.absorb(potionPad,
                            net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
                })
                .thenExecuteAfter(25, () -> {
                    helper.assertFalse(helper.getBlockState(elytra).getValue(elytraActive), "a powered elytra pad shows itself as working");
                    helper.assertTrue(helper.getBlockState(potion).getValue(potionActive), "a potion pad holding a potion does not show it is ready");
                    helper.setBlock(potion.east(), Blocks.REDSTONE_BLOCK);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertFalse(helper.getBlockState(potion).getValue(potionActive), "a powered potion pad shows itself as ready");
                    helper.setBlock(potion.east(), Blocks.AIR);
                    potionPad.setCooldown(200);
                })
                .thenExecuteAfter(2, () -> {
                    net.minecraft.world.level.block.state.BlockState cooling = helper.getBlockState(potion);
                    helper.assertTrue(cooling.getValue(com.simplebuilding.tweaks.block.PotionPadBlock.COOLING)
                            && !cooling.getValue(potionActive), "a cooling potion pad is " + cooling);
                })
                .thenSucceed();
    }

    private static String resource(GameTestHelper helper, String path) {
        try (java.io.InputStream in = PadOverhaulTests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "no " + path + " on the classpath");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }

    // ---- owner decisions 2026-09-29 (end)


    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /**
     * Ein Spieler in der Welt (wie {@code GameTestHelper#makeMockServerPlayerInLevel}), der jede
     * Aktionsleisten- und Chatmeldung in {@code texts} schreibt: {@code sendOverlayMessage} und
     * {@code sendSystemMessage} laufen beide ueber {@code sendSystemMessage(Component, boolean)}.
     */
    private static ServerPlayer textRecordingPlayer(GameTestHelper helper, Vec3 relative, List<Component> texts) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "text-recorder"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }

            @Override
            public void sendSystemMessage(Component message, boolean overlay) {
                // Jede Aktionsleistenzeile zaehlt, im Chat nur Texte der Mod - nicht Vanillas Beitritts- und
                // Fortschrittsmeldungen und nicht die Ergebniszeilen des Test-Rahmens (auch anderer Tests).
                if (overlay || (message.getContents() instanceof TranslatableContents key && key.getKey().contains("simplebuilding"))) {
                    texts.add(message);
                }
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        return player;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    private static ItemStack linked(GameTestHelper helper, BlockPos lodestone) {
        ItemStack compass = new ItemStack(TweaksItems.ECHO_COMPASS);
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(lodestone))), true));
        return compass;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    private static com.google.gson.JsonObject lang(GameTestHelper helper, String code) {
        try (java.io.InputStream in = PadOverhaulTests.class.getResourceAsStream("/assets/simplebuilding/lang/" + code + ".json")) {
            helper.assertTrue(in != null, "no " + code + ".json on the classpath");
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + code + ".json", e);
        }
    }

    private static Optional<RecipeHolder<SmithingRecipe>> smithing(GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());
    }

    private static SmithingRecipeInput input(Item template, ItemLike base, ItemLike addition) {
        return new SmithingRecipeInput(new ItemStack(template), new ItemStack(base), addition == null ? ItemStack.EMPTY : new ItemStack(addition));
    }

    private static ItemStack assemble(GameTestHelper helper, RecipeHolder<SmithingRecipe> holder, SmithingRecipeInput input) {
        return holder.value().assemble(input);
    }

    private static void expect(GameTestHelper helper, Item template, ItemLike base, ItemLike addition, ItemLike result) {
        SmithingRecipeInput in = input(template, base, addition);
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, in);
        helper.assertTrue(match.isPresent(), "no smithing recipe turns " + base + " with " + addition + " into " + result);
        ItemStack out = assemble(helper, match.get(), in);
        helper.assertTrue(out.is(result.asItem()), "smithing " + base + " with " + addition + " made " + out + " instead of " + result);
    }

    private static void expectNothing(GameTestHelper helper, Item template, ItemLike base, ItemLike addition) {
        Optional<RecipeHolder<SmithingRecipe>> match = smithing(helper, input(template, base, addition));
        helper.assertTrue(match.isEmpty(), "the old way still smiths " + base + " with " + addition + " (" + match.map(h -> h.id().toString()).orElse("") + ")");
    }
}
