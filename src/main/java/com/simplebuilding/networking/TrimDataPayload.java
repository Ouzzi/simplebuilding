package com.simplebuilding.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TrimDataPayload(int baseDist, int baseKills, int baseTime) implements CustomPayload {
    // FIX: Identifier.of verwenden
    public static final CustomPayload.Id<TrimDataPayload> ID = new CustomPayload.Id<>(Identifier.of("simplebuilding", "trim_data_sync"));

    public static final PacketCodec<RegistryByteBuf, TrimDataPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, TrimDataPayload::baseDist,
            PacketCodecs.INTEGER, TrimDataPayload::baseKills,
            PacketCodecs.INTEGER, TrimDataPayload::baseTime,
            TrimDataPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}