package com.simplebuilding.modules.simplemobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MobsFabric implements ModInitializer {
    @Override public void onInitialize() {
        WitherLoot.idMoves();
        MobsConfig.load(FabricLoader.getInstance().getConfigDir());
        MobsRegistry.registerAll();
        DisguiseRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(MobsServer::tick);
        EndNetherMobs.registerAll();
        FabricDefaultAttributeRegistry.register(EndNetherMobs.SHELLKER, ShellkerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(EndNetherMobs.SOULFIRE_BLAZE, net.minecraft.world.entity.monster.Blaze.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(EndNetherServer::tick);
        FearMobs.simplebuildingPresent = FabricLoader.getInstance().isModLoaded("simplebuilding");
        FearMobs.registerAll();
        FabricDefaultAttributeRegistry.register(FearMobs.DREAD, DreadEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FearMobs.WATCHER, HollowWatcherEntity.createAttributes());
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(FearNet.FovPayload.TYPE, FearNet.FovPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(FearNet.FovPayload.TYPE,
                (payload, context) -> context.server().execute(() -> FearNet.handle(payload, context.player())));
        ServerTickEvents.END_SERVER_TICK.register(FearServer::tick);
        com.simplelib.api.PlayerState.onLeave("simplemobs:fear", FearNet::forget);
        com.simplelib.api.PlayerState.onStop("simplemobs:fear", FearNet::forgetAll);
        WitherMobs.registerAll();
        com.simplebuilding.modules.simplemobs.guide.MobsGuide.register();
        MobsRegistry.registerTab();
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplebuilding.modules.simplemobs.guide.MobsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
        FabricDefaultAttributeRegistry.register(WitherMobs.BLIGHTED_WITHER, BlightedWither.createAttributes());
        FabricDefaultAttributeRegistry.register(WitherMobs.ABYSSAL_WITHER, BlightedWither.createAttributes(WitherLogic.ENDERITE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.NETHER_STAR, java.util.List.of(new ItemStack(WitherMobs.HEART), new ItemStack(WitherMobs.GEM)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> out.accept(com.simplebuilding.modules.simplemobs.guide.MobsGuide.stack()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.WITHER_SKELETON_SPAWN_EGG, java.util.List.of(new ItemStack(FearMobs.DREAD_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            out.insertAfter(Items.ENDERMAN_SPAWN_EGG, java.util.List.of(new ItemStack(FearMobs.WATCHER_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.ENDER_EYE, java.util.List.of(new ItemStack(FearMobs.LINK), new ItemStack(FearMobs.EYE)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> { if (!MobsConfig.vanillaTabs()) return; out.accept(FearMobs.AMULET); });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.SHULKER_SPAWN_EGG, java.util.List.of(new ItemStack(EndNetherMobs.SHELLKER_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            out.insertAfter(Items.BLAZE_SPAWN_EGG, java.util.List.of(new ItemStack(EndNetherMobs.SOULFIRE_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.SHULKER_SHELL, java.util.List.of(new ItemStack(EndNetherMobs.SHELL)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            out.insertAfter(Items.BLAZE_ROD, java.util.List.of(new ItemStack(EndNetherMobs.ROD)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            out.insertAfter(Items.BLAZE_POWDER, java.util.List.of(new ItemStack(EndNetherMobs.POWDER)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.insertAfter(Items.TURTLE_HELMET, java.util.List.of(new ItemStack(EndNetherMobs.HELMET)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            out.insertAfter(Items.FIRE_CHARGE, java.util.List.of(new ItemStack(EndNetherMobs.SOUL_FIRE_CHARGE)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            if (!MobsRegistry.on()) return;
            out.accept(MobsRegistry.EGG);
            out.insertAfter(Items.HOGLIN_SPAWN_EGG, java.util.List.of(new ItemStack(MobsRegistry.HOWLER_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            out.accept(DisguiseRegistry.CLOAK);
            if (MobsConfig.trickMirrorEnabled) out.accept(DisguiseRegistry.MIRROR);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!MobsConfig.vanillaTabs() || !MobsRegistry.on()) return;
            out.insertAfter(Items.ELYTRA, java.util.List.of(new ItemStack(WitherWings.WITHER), new ItemStack(WitherWings.NETHER), new ItemStack(WitherWings.END), new ItemStack(WitherWings.VOID)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!MobsConfig.vanillaTabs()) return;
            if (!MobsRegistry.on()) return;
            out.accept(MobsRegistry.CLOTH);
            out.accept(DisguiseRegistry.SHARD);
            out.insertAfter(Items.ECHO_SHARD, java.util.List.of(new ItemStack(MobsRegistry.ECHO_CORE)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
    }
}
