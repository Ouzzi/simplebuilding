package com.simplebuilding.tweaks.network;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: die Config-Werte, die der Client braucht ({@link SimpleTweaks.ServerValues}),
 * beim Einloggen und nach jedem Tweaks-Befehl (Audit 2026-09-26 #16).
 */
public record TweaksConfigPayload(int rocketStackSize, int maxBoosts, boolean laserEnabled, int laserRange) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TweaksConfigPayload> ID = new CustomPacketPayload.Type<>(SimpleTweaks.id("tweaks_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TweaksConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TweaksConfigPayload::rocketStackSize,
            ByteBufCodecs.VAR_INT, TweaksConfigPayload::maxBoosts,
            ByteBufCodecs.BOOL, TweaksConfigPayload::laserEnabled,
            ByteBufCodecs.VAR_INT, TweaksConfigPayload::laserRange,
            TweaksConfigPayload::new);

    public static TweaksConfigPayload of(SimpleTweaks.ServerValues values) {
        return new TweaksConfigPayload(values.rocketStackSize(), values.maxBoosts(), values.laserEnabled(), values.laserRange());
    }

    public SimpleTweaks.ServerValues values() {
        return new SimpleTweaks.ServerValues(rocketStackSize, maxBoosts, laserEnabled, laserRange);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
