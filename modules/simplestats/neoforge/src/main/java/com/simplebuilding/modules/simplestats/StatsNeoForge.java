package com.simplebuilding.modules.simplestats;

import net.neoforged.fml.common.Mod;

@Mod("simplestats")
public final class StatsNeoForge {
    public StatsNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleStats.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleStats.load();
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE)) StatsComponents.register();
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) com.simplebuilding.modules.simplestats.guide.StatsGuide.register();
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))
                e.accept(com.simplebuilding.modules.simplestats.guide.StatsGuide.stack(), net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simplestats.guide.StatsGuide.installQuests(SimpleStats.configDir);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> StatsCommands.register(e.getDispatcher()));
    }
}
