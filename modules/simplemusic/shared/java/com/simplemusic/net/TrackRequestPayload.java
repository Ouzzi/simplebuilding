package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: send me the file of {@code sha256} (only granted while a block near me plays it for me). */
public record TrackRequestPayload(String sha256) implements CustomPacketPayload {
    public static final Type<TrackRequestPayload> TYPE = new Type<>(SimpleMusic.id("track_request"));
    public static final StreamCodec<ByteBuf, TrackRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), TrackRequestPayload::sha256, TrackRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
