package com.simplebuilding.modules.simpleearth.net;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "send me these terrain tiles" (grid exponent e, tile x, tile z), at most {@link #MAX} per packet. */
public record TileRequestPayload(List<Key> keys) implements CustomPacketPayload {
    public static final int MAX = 64;
    public static final Type<TileRequestPayload> TYPE = new Type<>(SimpleEarth.id("tile_request"));
    public static final StreamCodec<FriendlyByteBuf, TileRequestPayload> CODEC = CustomPacketPayload.codec(TileRequestPayload::write, TileRequestPayload::read);

    public record Key(int e, int tx, int tz) {}

    private static TileRequestPayload read(FriendlyByteBuf buf) {
        int n = Math.min(MAX, buf.readVarInt());
        List<Key> keys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) keys.add(new Key(buf.readByte(), buf.readInt(), buf.readInt()));
        return new TileRequestPayload(keys);
    }

    private void write(FriendlyByteBuf buf) {
        int n = Math.min(MAX, keys.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Key k = keys.get(i);
            buf.writeByte(k.e());
            buf.writeInt(k.tx());
            buf.writeInt(k.tz());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
