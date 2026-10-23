package com.simplemusic.disc;

import com.simplemusic.SimpleMusic;
import com.simplemusic.net.TrackPlayPayload;
import com.simplemusic.upload.MusicServer;
import com.simplemusic.upload.TrackStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side of a custom disc that plays at a block (Orchestrion or, owner decision U1 b, the Vanilla jukebox):
 * which track, since when, and which players were told. Players that come within {@link #RANGE} get a
 * {@link TrackPlayPayload} with the current position; players that leave or for whom the track is not audible
 * (pending and not theirs, owner decision U2 b) get nothing / a stop. Kept in memory only: after a restart the block
 * starts the track again.
 *
 * <p>A placed Pocket Jukebox also plays Vanilla discs here ({@link #startSong}): the key is then the song's sound event,
 * every player in range may hear it, and the start can lie in the past (resume after a pause).
 */
public final class DiscPlayback {
    public static final int RANGE = 64;
    private static final Map<GlobalPos, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        /** Upload hash, or a Vanilla song's sound event ({@link #song}). */
        final String hash;
        final boolean song;
        final int ticks;
        final long started;
        /** The Pocket Jukebox's session id (0 = none): sent along so its sound moves instead of starting again. */
        final long pocket;
        final Set<UUID> told = new HashSet<>();

        Session(String hash, boolean song, int ticks, long started, long pocket) {
            this.hash = hash;
            this.song = song;
            this.ticks = ticks;
            this.started = started;
            this.pocket = pocket;
        }
    }

    private DiscPlayback() {}

    /** Starts (or restarts) the track at a block. */
    public static void start(ServerLevel level, BlockPos pos, TrackRef track) {
        start(level, pos, track.sha256(), false, Math.max(1, track.seconds()) * 20, 0);
    }

    /** Starts a Vanilla song (its sound event id) at a block, {@code offsetTicks} into it. */
    public static void startSong(ServerLevel level, BlockPos pos, String soundEvent, int lengthTicks, int offsetTicks) {
        start(level, pos, soundEvent, true, lengthTicks, offsetTicks);
    }

    /** Starts (or restarts) an upload hash or a song at a block, {@code offsetTicks} into it. */
    public static void start(ServerLevel level, BlockPos pos, String key, boolean song, int lengthTicks, int offsetTicks) {
        start(level, pos, key, song, lengthTicks, offsetTicks, 0L);
    }

    /** {@link #start} for a placed Pocket Jukebox: {@code pocket} is its session id (PJ23-5). */
    public static void start(ServerLevel level, BlockPos pos, String key, boolean song, int lengthTicks, int offsetTicks, long pocket) {
        stop(level, pos);
        SESSIONS.put(GlobalPos.of(level.dimension(), pos.immutable()),
                new Session(key, song, Math.max(1, lengthTicks), level.getGameTime() - Math.max(0, offsetTicks), pocket));
        update(level, pos);
    }

    /**
     * Ends the block's track without telling anyone and returns who heard it: a Pocket Jukebox broken while it plays goes
     * on as the dropped item, whose session takes these listeners over (PJ23-5).
     */
    public static Set<UUID> detach(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.remove(GlobalPos.of(level.dimension(), pos));
        return s == null ? Set.of() : Set.copyOf(s.told);
    }

    public static boolean playing(ServerLevel level, BlockPos pos) {
        return SESSIONS.containsKey(GlobalPos.of(level.dimension(), pos));
    }

    /** Ticks since the track started at the block, or -1. */
    public static long elapsed(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.get(GlobalPos.of(level.dimension(), pos));
        return s == null ? -1 : level.getGameTime() - s.started;
    }

    /** True when the track at the block has played to its end. */
    public static boolean finished(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.get(GlobalPos.of(level.dimension(), pos));
        return s != null && level.getGameTime() - s.started >= s.ticks;
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.remove(GlobalPos.of(level.dimension(), pos));
        if (s == null) return;
        for (UUID id : s.told) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(pos, s.pocket));
        }
    }

    /** Tells players that came near (once a second is enough), stops it for players that left. */
    public static void update(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.get(GlobalPos.of(level.dimension(), pos));
        if (s == null) return;
        TrackStore store = MusicServer.store(level.getServer());
        TrackStore.Track track = s.song || store == null ? null : store.get(s.hash);
        int offset = (int) Math.min(Integer.MAX_VALUE, level.getGameTime() - s.started);
        Set<UUID> near = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RANGE * RANGE) continue;
            if (!s.song && (track == null || !track.audibleFor(player.getUUID()))) continue;
            near.add(player.getUUID());
            if (s.told.add(player.getUUID())) {
                SimpleMusic.toClient.accept(player, new TrackPlayPayload(pos, s.hash, offset, Math.max(1, s.ticks / 20), false, s.pocket, -1));
            }
        }
        for (UUID id : Set.copyOf(s.told)) {
            if (near.contains(id)) continue;
            s.told.remove(id);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(pos, s.pocket));
        }
    }

    /** Whether some block near the player plays {@code hash} for them (the download gate). */
    public static boolean playsFor(ServerPlayer player, String hash) {
        for (Map.Entry<GlobalPos, Session> e : SESSIONS.entrySet()) {
            if (!e.getValue().song && e.getValue().hash.equals(hash) && e.getValue().told.contains(player.getUUID())
                    && e.getKey().dimension().equals(player.level().dimension())) return true;
        }
        return false;
    }

    /** Players told about a block's track (tests). */
    public static Set<UUID> listeners(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.get(GlobalPos.of(level.dimension(), pos));
        return s == null ? Set.of() : Set.copyOf(s.told);
    }

    /** Server stop: forget everything. */
    public static void clear() {
        SESSIONS.clear();
    }
}
