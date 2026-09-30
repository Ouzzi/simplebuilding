package com.simplebuilding.modules.simplemodels;
public final class FabricModelsClient implements net.fabricmc.api.ClientModInitializer {
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CataloguePayload.ID,
            (payload, context) -> context.client().execute(() -> com.simplebuilding.modules.simplemodels.client.ModelBrowser.accept(payload)));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            com.simplebuilding.modules.simplemodels.client.ModelBrowser.clear());
    }
}
