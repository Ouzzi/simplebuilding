package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.client.EndBoatRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class SimpleEndFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(com.simplebuilding.modules.simpleend.client.NibblemiteModel.LAYER, com.simplebuilding.modules.simpleend.client.NibblemiteModel::createBodyLayer);
        EntityRendererRegistry.register(EndRegistry.NIBBLEMITE, com.simplebuilding.modules.simpleend.client.NibblemiteRenderer::new);
        for (EndWood w : EndWood.ALL) {
            EntityRendererRegistry.register(w.boatType(), c -> new EndBoatRenderer(c, w, false));
            EntityRendererRegistry.register(w.chestBoatType(), c -> new EndBoatRenderer(c, w, true));
        }
    }
}
