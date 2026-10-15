package com.simplebuilding.modules.simplewanderers;

import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.logic.WandererSpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class WanderersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleWanderers.init(FabricLoader.getInstance().getConfigDir());
        WandererRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(WandererRegistry.WANDERER, WandererRegistry.wandererAttributes());
        FabricDefaultAttributeRegistry.register(WandererRegistry.HORSE, WandererRegistry.horseAttributes());
        ServerTickEvents.END_SERVER_TICK.register(WandererSpawner::tick);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!SimpleWanderers.on()) return;
            var eggs = WandererRegistry.eggStacks();
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WANDERING_TRADER_SPAWN_EGG))) {
                out.insertAfter(Items.WANDERING_TRADER_SPAWN_EGG, eggs, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                eggs.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleWanderers.on()) return;
            var book = java.util.List.of(com.simplewanderers.guide.WanderersGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplewanderers.guide.WanderersGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
