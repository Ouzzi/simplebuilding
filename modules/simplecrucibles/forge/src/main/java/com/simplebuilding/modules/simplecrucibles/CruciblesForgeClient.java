package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.client.CrucibleRenderer;
import com.simplecrucibles.client.CrucibleScreen;
import com.simplecrucibles.registry.CrucibleBlockEntities;
import com.simplecrucibles.registry.CrucibleMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge setup (dist guard in the entry point). */
final class CruciblesForgeClient {
    static void init(FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(CrucibleBlockEntities.CRUCIBLE, CrucibleRenderer::new));
        net.minecraftforge.client.event.RegisterColorHandlersEvent.Block.BUS.addListener(event ->
                event.register(java.util.List.of(com.simplecrucibles.client.ReinforcedCauldronTint.INSTANCE), com.simplecrucibles.registry.CrucibleBlocks.REINFORCED_CAULDRON));
        // Split stage 5b: the soul burn screen filter (soul lava's textures come with its fluid type).
        net.minecraftforge.client.event.AddGuiOverlayLayersEvent.BUS.addListener(event -> {
            var root = event.getLayeredDraw();
            root.addAbove(net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.POST_SLEEP_STACK, com.simplecrucibles.SimpleCrucibles.id("soul_burn_filter"),
                    net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.CHAT_OVERLAY, (gg, dt) -> com.simplecrucibles.client.SoulBurnOverlay.render(gg));
        });
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> {
                    CrucibleMenus.CRUCIBLES.values().forEach(type -> MenuScreens.register(type, CrucibleScreen::new));
                    CrucibleMenus.BARRELS.values().forEach(type -> MenuScreens.register(type, net.minecraft.client.gui.screens.inventory.ContainerScreen::new));
                }));
    }

    private CruciblesForgeClient() {}
}
