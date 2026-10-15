package com.simplebuilding.modules.simpletweaks.table.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: open the claim table. {@code visibility} = {@code TableVisibility} ordinal as the viewer sees it (OFF: no map). */
public record TableOpenPayload(BlockPos pos, int visibility, int chunkX, int chunkZ, int ownChunks, int maxChunks, String dimension) implements CustomPacketPayload {
    public static final Type<TableOpenPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "table_open"));
    public static final StreamCodec<ByteBuf, TableOpenPayload> CODEC = StreamCodec.of(TableOpenPayload::write, TableOpenPayload::read);

    private static void write(ByteBuf buf, TableOpenPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.pos);
        ByteBufCodecs.VAR_INT.encode(buf, p.visibility);
        ByteBufCodecs.INT.encode(buf, p.chunkX); ByteBufCodecs.INT.encode(buf, p.chunkZ);
        ByteBufCodecs.VAR_INT.encode(buf, p.ownChunks); ByteBufCodecs.VAR_INT.encode(buf, p.maxChunks);
        ByteBufCodecs.stringUtf8(128).encode(buf, p.dimension);
    }
    private static TableOpenPayload read(ByteBuf buf) {
        return new TableOpenPayload(BlockPos.STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.INT.decode(buf), ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.stringUtf8(128).decode(buf));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
