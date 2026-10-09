package com.simplebuilding.modules.simplecontainers;

import com.simplebuilding.modules.simplecontainers.client.StyleToggleClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class ContainersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StyleToggleClient.development = net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        KeyMappingHelper.registerKeyMapping(StyleToggleClient.KEY);
        ClientTickEvents.END_CLIENT_TICK.register(StyleToggleClient::tick);
    }
}
