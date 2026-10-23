package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: a button of the deed's map view. {@link #CLAIM_HERE} claims the chunk the player stands in (with
 * a credit, or buying the next step), {@link #BUY} buys the next step ahead, {@link #UNCLAIM} gives up the chunk
 * {@code chunk} in {@code dimension} (no refund, one credit back). Only the owner holding their deed; the server re-checks everything.
 */
public record DeedActionPayload(int hand, int action, String dimension, long chunk) implements CustomPacketPayload {
    public static final int CLAIM_HERE = 0, BUY = 1, UNCLAIM = 2;
    public static final Type<DeedActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_action"));
    public static final StreamCodec<ByteBuf, DeedActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeedActionPayload::hand, ByteBufCodecs.VAR_INT, DeedActionPayload::action,
            ByteBufCodecs.stringUtf8(128), DeedActionPayload::dimension, ByteBufCodecs.LONG, DeedActionPayload::chunk, DeedActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
