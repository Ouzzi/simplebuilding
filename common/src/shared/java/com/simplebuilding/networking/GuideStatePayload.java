package com.simplebuilding.networking;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the guide tabs ({@code GuideTabs}) open for this player. Sent at login and
 * whenever a tab opens; the client keeps it in {@code GuideUnlocks#receive}. Nothing goes the other
 * way: tabs open on the server alone, from the player's recipe book.
 */
public record GuideStatePayload(List<Identifier> tabs) implements CustomPacketPayload {
    public static final Type<GuideStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "guide_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuideStatePayload> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(256)), GuideStatePayload::tabs, GuideStatePayload::new);

    public GuideStatePayload {
        tabs = List.copyOf(tabs);
    }

    /** Client side, on the client thread. */
    public void apply() {
        com.simplebuilding.guide.GuideUnlocks.receive(tabs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
