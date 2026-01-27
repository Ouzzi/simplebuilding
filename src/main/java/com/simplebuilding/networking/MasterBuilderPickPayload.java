package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MasterBuilderPickPayload(ItemStack itemToPick) implements CustomPayload {
    public static final CustomPayload.Id<MasterBuilderPickPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "master_builder_pick"));
    public static final PacketCodec<RegistryByteBuf, MasterBuilderPickPayload> CODEC = PacketCodec.tuple(
            ItemStack.PACKET_CODEC, MasterBuilderPickPayload::itemToPick,
            MasterBuilderPickPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}