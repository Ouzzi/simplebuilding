package com.simplebuilding.modules.simpletweaks.protect;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client on join and on reload: which client-side protection rules apply on this server. */
public record ProtectRulesPayload(boolean freecamCollision) implements CustomPacketPayload {
    public static final Type<ProtectRulesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "protect_rules"));
    public static final StreamCodec<ByteBuf, ProtectRulesPayload> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, ProtectRulesPayload::freecamCollision, ProtectRulesPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
