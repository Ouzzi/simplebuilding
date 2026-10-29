package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.client.AnimalsClient;
import com.simpleanimals.client.BoarRenderer;
import com.simpleanimals.client.CrabRenderer;
import com.simpleanimals.client.DeerRenderer;
import com.simpleanimals.client.HedgehogRenderer;
import com.simpleanimals.client.StripedHorseRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class AnimalsNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                AnimalsClient.modelLayers((location, definition) -> event.registerLayerDefinition(location, definition::get)));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(AnimalsRegistry.DEER, DeerRenderer::new);
            event.registerEntityRenderer(AnimalsRegistry.BOAR, BoarRenderer::new);
            event.registerEntityRenderer(AnimalsRegistry.HEDGEHOG, HedgehogRenderer::new);
            event.registerEntityRenderer(AnimalsRegistry.CRAB, CrabRenderer::new);
            event.registerEntityRenderer(AnimalsRegistry.ZEBRA, ctx -> new StripedHorseRenderer<com.simpleanimals.entity.Zebra>(ctx, StripedHorseRenderer.ZEBRA));
            event.registerEntityRenderer(AnimalsRegistry.ZORSE, ctx -> new StripedHorseRenderer<com.simpleanimals.entity.Zorse>(ctx, StripedHorseRenderer.ZORSE));
        });
    }

    private AnimalsNeoClient() {}
}
