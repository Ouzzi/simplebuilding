package com.simplebuilding.modules.simpleslabs;

import com.simpleslabs.SimpleSlabs;
import com.simpleslabs.registry.SlabRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SlabsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleSlabs.init(FabricLoader.getInstance()::isModLoaded);
        SlabRegistry.registerBlocks();
        SlabRegistry.registerItems();
        SlabRegistry.registerFlammability();
        SlabRegistry.registerTab();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleSlabs.on()) return;
            var book = java.util.List.of(com.simpleslabs.guide.SlabsGuide.stack());
            // Before the writable book (like Simple Octets): a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.WRITABLE_BOOK))) {
                out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, book, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                book.forEach(s -> out.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleslabs.guide.SlabsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
