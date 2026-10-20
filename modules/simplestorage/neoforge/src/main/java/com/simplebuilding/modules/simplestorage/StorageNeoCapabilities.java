package com.simplebuilding.modules.simplestorage;

import com.simplestorage.registry.StorageBlockEntities;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * NeoForge attaches the item capability only to Vanilla's block entity types; pipes and other mods see the chests and
 * boxes through these. A double chest is one store of both halves (first the right one, like Vanilla); the wrappers ask
 * {@code getMaxStackSize(stack)}, so the x2/x4 slots of Netherite and Enderite show. The tier shulker boxes go sided
 * through their {@code WorldlyContainer} (no shulker box inside), like Vanilla's box.
 */
final class StorageNeoCapabilities {
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, StorageBlockEntities.TIERED_CHEST, (chest, side) -> chestHandler(chest));
        event.registerBlockEntity(Capabilities.Item.BLOCK, StorageBlockEntities.TRAPPED_COPPER_CHEST, (chest, side) -> chestHandler(chest));
        event.registerBlockEntity(Capabilities.Item.BLOCK, StorageBlockEntities.TIERED_SHULKER_BOX, WorldlyContainerWrapper::new);
    }

    static ResourceHandler<ItemResource> chestHandler(ChestBlockEntity chest) {
        var level = chest.getLevel();
        BlockState state = chest.getBlockState();
        if (level == null || !(state.getBlock() instanceof ChestBlock block)) return VanillaContainerWrapper.of(chest);
        return block.combine(state, level, chest.getBlockPos(), true).apply(new DoubleBlockCombiner.Combiner<ChestBlockEntity, ResourceHandler<ItemResource>>() {
            @Override
            public ResourceHandler<ItemResource> acceptDouble(ChestBlockEntity first, ChestBlockEntity second) {
                return new CombinedResourceHandler<>(VanillaContainerWrapper.of(first), VanillaContainerWrapper.of(second));
            }

            @Override
            public ResourceHandler<ItemResource> acceptSingle(ChestBlockEntity single) {
                return VanillaContainerWrapper.of(single);
            }

            @Override
            public ResourceHandler<ItemResource> acceptNone() {
                return VanillaContainerWrapper.of(chest);
            }
        });
    }

    private StorageNeoCapabilities() {}
}
