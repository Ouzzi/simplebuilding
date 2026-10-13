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
                r.register(DisguiseRegistry.MARKER_KEY, DisguiseRegistry.MARKER);
            });
            event.register(Registries.ITEM, r -> {
                r.register(MobsRegistry.CLOTH_KEY, MobsRegistry.CLOTH);
                r.register(MobsRegistry.EGG_KEY, MobsRegistry.EGG);
                r.register(MobsRegistry.ECHO_CORE_KEY, MobsRegistry.ECHO_CORE);
                r.register(MobsRegistry.HOWLER_EGG_KEY, MobsRegistry.HOWLER_EGG);
            });
            event.register(Registries.ITEM, r -> DisguiseRegistry.items(r::register));
            event.register(Registries.POTION, r -> DisguiseRegistry.potions(r::register));
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
            if (!MobsRegistry.on()) return;
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                e.accept(MobsRegistry.EGG);
                putAfter(e, new ItemStack(Items.HOGLIN_SPAWN_EGG), new ItemStack(MobsRegistry.HOWLER_EGG));
            }
            if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                e.accept(DisguiseRegistry.CLOAK);
                e.accept(DisguiseRegistry.MIRROR);
            }
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                e.accept(MobsRegistry.CLOTH);
                e.accept(DisguiseRegistry.SHARD);
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
                putAfter(e, new ItemStack(Items.BLAZE_POWDER), new ItemStack(EndNetherMobs.POWDER));
            }
            if (e.getTabKey().equals(CreativeModeTabs.COMBAT)) {
                putAfter(e, new ItemStack(Items.TURTLE_HELMET), new ItemStack(EndNetherMobs.HELMET));
                putAfter(e, new ItemStack(Items.FIRE_CHARGE), new ItemStack(EndNetherMobs.SOUL_FIRE_CHARGE));
            }
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> EndNetherServer.tick(e.server()));
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> FearMobs.entityTypes(r::register));
            event.register(Registries.ITEM, r -> FearMobs.items(r::register));
            event.register(Registries.MOB_EFFECT, r -> {
                FearMobs.simplebuildingPresent = net.minecraftforge.fml.ModList.isLoaded("simplebuilding");
                FearMobs.effects(r::register);
            });
            event.register(Registries.POTION, r -> FearMobs.potions(r::register));
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> {
            event.put(FearMobs.DREAD, DreadEntity.createAttributes().build());
            event.put(FearMobs.WATCHER, HollowWatcherEntity.createAttributes().build());
        });
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
                putAfter(e, new ItemStack(Items.WITHER_SKELETON_SPAWN_EGG), new ItemStack(FearMobs.DREAD_EGG));
                putAfter(e, new ItemStack(Items.ENDERMAN_SPAWN_EGG), new ItemStack(FearMobs.WATCHER_EGG));
            }
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
                putAfter(e, new ItemStack(Items.ENDER_EYE), new ItemStack(FearMobs.EYE));
                putAfter(e, new ItemStack(Items.ENDER_EYE), new ItemStack(FearMobs.LINK));
            }
            if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(FearMobs.AMULET);
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> FearServer.tick(e.server()));
        RegisterEvent.getBus(bus).addListener(event -> {
            event.register(Registries.ENTITY_TYPE, r -> WitherMobs.entityTypes(r::register));
            event.register(Registries.ITEM, r -> WitherMobs.items(r::register));
            if (event.getRegistryKey().equals(Registries.ITEM)) com.simplebuilding.modules.simplemobs.guide.MobsGuide.register();
        });
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(WitherMobs.BLIGHTED_WITHER, BlightedWither.createAttributes().build()));
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(WitherMobs.ABYSSAL_WITHER, BlightedWither.createAttributes(WitherLogic.ENDERITE).build()));
        BuildCreativeModeTabContentsEvent.BUS.addListener(e -> {
            if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) putAfter(e, new ItemStack(Items.NETHER_STAR), new ItemStack(WitherMobs.HEART));
            if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) putAfter(e, new ItemStack(Items.BRUSH), new ItemStack(WitherMobs.BRUSH));
            if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(com.simplebuilding.modules.simplemobs.guide.MobsGuide.stack());
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplebuilding.modules.simplemobs.guide.MobsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == Dist.CLIENT) ForgeClientExample.init();
    }

    private static void putAfter(BuildCreativeModeTabContentsEvent e, ItemStack anchor, ItemStack stack) {
        if (e.getEntries().contains(anchor)) e.getEntries().putAfter(anchor, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else e.accept(stack);
    }
}
