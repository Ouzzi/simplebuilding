package com.simplebuilding.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

// Dieses Paket sendet der Server an den Client, um Ghost Items zu syncen
public record SyncHopperGhostItemPayload(BlockPos pos, int slot, ItemStack stack) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncHopperGhostItemPayload> ID = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "sync_hopper_ghost_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHopperGhostItemPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SyncHopperGhostItemPayload::pos,
        ByteBufCodecs.INT, SyncHopperGhostItemPayload::slot,
        ItemStack.OPTIONAL_STREAM_CODEC, SyncHopperGhostItemPayload::stack,
        SyncHopperGhostItemPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}