package com.simplequalityoflife.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client only: the menu {@code containerId} gets {@code size} slots of the marked container
 * appended. Sent after the open-screen packet and before the menu's first content packet. Container id 0 is the
 * player's own inventory menu (kept while a container is marked); {@code size} 0 removes the slots again (mark
 * ended). {@code icon} is the marked block's item for the HUD slot.
 */
public record LinkedOpenPayload(int containerId, int size, Component title, ItemStack icon) implements CustomPacketPayload {
    public static final Type<LinkedOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simplequalityoflife", "linked_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkedOpenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LinkedOpenPayload::containerId,
            ByteBufCodecs.VAR_INT, LinkedOpenPayload::size,
            ComponentSerialization.STREAM_CODEC, LinkedOpenPayload::title,
            ItemStack.OPTIONAL_STREAM_CODEC, LinkedOpenPayload::icon,
            LinkedOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
