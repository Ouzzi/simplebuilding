package com.simplebuilding.modules.simpleovens;

import com.simpleovens.OvensRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client-only Forge setup (dist guard in the entry point). */
final class OvensForgeClient {
    static void init() {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                OvensRegistry.CARTS.values().forEach(type -> event.registerEntityRenderer(type, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART))));
    }

    private OvensForgeClient() {}
}
