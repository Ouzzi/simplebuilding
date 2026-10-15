package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.logic.WandererSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleWanderers.MOD_ID)
public final class WanderersNeoForge {
    public WanderersNeoForge(IEventBus bus, Dist dist) {
        SimpleWanderers.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> WandererRegistry.entityTypes(h::register));
            event.register(Registries.ITEM, h -> WandererRegistry.items(h::register));
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(WandererRegistry.WANDERER, WandererRegistry.wandererAttributes().build());
            event.put(WandererRegistry.HORSE, WandererRegistry.horseAttributes().build());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> WandererSpawner.tick(event.getServer()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!SimpleWanderers.on()) return;
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                WandererRegistry.eggStacks().forEach(s -> event.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                event.accept(com.simplewanderers.guide.WanderersGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplewanderers.guide.WanderersGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) WanderersNeoClient.init(bus);
    }
}
