package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.beacon.StrongBeaconBlockEntity;
import com.simplebeacons.conduit.StrongConduitBlockEntity;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class BeaconsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleBeacons.init(FabricLoader.getInstance().getConfigDir());
        BeaconsRegistry.registerBlocks();
        BeaconsRegistry.registerItems();
        BeaconsRegistry.registerBlockEntities(new BlockEntityType<>(StrongBeaconBlockEntity::new, Set.of(BeaconsRegistry.STRONG_BEACON)),
                new BlockEntityType<>(StrongConduitBlockEntity::new, Set.of(BeaconsRegistry.STRONG_CONDUIT)));
        BeaconsRegistry.registerMenus();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleBeacons.MOD_ID)) return;
            var stacks = BeaconsRegistry.tabStacks();
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.CONDUIT))) {
                out.insertAfter(Items.CONDUIT, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!SimpleBeacons.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleBeacons.MOD_ID)) return;
            var template = java.util.List.of(new net.minecraft.world.item.ItemStack(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE));
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))) {
                out.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, template, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                template.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleBeacons.on()) return;
            var book = java.util.List.of(com.simplebeacons.guide.BeaconsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebeacons.guide.BeaconsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
