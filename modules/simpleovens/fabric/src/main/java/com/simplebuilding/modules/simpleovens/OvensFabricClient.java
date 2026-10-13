package com.simplebuilding.modules.simpleovens;

import com.simpleovens.OvensRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;

/** Furnace minecarts: Vanilla's cart model with the tier furnace, like Vanilla's furnace cart. */
public final class OvensFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OvensRegistry.CARTS.values().forEach(type -> EntityRendererRegistry.register(type, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART)));
    }
}
