package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.JukeboxPlaylist;
import com.simplebuilding.util.SpeakerBoost;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

/**
 * Jukebox with a chest above it ({@code McVersion.JUKEBOX_CHEST_PLAYLIST}, queue N62, back port of the 26.4 snapshot 4
 * vanilla feature; facts in docs/ai/BERICHT-OC-JUKEBOX.md). Lines without the feature (or with vanilla's own) pass at once.
 */
public final class JukeboxPlaylistTests {
    private static final BlockPos JUKEBOX = new BlockPos(1, 2, 1);
    private static final BlockPos CHEST = new BlockPos(1, 3, 1);

    private JukeboxPlaylistTests() {
    }

    private static boolean enabled(GameTestHelper helper) {
        if (McVersion.JUKEBOX_CHEST_PLAYLIST) {
            return true;
        }
        helper.succeed();
        return false;
    }

    private static JukeboxBlockEntity jukebox(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, Blocks.JUKEBOX);
        return (JukeboxBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos rel, Item... slots) {
        helper.setBlock(rel, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        for (int i = 0; i < slots.length; i++) {
            chest.setItem(i, slots[i] == null ? ItemStack.EMPTY : new ItemStack(slots[i]));
        }
        return chest;
    }

    /** Lets the song run out the way vanilla does (the disc stays in the jukebox). */
    private static void finish(JukeboxBlockEntity jukebox) {
        jukebox.getSongPlayer().stop(jukebox.getLevel(), jukebox.getBlockState());
    }

    /** Puts the playing song three ticks from its end so the real tick path ends it. */
    private static void nearEnd(JukeboxBlockEntity jukebox) {
        Holder<JukeboxSong> song = JukeboxSong.fromStack(jukebox.getTheItem()).orElseThrow();
        jukebox.getSongPlayer().setSongWithoutPlaying(song, song.value().lengthInTicks() + 20L - 3L);
    }

    private static void assertPlaying(GameTestHelper helper, JukeboxBlockEntity jukebox, Item disc, String what) {
        helper.assertTrue(jukebox.getTheItem().is(disc), what + ": jukebox holds " + jukebox.getTheItem());
        helper.assertTrue(jukebox.getSongPlayer().isPlaying(), what + ": not playing");
        helper.assertTrue(jukebox.getBlockState().getValue(net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD), what + ": has_record false");
    }

    /** An empty jukebox takes the first disc (skipping empty slots and other items) by itself, no hopper needed. */
    public static void emptyJukeboxTakesFirstDiscOfTheChest(GameTestHelper helper) {
        if (!enabled(helper)) return;
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        ChestBlockEntity chest = chest(helper, CHEST, Items.DIRT, null, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_13);
        helper.runAfterDelay(15, () -> {
            assertPlaying(helper, jukebox, Items.MUSIC_DISC_CAT, "first disc");
            helper.assertTrue(chest.getItem(2).isEmpty(), "the disc stayed in the chest");
            helper.assertTrue(chest.getItem(0).is(Items.DIRT) && chest.getItem(3).is(Items.MUSIC_DISC_13), "other slots changed");
            helper.succeed();
        });
    }

    /** A disc that ran out through the real tick path swaps with the next disc; the finished one lies in its slot. */
    public static void finishedDiscSwapsWithTheNextOne(GameTestHelper helper) {
        if (!enabled(helper)) return;
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        ChestBlockEntity chest = chest(helper, CHEST, Items.DIRT, null, null, null, null, Items.MUSIC_DISC_13);
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
        nearEnd(jukebox);
        helper.runAfterDelay(8, () -> {
            assertPlaying(helper, jukebox, Items.MUSIC_DISC_13, "next disc");
            helper.assertTrue(chest.getItem(5).is(Items.MUSIC_DISC_CAT), "the finished disc did not go to the slot of the next one: " + chest.getItem(5));
            helper.assertTrue(chest.getItem(0).is(Items.DIRT), "dirt changed");
            helper.succeed();
        });
    }

    /** Slot order with skipping and wrapping: A, B, C, A, ... also with a disc of this mod in the list. */
    public static void discsPlayInSlotOrderAndWrapAround(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        Item modDisc = ModItems.MUSIC_DISC_VOIDLINE != null ? ModItems.MUSIC_DISC_VOIDLINE : Items.MUSIC_DISC_BLOCKS;
        chest(helper, CHEST, Items.MUSIC_DISC_CAT, Items.DIRT, Items.MUSIC_DISC_13, null, modDisc);
        BlockPos j = helper.absolutePos(JUKEBOX);
        Item[] expected = {Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_13, modDisc, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_13};
        for (int i = 0; i < expected.length; i++) {
            helper.assertTrue(JukeboxPlaylist.step(level, j, jukebox), "step " + i + " did nothing");
            assertPlaying(helper, jukebox, expected[i], "step " + i);
            finish(jukebox);
        }
        helper.succeed();
    }

    /** Without another disc in the chest the finished disc plays again (vanilla loops it) - also with an empty chest. */
    public static void lonelyDiscLoops(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        ChestBlockEntity chest = chest(helper, CHEST, Items.DIRT);
        BlockPos j = helper.absolutePos(JUKEBOX);
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
        finish(jukebox);
        helper.assertTrue(!jukebox.getSongPlayer().isPlaying(), "song did not end");
        helper.assertTrue(JukeboxPlaylist.step(level, j, jukebox), "no loop with only dirt in the chest");
        assertPlaying(helper, jukebox, Items.MUSIC_DISC_CAT, "loop (dirt chest)");
        chest.setItem(0, ItemStack.EMPTY);
        finish(jukebox);
        helper.assertTrue(JukeboxPlaylist.step(level, j, jukebox), "no loop with an empty chest");
        assertPlaying(helper, jukebox, Items.MUSIC_DISC_CAT, "loop (empty chest)");
        helper.assertTrue(chest.isEmpty(), "the loop moved items into the chest");
        helper.succeed();
    }

    /** Nothing to take: empty chest, a chest of other items, no chest, a double chest; a lone disc also stays put. */
    public static void nothingHappensWithoutPlaylist(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        BlockPos j = helper.absolutePos(JUKEBOX);
        chest(helper, CHEST);
        helper.assertTrue(!JukeboxPlaylist.step(level, j, jukebox) && jukebox.getTheItem().isEmpty(), "took something from an empty chest");
        chest(helper, CHEST, Items.DIRT, Items.STICK);
        helper.assertTrue(!JukeboxPlaylist.step(level, j, jukebox) && jukebox.getTheItem().isEmpty(), "took a non-disc");
        helper.setBlock(CHEST, Blocks.AIR);
        helper.assertTrue(!JukeboxPlaylist.step(level, j, jukebox) && jukebox.getTheItem().isEmpty(), "no chest but something happened");
        // Double chest: both halves hold discs, vanilla only uses a single chest.
        helper.setBlock(CHEST, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(CHEST.east(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ((ChestBlockEntity) level.getBlockEntity(helper.absolutePos(CHEST))).setItem(0, new ItemStack(Items.MUSIC_DISC_CAT));
        helper.assertTrue(!JukeboxPlaylist.step(level, j, jukebox) && jukebox.getTheItem().isEmpty(), "used a double chest");
        helper.succeed();
    }

    /** A full chest still swaps (the finished disc fits where the next one was); a full chest of other items does nothing. */
    public static void fullChestStillSwaps(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        BlockPos j = helper.absolutePos(JUKEBOX);
        ChestBlockEntity chest = chest(helper, CHEST);
        for (int i = 0; i < chest.getContainerSize(); i++) {
            chest.setItem(i, new ItemStack(Items.DIRT));
        }
        helper.assertTrue(!JukeboxPlaylist.step(level, j, jukebox) && jukebox.getTheItem().isEmpty(), "took a non-disc from a full chest");
        chest.setItem(chest.getContainerSize() - 1, new ItemStack(Items.MUSIC_DISC_13));
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
        finish(jukebox);
        helper.assertTrue(JukeboxPlaylist.step(level, j, jukebox), "no swap with a full chest");
        assertPlaying(helper, jukebox, Items.MUSIC_DISC_13, "full chest swap");
        helper.assertTrue(chest.getItem(chest.getContainerSize() - 1).is(Items.MUSIC_DISC_CAT), "the finished disc is not in the chest");
        boolean full = true;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            full &= !chest.getItem(i).isEmpty();
        }
        helper.assertTrue(full, "the chest lost an item");
        helper.succeed();
    }

    /** Taking the disc out by hand starts the next one (vanilla: ejecting skips to the next disc) - not the same again. */
    public static void ejectingSkipsToTheNextDisc(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        BlockPos j = helper.absolutePos(JUKEBOX);
        chest(helper, CHEST, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_13, Items.MUSIC_DISC_BLOCKS);
        JukeboxPlaylist.step(level, j, jukebox);
        assertPlaying(helper, jukebox, Items.MUSIC_DISC_CAT, "first");
        jukebox.popOutTheItem();
        helper.assertTrue(jukebox.getTheItem().isEmpty(), "the disc did not come out");
        JukeboxPlaylist.step(level, j, jukebox);
        assertPlaying(helper, jukebox, Items.MUSIC_DISC_13, "after ejecting");
        helper.succeed();
    }

    /** The place in the playlist is saved with the jukebox and read back. */
    public static void playlistPositionIsSavedWithTheJukebox(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        JukeboxBlockEntity jukebox = jukebox(helper, JUKEBOX);
        BlockPos j = helper.absolutePos(JUKEBOX);
        chest(helper, CHEST, Items.DIRT, Items.MUSIC_DISC_13);
        JukeboxPlaylist.step(level, j, jukebox);
        JukeboxPlaylist.Cursor cursor = (JukeboxPlaylist.Cursor) jukebox;
        helper.assertValueEqual(cursor.simplebuilding$getPlaylistSlot(), 1, "cursor after the first disc");
        var tag = jukebox.saveCustomOnly(level.registryAccess());
        helper.assertValueEqual(tag.getIntOr("simplebuilding_playlist_slot", -1), 1, "saved cursor");
        cursor.simplebuilding$setPlaylistSlot(-1);
        jukebox.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
        helper.assertValueEqual(cursor.simplebuilding$getPlaylistSlot(), 1, "loaded cursor");
        helper.succeed();
    }

    /** Exactly one implementation: the mod part is active here because vanilla has no #jukebox_containers tag yet. */
    public static void modPartIsActiveOnlyWithoutVanillasOwn(GameTestHelper helper) {
        if (!enabled(helper)) return;
        helper.assertTrue(JukeboxPlaylist.active(helper.getLevel()), "the mod playlist is off although the line has the flag and vanilla lacks the tag");
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.BLOCK)
                .get(net.minecraft.tags.TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.withDefaultNamespace("jukebox_containers")))
                .map(t -> t.size() == 0).orElse(true), "vanilla has the tag, the flag must be off on this line");
        helper.succeed();
    }

    /** Jukebox amplifier: a swap is a new start event, so the amplifier chain carries the next disc to a far listener. */
    public static void amplifierCarriesTheSwappedDisc(GameTestHelper helper) {
        if (!enabled(helper)) return;
        ServerLevel level = helper.getLevel();
        BlockPos jukeboxRel = new BlockPos(2, 30, 2);
        BlockPos chestRel = jukeboxRel.above();
        BlockPos ampRel = new BlockPos(2, 90, 2);
        JukeboxBlockEntity jukebox = jukebox(helper, jukeboxRel);
        chest(helper, chestRel, Items.MUSIC_DISC_13);
        helper.setBlock(ampRel, com.simplebuilding.blocks.ModBlocks.JUKEBOX_AMPLIFIER);
        BlockPos j = helper.absolutePos(jukeboxRel);
        List<Packet<?>> sink = new ArrayList<>();
        ServerPlayer far = MusicDiscTests.capturingPlayer(helper, sink);
        BlockPos absAmp = helper.absolutePos(ampRel);
        far.snapTo(absAmp.getX() + 0.5, absAmp.getY() + 40.0, absAmp.getZ() + 0.5, 0.0F, 0.0F);
        helper.assertTrue(far.position().distanceTo(Vec3.atCenterOf(j)) > 64.0, "far player inside vanilla range");
        helper.assertTrue(SpeakerBoost.inRelayRange(level, j, SpeakerBoost.Source.JUKEBOX).contains(absAmp), "the amplifier is not in radio range");
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));
        finish(jukebox);
        helper.assertTrue(JukeboxPlaylist.step(level, j, jukebox), "no swap");
        int song13 = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(JukeboxSong.fromStack(new ItemStack(Items.MUSIC_DISC_13)).orElseThrow().value());
        boolean heard = MusicDiscTests.captured(sink, ClientboundLevelEventPacket.class).stream()
                .anyMatch(e -> e.getType() == LevelEvent.SOUND_PLAY_JUKEBOX_SONG && e.getPos().equals(j) && e.getData() == song13);
        helper.assertTrue(heard, "the far listener did not get the start of the swapped-in disc");
        helper.succeed();
    }
}
