package com.simpleshops.client;

import com.simplelib.api.client.SoundSeek;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.logic.ShopMusic;
import com.simpleshops.logic.ShopMusicPick;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Shop music on the client (a-shopmusic37, plan docs/ai/PLAN-A-SHOPMUSIC37-2026-11-12.md), called once per client tick by
 * the loader. Every {@link #RESCAN_TICKS} ticks it collects the sales blocks with a disc in the loaded chunks around the player
 * (the server sent disc, start tick and reach with the block update); {@link ShopMusicPick} decides which group plays. One
 * sound for that group, at its shop nearest to the player, joined at the right moment of the loop; nothing plays out of reach.
 * Music notes rise above the playing shops like above a jukebox (A14: the player sees where the music comes from).
 */
public final class ShopMusicClient {
    /** So often the shops around the player are collected again. */
    public static final int RESCAN_TICKS = 10;
    /** Shops this far away are considered (covers the largest reach, 48 x 2.5 blocks). */
    public static final int SCAN = 128;
    /** Notes rise above playing shops this close to the player. */
    private static final double NOTE_RANGE = 24.0;

    private static List<ShopMusicPick.Shop> shops = List.of();
    private static ShopMusicSound playing;
    private static ShopMusicPick.Shop playingShop;
    private static long startedAt;
    private static int age;

    private ShopMusicClient() {}

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            release();
            shops = List.of();
            return;
        }
        if (age++ % RESCAN_TICKS == 0) shops = scan(level, mc.player.blockPosition());
        Vec3 ear = mc.player.getEyePosition();
        ShopMusicPick.Pick pick = shops.isEmpty() ? null : ShopMusicPick.pick(shops, ear.x, ear.y, ear.z, playingShop);
        if (pick == null) {
            release();
            return;
        }
        ShopMusicPick.Shop at = pick.at();
        boolean lost = playing != null && level.getGameTime() - startedAt > 40 && !mc.getSoundManager().isActive(playing);
        if (playing == null || lost || !playing.song.equals(at.song()) || playing.start != at.start()) {
            release();
            Identifier event = Identifier.tryParse(at.song());
            int offset = ShopMusicPick.offsetSeconds(level.getGameTime(), at.start(), at.seconds());
            Identifier file = event == null ? null : SoundSeek.file(mc.getSoundManager(), event, offset);
            if (file == null) return;
            playing = new ShopMusicSound(file, at.song(), at.start());
            playing.aim(at.x(), at.y(), at.z(), pick.gain());
            mc.getSoundManager().play(playing);
            startedAt = level.getGameTime();
        }
        playing.aim(at.x(), at.y(), at.z(), pick.gain());
        playingShop = at;
        if (level.getGameTime() % 20 == 0) notes(level, pick, ear);
    }

    /** The playing group fades out (leaving its reach, disc taken out, another group takes over). */
    private static void release() {
        if (playing != null) playing.leave();
        playing = null;
        playingShop = null;
    }

    /** Sales blocks with a disc (and the server's music switch on) within {@link #SCAN} blocks, from the loaded chunks. */
    static List<ShopMusicPick.Shop> scan(ClientLevel level, BlockPos center) {
        List<ShopMusicPick.Shop> out = new ArrayList<>();
        long range = (long) SCAN * SCAN;
        for (int cx = (center.getX() - SCAN) >> 4; cx <= (center.getX() + SCAN) >> 4; cx++) {
            for (int cz = (center.getZ() - SCAN) >> 4; cz <= (center.getZ() + SCAN) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof SalesBlockEntity sales) || sales.musicRange() <= 0 || sales.disc().isEmpty()) continue;
                    BlockPos pos = sales.getBlockPos();
                    if (pos.distSqr(center) > range) continue;
                    String song = ShopMusic.song(sales.disc());
                    float seconds = ShopMusic.seconds(sales.disc());
                    if (song == null || seconds <= 0.0F) continue;
                    out.add(new ShopMusicPick.Shop(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, song, sales.discStart(), seconds, sales.musicRange()));
                }
            }
        }
        return out;
    }

    /** A music note above each playing shop of the group near the player (above the display case when there is one). */
    private static void notes(ClientLevel level, ShopMusicPick.Pick pick, Vec3 ear) {
        for (ShopMusicPick.Shop shop : pick.group()) {
            if (ear.distanceToSqr(shop.x(), shop.y(), shop.z()) > NOTE_RANGE * NOTE_RANGE) continue;
            BlockPos pos = BlockPos.containing(shop.x(), shop.y(), shop.z());
            double top = pos.getY() + (level.getBlockEntity(pos.above()) instanceof DisplayCaseBlockEntity ? 2.2 : 1.2);
            level.addParticle(ParticleTypes.NOTE, shop.x(), top, shop.z(), level.getRandom().nextInt(4) / 24.0, 0.0, 0.0);
        }
    }
}
