package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ReinforcedBundleSelectionPayload(int slotId, int selectedIndex) implements CustomPayload {
    public static final CustomPayload.Id<ReinforcedBundleSelectionPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "bundle_selection"));
    
    public static final PacketCodec<RegistryByteBuf, ReinforcedBundleSelectionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, ReinforcedBundleSelectionPayload::slotId,
            PacketCodecs.INTEGER, ReinforcedBundleSelectionPayload::selectedIndex,
            ReinforcedBundleSelectionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}