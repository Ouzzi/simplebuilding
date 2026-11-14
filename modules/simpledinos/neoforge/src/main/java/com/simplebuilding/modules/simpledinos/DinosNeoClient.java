package com.simplebuilding.modules.simpledinos;

import com.simpledinos.DinosRegistry;
import com.simpledinos.client.DinosClient;
import com.simpledinos.client.ParasaurolophusRenderer;
import com.simpledinos.client.StegosaurusRenderer;
import com.simpledinos.client.TrexRenderer;
import com.simpledinos.client.TriceratopsRenderer;
import com.simpledinos.client.UtahraptorRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only; referenced only on the client dist. */
final class DinosNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                DinosClient.modelLayers((location, definition) -> event.registerLayerDefinition(location, definition::get)));
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(DinosRegistry.TREX, TrexRenderer::new);
            event.registerEntityRenderer(DinosRegistry.TRICERATOPS, TriceratopsRenderer::new);
            event.registerEntityRenderer(DinosRegistry.STEGOSAURUS, StegosaurusRenderer::new);
            event.registerEntityRenderer(DinosRegistry.PARASAUROLOPHUS, ParasaurolophusRenderer::new);
            event.registerEntityRenderer(DinosRegistry.UTAHRAPTOR, UtahraptorRenderer::new);
        });
    }

    private DinosNeoClient() {}
}
