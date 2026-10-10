package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("simplemobs")
public final class NeoForgeExample {
    public NeoForgeExample(IEventBus bus, Dist dist) {
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> h.register(MobsRegistry.DECEIVER_KEY, MobsRegistry.DECEIVER));
            event.register(Registries.ITEM, h -> {
                h.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                h.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
            });
        });
        bus.addListener((EntityAttributeCreationEvent event) ->
                event.put(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes().build()));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) event.accept(MobsRegistry.EGG);
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) event.accept(MobsRegistry.CLOTH);
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> MobsServer.tick(event.getServer()));
        if (dist == Dist.CLIENT) NeoForgeClientExample.init(bus);
    }
}
