package com.simplebuilding.modules.simpleovens;

import com.simpleovens.OvensRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class OvensNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                OvensRegistry.CARTS.values().forEach(type -> event.registerEntityRenderer(type, c -> new MinecartRenderer(c, ModelLayers.FURNACE_MINECART))));
    }

    private OvensNeoClient() {}
}
