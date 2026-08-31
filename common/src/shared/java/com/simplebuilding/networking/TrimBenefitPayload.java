package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrimBenefitPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<TrimBenefitPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "trim_benefits_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, TrimBenefitPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TrimBenefitPayload::enabled,
            TrimBenefitPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}