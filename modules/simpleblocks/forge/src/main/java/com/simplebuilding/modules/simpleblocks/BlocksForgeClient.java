package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;

/** Client-only Forge setup (dist guard in the entry point): the Shaping Saw screen, the rising block renderer, the grass slab tint. */
final class BlocksForgeClient {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        // The rising block is drawn like falling sand; the grass slab is tinted by the biome like the grass block.
        net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> event.registerEntityRenderer(
                com.simpleblocks.nature.NatureBlocks.LEVITATING_BLOCK, net.minecraft.client.renderer.entity.FallingBlockRenderer::new));
        net.minecraftforge.client.event.RegisterColorHandlersEvent.Block.BUS.addListener(event -> event.register(
                java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()), com.simpleblocks.nature.NatureBlocks.GRASS_SLAB));
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(event ->
                event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new)));
    }

    private BlocksForgeClient() {}
}
