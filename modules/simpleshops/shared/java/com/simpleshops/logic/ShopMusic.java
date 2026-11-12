package com.simpleshops.logic;

import com.simpleshops.SimpleShops;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.config.ShopsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Shop music, server side (a-shopmusic37, plan docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md): the sales block holds one music disc
 * (any item with Vanilla's {@code jukebox_playable}, so Simple Shops alone plays Vanilla discs) like a jukebox - in, it plays;
 * out, it stops. The server only stores the disc and the start tick and sends both with the block update; it never ticks the
 * music. Listening, grouping and "nearest shop wins" happen on each client ({@link ShopMusicPick}).
 */
public final class ShopMusic {
    /** A disc put into a shop takes the start tick of a loaded shop with the same song this close (blocks), so they play in step. */
    public static final int SYNC_RANGE = 64;

    private ShopMusic() {}

    /** The feature is on: module on and the server switch {@code shopMusic}. */
    public static boolean on() {
        return SimpleShops.on() && ShopsConfig.shopMusic;
    }

    /** A music disc: any item that a jukebox plays. */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    /**
     * The song of a disc as an id (its sound event), or {@code null}. Same id = same piece: shops sync and group per song, not per
     * item, so the A- and B-side of a disc (Simple Music, a-shopmusic37b) are two pieces.
     */
    public static @Nullable String song(ItemStack stack) {
        if (!accepts(stack)) return null;
        return JukeboxSong.fromStack(stack).map(h -> h.value().soundEvent().value().location().toString()).orElse(null);
    }

    /** Length of the disc's song in seconds (0 when unknown). */
    public static float seconds(ItemStack stack) {
        return JukeboxSong.fromStack(stack).map(h -> h.value().lengthInSeconds()).orElse(0.0F);
    }

    /**
     * The start tick for {@code disc} put into the shop at {@code pos}: the start of the nearest loaded shop with the same song
     * within {@link #SYNC_RANGE} blocks, otherwise now.
     */
    public static long startFor(Level level, BlockPos pos, ItemStack disc) {
        String song = song(disc);
        long now = level.getGameTime();
        if (song == null) return now;
        SalesBlockEntity best = null;
        double bestDistance = (double) SYNC_RANGE * SYNC_RANGE;
        int r = SYNC_RANGE;
        for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
            for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof SalesBlockEntity other) || other.getBlockPos().equals(pos) || !song.equals(song(other.disc()))) continue;
                    double d = other.getBlockPos().distSqr(pos);
                    if (d <= bestDistance) {
                        bestDistance = d;
                        best = other;
                    }
                }
            }
        }
        return best != null ? best.discStart() : now;
    }

    /**
     * Owner's right click with a disc in hand on a shop without one: the disc goes in and plays (like a jukebox). Returns false
     * when nothing happened (then the click opens the menu as before).
     */
    public static boolean tryInsert(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
        if (!on() || !(level.getBlockEntity(pos) instanceof SalesBlockEntity sales) || !sales.mayEdit(player) || !sales.disc().isEmpty()) return false;
        ItemStack held = player.getItemInHand(hand);
        if (!accepts(held) || !sales.lockFree()) return false;
        sales.setDisc(held.consumeAndReturn(1, player));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 0.8F);
        return true;
    }
}
