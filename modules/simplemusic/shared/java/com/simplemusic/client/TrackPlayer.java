package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.phys.AABB;

/**
 * Client side of {@link com.simplemusic.disc.DiscPlayback}: one {@link CustomDiscSound} per playing block. A track not
 * in the cache is requested once; when it arrives the sound starts where the song is by then (server offset plus the
 * ticks waited). A Vanilla disc of a placed Pocket Jukebox (sound event key) starts at once mid-song, shows "Now playing"
 * and lets parrots dance like Vanilla's jukebox.
 */
public final class TrackPlayer {
    private record Pending(String hash, int offsetTicks, long receivedTick) {}

    private static final Map<BlockPos, CustomDiscSound> PLAYING = new HashMap<>();
    private static final Map<BlockPos, Pending> WAITING = new HashMap<>();
    /** Blocks playing a Vanilla song (parrots dance there). */
    private static final Set<BlockPos> SONGS = new HashSet<>();
    private static long ticks;

    private TrackPlayer() {}

    public static void receive(TrackPlayPayload payload) {
        if (payload.carried() || payload.session() != 0L) {
            // a Pocket Jukebox session (carried, dropped or placed): one sound that moves with it (PJ23-5)
            PocketPlayer.receive(payload);
            return;
        }
        BlockPos pos = payload.pos();
        stop(pos);
        if (payload.isStop()) return;
        if (payload.isSong()) {
            startSong(pos, payload);
            return;
        }
        if (MusicClient.refuseForeign(payload.sha256())) return;
        Pending pending = new Pending(payload.sha256(), payload.offsetTicks(), ticks);
        if (TrackCache.has(payload.sha256())) {
            start(pos, pending);
        } else {
            boolean asked = WAITING.values().stream().anyMatch(p -> p.hash().equals(payload.sha256()));
            WAITING.put(pos, pending);
            if (!asked) SimpleMusic.toServer.accept(new TrackRequestPayload(payload.sha256()));
        }
    }

    public static void receive(TrackDataPayload chunk) {
        if (!TrackCache.receive(chunk)) return;
        PocketPlayer.arrived(chunk.sha256());
        WAITING.entrySet().removeIf(e -> {
            if (!e.getValue().hash().equals(chunk.sha256())) return false;
            start(e.getKey(), e.getValue());
            return true;
        });
    }

    private static void start(BlockPos pos, Pending pending) {
        int offset = (int) ((pending.offsetTicks() + ticks - pending.receivedTick()) / 20);
        CustomDiscSound sound = new CustomDiscSound(pos, pending.hash(), offset);
        PLAYING.put(pos, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void startSong(BlockPos pos, TrackPlayPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        Identifier event = Identifier.tryParse(payload.sha256());
        Identifier file = event == null ? null : PocketSound.songFile(mc.getSoundManager(), event, payload.offsetTicks() / 20);
        if (file == null || mc.level == null) return;
        CustomDiscSound sound = new CustomDiscSound(pos, file);
        PLAYING.put(pos, sound);
        mc.getSoundManager().play(sound);
        mc.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).stream()
                .filter(song -> song.soundEvent().value().location().equals(event)).findFirst()
                .map(JukeboxSong::description).ifPresent(mc.gui.hud::setNowPlaying);
        SONGS.add(pos);
        dance(pos, true);
    }

    /** Parrots and allays near {@code pos} dance (or stop), like at Vanilla's jukebox. */
    static void dance(BlockPos pos, boolean playing) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0))) {
            entity.setRecordPlayingNearby(pos, playing);
        }
    }

    private static void stop(BlockPos pos) {
        if (SONGS.remove(pos)) dance(pos, false);
        WAITING.remove(pos);
        CustomDiscSound old = PLAYING.remove(pos);
        if (old != null) Minecraft.getInstance().getSoundManager().stop(old);
    }

    public static void tick() {
        ticks++;
        if (Minecraft.getInstance().level == null && (!PLAYING.isEmpty() || !WAITING.isEmpty())) clear();
    }

    /** Level change / disconnect. */
    public static void clear() {
        for (CustomDiscSound s : PLAYING.values()) Minecraft.getInstance().getSoundManager().stop(s);
        PLAYING.clear();
        WAITING.clear();
        SONGS.clear();
        PocketPlayer.clear();
    }
}
