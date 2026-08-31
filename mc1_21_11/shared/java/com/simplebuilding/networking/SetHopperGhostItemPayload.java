package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record SetHopperGhostItemPayload(int slotIndex, ItemStack stack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetHopperGhostItemPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "set_hopper_ghost_item"));

    // FIX: ItemStack.OPTIONAL_PACKET_CODEC verwenden, damit auch leere Stacks (Löschen) erlaubt sind!
    public static final StreamCodec<RegistryFriendlyByteBuf, SetHopperGhostItemPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SetHopperGhostItemPayload::slotIndex,
            ItemStack.OPTIONAL_STREAM_CODEC, SetHopperGhostItemPayload::stack,
            SetHopperGhostItemPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}