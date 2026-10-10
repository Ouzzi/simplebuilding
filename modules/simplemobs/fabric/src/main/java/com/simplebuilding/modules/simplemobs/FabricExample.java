package com.simplebuilding.modules.simplemobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.item.CreativeModeTabs;

public final class FabricExample implements ModInitializer {
    @Override public void onInitialize() {
        MobsRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(MobsRegistry.DECEIVER, DeceiverEntity.createAttributes());
        ServerTickEvents.END_SERVER_TICK.register(MobsServer::tick);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(e -> e.accept(MobsRegistry.EGG));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(e -> e.accept(MobsRegistry.CLOTH));
    }
}
