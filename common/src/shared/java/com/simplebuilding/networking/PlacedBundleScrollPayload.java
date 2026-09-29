package com.simplebuilding.networking;

import com.simplebuilding.Simplebuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Schleichen + Mausrad auf ein abgestelltes Buendel (Client -&gt; Server, aus {@code MouseMixin}):
 * welches Buendel und in welche Richtung ({@code step} &gt; 0 = naechstes Item, Rad nach unten). Der
 * Server besitzt den gezeigten Index und prueft Reichweite und Schleichen selbst
 * ({@code PlacedBundles#scroll}); das Ergebnis kommt ueber das Block-Entity-Update zurueck.
 */
public record PlacedBundleScrollPayload(BlockPos pos, int step) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlacedBundleScrollPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "placed_bundle_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedBundleScrollPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PlacedBundleScrollPayload::pos,
            ByteBufCodecs.VAR_INT, PlacedBundleScrollPayload::step,
            PlacedBundleScrollPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
