package com.simplebuilding.modules.simpletweaks.table.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server, only while a claim table screen is open. {@code AREA}: a, b = min chunk x, z; c, d = width, height.
 * {@code LIST}: a = page (0-based), b = 1 for "only mine". The server re-checks the table, the distance, the sizes and
 * the rate; the dimension is always the table's.
 */
public record TableRequestPayload(BlockPos pos, int kind, int a, int b, int c, int d) implements CustomPacketPayload {
    public static final int AREA = 0, LIST = 1;
    public static final Type<TableRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "table_request"));
    public static final StreamCodec<ByteBuf, TableRequestPayload> CODEC = StreamCodec.of(TableRequestPayload::write, TableRequestPayload::read);

    private static void write(ByteBuf buf, TableRequestPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.pos);
        ByteBufCodecs.VAR_INT.encode(buf, p.kind);
        ByteBufCodecs.INT.encode(buf, p.a); ByteBufCodecs.INT.encode(buf, p.b);
        ByteBufCodecs.VAR_INT.encode(buf, p.c); ByteBufCodecs.VAR_INT.encode(buf, p.d);
    }
    private static TableRequestPayload read(ByteBuf buf) {
        return new TableRequestPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
