package com.simplebuilding.modules.simplestats;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("simplestats")
public final class StatsForge {
    public StatsForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplestats.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleStats.configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleStats.load();
        net.minecraftforge.registries.RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE)) StatsComponents.register();
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simplestats.guide.StatsGuide.register();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))
                e.getEntries().put(com.simplebuilding.modules.simplestats.guide.StatsGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simplestats.guide.StatsGuide.installQuests(SimpleStats.configDir);
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> StatsCommands.register(e.getDispatcher()));
    }
}
