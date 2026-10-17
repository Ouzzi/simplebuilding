package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.SimpleOctets;
import com.simpleoctets.block.MixedOctetBlockEntity;
import com.simpleoctets.registry.OctetRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class OctetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        SimpleOctets.init(loader.getConfigDir(), loader::isModLoaded);
        OctetRegistry.registerBlocks();
        OctetRegistry.registerItems();
        OctetRegistry.registerBlockEntity(new BlockEntityType<>(MixedOctetBlockEntity::new, Set.of(OctetRegistry.MIXED_OCTET)));
        SimpleOctets.registerCells();
        OctetRegistry.registerTab();
        for (var tab : OctetRegistry.VANILLA_TABS) {
            CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> {
                if (!SimpleOctets.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleOctets.MOD_ID)) return;
                for (Map.Entry<Item, List<ItemStack>> entry : OctetRegistry.vanillaPlacements(tab).entrySet()) insertAfter(out, entry.getKey(), entry.getValue());
            });
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleOctets.on()) return;
            var book = List.of(com.simpleoctets.guide.OctetsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        // Once every mod registered its blocks: cells of other mods' blocks for the carving tools.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> SimpleOctets.registerCells());
        if (loader.isModLoaded("ftbquests")) com.simpleoctets.guide.OctetsGuide.installQuests(loader.getConfigDir());
    }

    private static void insertAfter(FabricCreativeModeTabOutput out, Item anchor, List<ItemStack> stacks) {
        // Never twice (another mod may have placed them already).
        stacks = stacks.stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
        if (stacks.isEmpty()) return;
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
