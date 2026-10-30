package com.simplebuilding.modules.simpleresonance;

import com.simpleresonance.SimpleResonance;
import com.simpleresonance.guide.ResonanceGuide;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entry (port not requested yet): config, contract service registration and the guide book. The server events that feed
 * the service (ticks, kills, deaths) and {@code /resonance} are wired on Fabric only for now, so on NeoForge the service reports
 * no spawn levels; the pure rules and registrations are tested ({@link com.simpleresonance.test.ResonanceTests#PURE}).
 */
@Mod(SimpleResonance.MOD_ID)
public final class ResonanceNeoForge {
    public ResonanceNeoForge(IEventBus bus) {
        SimpleResonance.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(Registries.ITEM)) ResonanceGuide.register();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (SimpleResonance.on() && event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(ResonanceGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) ResonanceGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
