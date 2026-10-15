package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.WandererRegistry;
import com.simplewanderers.client.VillageWandererRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.HorseRenderer;

public final class WanderersFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(WandererRegistry.WANDERER, VillageWandererRenderer::new);
        EntityRendererRegistry.register(WandererRegistry.HORSE, HorseRenderer::new);
    }
}
