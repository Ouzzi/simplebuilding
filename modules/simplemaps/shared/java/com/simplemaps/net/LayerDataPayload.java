package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the layers of a map rectangle. {@code loaded} = bit per chunk (row by row, {@code w x h} chunks from
 * {@code minCx, minCz}; empty when not asked or the layer is off on the server), {@code markers} of the asked sources,
 * {@code available} = every source the server offers (id + title key) so the client can list the switches.
 */
public record LayerDataPayload(int mapId, Area area, byte[] loaded, List<Marker> markers, List<Source> available) implements CustomPacketPayload {
    public static final int MAX_MARKERS = 256, MAX_LINES = 6, MAX_LINE = 80, MAX_SPAN_CHUNKS = 128;
    public static final Type<LayerDataPayload> TYPE = new Type<>(SimpleMaps.id("layer_data"));

    /** Chunk rectangle the bit field describes. */
    public record Area(int minCx, int minCz, int w, int h) {
        static final StreamCodec<ByteBuf, Area> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, Area::minCx, ByteBufCodecs.INT, Area::minCz, ByteBufCodecs.VAR_INT, Area::w, ByteBufCodecs.VAR_INT, Area::h, Area::new);
    }

    /**
     * {@code kind}: "" = a diamond at the point; {@code "area:RRGGBB:M:W"} = a filled strip of W chunks (W 1..128, from the
     * chunk corner x, z towards +x, one chunk tall) in that color, M = 1 for the viewer's own land (see {@link #area}).
     */
    public record Marker(String source, int x, int z, String kind, List<String> lines) {
        public static final int MAX_KIND = 32;
        static final StreamCodec<ByteBuf, Marker> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(LayerRequestPayload.MAX_ID), Marker::source, ByteBufCodecs.INT, Marker::x, ByteBufCodecs.INT, Marker::z,
                ByteBufCodecs.stringUtf8(MAX_KIND), Marker::kind,
                ByteBufCodecs.stringUtf8(MAX_LINE).apply(ByteBufCodecs.list(MAX_LINES)), Marker::lines, Marker::new);

        /** {@code {rgb, mine, chunks}} of an area kind, or null for a point marker or malformed data (never trusted). */
        public int[] area() {
            if (!kind.startsWith("area:")) return null;
            String[] f = kind.split(":", -1);
            if (f.length != 4 || f[1].length() != 6) return null;
            try {
                int w = Integer.parseInt(f[3]);
                return w < 1 || w > MAX_SPAN_CHUNKS ? null : new int[]{Integer.parseInt(f[1], 16), f[2].equals("1") ? 1 : 0, w};
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public record Source(String id, String titleKey) {
        static final StreamCodec<ByteBuf, Source> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(LayerRequestPayload.MAX_ID), Source::id, ByteBufCodecs.stringUtf8(96), Source::titleKey, Source::new);
    }

    public static final StreamCodec<ByteBuf, LayerDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LayerDataPayload::mapId, Area.CODEC, LayerDataPayload::area,
            ByteBufCodecs.byteArray(MAX_SPAN_CHUNKS * MAX_SPAN_CHUNKS / 8), LayerDataPayload::loaded,
            Marker.CODEC.apply(ByteBufCodecs.list(MAX_MARKERS)), LayerDataPayload::markers,
            Source.CODEC.apply(ByteBufCodecs.list(LayerRequestPayload.MAX_SOURCES)), LayerDataPayload::available,
            LayerDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
