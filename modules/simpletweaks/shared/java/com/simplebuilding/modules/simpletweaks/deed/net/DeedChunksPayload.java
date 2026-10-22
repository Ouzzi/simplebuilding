package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: one page (of {@code pages}) of the deed owner's chunks in {@code dimension}. Each tile: chunk
 * {@code x, z}, {@code age} in ticks since its picture was taken ({@link #NO_PICTURE}: none yet) and the picture
 * (256 packed map colours, empty without one). At most {@value #PAGE} tiles per page, 256 chunks per owner.
 */
public record DeedChunksPayload(String dimension, int page, int pages, List<Tile> tiles) implements CustomPacketPayload {
    public static final int PAGE = 64, MAX_PAGES = 4, NO_PICTURE = -1;
    public record Tile(int x, int z, int age, byte[] pixels) {
        static final StreamCodec<ByteBuf, Tile> CODEC = StreamCodec.composite(ByteBufCodecs.INT, Tile::x, ByteBufCodecs.INT, Tile::z,
                ByteBufCodecs.VAR_INT, Tile::age, ByteBufCodecs.byteArray(256), Tile::pixels, Tile::new);
        public boolean hasPicture() { return age != NO_PICTURE && pixels.length == 256; }
        @Override public boolean equals(Object o) { return o instanceof Tile t && t.x == x && t.z == z && t.age == age && java.util.Arrays.equals(t.pixels, pixels); }
        @Override public int hashCode() { return java.util.Objects.hash(x, z, age, java.util.Arrays.hashCode(pixels)); }
    }
    public static final Type<DeedChunksPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_chunks"));
    public static final StreamCodec<ByteBuf, DeedChunksPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), DeedChunksPayload::dimension, ByteBufCodecs.VAR_INT, DeedChunksPayload::page,
            ByteBufCodecs.VAR_INT, DeedChunksPayload::pages, Tile.CODEC.apply(ByteBufCodecs.list(PAGE)), DeedChunksPayload::tiles, DeedChunksPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
