package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SpaceKeyPayload(boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SpaceKeyPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "space_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceKeyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SpaceKeyPayload::pressed,
            SpaceKeyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}