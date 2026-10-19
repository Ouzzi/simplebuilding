package com.simplebuilding.modules.simpleinterfaces.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Server to client only: the receiving player's own ender chest (Vanilla container codec), sent at login and whenever
 * that player closes the own ender inventory. Never sent continuously, never another player's chest.
 */
public record EnderChestSnapshotPayload(ItemContainerContents contents) implements CustomPacketPayload {
    public static final Type<EnderChestSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpleinterfaces", "ender_chest_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestSnapshotPayload> CODEC =
            ItemContainerContents.STREAM_CODEC.map(EnderChestSnapshotPayload::new, EnderChestSnapshotPayload::contents);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
