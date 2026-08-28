package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrimDataPayload(int baseDist, int baseTime, int baseHostile, int basePassive, int baseDamage) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TrimDataPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "trim_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrimDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TrimDataPayload::baseDist,
            ByteBufCodecs.INT, TrimDataPayload::baseTime,
            ByteBufCodecs.INT, TrimDataPayload::baseHostile,
            ByteBufCodecs.INT, TrimDataPayload::basePassive,
            ByteBufCodecs.INT, TrimDataPayload::baseDamage,
            TrimDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}