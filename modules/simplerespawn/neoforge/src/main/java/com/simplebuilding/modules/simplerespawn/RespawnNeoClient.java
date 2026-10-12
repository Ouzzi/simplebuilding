package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only; referenced only behind the dist check and from client-bound payload handlers. */
final class RespawnNeoClient {
    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> RespawnClient.reset());
    }

    static void receive(DownedListPayload payload) {
        RespawnClient.receive(payload);
    }

    static void receive(RespawnStatePayload payload) {
        RespawnClient.receive(payload);
    }

    private RespawnNeoClient() {}
}
