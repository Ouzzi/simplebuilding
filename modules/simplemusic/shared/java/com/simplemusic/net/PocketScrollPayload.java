package com.simplemusic.net;

import com.simplemusic.SimpleMusic;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: Shift + mouse wheel on the placed Pocket Jukebox at {@code pos}, {@code step} &gt; 0 = next disc (PJ23-1).
 * The server owns the shown disc and checks sneaking, reach and build rights itself
 * ({@link com.simplemusic.pocket.PocketJukeboxBlock#scroll}); the block entity update brings the result back.
 */
public record PocketScrollPayload(BlockPos pos, int step) implements CustomPacketPayload {
    public static final Type<PocketScrollPayload> TYPE = new Type<>(SimpleMusic.id("pocket_scroll"));
    public static final StreamCodec<ByteBuf, PocketScrollPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PocketScrollPayload::pos, ByteBufCodecs.VAR_INT, PocketScrollPayload::step, PocketScrollPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
