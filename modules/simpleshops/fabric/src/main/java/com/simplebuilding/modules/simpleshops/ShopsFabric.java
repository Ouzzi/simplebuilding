package com.simplebuilding.modules.simpleshops;

import com.simpleshops.ShopsRegistry;
import com.simpleshops.SimpleShops;
import com.simpleshops.block.DisplayCaseBlockEntity;
import com.simpleshops.block.SalesBlockEntity;
import com.simpleshops.block.SalesStorageBlockEntity;
import com.simpleshops.logic.ShopGuard;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class ShopsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleShops.init(FabricLoader.getInstance().getConfigDir());
        ShopsRegistry.registerBlocks();
        ShopsRegistry.registerItems();
        ShopsRegistry.registerBlockEntities(new BlockEntityType<>(SalesBlockEntity::new, ShopsRegistry.salesBlocks()),
                new BlockEntityType<>(DisplayCaseBlockEntity::new, ShopsRegistry.caseBlocks()),
                new BlockEntityType<>(SalesStorageBlockEntity::new, ShopsRegistry.storageBlocks()));
        ShopsRegistry.registerMenus();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
                com.simpleshops.logic.ServerShopCommands.register(dispatcher, registry));
        ServerLifecycleEvents.SERVER_STARTED.register(ShopGuard::register);
        ServerLifecycleEvents.SERVER_STOPPED.register(ShopGuard::unregister);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleShops.MOD_ID)) return;
            var stacks = ShopsRegistry.tabStacks();
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.GLOW_ITEM_FRAME))) {
                out.insertAfter(Items.GLOW_ITEM_FRAME, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleShops.on()) return;
            var book = java.util.List.of(com.simpleshops.guide.ShopsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleshops.guide.ShopsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
