package com.simplepads.network;

import com.simplepads.SimplePads;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the server's {@code simplepads-server.json} as JSON (boost count for the HUD, stranger break times). */
public record PadsConfigPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PadsConfigPayload> ID = new CustomPacketPayload.Type<>(SimplePads.id("config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PadsConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(65536), PadsConfigPayload::json, PadsConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
