package com.simplebuildingtools.networking;

import com.simplebuildingtools.SimpleBuildingTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the server's {@code simplebuildingtools-server.json} as JSON (picture import, blueprint switch). */
public record BtConfigPayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BtConfigPayload> ID = new CustomPacketPayload.Type<>(SimpleBuildingTools.id("config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BtConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(65536), BtConfigPayload::json, BtConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
