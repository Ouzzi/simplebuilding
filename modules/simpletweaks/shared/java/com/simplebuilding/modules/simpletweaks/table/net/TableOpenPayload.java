package com.simplebuilding.modules.simpletweaks.table.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: open (or refresh) the claim table. {@code visibility} = {@code TableVisibility} ordinal as the viewer
 * sees it (OFF: no map). {@code maxChunks} = the viewer's land budget. {@code state}: {@code BUY} = the next step is
 * offered ({@code nextChunks}, {@code nextLevels}, {@code nextCosts}), {@code MAX} = all steps bought, {@code LIMIT} = the
 * next step is above the server limit, {@code NONE} = expansion is off. The client only shows it; the server re-checks a purchase.
 */
public record TableOpenPayload(BlockPos pos, int visibility, int chunkX, int chunkZ, int ownChunks, int maxChunks, String dimension,
                               int state, int nextChunks, int nextLevels, List<Cost> nextCosts) implements CustomPacketPayload {
    public static final int BUY = 0, MAX = 1, LIMIT = 2, NONE = 3;
    public record Cost(String id, int count) {
        static final StreamCodec<ByteBuf, Cost> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Cost::id, ByteBufCodecs.VAR_INT, Cost::count, Cost::new);
    }
    public TableOpenPayload(BlockPos pos, int visibility, int chunkX, int chunkZ, int ownChunks, int maxChunks, String dimension) {
        this(pos, visibility, chunkX, chunkZ, ownChunks, maxChunks, dimension, NONE, 0, 0, List.of());
    }
    public static final Type<TableOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "table_open"));
    public static final StreamCodec<ByteBuf, TableOpenPayload> CODEC = StreamCodec.of(TableOpenPayload::write, TableOpenPayload::read);

    private static void write(ByteBuf buf, TableOpenPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.pos);
        ByteBufCodecs.VAR_INT.encode(buf, p.visibility);
        ByteBufCodecs.INT.encode(buf, p.chunkX); ByteBufCodecs.INT.encode(buf, p.chunkZ);
        ByteBufCodecs.VAR_INT.encode(buf, p.ownChunks); ByteBufCodecs.VAR_INT.encode(buf, p.maxChunks);
        ByteBufCodecs.stringUtf8(128).encode(buf, p.dimension);
        ByteBufCodecs.VAR_INT.encode(buf, p.state); ByteBufCodecs.VAR_INT.encode(buf, p.nextChunks); ByteBufCodecs.VAR_INT.encode(buf, p.nextLevels);
        COSTS.encode(buf, p.nextCosts);
    }
    private static final StreamCodec<ByteBuf, List<Cost>> COSTS = Cost.CODEC.apply(ByteBufCodecs.list(8));
    private static TableOpenPayload read(ByteBuf buf) {
        return new TableOpenPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.INT.decode(buf), ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.stringUtf8(128).decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), COSTS.decode(buf));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
