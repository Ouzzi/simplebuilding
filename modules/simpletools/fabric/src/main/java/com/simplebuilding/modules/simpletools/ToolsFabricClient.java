package com.simplebuilding.modules.simpletools;

import net.fabricmc.api.ClientModInitializer;

/** Client: receives the server values ({@code config_sync}) and forgets them when leaving. */
public final class ToolsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.minecraft.client.gui.screens.MenuScreens.register(ToolsRegistry.DEATH_VAULT_MENU, com.simplebuilding.modules.simpletools.client.DeathVaultScreen::new);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplebuilding.modules.simpletools.network.ConfigSyncPayload.TYPE,
                (p, c) -> c.client().execute(() -> SimpleTools.receiveSync(p.json())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> SimpleTools.clearSync());
    }
}
