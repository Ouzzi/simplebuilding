package com.simplebuilding.modules.simplechallenges.net;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the locked cell the player looks at (the crosshair line "price - can you pay it"). {@code show} false clears
 * the line. {@code currency} is items / credits / xp, {@code item} the item id for items, {@code have} what the player owns of it.
 */
public record CellInfoPayload(boolean show, int blockX, int blockZ, String currency, String item, int count, int have, boolean canPay) implements CustomPacketPayload {
    public static final Type<CellInfoPayload> TYPE = new Type<>(SimpleChallenges.id("cell_info"));
    public static final StreamCodec<FriendlyByteBuf, CellInfoPayload> CODEC = CustomPacketPayload.codec(CellInfoPayload::write, CellInfoPayload::read);
    public static final CellInfoPayload NONE = new CellInfoPayload(false, 0, 0, "", "", 0, 0, false);

    private static CellInfoPayload read(FriendlyByteBuf buf) {
        return new CellInfoPayload(buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readUtf(16), buf.readUtf(128), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(show);
        buf.writeInt(blockX);
        buf.writeInt(blockZ);
        buf.writeUtf(currency, 16);
        buf.writeUtf(item, 128);
        buf.writeVarInt(count);
        buf.writeVarInt(have);
        buf.writeBoolean(canPay);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
