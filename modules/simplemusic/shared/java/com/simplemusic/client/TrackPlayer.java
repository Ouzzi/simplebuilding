package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackDataPayload;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Client side of {@link com.simplemusic.disc.DiscPlayback}: one {@link CustomDiscSound} per playing block. A track not
 * in the cache is requested once; when it arrives the sound starts where the song is by then (server offset plus the
 * ticks waited).
 */
public final class TrackPlayer {
    private record Pending(String hash, int offsetTicks, long receivedTick) {}

    private static final Map<BlockPos, CustomDiscSound> PLAYING = new HashMap<>();
    private static final Map<BlockPos, Pending> WAITING = new HashMap<>();
    private static long ticks;

    private TrackPlayer() {}

    public static void receive(TrackPlayPayload payload) {
        if (payload.carried()) {
            PocketPlayer.receive(payload);
            return;
        }
        BlockPos pos = payload.pos();
        stop(pos);
        if (payload.isStop()) return;
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

    private static void stop(BlockPos pos) {
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
        PocketPlayer.clear();
    }
}
