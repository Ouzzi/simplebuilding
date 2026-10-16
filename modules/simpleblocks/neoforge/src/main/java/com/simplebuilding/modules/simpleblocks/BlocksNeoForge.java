package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksLoot;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleBlocks.MOD_ID)
public final class BlocksNeoForge {
    public BlocksNeoForge(IEventBus bus) {
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) BlocksRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) BlocksRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) BlocksRegistry.registerTab();
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.LootTableLoadEvent e) -> BlocksLoot.apply(
                net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, e.getName()), pool -> e.getTable().addPool(pool.build())));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBlocks.on()) {
                event.accept(com.simpleblocks.guide.BlocksGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            boolean natural = event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS);
            if (!natural && !event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) return;
            if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBlocks.MOD_ID)) return;
            var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                if (placement.natural() != natural) continue;
                var before = new ItemStack(placement.before());
                boolean anchored = event.getParentEntries().contains(before) && event.getSearchEntries().contains(before);
                for (ItemStack stack : placement.stacks()) {
                    if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) continue;
                    if (anchored) event.insertBefore(before, stack, visibility);
                    else event.accept(stack, visibility);
                }
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
