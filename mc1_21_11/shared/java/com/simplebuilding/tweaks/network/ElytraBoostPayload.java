package com.simplebuilding.tweaks.network;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> Server: Leertaste im Gleitflug mit Spawn-Elytra (Simple Tweaks: BoostPayload). */
public record ElytraBoostPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ElytraBoostPayload> ID = new CustomPacketPayload.Type<>(SimpleTweaks.id("elytra_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ElytraBoostPayload> CODEC = StreamCodec.unit(new ElytraBoostPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
