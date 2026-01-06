package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuildingWandConfigurePayload(int selectedRadius, int axisMode) implements CustomPayload {
    public static final CustomPayload.Id<BuildingWandConfigurePayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "building_wand_configure"));

    // Wir senden zwei Integer: Radius und AxisMode
    public static final PacketCodec<RegistryByteBuf, BuildingWandConfigurePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, BuildingWandConfigurePayload::selectedRadius,
            PacketCodecs.INTEGER, BuildingWandConfigurePayload::axisMode,
            BuildingWandConfigurePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}