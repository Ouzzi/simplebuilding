package com.simplebuilding.modules.simpletweaks.table.net;

import com.simplebuilding.modules.simpletweaks.table.ClaimGrid;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: owners of an area. {@code palette}: at most 255 entries (cell value n = entry n - 1); no ids, only what the
 * viewer may see ({@code name} empty = hidden owner). {@code cells}: {@code w * h} bytes, row by row (z then x), run-length coded.
 */
public record TableAreaPayload(int minX, int minZ, int w, int h, List<Entry> palette, byte[] cells) implements CustomPacketPayload {
    public record Entry(String name, int color, boolean mine) {}
    public static final Type<TableAreaPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "table_area"));
    private static final StreamCodec<ByteBuf, Entry> ENTRY = StreamCodec.of((b, e) -> {
        ByteBufCodecs.stringUtf8(16).encode(b, e.name); ByteBufCodecs.INT.encode(b, e.color); ByteBufCodecs.BOOL.encode(b, e.mine);
    }, b -> new Entry(ByteBufCodecs.stringUtf8(16).decode(b), ByteBufCodecs.INT.decode(b) & 0xFFFFFF, ByteBufCodecs.BOOL.decode(b)));
    public static final StreamCodec<ByteBuf, TableAreaPayload> CODEC = StreamCodec.of(TableAreaPayload::write, TableAreaPayload::read);

    private static void write(ByteBuf buf, TableAreaPayload p) {
        ByteBufCodecs.INT.encode(buf, p.minX); ByteBufCodecs.INT.encode(buf, p.minZ);
        ByteBufCodecs.VAR_INT.encode(buf, p.w); ByteBufCodecs.VAR_INT.encode(buf, p.h);
        ENTRY.apply(ByteBufCodecs.list(ClaimGrid.MAX_PALETTE)).encode(buf, p.palette);
        ByteBufCodecs.byteArray(ClaimGrid.MAX_ENCODED).encode(buf, p.cells);
    }
    private static TableAreaPayload read(ByteBuf buf) {
        return new TableAreaPayload(ByteBufCodecs.INT.decode(buf), ByteBufCodecs.INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ENTRY.apply(ByteBufCodecs.list(ClaimGrid.MAX_PALETTE)).decode(buf), ByteBufCodecs.byteArray(ClaimGrid.MAX_ENCODED).decode(buf));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
