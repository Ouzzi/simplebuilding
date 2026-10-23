package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * Client side of {@link com.simplemusic.pocket.PocketPlayback}: the one Pocket Jukebox sound of this player. Starting
 * stops the background music and every other disc sound (jukeboxes); an upload not in the cache is
 * requested once and starts where the song is by then.
 */
public final class PocketPlayer {
    private record Pending(String hash, int offsetTicks, long receivedTick) {}

    private static PocketSound playing;
    private static Pending waiting;
    private static long ticks;

    private PocketPlayer() {}

    public static void receive(TrackPlayPayload payload) {
        stop();
        if (payload.isStop()) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        mc.getSoundManager().stop(null, SoundSource.RECORDS);
        if (payload.isSong()) {
            Identifier event = Identifier.tryParse(payload.sha256());
            if (event != null) start(PocketSound.song(mc.getSoundManager(), event, payload.offsetTicks() / 20));
        } else if (TrackCache.has(payload.sha256())) {
            start(PocketSound.upload(payload.sha256(), payload.offsetTicks() / 20));
        } else {
            waiting = new Pending(payload.sha256(), payload.offsetTicks(), ticks);
            SimpleMusic.toServer.accept(new TrackRequestPayload(payload.sha256()));
        }
    }

    /** A download finished (called by {@link TrackPlayer}). */
    static void arrived(String hash) {
        if (waiting == null || !waiting.hash().equals(hash)) return;
        int offset = (int) ((waiting.offsetTicks() + ticks - waiting.receivedTick()) / 20);
        waiting = null;
        start(PocketSound.upload(hash, offset));
    }

    private static void start(PocketSound sound) {
        if (sound == null) return;
        playing = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void stop() {
        waiting = null;
        if (playing != null) Minecraft.getInstance().getSoundManager().stop(playing);
        playing = null;
    }

    public static void tick() {
        ticks++;
        if (Minecraft.getInstance().level == null && (playing != null || waiting != null)) stop();
    }

    /** Level change / disconnect. */
    public static void clear() {
        stop();
    }
}
