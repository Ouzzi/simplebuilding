package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.client.MixedOctetRenderer;
import com.simpleoctets.registry.OctetRegistry;
import net.fabricmc.api.ClientModInitializer;

public final class OctetsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(OctetRegistry.MIXED_OCTET_BE, MixedOctetRenderer::new);
    }
}
