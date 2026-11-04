package com.simplebuilding.modules.simplearrows;

import com.simplearrows.ArrowsRegistry;
import com.simplearrows.SimpleArrows;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class ArrowsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleArrows.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simplearrows config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleArrows.MOD_ID).json(com.simplearrows.config.ArrowsConfig::toJson, com.simplearrows.config.ArrowsConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simplearrows.config.ArrowsConfig.FILE).register();
        ArrowsRegistry.registerAll();
        ArrowsRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleArrows.MOD_ID)) return;
            var stacks = ArrowsRegistry.vanillaTabStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
            if (stacks.isEmpty()) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.SPECTRAL_ARROW))) out.insertAfter(Items.SPECTRAL_ARROW, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleArrows.on()) return;
            var book = java.util.List.of(com.simplearrows.guide.ArrowsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        // JEI24: the Fletching Table's parts (its recipe type only feeds the table's book) as one entry in "Special Ways".
        com.simplelib.api.RecipeViewer.special(SimpleArrows.MOD_ID, com.simplearrows.fletching.Fletchings::recipeViewerEntries);
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplearrows.guide.ArrowsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
