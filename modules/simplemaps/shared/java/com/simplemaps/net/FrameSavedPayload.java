package com.simplemaps.net;

import com.simplemaps.SimpleMaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the answer to a {@link FrameViewPayload} (owner 21.10.), {@code result} = {@link MapsNetwork.FrameSave#ordinal}. */
public record FrameSavedPayload(int entityId, int result) implements CustomPacketPayload {
    public static final Type<FrameSavedPayload> TYPE = new Type<>(SimpleMaps.id("frame_saved"));
    public static final StreamCodec<ByteBuf, FrameSavedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FrameSavedPayload::entityId, ByteBufCodecs.VAR_INT, FrameSavedPayload::result,
            FrameSavedPayload::new);

    public MapsNetwork.FrameSave save() {
        MapsNetwork.FrameSave[] all = MapsNetwork.FrameSave.values();
        return result >= 0 && result < all.length ? all[result] : MapsNetwork.FrameSave.INVALID;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
