package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the chunks (with pictures) of the land named by the deed in {@code hand}, in {@code dimension}. */
public record DeedChunksRequest(int hand, String dimension) implements CustomPacketPayload {
    public static final Type<DeedChunksRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_chunks_request"));
    public static final StreamCodec<ByteBuf, DeedChunksRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeedChunksRequest::hand, ByteBufCodecs.stringUtf8(128), DeedChunksRequest::dimension, DeedChunksRequest::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
