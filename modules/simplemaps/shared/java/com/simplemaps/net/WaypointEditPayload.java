package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: edit a waypoint of the map in {@code hand} (0 main, 1 off). {@code delete} removes the slot,
 * otherwise {@code waypoint} replaces it (create, move or configure).
 */
public record WaypointEditPayload(int hand, boolean delete, Waypoint waypoint) implements CustomPacketPayload {
    public static final Type<WaypointEditPayload> TYPE = new Type<>(SimpleMaps.id("waypoint"));
    public static final StreamCodec<ByteBuf, WaypointEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WaypointEditPayload::hand, ByteBufCodecs.BOOL, WaypointEditPayload::delete,
            Waypoint.STREAM_CODEC, WaypointEditPayload::waypoint, WaypointEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
