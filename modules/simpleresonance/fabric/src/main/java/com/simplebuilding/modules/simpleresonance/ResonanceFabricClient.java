package com.simplebuilding.modules.simpleresonance;

import com.simpleresonance.client.ResonanceClient;
import com.simpleresonance.client.ResonanceClientInit;
import com.simpleresonance.net.ResonanceStatsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric client entry (MOB27-3 R2): the stats receiver, clearing on disconnect, the resonance button in rebuilt inventories. */
public final class ResonanceFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ResonanceStatsPayload.TYPE, (payload, context) -> context.client().execute(() -> ResonanceClient.receive(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ResonanceClient::clear));
        ResonanceClientInit.init();
    }
}
