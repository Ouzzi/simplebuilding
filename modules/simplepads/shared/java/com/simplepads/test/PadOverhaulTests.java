package com.simplepads.test;

import com.mojang.authlib.GameProfile;
import com.simplepads.SimplePads;
import com.simplepads.PadsConfig;
import com.simplepads.block.LaunchpadBlock;
import com.simplepads.block.PadBlock;
import com.simplepads.block.PotionPadBlock;
import com.simplepads.block.LegacyTierBlock;
import com.simplepads.block.PadsBlocks;
import com.simplepads.block.PadFamilies;
import com.simplepads.block.entity.ElytraPadBlockEntity;
import com.simplepads.block.entity.LaunchpadBlockEntity;
import com.simplepads.block.entity.OwnedBlockEntity;
import com.simplepads.block.entity.PotionPadBlockEntity;
import com.simplepads.block.entity.SpawnTeleporterBlockEntity;
import com.simplepads.easter.EasterEggs;
import com.simplepads.item.PadsItems;
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
        helper.assertValueEqual(PadFamilies.tiers(PadFamilies.Family.SPAWN_TELEPORTER),
                List.of(PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.SPAWN_TELEPORTER_TIER_2, PadsBlocks.ENDERITE_SPAWN_TELEPORTER),
                "tiers of the spawn teleporter family");
        helper.assertValueEqual(EasterEggs.stageCount(PadFamilies.Family.SPAWN_TELEPORTER), 3, "easter stages of the spawn teleporter");

        // Kein eigenes Spawn-Ziel: die Config ist global, und parallel laufende Tests setzen es auch
        // (PadTweaksTests). Geprueft wird darum "weggesprungen" und "alle Stufen landen am selben Ort".
        Block[] tiers = {PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.SPAWN_TELEPORTER_TIER_2, PadsBlocks.ENDERITE_SPAWN_TELEPORTER};
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
        Block[] old = {PadsBlocks.SPAWN_TELEPORTER_TIER_3, PadsBlocks.SPAWN_TELEPORTER_TIER_4};
        Block[] now = {PadsBlocks.SPAWN_TELEPORTER_TIER_2, PadsBlocks.ENDERITE_SPAWN_TELEPORTER};
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
            helper.assertTrue(PadsBlocks.legacy().contains(legacy), legacy + " is not listed as a legacy block");
            helper.assertTrue(new ItemStack(legacy).is(hidden), legacy + " is not hidden from recipe viewers");
            for (PadsItems.Row row : PadsItems.padsRows()) {
                for (ItemStack stack : row.stacks()) {
                    helper.assertTrue(!stack.is(legacy.asItem()), legacy + " is still in the creative tab row " + row.name());
                }
            }
        }
        helper.succeed();
    }

    /**
     * Welt-Upgrade der Elytra-Pads (fuenf Stufen -> drei, 2026-10-07): ein altes Pad II wird beim ersten
     * Tick Stufe I, ein altes V Stufe III - Besitzer bleibt, eine Easter-Stufe wird auf die neue Stufe
     * umgerechnet (alt V mit Easter-Stufe 5 ist danach die letzte Stufe 3, doppelt so stark). Im Inventar
     * tauschen sich die Items samt Anzahl und Easter-Stufe; die alten Stufen stehen nicht im Kreativ-Tab,
     * sind in JEI ausgeblendet und gehoeren zu keiner Familie.
     */
    public static void oldElytraPadsBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper helper) {
        Block[] old = {PadsBlocks.REINFORCED_ELYTRA_PAD, PadsBlocks.FINE_ELYTRA_PAD};
        Block[] now = {PadsBlocks.ELYTRA_PAD, PadsBlocks.ENDERITE_ELYTRA_PAD};
        int[] oldStage = {2, 5};
        int[] newStage = {1, 3};
        UUID owner = UUID.randomUUID();
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 2; i++) {
            BlockPos pos = new BlockPos(2 + i * 3, 1, 3);
            helper.setBlock(pos, old[i]);
            com.simplepads.block.entity.ElytraPadBlockEntity be = helper.getBlockEntity(pos,
                    com.simplepads.block.entity.ElytraPadBlockEntity.class);
            be.setOwner(owner);
            be.setEasterStage(oldStage[i]);
            com.simplepads.block.entity.ElytraPadBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), be);
            helper.assertTrue(helper.getBlockState(pos).is(now[i]), old[i] + " became " + helper.getBlockState(pos).getBlock() + " instead of " + now[i]);
            OwnedBlockEntity fresh = helper.getBlockEntity(pos, OwnedBlockEntity.class);
            helper.assertTrue(owner.equals(fresh.getOwner()), "the migrated " + now[i] + " lost its owner");
            helper.assertValueEqual(fresh.easterStage(), newStage[i], "easter stage of the migrated " + now[i]);
            helper.assertTrue(((LegacyTierBlock) old[i]).target() == now[i], old[i] + " names the wrong new tier");
        }
        helper.assertTrue(EasterEggs.isBoosted(level, helper.absolutePos(new BlockPos(5, 1, 3))),
                "the old easter V did not become the final (doubled) easter stage of tier III");

        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        for (int i = 0; i < 2; i++) {
            ItemStack stack = new ItemStack(old[i], 3);
            stack.set(EasterEggs.EASTER_STAGE, oldStage[i]);
            player.getInventory().setItem(10 + i, stack);
            stack.getItem().inventoryTick(stack, level, player, null);
            ItemStack after = player.getInventory().getItem(10 + i);
            helper.assertTrue(after.is(now[i].asItem()) && after.getCount() == 3, "an old " + old[i] + " stack in the inventory became " + after);
            helper.assertValueEqual(EasterEggs.stageOf(after), newStage[i], "easter stage of the swapped old elytra pad");
        }
        TagKey<Item> hidden = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "hidden_from_recipe_viewers"));
        for (Block legacy : old) {
            helper.assertTrue(PadsBlocks.legacy().contains(legacy), legacy + " is not listed as a legacy block");
            helper.assertTrue(new ItemStack(legacy).is(hidden), legacy + " is not hidden from recipe viewers");
            for (PadsItems.Row row : PadsItems.padsRows()) {
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
        record Entry(PadFamilies.Family family, Item core, ItemLike plate, List<Item> unlocks, Block result) {
        }
        List<Item> trialHeads = com.simplepads.util.PadRefs.trialChamberHeads();
        List<Entry> entries = List.of(
                new Entry(PadFamilies.Family.CHUNK_LOADER, com.simplepads.util.PadRefs.core("copper"), PadsBlocks.COPPER_PRESSURE_PLATE, trialHeads, PadsBlocks.CHUNK_LOADER),
                new Entry(PadFamilies.Family.LAUNCHPAD, com.simplepads.util.PadRefs.core("iron"), Items.HEAVY_WEIGHTED_PRESSURE_PLATE, trialHeads, PadsBlocks.LAUNCHPAD),
                new Entry(PadFamilies.Family.SPAWN_TELEPORTER, com.simplepads.util.PadRefs.core("gold"), Items.LIGHT_WEIGHTED_PRESSURE_PLATE,
                        List.of(com.simplepads.util.PadRefs.mobHead("enderman_head")), PadsBlocks.SPAWN_TELEPORTER),
                new Entry(PadFamilies.Family.POTION_PAD, com.simplepads.util.PadRefs.core("netherite"), PadsBlocks.NETHERITE_PRESSURE_PLATE,
                        List.of(com.simplepads.util.PadRefs.mobHead("blaze_head")), PadsBlocks.POTION_PAD));
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
                .getTagOrEmpty(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("simplemobheads", "trial_chamber_heads")))) {
            tagged.add(holder.value());
        }
        helper.assertValueEqual(tagged, java.util.Set.copyOf(trialHeads), "items of #simplebuilding:trial_chamber_heads");
        helper.assertValueEqual(trialHeads.size(), 10, "trial chamber heads (zombie, husk, skeleton, stray, bogged, spider, cave spider, slime, silverfish, breeze)");
        for (Item other : List.of(com.simplepads.util.PadRefs.mobHead("blaze_head"), com.simplepads.util.PadRefs.mobHead("enderman_head"), com.simplepads.util.PadRefs.mobHead("shulker_head"), com.simplepads.util.PadRefs.mobHead("drowned_head"),
                Items.CREEPER_HEAD, Items.PIGLIN_HEAD, Items.WITHER_SKELETON_SKULL, Items.DRAGON_HEAD)) {
            expectNothing(helper, com.simplepads.util.PadRefs.core("copper"), PadsBlocks.COPPER_PRESSURE_PLATE, other);
            expectNothing(helper, com.simplepads.util.PadRefs.core("iron"), Items.HEAVY_WEIGHTED_PRESSURE_PLATE, other);
        }
        // Elytra-Pad I ist seit 2026-10-01 formlos an der Werkbank mit Reparatur-Elytra (MobHeadTests), nicht mehr geschmiedet.
        expectNothing(helper, com.simplepads.util.PadRefs.core("diamond"), PadsBlocks.DIAMOND_PRESSURE_PLATE, Items.ELYTRA);
        // Flypad I ist kein Schmiederezept mehr (Enderit-Vorlage + Platte + Kern).
        expectNothing(helper, com.simplepads.util.PadRefs.enderiteTemplate(), PadsBlocks.ENDERITE_PRESSURE_PLATE, com.simplepads.util.PadRefs.core("enderite"));
        // Die alten Wege sind weg: Elytra ohne Zutat, Diamant-Druckplatte als Zutat von Launchpad/Chunk-Loader,
        // Diamantblock/Netheritbarren beim Teleporter.
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.ELYTRA, null);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HEAVY_WEIGHTED_PRESSURE_PLATE, PadsBlocks.DIAMOND_PRESSURE_PLATE);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, PadsBlocks.COPPER_PRESSURE_PLATE, PadsBlocks.DIAMOND_PRESSURE_PLATE);
        expectNothing(helper, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.DIAMOND_BLOCK);
        expectNothing(helper, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.NETHERITE_INGOT);
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
        helper.setBlock(pad, PadsBlocks.LAUNCHPAD);
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
        helper.assertTrue(com.simplepads.block.PadBlock.isDisabledByRedstone(level, abs),
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
        helper.setBlock(fly, PadsBlocks.FLYPAD);
        BlockPos flyAbs = helper.absolutePos(fly);
        com.simplepads.block.entity.FlypadBlockEntity flypad =
                helper.getBlockEntity(fly, com.simplepads.block.entity.FlypadBlockEntity.class);
        Vec3 onFlypad = helper.absoluteVec(new Vec3(4.5, 1.3, 4.5));
        player.snapTo(onFlypad.x, onFlypad.y, onFlypad.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        helper.setBlock(fly.east(), Blocks.REDSTONE_BLOCK);
        com.simplepads.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
        helper.assertFalse(player.getAbilities().mayfly, "a powered flypad gave flight");
        helper.assertValueEqual(padSignal(helper, fly), 0, "comparator signal of a powered flypad");

        helper.setBlock(fly.east(), Blocks.AIR);
        com.simplepads.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
        helper.assertTrue(player.getAbilities().mayfly, "the unpowered flypad gave no flight");
        // At least one: the flypad's area is wide, a neighbouring test's player may stand in it too.
        helper.assertTrue(padSignal(helper, fly) >= 1, "comparator signal of a flypad serving a player: " + padSignal(helper, fly));

        helper.setBlock(fly.east(), Blocks.REDSTONE_BLOCK);
        com.simplepads.block.entity.FlypadBlockEntity.update(level, flyAbs, level.getBlockState(flyAbs), flypad);
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
        helper.setBlock(pad, PadsBlocks.LAUNCHPAD);
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

    /** Signal edges and strength changes restart the complete warmup, even between server ticks. */
    public static void spawnTeleporterSignalChangesResetTheEntireWarmup(GameTestHelper helper) {
        BlockPos pad = new BlockPos(2, 1, 2);
        helper.setBlock(pad, PadsBlocks.ENDERITE_SPAWN_TELEPORTER);
        helper.setBlock(pad.west().below(), Blocks.STONE);
        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pad);
        SpawnTeleporterBlockEntity be = helper.getBlockEntity(pad, SpawnTeleporterBlockEntity.class);
        ServerPlayer player = mockPlayer(helper, new Vec3(2.5, 1.1, 2.5));
        Vec3 start = player.position();
        int required = SpawnTeleporterBlockEntity.requiredTicks(level, absolute, 3);
        Runnable tick = () -> SpawnTeleporterBlockEntity.serverTick(level, absolute, level.getBlockState(absolute), be);
        for (int input : new int[] {15, 7, 15, 0, 15}) {
            for (int i = 0; i < required - 1; i++) tick.run();
            helper.assertTrue(be.comparatorSignal() > 0, "fixture did not charge");
            // An unrelated neighbor notification must leave the existing charge intact.
            int before = be.comparatorSignal();
            helper.setBlock(pad.east(), Blocks.STONE);
            helper.assertValueEqual(be.comparatorSignal(), before, "unchanged signal reset the charge");
            helper.setBlock(pad.east(), Blocks.AIR);
            var source = input == 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWER, input);
            helper.setBlock(pad.west(), source);
            helper.assertValueEqual(level.getBestNeighborSignal(absolute), input, "signal fixture strength");
            helper.assertValueEqual(be.comparatorSignal(), 0, "signal change kept the charge");
            helper.assertFalse(be.isTracking(), "signal change kept player progress");
            tick.run();
            helper.assertTrue(player.position().distanceTo(start) < 0.01, "signal change caused an accidental teleport");
            helper.assertValueEqual(SpawnTeleporterBlockEntity.destinationAt(level, absolute),
                    input > 0 ? SpawnTeleporterBlockEntity.Destination.WORLD_SPAWN : SpawnTeleporterBlockEntity.Destination.OWN_SPAWN,
                    "signal change changed the existing destination design");
            helper.setBlock(pad.west(), input > 0 ? Blocks.AIR : Blocks.REDSTONE_BLOCK);
            helper.setBlock(pad.west(), source);
            helper.assertValueEqual(be.comparatorSignal(), 0, "a short pulse kept the charge");
        }
        for (int i = 0; i < required - 1; i++) tick.run();
        helper.assertTrue(player.position().distanceTo(start) < 0.01, "warmup was shorter after reset");
        tick.run();
        helper.assertTrue(player.position().distanceTo(start) > 2, "a complete new warmup did not teleport");
        helper.succeed();
    }

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
        helper.setBlock(pad, PadsBlocks.ENDERITE_SPAWN_TELEPORTER);
        BlockPos padAbs = helper.absolutePos(pad);
        helper.assertValueEqual(SpawnTeleporterBlockEntity.destinationAt(level, padAbs), SpawnTeleporterBlockEntity.Destination.OWN_SPAWN,
                "destination of an unpowered spawn teleporter");
        helper.setBlock(pad.west(), Blocks.REDSTONE_BLOCK);
        helper.assertValueEqual(SpawnTeleporterBlockEntity.destinationAt(level, padAbs), SpawnTeleporterBlockEntity.Destination.WORLD_SPAWN,
                "destination of a powered spawn teleporter");
        helper.assertFalse(com.simplepads.block.PadBlock.isDisabledByRedstone(level, padAbs),
                "redstone switches the spawn teleporter off instead of choosing the world spawn");
        ServerPlayer rider = textRecordingPlayer(helper, new Vec3(2.5, 1.1, 2.5), texts);
        rider.setRespawnPosition(bedSpawn, false);
        rider.setDeltaMovement(Vec3.ZERO);
        texts.clear();
        Vec3 start = rider.position();
        helper.startSequence()
                .thenExecuteAfter(20, () -> helper.assertTrue(helper.getBlockState(pad).getValue(com.simplepads.block.SpawnTeleporterBlock.ACTIVE),
                        "the spawn teleporter does not show that someone is charging it"))
                .thenWaitUntil(() -> helper.assertTrue(rider.position().distanceTo(start) > 2.0, "the powered spawn teleporter did not send the player away"))
                .thenExecute(() -> {
                    helper.assertTrue(rider.position().distanceTo(bed) > 3.0,
                            "the powered spawn teleporter took the player to his bed instead of the world spawn: " + rider.position());
                    helper.assertTrue(texts.isEmpty(), "the spawn teleporter wrote on the screen: " + texts);
                })
                .thenWaitUntil(() -> helper.assertFalse(helper.getBlockState(pad).getValue(com.simplepads.block.SpawnTeleporterBlock.ACTIVE),
                        "the spawn teleporter still shows a charge after the player left"))
                .thenSucceed();
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
        helper.setBlock(elytra, PadsBlocks.ELYTRA_PAD);
        BlockPos potion = new BlockPos(5, 1, 5);
        helper.setBlock(potion, PadsBlocks.POTION_PAD);
        PotionPadBlockEntity potionPad = helper.getBlockEntity(potion, PotionPadBlockEntity.class);
        net.minecraft.world.level.block.state.properties.BooleanProperty elytraActive = com.simplepads.block.ElytraPadBlock.ACTIVE;
        net.minecraft.world.level.block.state.properties.BooleanProperty potionActive = com.simplepads.block.PotionPadBlock.ACTIVE;
        helper.assertFalse(helper.getBlockState(elytra).getValue(elytraActive), "a fresh elytra pad is active");
        helper.assertFalse(helper.getBlockState(potion).getValue(potionActive), "a fresh potion pad is active");

        // Blockstates and models (resources of this line).
        for (String id : List.of("elytra_pad", "reinforced_elytra_pad", "netherite_elytra_pad", "enderite_elytra_pad", "fine_elytra_pad",
                "spawn_teleporter", "spawn_teleporter_tier_2", "enderite_spawn_teleporter", "potion_pad", "reinforced_potion_pad", "infused_potion_pad")) {
            String blockstate = resource(helper, "/assets/simplepads/blockstates/" + id + ".json");
            helper.assertTrue(blockstate.contains("simplepads:block/" + id + "_active"), "the blockstate of " + id + " has no active model");
            String model = resource(helper, "/assets/simplepads/models/block/" + id + "_active.json");
            helper.assertTrue(model.contains("simplepads:block/" + id + "_active"), "the active model of " + id + " does not use its active texture");
            helper.assertTrue(PadOverhaulTests.class.getResource("/assets/simplepads/textures/block/" + id + "_active.png") != null,
                    "no active texture for " + id);
        }
        for (String id : List.of("diamond_pressure_plate", "netherite_pressure_plate", "enderite_pressure_plate", "copper_pressure_plate",
                "exposed_copper_pressure_plate", "weathered_copper_pressure_plate", "oxidized_copper_pressure_plate")) {
            String model = resource(helper, "/assets/simplepads/models/block/" + id + "_down.json");
            helper.assertTrue(model.contains("simplepads:block/" + id + "_active"), "the pressed " + id + " does not glow");
            helper.assertTrue(PadOverhaulTests.class.getResource("/assets/simplepads/textures/block/" + id + "_active.png") != null,
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
                    com.simplepads.block.PotionPadBlock.absorb(potionPad,
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
                    helper.assertTrue(cooling.getValue(com.simplepads.block.PotionPadBlock.COOLING)
                            && !cooling.getValue(potionActive), "a cooling potion pad is " + cooling);
                })
                .thenSucceed();
    }

    /** Real scheduled ticks: activation, cancellation, restart, cooling and bounded completion. */
    public static void padEffectsFadeInAndStopSchedulingWhenSettled(GameTestHelper helper) {
        if (!com.simplepads.util.PadsVersion.GADGET_REWORK) {
            helper.assertFalse(PadsBlocks.POTION_PAD.defaultBlockState().hasProperty(PadBlock.FADE), "fade leaked into 26.2");
            helper.succeed();
            return;
        }
        for (Block block : PadsBlocks.all()) {
            if (block instanceof com.simplepads.block.ElytraPadBlock
                    || block instanceof com.simplepads.block.FlypadBlock
                    || block instanceof com.simplepads.block.SpawnTeleporterBlock
                    || block instanceof PotionPadBlock) {
                helper.assertValueEqual(block.defaultBlockState().getValue(PadBlock.FADE), 0, "default fade of " + block);
                if (block instanceof com.simplepads.block.LegacyTierBlock) continue;
                var active = block instanceof com.simplepads.block.ElytraPadBlock ? com.simplepads.block.ElytraPadBlock.ACTIVE
                        : block instanceof com.simplepads.block.FlypadBlock ? com.simplepads.block.FlypadBlock.ACTIVE
                        : block instanceof com.simplepads.block.SpawnTeleporterBlock ? com.simplepads.block.SpawnTeleporterBlock.ACTIVE
                        : PotionPadBlock.ACTIVE;
                BlockPos sample = new BlockPos(1, 1, 1);
                helper.setBlock(sample, block);
                BlockPos absolute = helper.absolutePos(sample);
                PadBlock.setActive(helper.getLevel(), absolute, active, true);
                helper.assertTrue(helper.getLevel().getBlockTicks().hasScheduledTick(absolute, block), "no fade scheduled for " + block);
                PadBlock.setActive(helper.getLevel(), absolute, active, false);
                helper.setBlock(sample, Blocks.AIR);
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                String texture = block instanceof com.simplepads.block.FlypadBlock ? id + "_ender" : id;
                String states = resource(helper, "/assets/simplepads/blockstates/" + id + ".json");
                for (int step = 1; step <= 2; step++) {
                    helper.assertTrue(states.contains("_active_fade_" + step), "missing fade model for " + id);
                    String model = resource(helper, "/assets/simplepads/models/block/" + id + "_active_fade_" + step + ".json");
                    helper.assertTrue(model.contains(texture + "_active_fade_" + step), "wrong fade texture for " + id);
                    helper.assertTrue(PadOverhaulTests.class.getResource("/assets/simplepads/textures/block/" + texture + "_active_fade_" + step + ".png") != null,
                            "missing fade texture for " + id);
                    if (block instanceof PotionPadBlock) {
                        helper.assertTrue(states.contains("_cooling_fade_" + step), "missing cooling fade model for " + id);
                        resource(helper, "/assets/simplepads/textures/block/" + id + "_cooling_fade_" + step + ".png.mcmeta");
                    }
                }
            }
        }
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, PadsBlocks.POTION_PAD);
        BlockPos pos = helper.absolutePos(relative);
        ServerLevel level = helper.getLevel();
        PotionPadBlockEntity pad = helper.getBlockEntity(relative, PotionPadBlockEntity.class);
        UUID owner = UUID.randomUUID();
        pad.setOwner(owner);
        helper.startSequence()
                // Loading the structure does not immediately make its chunk block-ticking.
                .thenWaitUntil(() -> helper.assertTrue(level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)),
                        "pad chunk is not ready for scheduled ticks"))
                .thenExecute(() -> {
                    PotionPadBlock.absorb(pad, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                            Items.SPLASH_POTION, net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
                    PotionPadBlockEntity.serverTick(level, pos, level.getBlockState(pos), pad);
                    helper.assertTrue(level.getBlockState(pos).getValue(PotionPadBlock.ACTIVE), "loaded potion pad is not active");
                    helper.assertTrue(level.getBlockTicks().hasScheduledTick(pos, PadsBlocks.POTION_PAD), "activation did not schedule fade");
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 0, "effect must start invisible");
                })
                // GameTest sequences run before scheduled block ticks; observe the completed tick.
                .thenExecuteAfter(3, () -> {
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 1, "first fade step");
                    helper.setBlock(relative.east(), Blocks.REDSTONE_BLOCK);
                    PotionPadBlockEntity.serverTick(level, pos, level.getBlockState(pos), pad);
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 0, "cancel resets fade");
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertFalse(level.getBlockTicks().hasScheduledTick(pos, PadsBlocks.POTION_PAD), "off pad keeps scheduling ticks");
                    helper.setBlock(relative.east(), Blocks.AIR);
                    PotionPadBlockEntity.serverTick(level, pos, level.getBlockState(pos), pad);
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 0, "restart skips initial frame");
                })
                .thenExecuteAfter(3, () -> helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 1, "restart first step"))
                .thenExecuteAfter(2, () -> helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 2, "second fade step"))
                .thenExecuteAfter(2, () -> {
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 3, "full effect after six ticks");
                    helper.assertFalse(level.getBlockTicks().hasScheduledTick(pos, PadsBlocks.POTION_PAD), "settled pad keeps scheduling ticks");
                    helper.assertTrue(level.getBlockEntity(pos) == pad && owner.equals(pad.getOwner()), "fade replaced the owned block entity");
                    pad.setCooldown(100);
                    PotionPadBlockEntity.serverTick(level, pos, level.getBlockState(pos), pad);
                    helper.assertTrue(level.getBlockState(pos).getValue(PotionPadBlock.COOLING), "cooling never started");
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 0, "cooling starts without a fade");
                })
                .thenExecuteAfter(7, () -> {
                    helper.assertValueEqual(level.getBlockState(pos).getValue(PadBlock.FADE), 3, "cooling fade never finished");
                    helper.assertFalse(level.getBlockTicks().hasScheduledTick(pos, PadsBlocks.POTION_PAD), "cooling keeps scheduling fade ticks");
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


    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    private static com.google.gson.JsonObject lang(GameTestHelper helper, String code) {
        try (java.io.InputStream in = PadOverhaulTests.class.getResourceAsStream("/assets/simplepads/lang/" + code + ".json")) {
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
