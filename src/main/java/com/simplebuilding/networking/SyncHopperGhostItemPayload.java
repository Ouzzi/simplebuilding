package com.simplebuilding.networking;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Dieses Paket sendet der Server an den Client, um Ghost Items zu syncen
public record SyncHopperGhostItemPayload(BlockPos pos, int slot, ItemStack stack) implements CustomPayload {
    
    public static final CustomPayload.Id<SyncHopperGhostItemPayload> ID = 
        new CustomPayload.Id<>(Identifier.of("simplebuilding", "sync_hopper_ghost_item"));

    public static final PacketCodec<RegistryByteBuf, SyncHopperGhostItemPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, SyncHopperGhostItemPayload::pos,
        PacketCodecs.INTEGER, SyncHopperGhostItemPayload::slot,
        ItemStack.OPTIONAL_PACKET_CODEC, SyncHopperGhostItemPayload::stack,
        SyncHopperGhostItemPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}