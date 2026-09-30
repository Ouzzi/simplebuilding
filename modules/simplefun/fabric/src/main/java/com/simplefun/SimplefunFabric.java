package com.simplefun;

import com.simplefun.command.SimplefunCommands;
import com.simplefun.event.PlayerHeadDrop;
import com.simplefun.registry.ModItems;
import com.simplefun.registry.SimplefunRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class SimplefunFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    SimplefunCommon.init();
    SimplefunCommon.registerConfig();

    // Fabric registries are open during init.
    com.simplefun.heads.AnimalHeads.blocks();
    SimplefunRegistry.registerItems();
    com.simplefun.heads.AnimalHeads.items();
    com.simplefun.heads.AnimalHeads.tab();
    FunFabricData.register();
    SimplefunRegistry.registerEntities();
    SimplefunRegistry.registerEffects();

    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
        .register(entries -> entries.accept(ModItems.BRICK_SNOWBALL));
    ServerLivingEntityEvents.AFTER_DEATH.register(PlayerHeadDrop::onDeath);
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, access, environment) -> SimplefunCommands.register(dispatcher));
  }
}
