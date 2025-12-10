package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DoubleJumpPayload() implements CustomPayload {
    public static final CustomPayload.Id<DoubleJumpPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "double_jump"));
    public static final PacketCodec<RegistryByteBuf, DoubleJumpPayload> CODEC = PacketCodec.unit(new DoubleJumpPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}