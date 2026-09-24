package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server: die Rucksack-Taste wurde gedrueckt, waehrend ein Rucksack getragen wird. Der
 * Server prueft selbst noch einmal, ob einer getragen wird (siehe
 * {@code BackpackMenuProviders#canOpenWorn}).
 */
public record OpenBackpackPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBackpackPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "open_backpack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpackPayload> CODEC = StreamCodec.unit(new OpenBackpackPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
