package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

@Mod("simplemobs")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) { com.simplebuilding.modules.simplemobs.forge.ModuleForgeTests.register(context.getModBusGroup());
        MobsConfig.load(FMLPaths.CONFIGDIR.get());
        var bus = context.getModBusGroup();
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> {
                r.register(MobsRegistry.DECEIVER_KEY, MobsRegistry.DECEIVER);
                r.register(MobsRegistry.HOWLER_KEY, MobsRegistry.HOLLOW_HOWLER);
                r.register(MobsRegistry.RITUAL_KEY, MobsRegistry.HOWLER_RITUAL);
            });
            event.register(Registries.ITEM, r -> {
                r.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                r.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
                r.register(MobsRegistry.ECHO_CORE_KEY, MobsRegistry.ECHO_CORE);
                r.register(MobsRegistry.HOWLER_EGG_KEY, MobsRegistry.HOWLER_EGG);
            });
            event.register(Registries.MOB_EFFECT, r -> r.register(MobsRegistry.SILENCE_KEY, MobsRegistry.SILENCE));
            event.register(Registries.POTION, r -> {
                r.register(MobsRegistry.SILENCE_POTION_KEY, MobsRegistry.silencePotion());
                r.register(MobsRegistry.LONG_SILENCE_POTION_KEY, MobsRegistry.longSilencePotion());
            });
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes().build());
            event.put(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes().build());
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                e.accept(MobsRegistry.EGG);
                putAfter(e, new ItemStack(Items.HOGLIN_SPAWN_EGG), new ItemStack(MobsRegistry.HOWLER_EGG));
            }
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                e.accept(MobsRegistry.CLOTH);
                putAfter(e, new ItemStack(Items.ECHO_SHARD), new ItemStack(MobsRegistry.ECHO_CORE));
            }
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> MobsServer.tick(e.server()));
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> EndNetherMobs.entityTypes(r::register));
            event.register(Registries.ITEM, r -> EndNetherMobs.items(r::register));
            event.register(Registries.MOB_EFFECT, r -> EndNetherMobs.effects(r::register));
            event.register(Registries.POTION, r -> EndNetherMobs.potions(r::register));
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(EndNetherMobs.SHELLKER, ShellkerEntity.createAttributes().build());
            event.put(EndNetherMobs.SOULFIRE_BLAZE, net.minecraft.world.entity.monster.Blaze.createAttributes().build());
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                putAfter(e, new ItemStack(Items.SHULKER_SPAWN_EGG), new ItemStack(EndNetherMobs.SHELLKER_EGG));
                putAfter(e, new ItemStack(Items.BLAZE_SPAWN_EGG), new ItemStack(EndNetherMobs.SOULFIRE_EGG));
            }
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                putAfter(e, new ItemStack(Items.SHULKER_SHELL), new ItemStack(EndNetherMobs.SHELL));
                putAfter(e, new ItemStack(Items.BLAZE_ROD), new ItemStack(EndNetherMobs.ROD));
            }
            if (e.getTabKey().equals(CreativeModeTabs.COMBAT)) putAfter(e, new ItemStack(Items.TURTLE_HELMET), new ItemStack(EndNetherMobs.HELMET));
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> EndNetherServer.tick(e.server()));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) ForgeClientExample.init();
    }

    private static void putAfter(BuildCreativeModeTabContentsEvent e, ItemStack anchor, ItemStack stack) {
        if (e.getEntries().contains(anchor)) e.getEntries().putAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else e.accept(stack);
    }
}
