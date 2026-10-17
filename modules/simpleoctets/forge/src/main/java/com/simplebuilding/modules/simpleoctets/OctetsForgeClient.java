package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.client.MixedOctetRenderer;
import com.simpleoctets.registry.OctetRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class OctetsForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(OctetRegistry.MIXED_OCTET_BE, MixedOctetRenderer::new));
    }

    private OctetsForgeClient() {}
}
