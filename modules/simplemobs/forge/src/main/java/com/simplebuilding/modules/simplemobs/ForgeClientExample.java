package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.client.DeceiverModel;
import com.simplebuilding.modules.simplemobs.client.DeceiverRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class ForgeClientExample {
    static void init() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event ->
                event.registerLayerDefinition(DeceiverModel.LAYER, DeceiverModel::createBodyLayer));
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerEntityRenderer(MobsRegistry.DECEIVER, DeceiverRenderer::new));
    }
    private ForgeClientExample() {}
}
