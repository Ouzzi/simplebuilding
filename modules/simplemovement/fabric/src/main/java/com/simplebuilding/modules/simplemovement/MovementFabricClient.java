package com.simplebuilding.modules.simplemovement;

import net.fabricmc.api.ClientModInitializer;

/** Client: receives the server values ({@code config_sync}) and forgets them when leaving. */
public final class MovementFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.simplebuilding.modules.simplemovement.client.MovementClient.migrateKeys(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
        com.simplebuilding.modules.simplemovement.client.MovementClient.initClient();
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplebuilding.modules.simplemovement.client.MovementClient.autoWalkKey);
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplebuilding.modules.simplemovement.client.MovementClient.crawlKey);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.simplebuilding.modules.simplemovement.client.MovementClient::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simplemovement.network.CrawlStatePayload.TYPE,
                (p, c) -> c.client().execute(() -> com.simplebuilding.modules.simplemovement.client.MovementClient.receive(p)));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simplemovement.network.ConfigSyncPayload.TYPE,
                (p, c) -> c.client().execute(() -> SimpleMovement.receiveSync(p.json())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> SimpleMovement.clearSync());
    }
}
