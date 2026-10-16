package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.client.CrucibleRenderer;
import com.simplecrucibles.client.CrucibleScreen;
import com.simplecrucibles.registry.CrucibleBlockEntities;
import com.simplecrucibles.registry.CrucibleMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist. */
final class CruciblesNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(CrucibleBlockEntities.CRUCIBLE, CrucibleRenderer::new));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) ->
                event.register(java.util.List.of(com.simplecrucibles.client.ReinforcedCauldronTint.INSTANCE), com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CAULDRON));
        bus.addListener((RegisterMenuScreensEvent event) -> {
            CrucibleMenus.CRUCIBLES.values().forEach(type -> event.register(type, CrucibleScreen::new));
            CrucibleMenus.BARRELS.values().forEach(type -> event.register(type, net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
        });
    }

    private CruciblesNeoClient() {}
}
