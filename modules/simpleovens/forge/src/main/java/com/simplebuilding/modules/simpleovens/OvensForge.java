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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleOvens.MOD_ID)
public final class OvensForge {
    public OvensForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpleovens.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleOvens.init(FMLPaths.CONFIGDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) OvensRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) OvensRegistry.registerItems();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                OvensRegistry.registerBlockEntities(new BlockEntityType<>(TierFurnaceBlockEntity::new, Set.of(OvensRegistry.furnaces())),
                        new BlockEntityType<>(TierSmokerBlockEntity::new, Set.of(OvensRegistry.smokers())),
                        new BlockEntityType<>(TierBlastFurnaceBlockEntity::new, Set.of(OvensRegistry.blastFurnaces())));
            }
            if (key.equals(Registries.ENTITY_TYPE)) OvensRegistry.registerEntityTypes();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) OvensRegistry.registerTab();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            boolean vanillaTabs = com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleOvens.MOD_ID);
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                // Behind their Vanilla machine, where SimpleBuilding had them (the search tab keeps this order).
                insertAfter(event, net.minecraft.world.item.Items.FURNACE, OvensRegistry.familyStacks(OvensRegistry.FURNACES));
                insertAfter(event, net.minecraft.world.item.Items.SMOKER, OvensRegistry.familyStacks(OvensRegistry.SMOKERS));
                insertAfter(event, net.minecraft.world.item.Items.BLAST_FURNACE, OvensRegistry.familyStacks(OvensRegistry.BLAST_FURNACES));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                if (vanillaTabs) insertAfter(event, net.minecraft.world.item.Items.FURNACE_MINECART, OvensRegistry.cartStacks());
                if (SimpleOvens.on()) event.getEntries().put(com.simpleovens.guide.OvensGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpleovens.guide.OvensGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) OvensForgeClient.init();
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event,
                                    net.minecraft.world.item.Item anchorItem, java.util.List<net.minecraft.world.item.ItemStack> stacks) {
        var entries = event.getEntries();
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new net.minecraft.world.item.ItemStack(anchorItem);
        boolean anchored = entries.contains(anchor);
        var previous = anchor;
        for (var stack : stacks) {
            if (entries.contains(stack)) continue;
            if (anchored) {
                entries.putAfter(previous, stack, visibility);
                previous = stack;
            } else {
                entries.put(stack, visibility);
            }
        }
    }
}
