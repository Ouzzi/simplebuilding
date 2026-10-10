package com.simplebuilding.modules.simplemobs;

import com.simplebuilding.modules.simplemobs.client.DeceiverModel;
import com.simplebuilding.modules.simplemobs.client.DeceiverRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public final class FabricClientExample implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(DeceiverModel.LAYER, DeceiverModel::createBodyLayer);
        EntityRendererRegistry.register(MobsRegistry.DECEIVER, DeceiverRenderer::new);
    }
}
