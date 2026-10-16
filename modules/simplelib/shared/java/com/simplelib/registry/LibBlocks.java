package com.simplelib.registry;

import com.simplelib.SimpleLib;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Blocks of SimpleLib (the crucibles live in Simple Crucibles): the warped wart plant. */
public final class LibBlocks {
    public static com.simplelib.warped.WarpedWartBlock WARPED_WART;
    /** Forge swaps in a subclass that reports the nether plant type (IPlantable); set before registration. */
    public static Function<BlockBehaviour.Properties, com.simplelib.warped.WarpedWartBlock> WARPED_WART_FACTORY = com.simplelib.warped.WarpedWartBlock::new;

    public static void register() {
        if (WARPED_WART != null) return;
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleLib.id("warped_wart"));
        WARPED_WART = Registry.register(BuiltInRegistries.BLOCK, key, WARPED_WART_FACTORY.apply(
                BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_WART).mapColor(MapColor.WARPED_WART_BLOCK).setId(key)));
    }

    private LibBlocks() {}
}
