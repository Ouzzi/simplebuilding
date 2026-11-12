package com.simplepads;

import com.simplepads.block.entity.ChunkLoaderBlockEntity;
import com.simplepads.block.entity.ChunkLoaderRegistry;
import com.simplepads.network.PadsNetwork;
import com.simplepads.spawn.LaunchSafety;
import com.simplepads.spawn.SpawnElytra;
import com.simplepads.spawn.SpawnSetup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server events of Simple Pads (until 04.11. part of SimpleBuilding's {@code TweaksContent}); the loader hooks them up. */
public final class PadsContent {
    private PadsContent() {
    }

    public static void onServerTick(MinecraftServer server) {
        SpawnElytra.serverTick(server);
        // Chunk-Loader wecken bzw. anhalten (Besitzer kommt/geht, Schalter), siehe ChunkLoaderRegistry.
        if (server.getTickCount() % ChunkLoaderBlockEntity.CHECK_INTERVAL == 0) {
            ChunkLoaderRegistry.reconcile(server);
        }
        LaunchSafety.serverTick(server);
        // Audit b-audsb31-a (simplepads-P1-1): flypad flight that no pad covers any more (logout, restart, pad gone meanwhile).
        if (server.getTickCount() % com.simplepads.block.entity.FlypadBlockEntity.STALE_SWEEP_INTERVAL == 0) {
            com.simplepads.block.entity.FlypadBlockEntity.sweepStaleFlight(server);
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        PadsNetwork.sendConfig(player);
        SpawnSetup.onPlayerJoin(player);
        SpawnElytra.onJoinOrRespawn(player);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        SpawnElytra.onJoinOrRespawn(player);
    }
}
