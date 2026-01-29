package com.simplebuilding.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TrimDataPayload(int baseDist, int baseTime, int baseHostile, int basePassive, int baseDamage) implements CustomPayload {
    public static final CustomPayload.Id<TrimDataPayload> ID = new CustomPayload.Id<>(Identifier.of("simplebuilding", "trim_data_sync"));

    public static final PacketCodec<RegistryByteBuf, TrimDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, TrimDataPayload::baseDist,
            PacketCodecs.INTEGER, TrimDataPayload::baseTime,
            PacketCodecs.INTEGER, TrimDataPayload::baseHostile,
            PacketCodecs.INTEGER, TrimDataPayload::basePassive,
            PacketCodecs.INTEGER, TrimDataPayload::baseDamage,
            TrimDataPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}