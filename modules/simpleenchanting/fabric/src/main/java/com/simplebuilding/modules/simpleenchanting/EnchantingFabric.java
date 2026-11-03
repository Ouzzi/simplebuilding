package com.simplebuilding.modules.simpleenchanting;

import com.simpleenchanting.EnchantingRegistry;
import com.simpleenchanting.SimpleEnchanting;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class EnchantingFabric implements ModInitializer {
    /** SimpleBuilding runs this initializer before its own registration (its enchanter fields point at these blocks); the loader's second call returns. */
    private static boolean initialized;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        SimpleEnchanting.init(FabricLoader.getInstance().getConfigDir(), FabricLoader.getInstance().isModLoaded("simplebuilding"));
        // CMD24: /simpleenchanting config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleEnchanting.MOD_ID).json(com.simpleenchanting.config.EnchantingConfig::toJson,
                com.simpleenchanting.config.EnchantingConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simpleenchanting.config.EnchantingConfig.FILE).register();
        EnchantingRegistry.registerBlocks();
        EnchantingRegistry.registerItems();
        EnchantingRegistry.registerBlockEntity();
        EnchantingRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleEnchanting.MOD_ID)) return;
            var stacks = EnchantingRegistry.tableStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
            if (stacks.isEmpty()) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.ENCHANTING_TABLE))) out.insertAfter(Items.ENCHANTING_TABLE, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleEnchanting.on()) return;
            var book = java.util.List.of(com.simpleenchanting.guide.EnchantingGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleenchanting.guide.EnchantingGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
