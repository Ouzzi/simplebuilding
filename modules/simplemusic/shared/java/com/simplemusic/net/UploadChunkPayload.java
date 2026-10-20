package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import com.simplemusic.upload.UploadSession;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: chunk {@code index} of the running upload (at most 32 KiB). */
public record UploadChunkPayload(int index, byte[] bytes) implements CustomPacketPayload {
    public static final Type<UploadChunkPayload> TYPE = new Type<>(SimpleMusic.id("upload_chunk"));
    public static final StreamCodec<ByteBuf, UploadChunkPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UploadChunkPayload::index,
            ByteBufCodecs.byteArray(UploadSession.MAX_CHUNK), UploadChunkPayload::bytes,
            UploadChunkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
