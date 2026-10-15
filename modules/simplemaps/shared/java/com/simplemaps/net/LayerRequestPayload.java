package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: "send me the layers of this map inside this block rectangle" - loaded chunks when {@code chunks},
 * plus the extra sources by id (shops, chunk loaders ...). The server clamps the rectangle and answers only what the
 * map has explored.
 */
public record LayerRequestPayload(int mapId, int minX, int minZ, int maxX, int maxZ, boolean chunks, List<String> sources) implements CustomPacketPayload {
    public static final int MAX_SOURCES = 8, MAX_ID = 32;
    public static final Type<LayerRequestPayload> TYPE = new Type<>(SimpleMaps.id("layers"));

    private record Rect(int minX, int minZ, int maxX, int maxZ) {
        static final StreamCodec<ByteBuf, Rect> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, Rect::minX, ByteBufCodecs.INT, Rect::minZ, ByteBufCodecs.INT, Rect::maxX, ByteBufCodecs.INT, Rect::maxZ, Rect::new);
    }

    public static final StreamCodec<ByteBuf, LayerRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LayerRequestPayload::mapId,
            Rect.CODEC, p -> new Rect(p.minX, p.minZ, p.maxX, p.maxZ),
            ByteBufCodecs.BOOL, LayerRequestPayload::chunks,
            ByteBufCodecs.stringUtf8(MAX_ID).apply(ByteBufCodecs.list(MAX_SOURCES)), LayerRequestPayload::sources,
            (id, r, chunks, sources) -> new LayerRequestPayload(id, r.minX, r.minZ, r.maxX, r.maxZ, chunks, sources));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
