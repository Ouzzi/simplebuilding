package com.simplebuilding.modules.simplestats;

import net.fabricmc.api.ClientModInitializer;

/** Client: registers the key that opens the statistics of an item. */
public final class StatsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(com.simplebuilding.modules.simplestats.client.StatsClient.initKey());
    }
}
