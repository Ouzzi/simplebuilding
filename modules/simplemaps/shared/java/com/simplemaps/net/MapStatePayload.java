package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the dimension a map is bound to ("" = not bound yet) and its explored tile count. */
public record MapStatePayload(int mapId, String dimension, int tiles) implements CustomPacketPayload {
    public static final Type<MapStatePayload> TYPE = new Type<>(SimpleMaps.id("map_state"));
    public static final StreamCodec<ByteBuf, MapStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapStatePayload::mapId, ByteBufCodecs.stringUtf8(256), MapStatePayload::dimension,
            ByteBufCodecs.VAR_INT, MapStatePayload::tiles, MapStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
