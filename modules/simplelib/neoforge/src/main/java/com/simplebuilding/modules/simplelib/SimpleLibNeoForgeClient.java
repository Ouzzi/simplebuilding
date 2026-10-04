package com.simplebuilding.modules.simplelib;

import com.simplelib.client.CrucibleRenderer;
import com.simplelib.client.CrucibleScreen;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class SimpleLibNeoForgeClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(LibBlockEntities.CRUCIBLE, CrucibleRenderer::new));
        bus.addListener((RegisterMenuScreensEvent event) ->
                LibMenus.CRUCIBLES.values().forEach(type -> event.register(type, CrucibleScreen::new)));
    }

    private SimpleLibNeoForgeClient() {}
}
