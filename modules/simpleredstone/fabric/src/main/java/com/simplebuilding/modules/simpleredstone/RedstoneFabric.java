package com.simplebuilding.modules.simpleredstone;

import com.simpleredstone.RedstoneRegistry;
import com.simpleredstone.SimpleRedstone;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class RedstoneFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleRedstone.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleredstone config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleRedstone.MOD_ID).json(com.simpleredstone.config.RedstoneConfig::toJson, com.simpleredstone.config.RedstoneConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simpleredstone.config.RedstoneConfig.FILE).register();
        RedstoneRegistry.registerBlocks();
        RedstoneRegistry.registerItems();
        RedstoneRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleRedstone.MOD_ID)) return;
            var stacks = RedstoneRegistry.partStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
            if (stacks.isEmpty()) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.REPEATER))) out.insertAfter(Items.REPEATER, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleRedstone.on()) return;
            var book = java.util.List.of(com.simpleredstone.guide.RedstoneGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleredstone.guide.RedstoneGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
