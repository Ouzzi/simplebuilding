package com.simplebuilding.modules.simpleearth.net;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server, once per tick while parked in orbit: the glide step (blocks); the server caps it by the orbit speed. */
public record OrbitMovePayload(float dx, float dz) implements CustomPacketPayload {
    public static final Type<OrbitMovePayload> TYPE = new Type<>(SimpleEarth.id("orbit_move"));
    public static final StreamCodec<FriendlyByteBuf, OrbitMovePayload> CODEC = CustomPacketPayload.codec(OrbitMovePayload::write, OrbitMovePayload::new);

    private OrbitMovePayload(FriendlyByteBuf buf) {
        this(buf.readFloat(), buf.readFloat());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeFloat(dx);
        buf.writeFloat(dz);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
