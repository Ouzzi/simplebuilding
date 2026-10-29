package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.client.AnimalsClient;
import com.simpleanimals.client.BoarRenderer;
import com.simpleanimals.client.CrabRenderer;
import com.simpleanimals.client.DeerRenderer;
import com.simpleanimals.client.HedgehogRenderer;
import com.simpleanimals.client.StripedHorseRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class AnimalsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AnimalsClient.modelLayers((location, definition) -> ModelLayerRegistry.registerModelLayer(location, definition::get));
        EntityRendererRegistry.register(AnimalsRegistry.DEER, DeerRenderer::new);
        EntityRendererRegistry.register(AnimalsRegistry.BOAR, BoarRenderer::new);
        EntityRendererRegistry.register(AnimalsRegistry.HEDGEHOG, HedgehogRenderer::new);
        EntityRendererRegistry.register(AnimalsRegistry.CRAB, CrabRenderer::new);
        EntityRendererRegistry.register(AnimalsRegistry.ZEBRA, ctx -> new StripedHorseRenderer<com.simpleanimals.entity.Zebra>(ctx, StripedHorseRenderer.ZEBRA));
        EntityRendererRegistry.register(AnimalsRegistry.ZORSE, ctx -> new StripedHorseRenderer<com.simpleanimals.entity.Zorse>(ctx, StripedHorseRenderer.ZORSE));
    }
}
