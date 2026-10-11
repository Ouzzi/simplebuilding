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

public final class FabricExample implements ModInitializer {
    @Override public void onInitialize() {
        MobsConfig.load(FabricLoader.getInstance().getConfigDir());
        MobsRegistry.registerAll();
        DisguiseRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MobsRegistry.HOLLOW_HOWLER, HowlerEntity.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(MobsServer::tick);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            out.accept(MobsRegistry.EGG);
            out.insertAfter(Items.HOGLIN_SPAWN_EGG, java.util.List.of(new ItemStack(MobsRegistry.HOWLER_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            out.accept(DisguiseRegistry.CLOAK);
            out.accept(DisguiseRegistry.MIRROR);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            out.accept(MobsRegistry.CLOTH);
            out.accept(DisguiseRegistry.SHARD);
            out.insertAfter(Items.ECHO_SHARD, java.util.List.of(new ItemStack(MobsRegistry.ECHO_CORE)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
    }
}
