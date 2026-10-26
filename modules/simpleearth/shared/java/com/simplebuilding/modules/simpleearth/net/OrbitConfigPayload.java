package com.simplebuilding.modules.simpleearth.net;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the orbit values of the version the player chose (the server clamps them, {@code EarthServerConfig}). */
public record OrbitConfigPayload(int activationY, int switchY, int edge) implements CustomPacketPayload {
    public static final Type<OrbitConfigPayload> TYPE = new Type<>(SimpleEarth.id("orbit_config"));
    public static final StreamCodec<FriendlyByteBuf, OrbitConfigPayload> CODEC = CustomPacketPayload.codec(OrbitConfigPayload::write, OrbitConfigPayload::new);

    private OrbitConfigPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(activationY);
        buf.writeVarInt(switchY);
        buf.writeVarInt(edge);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
