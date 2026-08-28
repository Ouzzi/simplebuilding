package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record MasterBuilderPickPayload(ItemStack itemToPick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MasterBuilderPickPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "master_builder_pick"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MasterBuilderPickPayload> CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, MasterBuilderPickPayload::itemToPick,
            MasterBuilderPickPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}