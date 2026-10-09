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

    // Beside the vanilla models, so the search tab lists them there (AnimalHeads.SNOWBALL_ANCHOR).
    var visibility = net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
        .register(
            entries -> {
              if(!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(),"simplefun"))return;
              if (entries.getDisplayStacks().stream()
                  .anyMatch(s -> s.is(com.simplefun.heads.AnimalHeads.SNOWBALL_ANCHOR)))
                entries.insertAfter(
                    com.simplefun.heads.AnimalHeads.SNOWBALL_ANCHOR,
                    java.util.List.of(new net.minecraft.world.item.ItemStack(ModItems.BRICK_SNOWBALL)),
                    visibility);
            });
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
        .register(
            entries -> {
              if(!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(),"simplefun"))return;
              if (entries.getDisplayStacks().stream()
                  .anyMatch(s -> s.is(com.simplefun.heads.AnimalHeads.HEAD_ANCHOR)))
                entries.insertAfter(
                    com.simplefun.heads.AnimalHeads.HEAD_ANCHOR,
                    com.simplefun.heads.AnimalHeads.headStacks(),
                    visibility);
            });
    ServerLivingEntityEvents.AFTER_DEATH.register(PlayerHeadDrop::onDeath);
    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))
      com.simplefun.guide.FunGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, access, environment) -> SimplefunCommands.register(dispatcher));
  }
}
