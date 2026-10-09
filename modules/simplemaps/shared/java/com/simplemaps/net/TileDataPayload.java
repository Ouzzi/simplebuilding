package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one tile of one zoom level. {@code data} = deflated colours (128x128) followed by heights; empty
 * with version 0 when nothing is explored there.
 */
public record TileDataPayload(int mapId, int zoom, int tx, int tz, int version, byte[] data) implements CustomPacketPayload {
    public static final Type<TileDataPayload> TYPE = new Type<>(SimpleMaps.id("tile"));
    public static final StreamCodec<ByteBuf, TileDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TileDataPayload::mapId, ByteBufCodecs.VAR_INT, TileDataPayload::zoom,
            ByteBufCodecs.INT, TileDataPayload::tx, ByteBufCodecs.INT, TileDataPayload::tz,
            ByteBufCodecs.INT, TileDataPayload::version, ByteBufCodecs.BYTE_ARRAY, TileDataPayload::data,
            TileDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
