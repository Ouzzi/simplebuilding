package com.simplebuilding.modules.simpleinterfaces.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server only: Escape in the world ends the player's container mark (no data; the server owns the mark). */
public record LinkedCancelPayload() implements CustomPacketPayload {
    public static final Type<LinkedCancelPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpleinterfaces", "linked_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedCancelPayload> CODEC = StreamCodec.unit(new LinkedCancelPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
