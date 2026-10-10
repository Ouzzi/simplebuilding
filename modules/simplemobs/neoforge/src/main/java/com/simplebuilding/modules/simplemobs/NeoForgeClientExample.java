package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.client.DeceiverModel;
import com.simplebuilding.modules.simplemobs.client.DeceiverRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only behind the dist check. */
final class NeoForgeClientExample {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(DeceiverModel.LAYER, DeceiverModel::createBodyLayer));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(MobsRegistry.DECEIVER, DeceiverRenderer::new));
    }
    private NeoForgeClientExample() {}
}
