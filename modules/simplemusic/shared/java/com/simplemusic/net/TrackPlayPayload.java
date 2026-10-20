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
 */
public record TrackPlayPayload(BlockPos pos, String sha256, int offsetTicks, int seconds) implements CustomPacketPayload {
    public static final Type<TrackPlayPayload> TYPE = new Type<>(SimpleMusic.id("track_play"));
    public static final StreamCodec<ByteBuf, TrackPlayPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TrackPlayPayload::pos,
            ByteBufCodecs.stringUtf8(64), TrackPlayPayload::sha256,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::offsetTicks,
            ByteBufCodecs.VAR_INT, TrackPlayPayload::seconds,
            TrackPlayPayload::new);

    public static TrackPlayPayload stop(BlockPos pos) {
        return new TrackPlayPayload(pos, "", 0, 0);
    }

    public boolean isStop() {
        return sha256.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
