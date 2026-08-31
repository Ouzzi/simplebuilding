package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SurvivalSyncPayload(int currentDist, int currentTime, int currentHostile, int currentPassive, int currentDamage) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SurvivalSyncPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "survival_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SurvivalSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SurvivalSyncPayload::currentDist,
            ByteBufCodecs.INT, SurvivalSyncPayload::currentTime,
            ByteBufCodecs.INT, SurvivalSyncPayload::currentHostile,
            ByteBufCodecs.INT, SurvivalSyncPayload::currentPassive,
            ByteBufCodecs.INT, SurvivalSyncPayload::currentDamage,
            SurvivalSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}