package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReinforcedBundleSelectionPayload(int slotId, int selectedIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReinforcedBundleSelectionPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "bundle_selection"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ReinforcedBundleSelectionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ReinforcedBundleSelectionPayload::slotId,
            ByteBufCodecs.INT, ReinforcedBundleSelectionPayload::selectedIndex,
            ReinforcedBundleSelectionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}