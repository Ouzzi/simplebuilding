package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.client.EndBoatRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class SimpleEndFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        for (EndWood w : EndWood.ALL) {
            EntityRendererRegistry.register(w.boatType(), c -> new EndBoatRenderer(c, w, false));
            EntityRendererRegistry.register(w.chestBoatType(), c -> new EndBoatRenderer(c, w, true));
        }
    }
}
