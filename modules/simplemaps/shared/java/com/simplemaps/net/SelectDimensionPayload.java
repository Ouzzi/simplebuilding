package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the mega wayfinder map in this hand (0 = main, 1 = off) now shows this dimension (owner B23-K1). */
public record SelectDimensionPayload(int hand, String dimension) implements CustomPacketPayload {
    public static final Type<SelectDimensionPayload> TYPE = new Type<>(SimpleMaps.id("select_dimension"));
    public static final StreamCodec<ByteBuf, SelectDimensionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectDimensionPayload::hand, ByteBufCodecs.stringUtf8(256), SelectDimensionPayload::dimension,
            SelectDimensionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
