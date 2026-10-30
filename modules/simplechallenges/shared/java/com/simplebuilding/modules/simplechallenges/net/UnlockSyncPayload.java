package com.simplebuilding.modules.simplechallenges.net;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: free cells of one dimension. {@code full} replaces the client's set (first packet of a join sync), else the
 * keys are added (later packets of the join sync, and one packet per unlock). Large areas are sent in batches of
 * {@link #BATCH} cells. {@code cell} 0 with no keys tells the client that the challenge ended.
 */
public record UnlockSyncPayload(String dimension, int cell, boolean full, boolean exemptCreative, long[] keys) implements CustomPacketPayload {
    public static final int BATCH = 8192;
    public static final Type<UnlockSyncPayload> TYPE = new Type<>(SimpleChallenges.id("unlock_sync"));
    public static final StreamCodec<FriendlyByteBuf, UnlockSyncPayload> CODEC = CustomPacketPayload.codec(UnlockSyncPayload::write, UnlockSyncPayload::read);

    private static UnlockSyncPayload read(FriendlyByteBuf buf) {
        String dim = buf.readUtf(128);
        int cell = buf.readVarInt();
        boolean full = buf.readBoolean(), exempt = buf.readBoolean();
        int n = Math.min(BATCH, buf.readVarInt());
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) keys[i] = buf.readLong();
        return new UnlockSyncPayload(dim, cell, full, exempt, keys);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(dimension, 128);
        buf.writeVarInt(cell);
        buf.writeBoolean(full);
        buf.writeBoolean(exemptCreative);
        int n = Math.min(BATCH, keys.length);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeLong(keys[i]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
