package com.simplebuilding.modules.simplemovement.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: the server values as JSON (client prediction must use them). Never registered client -> server. */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<ConfigSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simplemovement", "config_sync"));
    public static final StreamCodec<io.netty.buffer.ByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(32768), ConfigSyncPayload::json, ConfigSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
