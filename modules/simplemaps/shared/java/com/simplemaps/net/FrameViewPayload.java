package com.simplemaps.net;

import com.simplemaps.MapsComponents;
import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: save {@code view} on the framed wayfinder map in this item frame (Feature 4; owner 21.10.: only after the two-step confirmation). */
public record FrameViewPayload(int entityId, MapsComponents.View view) implements CustomPacketPayload {
    public static final Type<FrameViewPayload> TYPE = new Type<>(SimpleMaps.id("frame_view"));
    public static final StreamCodec<ByteBuf, FrameViewPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FrameViewPayload::entityId, MapsComponents.View.STREAM_CODEC, FrameViewPayload::view,
            FrameViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
