package com.simplebuilding.modules.simplelib;

import com.simplelib.client.CrucibleRenderer;
import com.simplelib.client.CrucibleScreen;
import com.simplelib.registry.LibBlockEntities;
import com.simplelib.registry.LibMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge setup (dist guard in the entry point). */
final class SimpleLibForgeClient {
    static void init(FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
                event.registerBlockEntityRenderer(LibBlockEntities.CRUCIBLE, CrucibleRenderer::new));
        net.minecraftforge.client.event.RegisterColorHandlersEvent.Block.BUS.addListener(event ->
                event.register(java.util.List.of(com.simplelib.client.ReinforcedCauldronTint.INSTANCE), com.simplelib.registry.LibBlocks.REINFORCED_CAULDRON));
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> LibMenus.CRUCIBLES.values().forEach(type -> MenuScreens.register(type, CrucibleScreen::new))));
    }

    private SimpleLibForgeClient() {}
}
