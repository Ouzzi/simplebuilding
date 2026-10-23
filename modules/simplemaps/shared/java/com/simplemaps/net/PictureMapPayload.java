package com.simplemaps.net;

import com.simplemaps.PictureMaps;
import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server (AUD-6): the colour bytes of map {@code (col, row)} of a {@code cols x rows} picture wall. The picture
 * itself never leaves the client, only these already converted map colours; the server checks every field.
 */
public record PictureMapPayload(int cols, int rows, int col, int row, byte[] colors) implements CustomPacketPayload {
    public static final Type<PictureMapPayload> TYPE = new Type<>(SimpleMaps.id("picture_map"));
    public static final StreamCodec<ByteBuf, PictureMapPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PictureMapPayload::cols, ByteBufCodecs.VAR_INT, PictureMapPayload::rows,
            ByteBufCodecs.VAR_INT, PictureMapPayload::col, ByteBufCodecs.VAR_INT, PictureMapPayload::row,
            ByteBufCodecs.byteArray(PictureMaps.BYTES), PictureMapPayload::colors, PictureMapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
