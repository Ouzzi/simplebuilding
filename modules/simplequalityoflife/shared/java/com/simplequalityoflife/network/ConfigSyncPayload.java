package com.simplequalityoflife.network;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> Client Paket, das die (als JSON serialisierte) Server-Config überträgt.
 */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Simplequalityoflife.MOD_ID, "config_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(32768), ConfigSyncPayload::json, ConfigSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
