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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleOctets.MOD_ID)
public final class OctetsNeoForge {
    public OctetsNeoForge(IEventBus bus, Dist dist) {
        SimpleOctets.init(FMLPaths.CONFIGDIR.get(), id -> net.neoforged.fml.ModList.get().isLoaded(id));
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) OctetRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) {
                OctetRegistry.registerItems();
                SimpleOctets.registerCells();
            }
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                OctetRegistry.registerBlockEntity(new BlockEntityType<>(MixedOctetBlockEntity::new, Set.of(OctetRegistry.MIXED_OCTET), false));
            }
            if (key.equals(Registries.CREATIVE_MODE_TAB)) OctetRegistry.registerTab();
        });
        // Once every mod registered its blocks: cells of other mods' blocks for the carving tools.
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> SimpleOctets.registerCells());
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleOctets.on()) {
                event.accept(com.simpleoctets.guide.OctetsGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleoctets.guide.OctetsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) OctetsNeoClient.init(bus);
    }
}
