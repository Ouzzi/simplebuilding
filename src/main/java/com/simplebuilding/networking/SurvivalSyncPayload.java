package com.simplebuilding.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SurvivalSyncPayload(int currentDist, int currentTime, int currentHostile, int currentPassive, int currentDamage) implements CustomPayload {
    public static final CustomPayload.Id<SurvivalSyncPayload> ID = new CustomPayload.Id<>(Identifier.of("simplebuilding", "survival_sync"));

    public static final PacketCodec<RegistryByteBuf, SurvivalSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SurvivalSyncPayload::currentDist,
            PacketCodecs.INTEGER, SurvivalSyncPayload::currentTime,
            PacketCodecs.INTEGER, SurvivalSyncPayload::currentHostile,
            PacketCodecs.INTEGER, SurvivalSyncPayload::currentPassive,
            PacketCodecs.INTEGER, SurvivalSyncPayload::currentDamage,
            SurvivalSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}