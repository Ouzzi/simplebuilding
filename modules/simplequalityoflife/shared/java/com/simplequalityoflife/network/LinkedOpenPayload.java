package com.simplequalityoflife.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client only: the menu {@code containerId} gets {@code size} slots of the marked container
 * appended. Sent after the open-screen packet and before the menu's first content packet.
 */
public record LinkedOpenPayload(int containerId, int size, Component title) implements CustomPacketPayload {
    public static final Type<LinkedOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simplequalityoflife", "linked_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedOpenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LinkedOpenPayload::containerId,
            ByteBufCodecs.VAR_INT, LinkedOpenPayload::size,
            ComponentSerialization.STREAM_CODEC, LinkedOpenPayload::title,
            LinkedOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
