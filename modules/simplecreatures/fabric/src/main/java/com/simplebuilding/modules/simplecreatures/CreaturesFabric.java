package com.simplebuilding.modules.simplecreatures;

import net.fabricmc.api.ModInitializer;

public final class CreaturesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleCreatures.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleCreatures.load();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> CreaturesCommands.register(d));
    }
}
