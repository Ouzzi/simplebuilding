package com.simplefun;

import com.simplefun.command.SimplefunCommands;
import com.simplefun.registry.ModItems;
import com.simplefun.registry.SimplefunRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class SimplefunFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    SimplefunCommon.init();
    SimplefunCommon.registerConfig();

    // Fabric registries are open during init.
    SimplefunRegistry.registerItems();
    com.simplefun.registry.FunContent.items();
    com.simplefun.registry.FunContent.tab();
    FunFabricData.register();
    SimplefunRegistry.registerEntities();
    SimplefunRegistry.registerEffects();

    // Beside the vanilla models, so the search tab lists them there (FunContent.SNOWBALL_ANCHOR).
    var visibility = net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
        .register(
            entries -> {
              if(!com.simplefun.SimplefunCommon.on()||!com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(),"simplefun"))return;
              if (entries.getDisplayStacks().stream()
                  .anyMatch(s -> s.is(com.simplefun.registry.FunContent.SNOWBALL_ANCHOR)))
                entries.insertAfter(
                    com.simplefun.registry.FunContent.SNOWBALL_ANCHOR,
                    java.util.List.of(new net.minecraft.world.item.ItemStack(ModItems.BRICK_SNOWBALL)),
                    visibility);
            });
    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))
      com.simplefun.guide.FunGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, access, environment) -> SimplefunCommands.register(dispatcher));
  }
}
