package com.simplemusic.disc;

import com.simplemusic.SimpleMusic;
import com.simplemusic.config.MusicConfig;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

/**
 * Custom discs in the Vanilla jukebox (owner decision U1 b, mixins {@code JukeboxBlockMixin} and
 * {@code JukeboxBlockEntityMixin} on every loader). The jukebox holds the disc like a Vanilla one; a track runs
 * through {@link DiscPlayback}, a pressed composition through {@link SheetPlayback} (server tones), each once like a
 * Vanilla disc (no loop).
 *
 * <p>Guard: config {@code jukeboxCustomDiscs} and no Vanilla jukebox playlist. SimpleBuilding's chest-above playlist
 * back port ({@code JukeboxPlaylist}) only moves discs with a Vanilla {@code jukebox_song} and leaves custom discs
 * alone; the real 26.4 feature (block tag {@code #minecraft:jukebox_containers}) is not known yet in detail, so with
 * that tag present the jukebox path switches itself off (custom discs then play only in the Pocket Jukebox)
 * (docs/ai/PLAN-B-MUSIC-2026-10-20.md, port note).
 */
public final class JukeboxDiscs {
    private static final TagKey<Block> VANILLA_PLAYLIST = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("jukebox_containers"));
    private static final Set<GlobalPos> FINISHED = new HashSet<>();

    private JukeboxDiscs() {}

    public static boolean on(Level level) {
        if (!SimpleMusic.on() || !MusicConfig.jukeboxCustomDiscs) return false;
        return level.registryAccess().lookupOrThrow(Registries.BLOCK).get(VANILLA_PLAYLIST).map(tag -> tag.size() == 0).orElse(true);
    }

    public static boolean isDisc(ItemStack stack) {
        return CustomDiscItem.hasMusic(stack);
    }

    /** After the Vanilla jukebox tick (server). */
    public static void tick(ServerLevel level, BlockPos pos, JukeboxBlockEntity jukebox) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        ItemStack item = jukebox.getTheItem();
        if (!isDisc(item) || !on(level)) {
            FINISHED.remove(key);
            if (DiscPlayback.playing(level, pos)) DiscPlayback.stop(level, pos);
            SheetPlayback.stop(level, pos);
            return;
        }
        if (FINISHED.contains(key)) return;
        TrackRef track = CustomDiscItem.track(item);
        if (track == null) {
            // a pressed composition: server tones like a note block, once
            if (!SheetPlayback.playing(level, pos) && !SheetPlayback.start(level, pos, CustomDiscItem.composition(item).code())) {
                FINISHED.add(key);
                return;
            }
            if (SheetPlayback.tick(level, pos)) FINISHED.add(key);
            return;
        }
        if (!DiscPlayback.playing(level, pos)) {
            DiscPlayback.start(level, pos, track);
            return;
        }
        if (DiscPlayback.finished(level, pos)) {
            DiscPlayback.stop(level, pos);
            FINISHED.add(key);
            return;
        }
        if (level.getGameTime() % 20 == 0) DiscPlayback.update(level, pos);
    }

    /** Disc taken out or jukebox gone. */
    public static void removed(ServerLevel level, BlockPos pos) {
        FINISHED.remove(GlobalPos.of(level.dimension(), pos));
        DiscPlayback.stop(level, pos);
        SheetPlayback.stop(level, pos);
    }
}
