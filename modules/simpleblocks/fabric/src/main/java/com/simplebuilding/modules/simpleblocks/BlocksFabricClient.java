package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.saw.Sawing;
import com.simpleblocks.saw.ShapingSawMenu;
import com.simpleblocks.saw.client.ShapingSawScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

/** Fabric client: the Shaping Saw screen and the receiver of its recipe list, the rising block renderer, the grass slab tint. */
public final class BlocksFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.<ShapingSawMenu, ShapingSawScreen>register(Sawing.MENU, ShapingSawScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(Sawing.Payload.TYPE, (payload, context) -> context.client().execute(() -> Sawing.receive(payload.entries())));
        // The rising block is drawn like falling sand; the grass slab is tinted by the biome like the grass block.
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.simpleblocks.nature.NatureBlocks.LEVITATING_BLOCK,
                net.minecraft.client.renderer.entity.FallingBlockRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()),
                com.simpleblocks.nature.NatureBlocks.GRASS_SLAB);
    }
}
