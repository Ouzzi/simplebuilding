package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: "Unlock anyway" on a locked guide tab (owner 2026-10-02). The server opens it
 * only for a player in creative mode and only a recipe-gated tab ({@code GuideUnlocks#creativeUnlock});
 * the Server Admin tab stays operator-only.
 */
public record GuideUnlockPayload(Identifier tab) implements CustomPacketPayload {
    public static final Type<GuideUnlockPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "guide_unlock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideUnlockPayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, GuideUnlockPayload::tab, GuideUnlockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
