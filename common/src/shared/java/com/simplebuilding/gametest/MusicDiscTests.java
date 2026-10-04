package com.simplebuilding.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.config.ServerTuningConfig;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.loot.LootInjection;
import com.simplebuilding.util.DiscFlips;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.MusicDiscs;
import com.simplebuilding.util.SpeakerBoost;
import com.simplebuilding.util.TransformTargets;
import com.simplebuilding.version.McVersion;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Schallplatten der Dimensionen, ihre B-Seiten und die Lautsprecher (2026-10-03, {@code McVersion.MUSIC_DISCS};
 * Plan {@code docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md}). Auf Linien ohne das Feature bestehen alle Tests sofort.
 *
 * <p>Nicht pruefbar im Server-Test: wie laut der Client ein Stueck hoert ({@code LevelEventHandlerSpeakerMixin}) -
 * geprueft wird die Rechnung, die er benutzt ({@link SpeakerBoost}), und alles, was der Server selbst tut.
 */
public final class MusicDiscTests {
    private MusicDiscTests() {
    }

    private static boolean enabled(GameTestHelper helper) {
        if (McVersion.MUSIC_DISCS) {
            return true;
        }
        helper.succeed();
        return false;
    }

    /** Jede Platte: Stapel 1, selten, ablegbar, mit einem geladenen Song, dessen Sound-Event registriert ist. */
    public static void discsCarryLoadedSongsWithRegisteredSounds(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        var songs = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
        List<String> problems = new ArrayList<>();
        int tracks = 0;
        for (MusicDiscs.Song song : MusicDiscs.SONGS) {
            tracks += song.tracks().size();
        }
        helper.assertValueEqual(MusicDiscs.items().size(), tracks, "disc items = tracks in MusicDiscs.SONGS");
        for (MusicDiscs.Disc disc : MusicDiscs.discs()) {
            List<Integer> trackNumbers = disc.song().tracks();
            if (trackNumbers.size() != disc.tracks().size() || trackNumbers.size() < 2) {
                problems.add(disc.song().name() + " has tracks " + trackNumbers + " but items " + disc.tracks());
                continue;
            }
            for (int t = 0; t < disc.tracks().size(); t++) {
                Item item = disc.tracks().get(t);
                String song = disc.song().trackName(trackNumbers.get(t));
                ItemStack stack = new ItemStack(item);
                JukeboxPlayable playable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
                if (playable == null) {
                    problems.add(item + " has no jukebox_playable");
                    continue;
                }
                Optional<Holder<JukeboxSong>> holder = JukeboxSong.fromStack(stack);
                if (holder.isEmpty()) {
                    problems.add(item + " names no loaded song");
                    continue;
                }
                ResourceKey<JukeboxSong> key = MusicDiscs.songKey(song);
                if (!holder.get().is(key)) {
                    problems.add(item + " plays " + holder.get() + ", expected " + key.identifier());
                }
                JukeboxSong value = holder.get().value();
                if (!value.soundEvent().is(MusicDiscs.soundId(song)) || !BuiltInRegistries.SOUND_EVENT.containsKey(MusicDiscs.soundId(song))) {
                    problems.add(song + " sound " + value.soundEvent() + " is not the registered " + MusicDiscs.soundId(song));
                }
                if (value.comparatorOutput() != disc.song().comparator() || value.lengthInSeconds() != disc.song().length(trackNumbers.get(t))) {
                    problems.add(song + " comparator " + value.comparatorOutput() + " length " + value.lengthInSeconds());
                }
                if (stack.getMaxStackSize() != 1 || stack.get(DataComponents.RARITY) != net.minecraft.world.item.Rarity.RARE) {
                    problems.add(item + " stacks to " + stack.getMaxStackSize() + ", rarity " + stack.get(DataComponents.RARITY));
                }
                if (!stack.is(ModTags.Items.PLACEABLE_SMALL)) {
                    problems.add(item + " cannot be placed as a small part");
                }
                if (!songs.containsKey(key)) {
                    problems.add(key.identifier() + " missing from the jukebox_song registry");
                }
            }
        }
        // Creeper-von-Skelett: nur die beiden Oberwelt-A-Seiten, wie Vanillas Oberwelt-Platten.
        for (Item item : MusicDiscs.items()) {
            boolean creeper = new ItemStack(item).is(ItemTags.CREEPER_DROP_MUSIC_DISCS);
            boolean expected = item == ModItems.MUSIC_DISC_DRIFTWOOD || item == ModItems.MUSIC_DISC_DAYBREAK;
            if (creeper != expected) {
                problems.add(item + (creeper ? " drops from creepers" : " does not drop from creepers"));
            }
        }
        // Die 26.3-sounds.json verdeckt die gemeinsame: sie muss den Kolbenklang weiter fuehren.
        try (InputStream in = MusicDiscTests.class.getResourceAsStream("/assets/simplebuilding/sounds.json")) {
            String sounds = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (!sounds.contains("\"block.piston.bore\"")) {
                problems.add("sounds.json lost the piston bore sound");
            }
            for (String song : MusicDiscs.songNames()) {
                if (!sounds.contains("\"music_disc." + song + "\"") || !sounds.contains("records/" + song)) {
                    problems.add("sounds.json has no streamed track for " + song);
                }
            }
        } catch (java.io.IOException e) {
            problems.add("sounds.json unreadable: " + e);
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " disc problems: " + problems);
        helper.succeed();
    }

    /** Die A-Seiten liegen in ihren Struktur-Truhen (Inject-Tabellen), die B-Seiten nirgends. */
    public static void discsLieInTheirStructureChests(GameTestHelper helper) {
        if (!enabled(helper)) return;
        var server = helper.getLevel().getServer();
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Object[][] cases = {
                {BuiltInLootTables.END_CITY_TREASURE, ModItems.MUSIC_DISC_VOIDLINE},
                {BuiltInLootTables.BASTION_OTHER, ModItems.MUSIC_DISC_BRIMSTONE},
                {BuiltInLootTables.WOODLAND_MANSION, ModItems.MUSIC_DISC_DRIFTWOOD},
                {BuiltInLootTables.ANCIENT_CITY, ModItems.MUSIC_DISC_DAYBREAK}};
        List<String> problems = new ArrayList<>();
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            ResourceKey<LootTable> key = (ResourceKey<LootTable>) c[0];
            LootTable table = server.reloadableRegistries().getLootTable(LootInjection.injectKey(key));
            String json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().toString();
            String disc = BuiltInRegistries.ITEM.getKey((Item) c[1]).toString();
            if (!json.contains("\"" + disc + "\"")) {
                problems.add(key.identifier() + " does not hold " + disc);
            }
            for (Item item : MusicDiscs.items()) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (item != c[1] && json.contains("\"" + id + "\"")) {
                    problems.add(key.identifier() + " also holds " + id);
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "disc loot: " + problems);
        helper.succeed();
    }

    /** Hammer wendet die abgelegte Platte hin und zurueck; der Plattenspieler spielt jeweils den passenden Song. */
    public static void hammerFlipsPlacedDiscBackAndForth(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
        pile.setParts(List.of(new ItemStack(Items.FLINT), new ItemStack(ModItems.MUSIC_DISC_VOIDLINE)));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));

        // Kein Hammer: eine Spitzhacke wendet nichts und neigt sich nicht.
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        helper.assertTrue(!TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "pickaxe hints a flip");
        click(helper, player, rel, pickaxe);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE), "a pickaxe flipped the disc");

        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        helper.assertTrue(TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "no hint for the hammer");
        click(helper, player, rel, hammer);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE_B_SIDE), "A -> B: " + pile.parts());
        helper.assertTrue(pile.parts().get(0).is(Items.FLINT), "the flint changed: " + pile.parts());
        helper.assertValueEqual(hammer.getDamageValue(), 1, "hammer damage after one flip");
        // Weiter ueber alle vorhandenen Tracks der Platte (End: 3 seit der Musik des Besitzers) zurueck zu Track 1.
        List<Item> tracks = MusicDiscs.discs().get(0).tracks();
        for (int i = 2; i < tracks.size(); i++) {
            click(helper, player, rel, hammer);
            helper.assertTrue(pile.parts().get(1).is(tracks.get(i)), "track " + i + " -> " + (i + 1) + ": " + pile.parts());
        }
        click(helper, player, rel, hammer);
        helper.assertTrue(pile.parts().get(1).is(ModItems.MUSIC_DISC_VOIDLINE), "last track -> track 1: " + pile.parts());
        helper.assertValueEqual(hammer.getDamageValue(), tracks.size(), "one durability per switch");

        // Ohne Platte im Haeufchen tut der Hammer hier nichts.
        pile.setParts(List.of(new ItemStack(Items.FLINT)));
        helper.assertTrue(!DiscFlips.canFlip(level, helper.absolutePos(rel), hammer), "flip without a disc");

        // Der Plattenspieler spielt die A- bzw. B-Seite.
        var songs = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
        BlockPos box = new BlockPos(4, 1, 2);
        for (Item item : List.of(ModItems.MUSIC_DISC_BRIMSTONE, ModItems.MUSIC_DISC_BRIMSTONE_B_SIDE)) {
            helper.setBlock(box, Blocks.AIR);
            helper.setBlock(box, Blocks.JUKEBOX);
            JukeboxBlockEntity jukebox = (JukeboxBlockEntity) level.getBlockEntity(helper.absolutePos(box));
            jukebox.setTheItem(new ItemStack(item));
            String expected = item == ModItems.MUSIC_DISC_BRIMSTONE ? "brimstone" : "brimstone_b_side";
            helper.assertTrue(jukebox.getSongPlayer().isPlaying()
                    && jukebox.getSongPlayer().getSong() == songs.getValue(MusicDiscs.songKey(expected)), item + " does not play " + expected);
        }
        helper.succeed();
    }

    /** Astralit nur am Plattenspieler, Nihilit nur am Notenblock; Standard: hoechstens zwei zaehlen, +50 % je Stueck. */
    public static void speakersBoostOnlyTheirOwnSource(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos jukebox = new BlockPos(2, 2, 2);
        BlockPos note = new BlockPos(6, 2, 2);
        helper.setBlock(jukebox, Blocks.JUKEBOX);
        helper.setBlock(note, Blocks.NOTE_BLOCK);
        BlockPos j = helper.absolutePos(jukebox);
        BlockPos n = helper.absolutePos(note);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.0F, "jukebox without speakers");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 3.0F, "vanilla note block volume");

        // Falsche Lautsprecher: Nihilit am Plattenspieler, Astralit am Notenblock wirken nicht.
        helper.setBlock(jukebox.east(), ModBlocks.NIHILITH_SPEAKER);
        helper.setBlock(note.east(), ModBlocks.ASTRALIT_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.0F, "nihilit speaker boosted a jukebox");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 3.0F, "astralit speaker boosted a note block");

        // Richtige Lautsprecher: einer gibt +50 %.
        helper.setBlock(jukebox.west(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(note.west(), ModBlocks.NIHILITH_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.5F, "one astralit speaker");
        helper.assertValueEqual(SpeakerBoost.noteBlockVolume(level, n), 4.5F, "one nihilit speaker");

        // Vier ringsum: es zaehlen nur zwei (Standard), also hoechstens doppelt.
        helper.setBlock(jukebox.east(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(jukebox.north(), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(jukebox.above(), ModBlocks.ASTRALIT_SPEAKER);
        helper.assertValueEqual(SpeakerBoost.adjacentSpeakers(level, j, SpeakerBoost.Source.JUKEBOX), 4, "adjacent astralit speakers");
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 2.0F, "capped at two speakers");
        helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(2.0F), 128.0, "jukebox reach with two speakers");
        helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(1.0F), 64.0, "vanilla jukebox reach");
        helper.succeed();
    }

    /** Start/Stopp jenseits von 64 Bloecken nur an die Spieler bis zur verstaerkten Hoerweite; Config-Grenzen. */
    public static void speakerRangeAndConfigCaps(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerPlayer near = helper.makeMockServerPlayerInLevel();
        ServerPlayer middle = helper.makeMockServerPlayerInLevel();
        ServerPlayer far = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> {
            level.getServer().getPlayerList().remove(near);
            level.getServer().getPlayerList().remove(middle);
            level.getServer().getPlayerList().remove(far);
        });
        near.snapTo(origin.getX() + 30.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        middle.snapTo(origin.getX() + 100.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        far.snapTo(origin.getX() + 140.5, origin.getY(), origin.getZ() + 0.5, 0.0F, 0.0F);
        // Andere Spieler der Testwelt koennen auch in der Zone stehen: gezaehlt wird nur, dass "middle" dabei ist.
        int beyond = SpeakerBoost.sendBeyondVanilla(level, origin,
                new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, origin, 0, false), SpeakerBoost.jukeboxEventRange(2.0F));
        helper.assertTrue(beyond >= 1, "the player 100 blocks away got no packet");
        helper.assertValueEqual(SpeakerBoost.sendBeyondVanilla(level, origin,
                new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, origin, 0, false), 64.0), 0, "vanilla range sends extra packets");

        ServerTuningConfig config = new ServerTuningConfig();
        config.speakers.maxSpeakers = 9;
        config.speakers.boostPercent = 500;
        config.validate();
        helper.assertValueEqual(config.speakers.maxSpeakers, ServerTuning.MAX_SPEAKERS, "max speakers cap");
        helper.assertValueEqual(config.speakers.boostPercent, ServerTuning.MAX_SPEAKER_BOOST_PERCENT, "boost cap");
        config.speakers.maxSpeakers = -4;
        config.validate();
        helper.assertValueEqual(config.speakers.maxSpeakers, 0, "max speakers floor");

        ServerTuningConfig.Speakers live = ServerTuning.local().speakers;
        int oldMax = live.maxSpeakers;
        int oldBoost = live.boostPercent;
        try {
            live.maxSpeakers = 0;
            helper.assertValueEqual(SpeakerBoost.multiplier(3), 1.0F, "maxSpeakers 0 switches speakers off");
            live.maxSpeakers = 99;
            live.boostPercent = 99;
            // Handeditierte Werte ueber der Grenze: hoechstens 3 x 50 % = 2,5-fach, Plattenspieler 160 Bloecke.
            helper.assertValueEqual(SpeakerBoost.multiplier(6), 2.5F, "hard cap of the multiplier");
            helper.assertValueEqual(SpeakerBoost.jukeboxEventRange(SpeakerBoost.maxMultiplier()), 160.0, "hard cap of the jukebox reach");
        } finally {
            live.maxSpeakers = oldMax;
            live.boostPercent = oldBoost;
        }
        helper.succeed();
    }

    /** Track-Zyklus 1 -> 2 -> 3 -> 4 -> 1 nur ueber vorhandene Tracks; Dateinamen des Besitzers je Track. */
    public static void trackCycleSkipsMissingTracks(GameTestHelper helper) {
        if (!enabled(helper)) return;
        MusicDiscs.Song gap = new MusicDiscs.Song("test", "end", 1, 3.0F, 4.0F, 0.0F, 5.0F);
        helper.assertTrue(gap.tracks().equals(List.of(1, 2, 4)), "tracks with a missing third: " + gap.tracks());
        helper.assertTrue(gap.trackName(3).equals("test_track_3") && gap.trackName(2).equals("test_b_side"), "track names");
        helper.assertTrue(gap.trackFile(2).equals("end_alt") && gap.trackFile(3).equals("end_3") && gap.trackFile(4).equals("end_4"), "owner files");
        List<String> cycle = List.of("one", "two", "four");
        helper.assertTrue("two".equals(MusicDiscs.next(cycle, "one")) && "four".equals(MusicDiscs.next(cycle, "two"))
                && "one".equals(MusicDiscs.next(cycle, "four")), "cycle 1 -> 2 -> 4 -> 1");
        helper.assertTrue(MusicDiscs.next(cycle, "three") == null && MusicDiscs.next(List.of("solo"), "solo") == null, "no cycle");
        for (MusicDiscs.Disc disc : MusicDiscs.discs()) {
            Item item = disc.aSide();
            for (int i = 0; i < disc.tracks().size(); i++) {
                item = MusicDiscs.nextTrack(item);
            }
            helper.assertTrue(item == disc.aSide(), disc.song().name() + " does not come back to track 1 after a full cycle");
            helper.assertTrue(MusicDiscs.nextTrack(disc.aSide()) == disc.bSide(), disc.song().name() + ": track 1 -> 2");
        }
        helper.assertTrue(MusicDiscs.nextTrack(Items.MUSIC_DISC_PIGSTEP) == null, "a vanilla disc has a next track");
        helper.succeed();
    }

    /** Kette: ueber mehrere Lautsprecher derselben Sorte, falsche Sorte oder Luecke unterbricht, Obergrenze aus der Config. */
    public static void speakerChainsFollowTheirKindUpToTheLimit(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos jukebox = new BlockPos(1, 2, 1);
        helper.setBlock(jukebox, Blocks.JUKEBOX);
        for (int x = 2; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 2, 1), ModBlocks.ASTRALIT_SPEAKER);
        }
        // Abzweig nach oben am dritten Lautsprecher; hinter dem letzten ein Nihilit-Lautsprecher und dahinter Astralit.
        helper.setBlock(new BlockPos(4, 3, 1), ModBlocks.ASTRALIT_SPEAKER);
        helper.setBlock(new BlockPos(7, 2, 1), ModBlocks.NIHILITH_SPEAKER);
        helper.setBlock(new BlockPos(8, 2, 1), ModBlocks.ASTRALIT_SPEAKER);
        BlockPos j = helper.absolutePos(jukebox);
        List<BlockPos> chain = SpeakerBoost.chain(level, j, SpeakerBoost.Source.JUKEBOX);
        helper.assertValueEqual(chain.size(), 6, "chain over five speakers and the branch: " + chain);
        helper.assertTrue(!chain.contains(helper.absolutePos(new BlockPos(8, 2, 1))), "the chain crossed a nihilit speaker");
        helper.assertTrue(SpeakerBoost.chain(level, j, SpeakerBoost.Source.NOTE_BLOCK).isEmpty(), "astralit speakers chain a note block");
        // Die Verstaerkung an der Quelle bleibt: ein Nachbar = +50 %.
        helper.assertValueEqual(SpeakerBoost.multiplier(level, j, SpeakerBoost.Source.JUKEBOX), 1.5F, "boost at the source");

        ServerTuningConfig.Speakers live = ServerTuning.local().speakers;
        int oldChain = live.maxChain;
        try {
            live.maxChain = 3;
            helper.assertValueEqual(SpeakerBoost.chain(level, j, SpeakerBoost.Source.JUKEBOX).size(), 3, "chain limit 3");
            live.maxChain = 0;
            helper.assertTrue(SpeakerBoost.chain(level, j, SpeakerBoost.Source.JUKEBOX).isEmpty(), "maxChain 0 switches chains off");
            live.maxChain = 1000;
            helper.assertValueEqual(ServerTuning.maxSpeakerChain(), ServerTuning.MAX_SPEAKER_CHAIN, "hard cap of the chain");
        } finally {
            live.maxChain = oldChain;
        }
        ServerTuningConfig config = new ServerTuningConfig();
        config.speakers.maxChain = 500;
        config.validate();
        helper.assertValueEqual(config.speakers.maxChain, ServerTuning.MAX_SPEAKER_CHAIN, "maxChain cap");

        // Cache: dieselbe Kette bis zur naechsten Lautsprecher-Aenderung, dann neu gesucht.
        List<BlockPos> cached = SpeakerBoost.cachedChain(level, j, SpeakerBoost.Source.JUKEBOX);
        helper.assertTrue(cached == SpeakerBoost.cachedChain(level, j, SpeakerBoost.Source.JUKEBOX), "the chain was not cached");
        helper.setBlock(new BlockPos(6, 2, 1), Blocks.AIR);
        List<BlockPos> after = SpeakerBoost.cachedChain(level, j, SpeakerBoost.Source.JUKEBOX);
        helper.assertValueEqual(after.size(), 5, "removing a speaker did not invalidate the cached chain: " + after);
        helper.succeed();
    }

    /** Notenblock-Kette: jeder Spieler hoert genau einmal, vom naechsten Punkt; der Plattenspieler-Stopp erreicht alle. */
    public static void chainedSoundReachesEachPlayerOnceAndStopReachesAll(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos note = new BlockPos(1, 2, 1);
        helper.setBlock(note, Blocks.NOTE_BLOCK);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 1), ModBlocks.NIHILITH_SPEAKER);
        }
        BlockPos n = helper.absolutePos(note);
        List<BlockPos> chain = SpeakerBoost.chain(level, n, SpeakerBoost.Source.NOTE_BLOCK);
        helper.assertValueEqual(chain.size(), 8, "note block chain");
        ServerPlayer atSource = helper.makeMockServerPlayerInLevel();
        ServerPlayer atEnd = helper.makeMockServerPlayerInLevel();
        ServerPlayer farAway = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> {
            level.getServer().getPlayerList().remove(atSource);
            level.getServer().getPlayerList().remove(atEnd);
            level.getServer().getPlayerList().remove(farAway);
        });
        BlockPos end = helper.absolutePos(new BlockPos(9, 2, 1));
        atSource.snapTo(n.getX() + 0.5, n.getY(), n.getZ() - 3.5, 0.0F, 0.0F);
        atEnd.snapTo(end.getX() + 0.5, end.getY(), end.getZ() + 40.5, 0.0F, 0.0F);
        farAway.snapTo(n.getX() + 0.5, n.getY(), n.getZ() + 5000.5, 0.0F, 0.0F);
        Map<ServerPlayer, Vec3> heard = SpeakerBoost.playChained(level, null, n, chain, SoundEvents.NOTE_BLOCK_HARP, SoundSource.RECORDS,
                SpeakerBoost.NOTE_BLOCK_VOLUME, 1.0F, 7L);
        // Je Spieler genau ein Eintrag = genau ein Klang-Paket (kein Doppel-Abspielen).
        helper.assertTrue(Vec3.atCenterOf(n).equals(heard.get(atSource)), "the player at the note block hears " + heard.get(atSource));
        helper.assertTrue(Vec3.atCenterOf(end).equals(heard.get(atEnd)), "the player at the chain end hears " + heard.get(atEnd));
        helper.assertTrue(!heard.containsKey(farAway), "a player 5000 blocks away heard the note");

        // Plattenspieler mit Kette: Start und Stopp erreichen einen Spieler 92 Bloecke hinter dem Kettenende.
        BlockPos box = new BlockPos(1, 4, 1);
        helper.setBlock(box, Blocks.JUKEBOX);
        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 4, 1), ModBlocks.ASTRALIT_SPEAKER);
        }
        BlockPos b = helper.absolutePos(box);
        List<BlockPos> jchain = SpeakerBoost.chain(level, b, SpeakerBoost.Source.JUKEBOX);
        ServerPlayer behind = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(behind));
        BlockPos jend = helper.absolutePos(new BlockPos(9, 4, 1));
        behind.snapTo(jend.getX() + 0.5 + 92.0, jend.getY(), jend.getZ() + 0.5, 0.0F, 0.0F);
        float mult = SpeakerBoost.multiplier(level, b, SpeakerBoost.Source.JUKEBOX);
        List<ServerPlayer> started = SpeakerBoost.sendBeyondVanilla(level, b, jchain,
                new ClientboundLevelEventPacket(LevelEvent.SOUND_PLAY_JUKEBOX_SONG, b, 0, false), SpeakerBoost.jukeboxEventRange(mult));
        helper.assertTrue(started.contains(behind), "the start did not reach the player behind the chain end");
        helper.assertTrue(started.stream().distinct().count() == started.size(), "a player got the start twice");
        List<ServerPlayer> stopped = SpeakerBoost.sendBeyondVanilla(level, b, List.of(),
                new ClientboundLevelEventPacket(LevelEvent.SOUND_STOP_JUKEBOX_SONG, b, 0, false), SpeakerBoost.jukeboxStopRange());
        helper.assertTrue(stopped.contains(behind), "the stop did not reach the player behind the chain end");
        helper.succeed();
    }

    /** Acht Bretter um Astralitstaub bzw. Nihilitsplitter ergeben den Lautsprecher (jede Holzart). */
    public static void speakerRecipesUseAnyPlanks(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        Object[][] cases = {{ModItems.ASTRALIT_DUST, ModItems.ASTRALIT_SPEAKER, Items.OAK_PLANKS},
                {ModItems.NIHILITH_SHARD, ModItems.NIHILITH_SPEAKER, Items.CRIMSON_PLANKS}};
        for (Object[] c : cases) {
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                grid.add(new ItemStack(i == 4 ? (Item) c[0] : (Item) c[2]));
            }
            CraftingInput input = CraftingInput.of(3, 3, grid);
            Optional<RecipeHolder<CraftingRecipe>> match = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            helper.assertTrue(match.isPresent() && match.get().value().assemble(input).is((Item) c[1]), "no speaker recipe for " + c[0]);
        }
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos rel) {
        BlockPos absolute = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atBottomCenterOf(absolute).add(0.0, 0.05, 0.0), Direction.UP, absolute, false);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(helper, rel));
    }
}
