package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Diese Klasse ist der Briefumschlag, der vom Client zum Server geschickt wird
public record ToggleHopperFilterPayload() implements CustomPacketPayload {

    // Die eindeutige ID des Pakets
    public static final CustomPacketPayload.Type<ToggleHopperFilterPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "toggle_filter"));

    // Der Codec, um das Paket zu verpacken (es ist leer, da wir nur ein Signal brauchen)
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleHopperFilterPayload> CODEC =
            StreamCodec.unit(new ToggleHopperFilterPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}