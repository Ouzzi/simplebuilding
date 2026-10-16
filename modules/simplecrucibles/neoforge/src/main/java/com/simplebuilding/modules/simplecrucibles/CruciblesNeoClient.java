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
        // Split stage 5b: soul lava rendering (turquoise lava textures) and the soul burn screen filter.
        bus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) -> event.register(
                com.simplecrucibles.client.SoulLavaModel.unbaked(), com.simplecrucibles.fluid.CrucibleFluids.SOUL_LAVA,
                com.simplecrucibles.fluid.CrucibleFluids.FLOWING_SOUL_LAVA));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) -> event.registerAbove(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "chat"), com.simplecrucibles.SimpleCrucibles.id("soul_burn_filter"),
                (extractor, deltaTracker) -> com.simplecrucibles.client.SoulBurnOverlay.render(extractor)));
        bus.addListener((RegisterMenuScreensEvent event) -> {
            CrucibleMenus.CRUCIBLES.values().forEach(type -> event.register(type, CrucibleScreen::new));
            CrucibleMenus.BARRELS.values().forEach(type -> event.register(type, net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
        });
    }

    private CruciblesNeoClient() {}
}
