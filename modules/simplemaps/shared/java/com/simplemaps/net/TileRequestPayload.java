package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "send me these tiles unless my version is current". At most {@link #MAX} entries. */
public record TileRequestPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX = 48;
    public static final Type<TileRequestPayload> TYPE = new Type<>(SimpleMaps.id("tiles"));

    public record Entry(int mapId, int zoom, int tx, int tz, int version) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::mapId, ByteBufCodecs.VAR_INT, Entry::zoom,
                ByteBufCodecs.INT, Entry::tx, ByteBufCodecs.INT, Entry::tz, ByteBufCodecs.INT, Entry::version, Entry::new);
    }

    public static final StreamCodec<ByteBuf, TileRequestPayload> CODEC =
            Entry.CODEC.apply(ByteBufCodecs.list(MAX)).map(TileRequestPayload::new, TileRequestPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
