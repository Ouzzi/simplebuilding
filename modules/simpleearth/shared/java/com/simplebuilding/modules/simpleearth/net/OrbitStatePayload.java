package com.simplebuilding.modules.simpleearth.net;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the player's orbit state - active (above the activation height), parked (above the render switch, the
 * figure stays put and only the orbit position glides), centre A, orbit position, the effective (clamped) values, the sea
 * level for water shading, the finest sample grid the server serves and other players in orbit (E9, absolute orbit x/z).
 */
public record OrbitStatePayload(boolean enabled, boolean active, boolean parked, double ax, double az, double ox, double oz,
                                int activationY, int switchY, int edge, float speed, int seaLevel, boolean exploredOnly,
                                int minGrid, List<float[]> others) implements CustomPacketPayload {
    public static final int MAX_OTHERS = 16;
    public static final Type<OrbitStatePayload> TYPE = new Type<>(SimpleEarth.id("orbit_state"));
    public static final StreamCodec<FriendlyByteBuf, OrbitStatePayload> CODEC = CustomPacketPayload.codec(OrbitStatePayload::write, OrbitStatePayload::read);

    private static OrbitStatePayload read(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean(), active = buf.readBoolean(), parked = buf.readBoolean();
        double ax = buf.readDouble(), az = buf.readDouble(), ox = buf.readDouble(), oz = buf.readDouble();
        int act = buf.readVarInt(), sw = buf.readVarInt(), edge = buf.readVarInt();
        float speed = buf.readFloat();
        int sea = buf.readVarInt();
        boolean explored = buf.readBoolean();
        int minGrid = buf.readVarInt();
        int n = Math.min(MAX_OTHERS, buf.readVarInt());
        List<float[]> others = new ArrayList<>(n);
        for (int i = 0; i < n; i++) others.add(new float[]{buf.readFloat(), buf.readFloat()});
        return new OrbitStatePayload(enabled, active, parked, ax, az, ox, oz, act, sw, edge, speed, sea, explored, minGrid, others);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeBoolean(active);
        buf.writeBoolean(parked);
        buf.writeDouble(ax);
        buf.writeDouble(az);
        buf.writeDouble(ox);
        buf.writeDouble(oz);
        buf.writeVarInt(activationY);
        buf.writeVarInt(switchY);
        buf.writeVarInt(edge);
        buf.writeFloat(speed);
        buf.writeVarInt(seaLevel);
        buf.writeBoolean(exploredOnly);
        buf.writeVarInt(minGrid);
        int n = Math.min(MAX_OTHERS, others.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeFloat(others.get(i)[0]);
            buf.writeFloat(others.get(i)[1]);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
