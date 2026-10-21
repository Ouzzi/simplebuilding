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
 * {@code carried}: the player's own Pocket Jukebox ({@code pos} unused); then {@code sha256} is an upload hash or, with a
 * {@code :}, the sound event of a Vanilla disc.
 */
public record TrackPlayPayload(BlockPos pos, String sha256, int offsetTicks, int seconds, boolean carried) implements CustomPacketPayload {
    public static final Type<TrackPlayPayload> TYPE = new Type<>(SimpleMusic.id("track_play"));
    public static final StreamCodec<ByteBuf, TrackPlayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TrackPlayPayload::pos,
            ByteBufCodecs.stringUtf8(256), TrackPlayPayload::sha256,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::offsetTicks,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::seconds,
            ByteBufCodecs.BOOL, TrackPlayPayload::carried,
            TrackPlayPayload::new);

    public TrackPlayPayload(BlockPos pos, String sha256, int offsetTicks, int seconds) {
        this(pos, sha256, offsetTicks, seconds, false);
    }

    public static TrackPlayPayload stop(BlockPos pos) {
        return new TrackPlayPayload(pos, "", 0, 0);
    }

    /** The carrier's Pocket Jukebox plays {@code key} (upload hash or sound event id) from {@code offsetTicks} on. */
    public static TrackPlayPayload carried(String key, int offsetTicks, int seconds) {
        return new TrackPlayPayload(BlockPos.ZERO, key, offsetTicks, seconds, true);
    }

    public static TrackPlayPayload carriedStop() {
        return new TrackPlayPayload(BlockPos.ZERO, "", 0, 0, true);
    }

    /** A Vanilla disc's sound event rather than an upload hash (carried only). */
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
