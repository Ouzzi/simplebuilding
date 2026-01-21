package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SetHopperGhostItemPayload(int slotIndex, ItemStack stack) implements CustomPayload {
    public static final CustomPayload.Id<SetHopperGhostItemPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "set_hopper_ghost_item"));

    public static final PacketCodec<RegistryByteBuf, SetHopperGhostItemPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SetHopperGhostItemPayload::slotIndex,
            ItemStack.PACKET_CODEC, SetHopperGhostItemPayload::stack,
            SetHopperGhostItemPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}