package com.simplebuilding.modules.simplefarming;

import net.fabricmc.api.ClientModInitializer;

/** Client: receives the server values ({@code config_sync}) and forgets them when leaving. */
public final class FarmingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simplefarming.network.ConfigSyncPayload.TYPE,
                (p, c) -> c.client().execute(() -> SimpleFarming.receiveSync(p.json())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> SimpleFarming.clearSync());
    }
}
