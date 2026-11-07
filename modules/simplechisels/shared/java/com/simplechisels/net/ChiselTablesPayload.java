package com.simplechisels.net;

import com.simplechisels.SimpleChisels;
import com.simplechisels.data.ChiselTables;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the {@code chisel_transformations} files of the server's datapacks (file id -> JSON text), sent on join
 * and after every {@code /reload}; the client applies them with {@link ChiselTables#receive}, so its click prediction and the
 * hand hint follow the server's tables.
 */
public record ChiselTablesPayload(Map<String, String> files) implements CustomPacketPayload {
    public static final Type<ChiselTablesPayload> TYPE = new Type<>(SimpleChisels.id("tables"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChiselTablesPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(1024), ByteBufCodecs.stringUtf8(1 << 20)), ChiselTablesPayload::files,
            ChiselTablesPayload::new);

    /** The payload for the current files, or null when there is nothing to send. */
    public static ChiselTablesPayload current() {
        Map<String, String> files = ChiselTables.syncFiles();
        return files.isEmpty() ? null : new ChiselTablesPayload(files);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
