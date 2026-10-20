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
 */
public final class DiscPlayback {
    public static final int RANGE = 64;
    private static final Map<GlobalPos, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final String hash;
        final int seconds;
        final long started;
        final Set<UUID> told = new HashSet<>();

        Session(String hash, int seconds, long started) {
            this.hash = hash;
            this.seconds = seconds;
            this.started = started;
        }
    }

    private DiscPlayback() {}

    /** Starts (or restarts) the track at a block. */
    public static void start(ServerLevel level, BlockPos pos, TrackRef track) {
        stop(level, pos);
        SESSIONS.put(GlobalPos.of(level.dimension(), pos.immutable()), new Session(track.sha256(), Math.max(1, track.seconds()), level.getGameTime()));
        update(level, pos);
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
        return s != null && level.getGameTime() - s.started >= s.seconds * 20L;
    }

    public static void stop(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.remove(GlobalPos.of(level.dimension(), pos));
        if (s == null) return;
        for (UUID id : s.told) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(pos));
        }
    }

    /** Tells players that came near (once a second is enough), stops it for players that left. */
    public static void update(ServerLevel level, BlockPos pos) {
        Session s = SESSIONS.get(GlobalPos.of(level.dimension(), pos));
        if (s == null) return;
        TrackStore store = MusicServer.store(level.getServer());
        TrackStore.Track track = store == null ? null : store.get(s.hash);
        int offset = (int) Math.min(Integer.MAX_VALUE, level.getGameTime() - s.started);
        Set<UUID> near = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RANGE * RANGE) continue;
            if (track == null || !track.audibleFor(player.getUUID())) continue;
            near.add(player.getUUID());
            if (s.told.add(player.getUUID())) {
                SimpleMusic.toClient.accept(player, new TrackPlayPayload(pos, s.hash, offset, s.seconds));
            }
        }
        for (UUID id : Set.copyOf(s.told)) {
            if (near.contains(id)) continue;
            s.told.remove(id);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) SimpleMusic.toClient.accept(player, TrackPlayPayload.stop(pos));
        }
    }

    /** Whether some block near the player plays {@code hash} for them (the download gate). */
    public static boolean playsFor(ServerPlayer player, String hash) {
        for (Map.Entry<GlobalPos, Session> e : SESSIONS.entrySet()) {
            if (e.getValue().hash.equals(hash) && e.getValue().told.contains(player.getUUID())
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
