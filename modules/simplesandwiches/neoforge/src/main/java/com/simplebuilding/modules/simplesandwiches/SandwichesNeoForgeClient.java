package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.client.CuttingBoardRenderer;
import com.simplesandwiches.registry.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class SandwichesNeoForgeClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ModBlockEntities.CUTTING_BOARD, CuttingBoardRenderer::new));
    }

    private SandwichesNeoForgeClient() {}
}
