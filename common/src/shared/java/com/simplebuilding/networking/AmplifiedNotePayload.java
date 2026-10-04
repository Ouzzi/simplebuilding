package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;

/** A server-selected note in range, played without client distance attenuation. */
public record AmplifiedNotePayload(ClientboundSoundPacket sound) implements CustomPacketPayload {
    public static final Type<AmplifiedNotePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "amplified_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AmplifiedNotePayload> CODEC =
            ClientboundSoundPacket.STREAM_CODEC.map(AmplifiedNotePayload::new, AmplifiedNotePayload::sound);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
