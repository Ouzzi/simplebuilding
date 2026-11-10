package com.simplelib.net;

import com.simplelib.SimpleLib;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client (BACK22 core §5.5): a player just went through a portal door - show the fog pulse in {@code color} (RGB) for
 * {@code ticks} (it hides the frames the client needs to mesh the sections it had never seen).
 */
public record PortalPulsePayload(int color, int ticks) implements CustomPacketPayload {
    public static final Type<PortalPulsePayload> TYPE = new Type<>(SimpleLib.id("portal_pulse"));
    public static final StreamCodec<ByteBuf, PortalPulsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PortalPulsePayload::color, ByteBufCodecs.VAR_INT, PortalPulsePayload::ticks, PortalPulsePayload::new);

    /** Alpha 0..255 of the pulse {@code elapsed} ns after it started ({@code length} ns long): full at first, then fading. */
    public static int alpha(long elapsed, long length) {
        if (length <= 0 || elapsed < 0 || elapsed >= length) return 0;
        return (int) Math.round(255.0 * (1.0 - (double) elapsed / length));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
