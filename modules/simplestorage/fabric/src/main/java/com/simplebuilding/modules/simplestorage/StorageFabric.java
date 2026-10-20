package com.simplebuilding.modules.simplestorage;

import com.simplestorage.SimpleStorage;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class StorageFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleStorage.init();
        FabricLoader loader = FabricLoader.getInstance();
        com.simplestorage.registry.StorageComponents.register();
        StorageBlocks.register(loader.isModLoaded(com.simplestorage.crate.CrateWood.SIMPLE_END));
        StorageItems.register();
        StorageBlockEntities.register(new StorageBlockEntities.Types() {
            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> create(
                    BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<net.minecraft.world.level.block.Block> blocks) {
                return new BlockEntityType<>(factory, blocks);
            }
        });
        StorageMenus.register();
        StorageRegistry.registerTab();
        StorageBlocks.registerFlammability();
        com.simplestorage.registry.StorageRegistry.registerBehaviours();
        boolean sb = loader.isModLoaded("simplebuilding");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!StorageRegistry.fillsVanillaTabs(sb, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleStorage.MOD_ID))) return;
            // On its own: the table behind the crafting table, the crates behind the barrel.
            insertAfter(out, Items.CRAFTING_TABLE, List.of(new ItemStack(StorageItems.STORAGE_CRAFTING_TABLE)));
            insertAfter(out, Items.BARREL, StorageRegistry.crateStacks());
            insertAfter(out, Items.TRAPPED_CHEST, StorageRegistry.chestStacks());
            insertAfter(out, Items.SHULKER_BOX, StorageRegistry.shulkerBoxStacks());
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleStorage.on()) return;
            var book = List.of(com.simplestorage.guide.StorageGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(loader.getConfigDir());
    }

    private static void insertAfter(net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput out, net.minecraft.world.item.Item anchor, List<ItemStack> stacks) {
        // Never twice (another mod may have placed them already).
        stacks = stacks.stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
        if (stacks.isEmpty()) return;
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
