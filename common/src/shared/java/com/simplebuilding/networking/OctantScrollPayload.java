package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Neu: boolean shift, boolean control
public record OctantScrollPayload(int amount, boolean shift, boolean control, boolean alt) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OctantScrollPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "octant_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OctantScrollPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OctantScrollPayload::amount,
            ByteBufCodecs.BOOL, OctantScrollPayload::shift,
            ByteBufCodecs.BOOL, OctantScrollPayload::control,
            ByteBufCodecs.BOOL, OctantScrollPayload::alt,
            OctantScrollPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}