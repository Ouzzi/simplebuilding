package com.simplebuilding.modules.simpletrims;

import com.simpletrims.TrimsRegistry;
import com.simpletrims.client.PlacedTrimTemplateRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class TrimsForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(TrimsRegistry.PLACED_TEMPLATE_BE, PlacedTrimTemplateRenderer::new));
    }

    private TrimsForgeClient() {}
}
