package com.simplebuilding.networking;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: the chisel transformation and sledgehammer upgrade files of the server's
 * datapacks (file id -> JSON text), sent on join and after every {@code /reload}
 * ({@code com.simplebuilding.data.ModDataTables}).
 */
public record DataTablesSyncPayload(Map<String, String> chisel, Map<String, String> upgrades) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DataTablesSyncPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("simplebuilding", "data_tables_sync"));

    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<String, String>> FILES =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(1024), ByteBufCodecs.stringUtf8(1 << 20));

    public static final StreamCodec<RegistryFriendlyByteBuf, DataTablesSyncPayload> CODEC = StreamCodec.composite(
            FILES, DataTablesSyncPayload::chisel,
            FILES, DataTablesSyncPayload::upgrades,
            DataTablesSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
