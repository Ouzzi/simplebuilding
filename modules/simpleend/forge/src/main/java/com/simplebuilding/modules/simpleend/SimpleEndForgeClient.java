package com.simplebuilding.modules.simpleend;

import com.simplebuilding.modules.simpleend.client.EndBoatRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class SimpleEndForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event -> event.registerLayerDefinition(com.simplebuilding.modules.simpleend.client.NibblemiteModel.LAYER, com.simplebuilding.modules.simpleend.client.NibblemiteModel::createBodyLayer));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(EndRegistry.NIBBLEMITE, com.simplebuilding.modules.simpleend.client.NibblemiteRenderer::new);
            for (EndWood w : EndWood.ALL) {
                event.registerEntityRenderer(w.boatType(), c -> new EndBoatRenderer(c, w, false));
                event.registerEntityRenderer(w.chestBoatType(), c -> new EndBoatRenderer(c, w, true));
            }
        });
    }
    private SimpleEndForgeClient() {}
}
