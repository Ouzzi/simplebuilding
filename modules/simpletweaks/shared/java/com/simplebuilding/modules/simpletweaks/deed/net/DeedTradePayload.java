package com.simplebuilding.modules.simpletweaks.deed.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server: the "Trade" button of the deed's buy tab (WH26-9): offer {@code offer} of the server's deed trades,
 * for the deed in {@code hand}. Only the owner holding their deed; the server re-checks the offer, the items and the
 * limit and answers with a fresh {@link DeedOpenPayload} naming the result ({@code trade.ok}, {@code trade.items}, ...).
 */
public record DeedTradePayload(int hand, int offer) implements CustomPacketPayload {
    public static final Type<DeedTradePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("simpletweaks", "deed_trade"));
    public static final StreamCodec<ByteBuf, DeedTradePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeedTradePayload::hand, ByteBufCodecs.VAR_INT, DeedTradePayload::offer, DeedTradePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
