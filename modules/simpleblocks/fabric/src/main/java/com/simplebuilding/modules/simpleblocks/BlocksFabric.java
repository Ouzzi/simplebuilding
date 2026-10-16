package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksLoot;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class BlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlocksRegistry.registerAll();
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> BlocksLoot.apply(key, table::withPool));
        for (boolean natural : new boolean[]{false, true}) {
            CreativeModeTabEvents.modifyOutputEvent(natural ? CreativeModeTabs.NATURAL_BLOCKS : CreativeModeTabs.BUILDING_BLOCKS).register(out -> {
                if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleBlocks.MOD_ID)) return;
                for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                    if (placement.natural() != natural) continue;
                    // Never twice (another mod may have placed them already); at the end when the Vanilla item is missing.
                    var stacks = placement.stacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
                    if (stacks.isEmpty()) continue;
                    if (out.getDisplayStacks().stream().anyMatch(s -> s.is(placement.before()))) out.insertBefore(placement.before(), stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                }
            });
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleBlocks.on()) return;
            var book = java.util.List.of(com.simpleblocks.guide.BlocksGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.WRITABLE_BOOK))) out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
