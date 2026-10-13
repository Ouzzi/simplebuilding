package com.simplebuilding.modules.simpleovens;

import com.simpleovens.OvensRegistry;
import com.simpleovens.SimpleOvens;
import com.simpleovens.block.TierBlastFurnaceBlockEntity;
import com.simpleovens.block.TierFurnaceBlockEntity;
import com.simpleovens.block.TierSmokerBlockEntity;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class OvensFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleOvens.init(FabricLoader.getInstance().getConfigDir());
        OvensRegistry.registerBlocks();
        OvensRegistry.registerItems();
        OvensRegistry.registerBlockEntities(new BlockEntityType<>(TierFurnaceBlockEntity::new, Set.of(OvensRegistry.furnaces())),
                new BlockEntityType<>(TierSmokerBlockEntity::new, Set.of(OvensRegistry.smokers())),
                new BlockEntityType<>(TierBlastFurnaceBlockEntity::new, Set.of(OvensRegistry.blastFurnaces())));
        OvensRegistry.registerEntityTypes();
        OvensRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleOvens.MOD_ID)) return;
            // Behind their Vanilla machine, where SimpleBuilding had them (the search tab keeps this order).
            insertAfter(out, Items.FURNACE, OvensRegistry.familyStacks(OvensRegistry.FURNACES));
            insertAfter(out, Items.SMOKER, OvensRegistry.familyStacks(OvensRegistry.SMOKERS));
            insertAfter(out, Items.BLAST_FURNACE, OvensRegistry.familyStacks(OvensRegistry.BLAST_FURNACES));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleOvens.MOD_ID)) {
                insertAfter(out, Items.FURNACE_MINECART, OvensRegistry.cartStacks());
            }
            if (!SimpleOvens.on()) return;
            var book = java.util.List.of(com.simpleovens.guide.OvensGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleovens.guide.OvensGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }

    private static void insertAfter(net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput out, net.minecraft.world.item.Item anchor, java.util.List<ItemStack> stacks) {
        // Never twice (another mod may have placed them already).
        stacks = stacks.stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
        if (stacks.isEmpty()) return;
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
