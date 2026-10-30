package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.net.UnlockSyncPayload;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.Arrays;
import net.minecraft.server.level.ServerPlayer;

/** Sends the free cells to clients that have the mod: everything at join, one cell per unlock, an end marker when the challenge stops. */
public final class ChallengeSync {
    private ChallengeSync() {}

    public static void sendFull(ServerPlayer player, Active a) {
        if (!SimpleChallenges.canSend.test(player, UnlockSyncPayload.TYPE)) return;
        for (String dim : a.dimensions()) {
            UnlockGrid g = a.grid(dim);
            if (g == null) continue;
            long[] keys = g.keys();
            int from = 0;
            do {
                int to = Math.min(keys.length, from + UnlockSyncPayload.BATCH);
                SimpleChallenges.toClient.accept(player, new UnlockSyncPayload(dim, g.cell(), from == 0, a.exemptCreative(), Arrays.copyOfRange(keys, from, to)));
                from = to;
            } while (from < keys.length);
        }
    }

    public static void sendDelta(Active a, String dim, int cx, int cz) {
        UnlockGrid g = a.grid(dim);
        if (g == null) return;
        UnlockSyncPayload p = new UnlockSyncPayload(dim, g.cell(), false, a.exemptCreative(), new long[]{UnlockGrid.key(cx, cz)});
        for (ServerPlayer player : a.server.getPlayerList().getPlayers()) {
            if (SimpleChallenges.canSend.test(player, UnlockSyncPayload.TYPE)) SimpleChallenges.toClient.accept(player, p);
        }
    }

    /** Tells every client the challenge ended: the cell size 0 payload clears its view. */
    public static void sendEnd(net.minecraft.server.MinecraftServer server) {
        UnlockSyncPayload p = new UnlockSyncPayload("", 0, true, false, new long[0]);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SimpleChallenges.canSend.test(player, UnlockSyncPayload.TYPE)) SimpleChallenges.toClient.accept(player, p);
        }
    }
}
