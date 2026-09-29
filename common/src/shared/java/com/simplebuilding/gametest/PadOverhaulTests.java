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
 * Fabric-Adapter {@code PadOverhaulGameTest}, Test-IDs {@code simplebuilding:pad_overhaul_game_test_*}.
 */
public final class PadOverhaulTests {
    /** Budget fuer den Bildschirmtext-Test (Teleporter III: 5 s Stehen plus Abbruch davor). */
    public static final int NO_TEXT_MAX_TICKS = 300;
    /** Budget fuer den Wartezeit-Test (Stufe I darf nach Stufe III' 5 s noch nicht springen). */
    public static final int WAIT_MAX_TICKS = 200;
    /** Budget fuer den Trichter-Test (vier Windkugeln, eine je 8 Ticks, plus Luft). */
    public static final int HOPPER_MAX_TICKS = 200;

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
     * Stufe I jeder Pad-Familie (Besitzer 2026-09-29): immer der Materialkern der Familie im Vorlagen-Feld +
     * Druckplatte der Familie (Basis) + Freischalt-Zutat, in Erz-Reihenfolge - Chunk-Loader: Kupferkern +
     * Kupfer-Druckplatte + Trial-Chamber-Kopf, Launchpad: Eisenkern + schwere Waegeplatte + Trial-Chamber-Kopf,
     * Spawn-Teleporter: Goldkern + leichte Waegeplatte + Endermankopf, Elytra-Pad: Diamantkern +
     * Diamant-Druckplatte + Elytra, Trank-Pad: Netheritkern + Netherit-Druckplatte + Lohenkopf (Flypad I
     * formlos an der Werkbank, {@link MobHeadTests#flypadOneNeedsTheShulkerHeadAndAnElytraWithMending}). Jeder der
     * zehn Koepfe aus {@code simplebuilding:trial_chamber_heads} geht, kein anderer; eine Vorlage statt des Kerns
     * geht nicht mehr, ein fremder Kern auch nicht. Der Einstieg in die Easter-Kette nimmt dieselben Zutaten.
     */
    public static void tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem(GameTestHelper helper) {
        record Entry(TweaksFamilies.Family family, Item core, ItemLike plate, List<Item> unlocks, Block result) {
        }
        List<Item> trialHeads = com.simplebuilding.tweaks.heads.ModHeads.trialChamberHeads();
        List<Entry> entries = List.of(
                new Entry(TweaksFamilies.Family.CHUNK_LOADER, ModItems.COPPER_CORE, TweaksBlocks.COPPER_PRESSURE_PLATE, trialHeads, TweaksBlocks.CHUNK_LOADER),
                new Entry(TweaksFamilies.Family.LAUNCHPAD, ModItems.IRON_CORE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, trialHeads, TweaksBlocks.LAUNCHPAD),
                new Entry(TweaksFamilies.Family.SPAWN_TELEPORTER, ModItems.GOLD_CORE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                        List.of(TweaksItems.ENDERMAN_HEAD), TweaksBlocks.SPAWN_TELEPORTER),
                new Entry(TweaksFamilies.Family.ELYTRA_PAD, ModItems.DIAMOND_CORE, TweaksBlocks.DIAMOND_PRESSURE_PLATE,
                        List.of(Items.ELYTRA), TweaksBlocks.ELYTRA_PAD),
                new Entry(TweaksFamilies.Family.POTION_PAD, ModItems.NETHERITE_CORE, TweaksBlocks.NETHERITE_PRESSURE_PLATE,
                        List.of(TweaksItems.BLAZE_HEAD), TweaksBlocks.POTION_PAD));
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            for (Item unlock : entry.unlocks()) {
                expect(helper, entry.core(), entry.plate(), unlock, entry.result());
            }
            Item unlock = entry.unlocks().get(0);
            for (Item template : List.of(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                expectNothing(helper, template, entry.plate(), unlock);
            }
            Item otherCore = entries.get((i + 1) % entries.size()).core();
            expectNothing(helper, otherCore, entry.plate(), unlock);
            expectNothing(helper, entry.core(), entry.plate(), null);
            EasterEggs.Step easter = EasterEggs.steps(entry.family()).get(0);
            helper.assertValueEqual(easter.templates(), List.of(entry.core()), "template of the easter entry of " + entry.family());
            helper.assertValueEqual(easter.additions(), entry.unlocks(), "unlock items of the easter entry of " + entry.family());
        }
        // Nur die Trial-Chamber-Koepfe: das Tag hat genau die zehn, andere Koepfe passen nicht.
        java.util.Set<Item> tagged = new java.util.HashSet<>();
        for (net.minecraft.core.Holder<Item> holder : net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getTagOrEmpty(com.simplebuilding.tweaks.heads.ModHeads.TRIAL_CHAMBER_HEADS)) {
            tagged.add(holder.value());
        }
        helper.assertValueEqual(tagged, java.util.Set.copyOf(trialHeads), "items of #simplebuilding:trial_chamber_heads");
        helper.assertValueEqual(trialHeads.size(), 10, "trial chamber heads (zombie, husk, skeleton, stray, bogged, spider, cave spider, slime, silverfish, breeze)");
        for (Item other : List.of(TweaksItems.BLAZE_HEAD, TweaksItems.ENDERMAN_HEAD, TweaksItems.SHULKER_HEAD, TweaksItems.DROWNED_HEAD,
                Items.CREEPER_HEAD, Items.PIGLIN_HEAD, Items.WITHER_SKELETON_SKULL, Items.DRAGON_HEAD)) {
            expectNothing(helper, ModItems.COPPER_CORE, TweaksBlocks.COPPER_PRESSURE_PLATE, other);
            expectNothing(helper, ModItems.IRON_CORE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, other);
        }
        // Flypad I ist kein Schmiederezept mehr (Enderit-Vorlage + Platte + Kern).
        expectNothing(helper, ModItems.ENDERITE_UPGRADE_TEMPLATE, TweaksBlocks.ENDERITE_PRESSURE_PLATE, ModItems.ENDERITE_CORE);
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
