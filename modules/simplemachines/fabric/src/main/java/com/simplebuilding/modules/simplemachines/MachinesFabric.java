package com.simplebuilding.modules.simplemachines;

import com.simplemachines.MachinesRegistry;
import com.simplemachines.SimpleMachines;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class MachinesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMachines.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simplemachines config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleMachines.MOD_ID).json(com.simplemachines.config.MachinesConfig::toJson, com.simplemachines.config.MachinesConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simplemachines.config.MachinesConfig.FILE).register();
        MachinesRegistry.registerBlocks();
        MachinesRegistry.registerItems();
        MachinesRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleMachines.MOD_ID)) return;
            var stacks = MachinesRegistry.partStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
            if (stacks.isEmpty()) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.REPEATER))) out.insertAfter(Items.REPEATER, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleMachines.on()) return;
            var book = java.util.List.of(com.simplemachines.guide.MachinesGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplemachines.guide.MachinesGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
