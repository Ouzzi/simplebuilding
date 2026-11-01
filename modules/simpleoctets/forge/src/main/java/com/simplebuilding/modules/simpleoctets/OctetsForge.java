package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.SimpleOctets;
import com.simpleoctets.block.MixedOctetBlockEntity;
import com.simpleoctets.registry.OctetRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleOctets.MOD_ID)
public final class OctetsForge {
    public OctetsForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simpleoctets.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleOctets.init(FMLPaths.CONFIGDIR.get(), net.minecraftforge.fml.ModList::isLoaded);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) OctetRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) {
                OctetRegistry.registerItems();
                SimpleOctets.registerCells();
            }
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                OctetRegistry.registerBlockEntity(new BlockEntityType<>(MixedOctetBlockEntity::new, Set.of(OctetRegistry.MIXED_OCTET)));
            }
            if (key.equals(Registries.CREATIVE_MODE_TAB)) OctetRegistry.registerTab();
        });
        // Once every mod registered its blocks: cells of other mods' blocks for the carving tools.
        net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(event -> SimpleOctets.registerCells());
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleOctets.on()) {
                event.getEntries().put(com.simpleoctets.guide.OctetsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpleoctets.guide.OctetsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) OctetsForgeClient.init();
    }
}
