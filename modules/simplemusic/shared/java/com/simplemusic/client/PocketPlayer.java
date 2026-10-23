package com.simplemusic.client;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.net.TrackRequestPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.JukeboxSong;

/**
 * Client side of the Pocket Jukebox sessions ({@link com.simplemusic.pocket.PocketPlayback}, placed ones through
 * {@link com.simplemusic.disc.DiscPlayback}): one {@link PocketSound} per session. A play message of a session that
 * already plays the same song only moves the sound (carried / dropped item / block, PJ23-5) - no gap, no jump; anything
 * else starts it anew. Starting a carried one stops the background music and the other disc sounds; an upload not in
 * the cache is requested once and starts where the song is by then. A Vanilla disc heard at a block or a dropped item
 * shows "Now playing", and parrots dance at a block like at Vanilla's jukebox.
 */
public final class PocketPlayer {
    private static final class Entry {
        final String key;
        PocketSound sound;
        PocketSound.Anchor anchor;
        /** Waiting for the upload: the server's offset and when it came. */
        int offsetTicks;
        long receivedTick;

        Entry(String key, PocketSound.Anchor anchor) {
            this.key = key;
            this.anchor = anchor;
        }
    }

    private static final Map<Long, Entry> SESSIONS = new HashMap<>();
    private static long ticks;

    private PocketPlayer() {}

    public static void receive(TrackPlayPayload payload) {
        long id = payload.session();
        if (payload.isStop()) {
            stop(id);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        PocketSound.Anchor anchor = payload.carried() ? PocketSound.Anchor.carried()
                : new PocketSound.Anchor(false, payload.entity(), payload.pos());
        if (payload.carried()) mc.getMusicManager().stopPlaying();
        Entry old = SESSIONS.get(id);
        if (old != null && old.key.equals(payload.sha256())) {
            if (old.sound != null && mc.getSoundManager().isActive(old.sound)) {
                move(old, anchor);
                return;
            }
            if (old.sound == null) {
                // still downloading: it starts at the new place
                move(old, anchor);
                return;
            }
        }
        stop(id);
        if (payload.carried()) mc.getSoundManager().stop(null, SoundSource.RECORDS);
        Entry entry = new Entry(payload.sha256(), anchor);
        SESSIONS.put(id, entry);
        if (payload.isSong()) {
            Identifier event = Identifier.tryParse(payload.sha256());
            if (event == null) return;
            start(entry, PocketSound.song(mc.getSoundManager(), event, payload.offsetTicks() / 20, anchor));
            if (!payload.carried() && mc.level != null) {
                mc.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).stream()
                        .filter(song -> song.soundEvent().value().location().equals(event)).findFirst()
                        .map(JukeboxSong::description).ifPresent(mc.gui.hud::setNowPlaying);
            }
        } else if (TrackCache.has(payload.sha256())) {
            start(entry, PocketSound.upload(payload.sha256(), payload.offsetTicks() / 20, anchor));
        } else {
            entry.offsetTicks = payload.offsetTicks();
            entry.receivedTick = ticks;
            boolean asked = SESSIONS.values().stream().anyMatch(e -> e != entry && e.sound == null && e.key.equals(payload.sha256()));
            if (!asked) SimpleMusic.toServer.accept(new TrackRequestPayload(payload.sha256()));
        }
    }

    /** A download finished (called by {@link TrackPlayer}). */
    static void arrived(String hash) {
        for (Entry e : SESSIONS.values()) {
            if (e.sound != null || !e.key.equals(hash)) continue;
            int offset = (int) ((e.offsetTicks + ticks - e.receivedTick) / 20);
            start(e, PocketSound.upload(hash, offset, e.anchor));
        }
    }

    private static void start(Entry entry, PocketSound sound) {
        if (sound == null) return;
        entry.sound = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
        dance(entry.anchor, true);
    }

    private static void move(Entry entry, PocketSound.Anchor anchor) {
        if (entry.sound != null) dance(entry.anchor, false);
        entry.anchor = anchor;
        if (entry.sound != null) {
            entry.sound.moveTo(anchor);
            dance(anchor, true);
        }
    }

    private static void dance(PocketSound.Anchor anchor, boolean on) {
        if (!anchor.self() && anchor.entity() < 0) TrackPlayer.dance(anchor.pos(), on);
    }

    private static void stop(long id) {
        Entry e = SESSIONS.remove(id);
        if (e == null || e.sound == null) return;
        dance(e.anchor, false);
        e.sound.stopNow();
        Minecraft.getInstance().getSoundManager().stop(e.sound);
    }

    public static void tick() {
        ticks++;
        if (Minecraft.getInstance().level == null && !SESSIONS.isEmpty()) clear();
    }

    /** Level change / disconnect. */
    public static void clear() {
        for (Long id : Map.copyOf(SESSIONS).keySet()) stop(id);
        SESSIONS.clear();
    }
}
