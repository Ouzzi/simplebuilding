package com.simplebuilding.modules.simpleinterfaces.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server only (WE25-3): sort the area of the menu {@code containerId} that holds slot {@code slotIndex}; the server decides what is sorted. */
public record SortPayload(int containerId, int slotIndex) implements CustomPacketPayload {
    public static final Type<SortPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpleinterfaces", "sort"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SortPayload::containerId,
            ByteBufCodecs.VAR_INT, SortPayload::slotIndex,
            SortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
