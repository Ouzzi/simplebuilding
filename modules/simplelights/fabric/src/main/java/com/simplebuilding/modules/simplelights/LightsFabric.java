package com.simplebuilding.modules.simplelights;

import com.simplelights.LightsRegistry;
import com.simplelights.SimpleLights;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class LightsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLights.init();
        LightsRegistry.registerBlocks();
        LightsRegistry.registerItems();
        LightsRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            // With SimpleBuilding its search tab chain puts them behind the construction light (by id); alone behind the redstone lamp.
            if (FabricLoader.getInstance().isModLoaded(SimpleLights.OLD_NAMESPACE)) return;
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleLights.MOD_ID)) return;
            var stacks = LightsRegistry.paneStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
            if (stacks.isEmpty()) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.REDSTONE_LAMP))) out.insertAfter(Items.REDSTONE_LAMP, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleLights.on()) return;
            var book = java.util.List.of(com.simplelights.guide.LightsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplelights.guide.LightsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
