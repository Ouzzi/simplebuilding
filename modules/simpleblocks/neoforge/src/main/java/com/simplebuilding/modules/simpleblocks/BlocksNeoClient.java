package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only; referenced only on the client dist: the Shaping Saw screen, the rising block renderer, the grass slab tint. */
final class BlocksNeoClient {
    static void init(IEventBus bus) {
        bus.addListener((RegisterMenuScreensEvent event) -> event.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new));
        // The rising block is drawn like falling sand; the grass slab is tinted by the biome like the grass block.
        bus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(
                com.simpleblocks.nature.NatureBlocks.LEVITATING_BLOCK, net.minecraft.client.renderer.entity.FallingBlockRenderer::new));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) -> event.register(
                java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()), com.simpleblocks.nature.NatureBlocks.GRASS_SLAB));
    }

    private BlocksNeoClient() {}
}
