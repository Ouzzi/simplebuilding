package com.simplebuilding.modules.simpletweaks.table.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one page (at most {@value #PAGE} rows) of the owner list. A row holds the owner's name (empty = hidden), the
 * chunk count in all dimensions, the number of dimensions, and one chunk of the table's dimension to center the map on
 * ({@link #NONE} if there is none there). Never member lists or codes.
 */
public record TableListPayload(int page, int total, boolean onlyMine, List<Row> rows) implements CustomPacketPayload {
    public static final int PAGE = 50, NONE = Integer.MIN_VALUE;
    public record Row(String name, int chunks, int dimensions, int chunkX, int chunkZ, int color, boolean mine) {}
    public static final Type<TableListPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "table_list"));
    private static final StreamCodec<ByteBuf, Row> ROW = StreamCodec.of((b, r) -> {
        ByteBufCodecs.stringUtf8(16).encode(b, r.name); ByteBufCodecs.VAR_INT.encode(b, r.chunks); ByteBufCodecs.VAR_INT.encode(b, r.dimensions);
        ByteBufCodecs.INT.encode(b, r.chunkX); ByteBufCodecs.INT.encode(b, r.chunkZ); ByteBufCodecs.INT.encode(b, r.color); ByteBufCodecs.BOOL.encode(b, r.mine);
    }, b -> new Row(ByteBufCodecs.stringUtf8(16).decode(b), ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
            ByteBufCodecs.INT.decode(b), ByteBufCodecs.INT.decode(b), ByteBufCodecs.INT.decode(b) & 0xFFFFFF, ByteBufCodecs.BOOL.decode(b)));
    public static final StreamCodec<ByteBuf, TableListPayload> CODEC = StreamCodec.of(TableListPayload::write, TableListPayload::read);

    private static void write(ByteBuf buf, TableListPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.page); ByteBufCodecs.VAR_INT.encode(buf, p.total); ByteBufCodecs.BOOL.encode(buf, p.onlyMine);
        ROW.apply(ByteBufCodecs.list(PAGE)).encode(buf, p.rows);
    }
    private static TableListPayload read(ByteBuf buf) {
        return new TableListPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf), ROW.apply(ByteBufCodecs.list(PAGE)).decode(buf));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
