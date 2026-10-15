package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.WandererRegistry;
import com.simplewanderers.client.VillageWandererRenderer;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class WanderersNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(WandererRegistry.WANDERER, VillageWandererRenderer::new);
            event.registerEntityRenderer(WandererRegistry.HORSE, HorseRenderer::new);
        });
    }

    private WanderersNeoClient() {}
}
