package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration as Fabric/NeoForge. Ancient City loot comes in through ReloadableServerRegistriesMixin. */
@Mod(SimpleBlocks.MOD_ID)
public final class BlocksForge {
    public BlocksForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpleblocks.forge.ModuleForgeTests.register(context.getModBusGroup());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) BlocksRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) BlocksRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) BlocksRegistry.registerTab();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBlocks.on()) {
                event.getEntries().put(com.simpleblocks.guide.BlocksGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            boolean natural = event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS);
            if (!natural && !event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) return;
            if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBlocks.MOD_ID)) return;
            var entries = event.getEntries();
            var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                if (placement.natural() != natural) continue;
                var before = new ItemStack(placement.before());
                boolean anchored = entries.contains(before);
                for (ItemStack stack : placement.stacks()) {
                    if (entries.contains(stack)) continue;
                    if (anchored) entries.putBefore(before, stack, visibility);
                    else entries.put(stack, visibility);
                }
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
