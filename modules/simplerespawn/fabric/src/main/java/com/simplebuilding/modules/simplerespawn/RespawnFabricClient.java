package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.client.RespawnClient;
import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client: receives the downed list and the own state, forgets both when leaving. */
public final class RespawnFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DownedListPayload.TYPE, (p, c) -> c.client().execute(() -> RespawnClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(RespawnStatePayload.TYPE, (p, c) -> c.client().execute(() -> RespawnClient.receive(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> c.execute(RespawnClient::reset));
    }
}
