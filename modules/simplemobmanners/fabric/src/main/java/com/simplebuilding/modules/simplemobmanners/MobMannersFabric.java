package com.simplebuilding.modules.simplemobmanners;

import net.fabricmc.api.ModInitializer;

public final class MobMannersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleMobManners.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleMobManners.load();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> MobMannersCommands.register(d));
    }
}
