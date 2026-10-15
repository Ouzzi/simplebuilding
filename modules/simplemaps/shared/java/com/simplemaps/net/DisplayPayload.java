package com.simplemaps.net;

import com.simplemaps.MapsComponents;
import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the wayfinder map in this hand (0 = main, 1 = off) now has this display mode (owner N65). */
public record DisplayPayload(int hand, MapsComponents.Display display) implements CustomPacketPayload {
    public static final Type<DisplayPayload> TYPE = new Type<>(SimpleMaps.id("display"));
    public static final StreamCodec<ByteBuf, DisplayPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DisplayPayload::hand, MapsComponents.Display.STREAM_CODEC, DisplayPayload::display,
            DisplayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
