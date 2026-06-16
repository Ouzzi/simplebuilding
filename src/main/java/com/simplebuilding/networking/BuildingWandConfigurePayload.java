package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BuildingWandConfigurePayload(int selectedRadius, int axisMode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BuildingWandConfigurePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "building_wand_configure"));

    // Wir senden zwei Integer: Radius und AxisMode
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildingWandConfigurePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BuildingWandConfigurePayload::selectedRadius,
            ByteBufCodecs.INT, BuildingWandConfigurePayload::axisMode,
            BuildingWandConfigurePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}