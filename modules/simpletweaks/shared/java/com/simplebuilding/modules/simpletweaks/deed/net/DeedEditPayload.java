package com.simplebuilding.modules.simpletweaks.deed.net;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: save the rights code of the deed in {@code hand} (0 main, 1 off). {@code revision} is the
 * version of the land the editor was opened on; the server refuses a stale one. The text is bounded by the codec.
 */
public record DeedEditPayload(int hand, int revision, String code) implements CustomPacketPayload {
    public static final Type<DeedEditPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_edit"));
    public static final StreamCodec<ByteBuf, DeedEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeedEditPayload::hand, ByteBufCodecs.INT, DeedEditPayload::revision,
            ByteBufCodecs.stringUtf8(ClaimCode.MAX_CHARS), DeedEditPayload::code, DeedEditPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
