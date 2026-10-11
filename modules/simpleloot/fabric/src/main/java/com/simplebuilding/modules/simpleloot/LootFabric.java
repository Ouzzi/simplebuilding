package com.simplebuilding.modules.simpleloot;

import net.fabricmc.api.ModInitializer;

public final class LootFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLoot.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleLoot.load();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> LootCommands.register(d));
    }
}
