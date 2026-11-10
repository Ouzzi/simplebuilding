package com.simplebuilding.modules.simplelayers.backrooms.api;

import java.util.function.Function;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * What a level registers itself (BACK22 core §6, API freeze): blocks and items in namespace {@code simplelayers}, ids prefixed with
 * the level's theme id ({@code <id>_…}, test {@code bands_ids_unique}). Items land in the Simple Layers tab after the core's, in
 * {@code Bands.ORDER}. Called once while the registries are open, after the core's own blocks and items.
 */
public interface BandRegistrar {
    Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties);

    Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties);

    /** The plain block item of {@code block} (same id). */
    Item blockItem(Block block);
}
