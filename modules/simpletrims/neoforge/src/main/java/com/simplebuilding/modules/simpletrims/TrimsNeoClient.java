package com.simplebuilding.modules.simpletrims;

import com.simpletrims.TrimsRegistry;
import com.simpletrims.client.PlacedTrimTemplateRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class TrimsNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(TrimsRegistry.PLACED_TEMPLATE_BE, PlacedTrimTemplateRenderer::new));
    }

    private TrimsNeoClient() {}
}
