package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.client.MixedOctetRenderer;
import com.simpleoctets.registry.OctetRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class OctetsNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(OctetRegistry.MIXED_OCTET_BE, MixedOctetRenderer::new));
    }

    private OctetsNeoClient() {}
}
