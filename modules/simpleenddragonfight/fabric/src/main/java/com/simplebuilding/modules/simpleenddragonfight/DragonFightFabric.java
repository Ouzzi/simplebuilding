package com.simplebuilding.modules.simpleenddragonfight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DragonFightFabric implements ModInitializer {
    @Override public void onInitialize() {
        DragonFightConfig.load(FabricLoader.getInstance().getConfigDir());
        DragonFightRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(DragonFightRegistry.DRAGONMITE, Dragonmite.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(DragonFightServer::tick);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            if (!DragonFightRegistry.on()) return;
            out.insertAfter(Items.DRAGON_BREATH, java.util.List.of(new ItemStack(DragonFightRegistry.DRAGON_CHITIN)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!DragonFightRegistry.on() || !DragonFightConfig.exposeSpawnEgg) return;
            out.insertAfter(Items.ENDERMITE_SPAWN_EGG, java.util.List.of(new ItemStack(DragonFightRegistry.DRAGONMITE_EGG)), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
    }
}
