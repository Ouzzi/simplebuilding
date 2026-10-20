package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import com.simplemusic.upload.UploadSession;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: chunk {@code index} of {@code sha256}, {@code total} bytes in all. */
public record TrackDataPayload(String sha256, int index, int total, byte[] bytes) implements CustomPacketPayload {
    public static final Type<TrackDataPayload> TYPE = new Type<>(SimpleMusic.id("track_data"));
    public static final StreamCodec<ByteBuf, TrackDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), TrackDataPayload::sha256,
            ByteBufCodecs.VAR_INT, TrackDataPayload::index,
            ByteBufCodecs.VAR_INT, TrackDataPayload::total,
            ByteBufCodecs.byteArray(UploadSession.MAX_CHUNK), TrackDataPayload::bytes,
            TrackDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
