package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DoubleJumpPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DoubleJumpPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "double_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> CODEC = StreamCodec.unit(new DoubleJumpPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}