package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpaceKeyPayload(boolean pressed) implements CustomPayload {
    public static final CustomPayload.Id<SpaceKeyPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "space_key"));
    public static final PacketCodec<RegistryByteBuf, SpaceKeyPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, SpaceKeyPayload::pressed,
            SpaceKeyPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}