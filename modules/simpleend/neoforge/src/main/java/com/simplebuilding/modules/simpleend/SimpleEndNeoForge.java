package com.simplebuilding.modules.simpleend;

import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleEnd.ID)
public final class SimpleEndNeoForge {
    public SimpleEndNeoForge(IEventBus bus, Dist dist) {
        SimpleEnd.init(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener(EventPriority.HIGH, (RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) EndRegistry.blocks();
            if (key.equals(Registries.ITEM)) EndRegistry.items();
            if (key.equals(Registries.MOB_EFFECT)) EndRegistry.effects();
            if (key.equals(Registries.ENTITY_TYPE)) EndRegistry.entities();
            if (key.equals(Registries.FEATURE_TYPE)) EndRegistry.featureTypes();
            if (key.equals(Registries.SOUND_EVENT)) EndRegistry.sounds();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) EndRegistry.tab();
        });
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) -> event.put(EndRegistry.NIBBLEMITE, com.simplebuilding.modules.simpleend.entity.Nibblemite.createAttributes().build()));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            var stacks = EndRegistry.vanillaTabStacks().get(event.getTabKey());
            if (stacks == null || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleEnd.ID)) return;
            stacks.forEach(s -> event.accept(s, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(EndRegistry::common));
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplebuilding.modules.simpleend.guide.EndGuide.installQuests(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> { SimpleEnd.applySubSwitches(); com.simplebuilding.modules.simpleend.world.EndBiomes.serverStarting(event.getServer()); });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> com.simplebuilding.modules.simpleend.world.EndBiomes.serverStopped());
        if (dist == Dist.CLIENT) SimpleEndNeoClient.init(bus);
    }
}
