package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("simplemobs")
public final class NeoForgeExample {
    public NeoForgeExample(IEventBus bus, Dist dist) {
        MobsConfig.load(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            event.register(Registries.ENTITY_TYPE, h -> {
                h.register(MobsRegistry.DECEIVER_KEY, MobsRegistry.DECEIVER);
                h.register(MobsRegistry.HOWLER_KEY, MobsRegistry.HOLLOW_HOWLER);
                h.register(MobsRegistry.RITUAL_KEY, MobsRegistry.HOWLER_RITUAL);
            });
            event.register(Registries.ITEM, h -> {
                h.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                h.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
                h.register(MobsRegistry.ECHO_CORE_KEY, MobsRegistry.ECHO_CORE);
                h.register(MobsRegistry.HOWLER_EGG_KEY, MobsRegistry.HOWLER_EGG);
            });
            event.register(Registries.MOB_EFFECT, h -> h.register(MobsRegistry.SILENCE_KEY, MobsRegistry.SILENCE));
            event.register(Registries.POTION, h -> {
                h.register(MobsRegistry.SILENCE_POTION_KEY, MobsRegistry.silencePotion());
                h.register(MobsRegistry.LONG_SILENCE_POTION_KEY, MobsRegistry.longSilencePotion());
            });
        });
        bus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes().build());
            event.put(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes().build());
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                event.accept(MobsRegistry.EGG);
                insertAfter(event, new ItemStack(Items.HOGLIN_SPAWN_EGG), new ItemStack(MobsRegistry.HOWLER_EGG));
            }
            if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                event.accept(MobsRegistry.CLOTH);
                insertAfter(event, new ItemStack(Items.ECHO_SHARD), new ItemStack(MobsRegistry.ECHO_CORE));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> MobsServer.tick(event.getServer()));
        if (dist == Dist.CLIENT) NeoForgeClientExample.init(bus);
    }

    private static void insertAfter(BuildCreativeModeTabContentsEvent event, ItemStack anchor, ItemStack stack) {
        if (event.getParentEntries().contains(anchor)) event.insertAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else event.accept(stack);
    }
}
