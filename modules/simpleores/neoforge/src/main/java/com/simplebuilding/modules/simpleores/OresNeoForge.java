package com.simplebuilding.modules.simpleores;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. The silver veins come from the biome
 * modifier files in data/simpleores/neoforge/biome_modifier.
 */
@Mod(SimpleOres.MOD_ID)
public final class OresNeoForge {
    public OresNeoForge(IEventBus bus) {
        SimpleOres.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.BLOCK, h -> OresRegistry.blocks(h::register));
            event.register(Registries.ITEM, h -> OresRegistry.items(h::register));
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!SimpleOres.on()) return;
            var vis = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            var tab = event.getTabKey();
            if (tab.equals(CreativeModeTabs.NATURAL_BLOCKS)) OresRegistry.stacks(OresRegistry.NATURAL).forEach(s -> event.accept(s, vis));
            if (tab.equals(CreativeModeTabs.BUILDING_BLOCKS)) OresRegistry.stacks(OresRegistry.BUILDING).forEach(s -> event.accept(s, vis));
            if (tab.equals(CreativeModeTabs.INGREDIENTS)) OresRegistry.stacks(OresRegistry.INGREDIENTS).forEach(s -> event.accept(s, vis));
            if (tab.equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                OresRegistry.stacks(OresRegistry.TOOLS).forEach(s -> event.accept(s, vis));
                event.accept(com.simpleores.guide.OresGuide.stack(), vis);
            }
            if (tab.equals(CreativeModeTabs.COMBAT)) OresRegistry.stacks(OresRegistry.COMBAT).forEach(s -> event.accept(s, vis));
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleores.guide.OresGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
