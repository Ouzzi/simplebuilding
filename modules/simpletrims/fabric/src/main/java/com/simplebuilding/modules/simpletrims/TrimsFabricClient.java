package com.simplebuilding.modules.simpletrims;

import com.simpletrims.TrimsRegistry;
import com.simpletrims.client.PlacedTrimTemplateRenderer;
import net.fabricmc.api.ClientModInitializer;

public final class TrimsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(TrimsRegistry.PLACED_TEMPLATE_BE, PlacedTrimTemplateRenderer::new);
    }
}
