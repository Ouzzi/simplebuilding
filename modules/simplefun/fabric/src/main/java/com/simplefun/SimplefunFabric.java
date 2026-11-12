package com.simplefun;

import com.simplefun.command.SimplefunCommands;
import com.simplefun.registry.SimplefunRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class SimplefunFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    SimplefunCommon.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
    SimplefunCommon.init();
    SimplefunCommon.registerConfig();
    // CMD24: /simplefun config for every option (the existing /simplefun tweaks switches stay).
    com.simplelib.api.config.ConfigCommands.server(Constants.MOD_ID)
        .gson(com.simplefun.config.SimplefunConfig.class, SimplefunCommon::getConfig, SimplefunCommon::replaceConfig,
            () -> { var d = new com.simplefun.config.SimplefunConfig(); d.normalize(); return d; })
        .save(SimplefunCommon::saveConfig).register();

    // Fabric registries are open during init.
    // FUN11-1..10 (b-fun14): the duck block before the items.
    com.simplefun.registry.ModToys.registerBlocks();
    SimplefunRegistry.registerItems();
    com.simplefun.registry.ModToys.registerItems();
    com.simplefun.registry.FunContent.items();
    com.simplefun.registry.FunContent.tab();
    FunFabricData.register();
    // AUDSB31E1 F11: the Knockback override (max level 4, more supported items) is a built-in data pack that is only added
    // while Simple Fun and enableHigherKnockback are on; otherwise the Vanilla enchantment stays untouched.
    SimplefunCommon.higherKnockbackData =
        SimplefunCommon.wantsHigherKnockbackData()
            && net.fabricmc.fabric.api.resource.v1.ResourceLoader.registerBuiltinPack(
                com.simplefun.registry.FunContent.id("higher_knockback"),
                net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(Constants.MOD_ID).orElseThrow(),
                net.fabricmc.fabric.api.resource.v1.pack.PackActivationType.ALWAYS_ENABLED);
    SimplefunRegistry.registerEntities();
    SimplefunRegistry.registerEffects();
    com.simplefun.registry.ModToys.registerEntities();
    com.simplefun.registry.ModToys.registerMisc();

    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("ftbquests"))
      com.simplefun.guide.FunGuide.installQuests(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, access, environment) -> SimplefunCommands.register(dispatcher));
  }
}
