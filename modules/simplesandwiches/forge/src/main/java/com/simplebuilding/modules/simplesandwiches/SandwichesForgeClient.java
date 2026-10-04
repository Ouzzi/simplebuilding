package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.client.CuttingBoardRenderer;
import com.simplesandwiches.registry.ModBlockEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class SandwichesForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(ModBlockEntities.CUTTING_BOARD, CuttingBoardRenderer::new));
    }

    private SandwichesForgeClient() {}
}
