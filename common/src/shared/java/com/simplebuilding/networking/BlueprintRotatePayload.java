package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Strg+Mausrad im Baumodus: dreht die Blaupause in der Nebenhand um {@code amount} Viertel. */
public record BlueprintRotatePayload(int amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintRotatePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "blueprint_rotate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintRotatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BlueprintRotatePayload::amount,
            BlueprintRotatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
