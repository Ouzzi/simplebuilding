package com.simplebuilding.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: play a building core motion ({@code CoreHandMotion.Motion} ordinal) in a hand (ordinal of
 * {@code InteractionHand}). The server sends it only for the ore transmutation, which it alone decides; normal
 * uses start their rolled motion on the client. Applied by {@code com.simplebuilding.client.CoreMotionClient}.
 */
public record CoreMotionPayload(int hand, int motion) implements CustomPacketPayload {
    public static final Type<CoreMotionPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "core_motion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoreMotionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CoreMotionPayload::hand, ByteBufCodecs.VAR_INT, CoreMotionPayload::motion, CoreMotionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
