package com.simplebuilding.networking;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetHopperGhostItemPayload(int slotIndex, ItemStack stack) implements CustomPayload {
    public static final CustomPayload.Id<SetHopperGhostItemPayload> ID = new CustomPayload.Id<>(Identifier.of("simplebuilding", "set_hopper_ghost_item"));

    // FIX: ItemStack.OPTIONAL_PACKET_CODEC verwenden, damit auch leere Stacks (Löschen) erlaubt sind!
    public static final PacketCodec<RegistryByteBuf, SetHopperGhostItemPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SetHopperGhostItemPayload::slotIndex,
            ItemStack.OPTIONAL_PACKET_CODEC, SetHopperGhostItemPayload::stack,
            SetHopperGhostItemPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}