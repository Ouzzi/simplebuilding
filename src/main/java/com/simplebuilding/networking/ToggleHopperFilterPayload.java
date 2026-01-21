package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Leerer Record -> Keine Argumente im Konstruktor
public record ToggleHopperFilterPayload() implements CustomPayload {
    public static final CustomPayload.Id<ToggleHopperFilterPayload> ID = new CustomPayload.Id<>(Identifier.of(Simplebuilding.MOD_ID, "toggle_hopper_filter"));

    public static final PacketCodec<RegistryByteBuf, ToggleHopperFilterPayload> CODEC = PacketCodec.unit(new ToggleHopperFilterPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}