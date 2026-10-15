package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: claim the chunk with the unbound deed in {@code hand} (same as {@code /claim create}). */
public record DeedClaimPayload(int hand) implements CustomPacketPayload {
    public static final Type<DeedClaimPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_claim"));
    public static final StreamCodec<ByteBuf, DeedClaimPayload> CODEC = ByteBufCodecs.VAR_INT.map(DeedClaimPayload::new, DeedClaimPayload::hand).cast();
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
