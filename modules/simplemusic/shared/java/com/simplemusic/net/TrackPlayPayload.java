package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the block at {@code pos} plays track {@code sha256} from tick {@code offsetTicks} on (empty hash =
 * stop). A client without the file asks for it with {@link TrackRequestPayload} and starts where the song is by then.
 * {@code carried}: the player's own Pocket Jukebox ({@code pos} unused). In both kinds {@code sha256} is an upload hash or,
 * with a {@code :}, the sound event of a Vanilla disc (carried, or at a placed Pocket Jukebox).
 *
 * <p>{@code session} is the Pocket Jukebox's id (0 = no Pocket Jukebox, e.g. the Orchestrion): one session keeps one
 * sound on the client, which only moves when the same session plays the same song somewhere else (carried, at the
 * dropped item {@code entity} (-1 = none) or at a block) - so putting it down, dropping or picking it up does not cut
 * the music (PJ23-5).
 */
public record TrackPlayPayload(BlockPos pos, String sha256, int offsetTicks, int seconds, boolean carried, long session, int entity)
        implements CustomPacketPayload {
    public static final Type<TrackPlayPayload> TYPE = new Type<>(SimpleMusic.id("track_play"));
    public static final StreamCodec<ByteBuf, TrackPlayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TrackPlayPayload::pos,
            ByteBufCodecs.stringUtf8(256), TrackPlayPayload::sha256,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::offsetTicks,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::seconds,
            ByteBufCodecs.BOOL, TrackPlayPayload::carried,
            ByteBufCodecs.VAR_LONG, TrackPlayPayload::session,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::entity,
            TrackPlayPayload::new);

    public TrackPlayPayload(BlockPos pos, String sha256, int offsetTicks, int seconds) {
        this(pos, sha256, offsetTicks, seconds, false, 0L, -1);
    }

    public static TrackPlayPayload stop(BlockPos pos) {
        return new TrackPlayPayload(pos, "", 0, 0);
    }

    /** Stop of a Pocket Jukebox session, wherever it played. */
    public static TrackPlayPayload stop(BlockPos pos, long session) {
        return new TrackPlayPayload(pos, "", 0, 0, false, session, -1);
    }

    /** The carrier's Pocket Jukebox {@code session} plays {@code key} (upload hash or sound event id) from {@code offsetTicks} on. */
    public static TrackPlayPayload carried(String key, int offsetTicks, int seconds, long session) {
        return new TrackPlayPayload(BlockPos.ZERO, key, offsetTicks, seconds, true, session, -1);
    }

    public static TrackPlayPayload carriedStop(long session) {
        return new TrackPlayPayload(BlockPos.ZERO, "", 0, 0, true, session, -1);
    }

    /** A dropped Pocket Jukebox (item entity {@code entity}) plays {@code key}, positional, for everyone near it. */
    public static TrackPlayPayload dropped(BlockPos near, String key, int offsetTicks, int seconds, long session, int entity) {
        return new TrackPlayPayload(near, key, offsetTicks, seconds, false, session, entity);
    }

    /** A Vanilla disc's sound event rather than an upload hash. */
    public boolean isSong() {
        return sha256.indexOf(':') >= 0;
    }

    public boolean isStop() {
        return sha256.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
