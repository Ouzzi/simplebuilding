package com.simplemaps.net;

import com.simplemaps.MapsComponents;
import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the framed wayfinder map in this item frame now shows {@code view} (Feature 4). */
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
