package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.client.CuttingBoardRenderer;
import com.simplesandwiches.registry.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;

public final class SandwichesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(ModBlockEntities.CUTTING_BOARD, CuttingBoardRenderer::new);
    }
}
