package com.simplebuilding.modules.simpledinos;

import com.simpledinos.DinosRegistry;
import com.simpledinos.client.DinosClient;
import com.simpledinos.client.TrexRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class DinosFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DinosClient.modelLayers((location, definition) -> ModelLayerRegistry.registerModelLayer(location, definition::get));
        EntityRendererRegistry.register(DinosRegistry.TREX, TrexRenderer::new);
    }
}
