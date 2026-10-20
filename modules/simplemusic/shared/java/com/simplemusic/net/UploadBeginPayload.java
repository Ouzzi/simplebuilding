package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: start burning the blank disc in {@code hand}: title and the announced file size. */
public record UploadBeginPayload(int hand, String title, int size) implements CustomPacketPayload {
    public static final Type<UploadBeginPayload> TYPE = new Type<>(SimpleMusic.id("upload_begin"));
    public static final StreamCodec<ByteBuf, UploadBeginPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UploadBeginPayload::hand,
            ByteBufCodecs.stringUtf8(128), UploadBeginPayload::title,
            ByteBufCodecs.VAR_INT, UploadBeginPayload::size,
            UploadBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
