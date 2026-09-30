package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Only a chapter ordinal is accepted. Items, masks and the book are chosen by the server. */
public record GuideUnlockPayload(int chapter) implements CustomPacketPayload {
    public static final Type<GuideUnlockPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "guide_unlock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideUnlockPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GuideUnlockPayload::chapter, GuideUnlockPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
