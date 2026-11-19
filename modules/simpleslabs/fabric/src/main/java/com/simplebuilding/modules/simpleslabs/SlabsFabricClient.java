package com.simplebuilding.modules.simpleslabs;

import com.simpleslabs.registry.SlabRegistry;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client: the grass half slab (and its side form) is tinted by the biome like the grass block (N86-5). */
public final class SlabsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (SlabRegistry.GRASS.isEmpty()) return;
        net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(
                java.util.List.of(net.minecraft.client.color.block.BlockTintSources.grassBlock()),
                SlabRegistry.GRASS.toArray(new net.minecraft.world.level.block.Block[0]));
    }
}
