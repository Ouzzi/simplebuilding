package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.WandererRegistry;
import com.simplewanderers.client.VillageWandererRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class WanderersForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(WandererRegistry.WANDERER, VillageWandererRenderer::new);
            event.registerEntityRenderer(WandererRegistry.HORSE, HorseRenderer::new);
        });
    }

    private WanderersForgeClient() {}
}
