package com.simplebuilding.modules.simpleearth.net;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: one terrain tile, {@code data} = {@code TileCodec} (deflated map colour ids + heights of 64 x 64 samples). */
public record EarthTilePayload(int e, int tx, int tz, byte[] data) implements CustomPacketPayload {
    public static final int MAX_BYTES = 64 * 1024;
    public static final Type<EarthTilePayload> TYPE = new Type<>(SimpleEarth.id("tile"));
    public static final StreamCodec<FriendlyByteBuf, EarthTilePayload> CODEC = CustomPacketPayload.codec(EarthTilePayload::write, EarthTilePayload::new);

    private EarthTilePayload(FriendlyByteBuf buf) {
        this(buf.readByte(), buf.readInt(), buf.readInt(), buf.readByteArray(MAX_BYTES));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeByte(e);
        buf.writeInt(tx);
        buf.writeInt(tz);
        buf.writeByteArray(data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
