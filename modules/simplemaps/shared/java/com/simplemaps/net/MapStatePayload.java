package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the dimension a map is bound to ("" = not bound yet), its explored tile count and, only when they
 * changed since the last answer to this player, the discovered structures ({@code sendMarks}; Feature 3).
 */
public record MapStatePayload(int mapId, String dimension, int tiles, boolean sendMarks, List<WayfinderData.Mark> marks) implements CustomPacketPayload {
    public static final Type<MapStatePayload> TYPE = new Type<>(SimpleMaps.id("map_state"));
    private static final StreamCodec<ByteBuf, WayfinderData.Mark> MARK = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(WayfinderData.Mark.MAX_ID), WayfinderData.Mark::id,
            ByteBufCodecs.INT, WayfinderData.Mark::x, ByteBufCodecs.INT, WayfinderData.Mark::z, WayfinderData.Mark::new);
    public static final StreamCodec<ByteBuf, MapStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapStatePayload::mapId, ByteBufCodecs.stringUtf8(256), MapStatePayload::dimension,
            ByteBufCodecs.VAR_INT, MapStatePayload::tiles, ByteBufCodecs.BOOL, MapStatePayload::sendMarks,
            MARK.apply(ByteBufCodecs.list(WayfinderData.Mark.MAX_PER_MAP)), MapStatePayload::marks,
            MapStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
