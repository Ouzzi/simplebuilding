package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Neu: boolean shift, boolean control
public record OctantScrollPayload(int amount, boolean shift, boolean control, boolean alt) implements CustomPayload {
    public static final CustomPayload.Id<OctantScrollPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "octant_scroll"));

    public static final PacketCodec<RegistryByteBuf, OctantScrollPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, OctantScrollPayload::amount,
            PacketCodecs.BOOLEAN, OctantScrollPayload::shift,
            PacketCodecs.BOOLEAN, OctantScrollPayload::control,
            PacketCodecs.BOOLEAN, OctantScrollPayload::alt,
            OctantScrollPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}