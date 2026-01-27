package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TrimBenefitPayload(boolean enabled) implements CustomPayload {
    public static final Id<TrimBenefitPayload> ID = new Id<>(Identifier.of(Simplebuilding.MOD_ID, "trim_benefits_sync"));
    
    public static final PacketCodec<RegistryByteBuf, TrimBenefitPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, TrimBenefitPayload::enabled,
            TrimBenefitPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}