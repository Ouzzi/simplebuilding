package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Diese Klasse ist der Briefumschlag, der vom Client zum Server geschickt wird
public record ToggleHopperFilterPayload() implements CustomPayload {

    // Die eindeutige ID des Pakets
    public static final CustomPayload.Id<ToggleHopperFilterPayload> ID =
            new CustomPayload.Id<>(Identifier.of("simplebuilding", "toggle_filter"));

    // Der Codec, um das Paket zu verpacken (es ist leer, da wir nur ein Signal brauchen)
    public static final PacketCodec<RegistryByteBuf, ToggleHopperFilterPayload> CODEC =
            PacketCodec.unit(new ToggleHopperFilterPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}