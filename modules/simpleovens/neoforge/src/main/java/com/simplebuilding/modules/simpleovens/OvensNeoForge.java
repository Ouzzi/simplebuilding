package com.simplebuilding.modules.simpleovens;

import com.simpleovens.OvensRegistry;
import com.simpleovens.SimpleOvens;
import com.simpleovens.block.TierBlastFurnaceBlockEntity;
import com.simpleovens.block.TierFurnaceBlockEntity;
import com.simpleovens.block.TierSmokerBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleOvens.MOD_ID)
public final class OvensNeoForge {
    public OvensNeoForge(IEventBus bus, Dist dist) {
        SimpleOvens.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) OvensRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) OvensRegistry.registerItems();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                OvensRegistry.registerBlockEntities(new BlockEntityType<>(TierFurnaceBlockEntity::new, Set.of(OvensRegistry.furnaces()), false),
                        new BlockEntityType<>(TierSmokerBlockEntity::new, Set.of(OvensRegistry.smokers()), false),
                        new BlockEntityType<>(TierBlastFurnaceBlockEntity::new, Set.of(OvensRegistry.blastFurnaces()), false));
            }
            if (key.equals(Registries.ENTITY_TYPE)) OvensRegistry.registerEntityTypes();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) OvensRegistry.registerTab();
        });
        // Pipes and hoppers of other mods see the furnace slots like a Vanilla furnace's (sided).
        bus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(Capabilities.Item.BLOCK, OvensRegistry.FURNACE_BE, WorldlyContainerWrapper::new);
            event.registerBlockEntity(Capabilities.Item.BLOCK, OvensRegistry.SMOKER_BE, WorldlyContainerWrapper::new);
            event.registerBlockEntity(Capabilities.Item.BLOCK, OvensRegistry.BLAST_FURNACE_BE, WorldlyContainerWrapper::new);
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleOvens.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                // Behind their Vanilla machine, where SimpleBuilding had them (the search tab keeps this order).
                insertAfter(event, net.minecraft.world.item.Items.FURNACE, OvensRegistry.familyStacks(OvensRegistry.FURNACES));
                insertAfter(event, net.minecraft.world.item.Items.SMOKER, OvensRegistry.familyStacks(OvensRegistry.SMOKERS));
                insertAfter(event, net.minecraft.world.item.Items.BLAST_FURNACE, OvensRegistry.familyStacks(OvensRegistry.BLAST_FURNACES));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                if (vanillaTabs) insertAfter(event, net.minecraft.world.item.Items.FURNACE_MINECART, OvensRegistry.cartStacks());
                if (SimpleOvens.on()) event.accept(com.simpleovens.guide.OvensGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleovens.guide.OvensGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) OvensNeoClient.init(bus);
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.Item anchorItem, java.util.List<net.minecraft.world.item.ItemStack> stacks) {
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new net.minecraft.world.item.ItemStack(anchorItem);
        boolean anchored = event.getParentEntries().contains(anchor) && event.getSearchEntries().contains(anchor);
        var previous = anchor;
        for (var stack : stacks) {
            if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) continue;
            if (anchored) {
                event.insertAfter(previous, stack, visibility);
                previous = stack;
            } else {
                event.accept(stack, visibility);
            }
        }
    }
}
