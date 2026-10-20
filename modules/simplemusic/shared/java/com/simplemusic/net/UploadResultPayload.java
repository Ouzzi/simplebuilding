package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the upload's state. {@code key} is {@code accepted}, {@code pending} (stored, waiting for an OP),
 * {@code next} (send the next chunk; flow control) or a problem key {@code simplemusic.upload.error.<key>}.
 */
public record UploadResultPayload(String key, List<String> args) implements CustomPacketPayload {
    public static final Type<UploadResultPayload> TYPE = new Type<>(SimpleMusic.id("upload_result"));
    public static final StreamCodec<ByteBuf, UploadResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), UploadResultPayload::key,
            ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(8)), UploadResultPayload::args,
            UploadResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
