package com.simplebuilding.modules.simplelib;

import net.fabricmc.api.ClientModInitializer;

public final class SimpleLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.simplelib.net.UpgradeTablePayload.TYPE,
                (payload, context) -> context.client().execute(() -> com.simplelib.api.InWorldUpgrades.receive(payload.files())));
    }
}
