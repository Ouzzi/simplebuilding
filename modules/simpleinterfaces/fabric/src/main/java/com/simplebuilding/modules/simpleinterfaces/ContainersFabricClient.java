package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.client.StyleToggleClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class ContainersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.simplebuilding.modules.simpleinterfaces.client.LegacyMigration.run(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir());
        StyleToggleClient.development = net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        KeyMappingHelper.registerKeyMapping(StyleToggleClient.KEY);
        ClientTickEvents.END_CLIENT_TICK.register(StyleToggleClient::tick);
        // Server values (linked containers, Easy Shulkers) and the linked panel (formerly Simple QoL).
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpleinterfaces.network.ConfigSyncPayload.TYPE,
                (p, c) -> c.client().execute(() -> SimpleInterfaces.receiveSync(p.json())));
        // Runs on the client thread in packet order: the slots must exist before the menu's first content packet.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpleinterfaces.network.LinkedOpenPayload.TYPE,
                (p, c) -> com.simplebuilding.modules.simpleinterfaces.client.LinkedPanel.receive(p));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> SimpleInterfaces.clearSync());
    }
}
