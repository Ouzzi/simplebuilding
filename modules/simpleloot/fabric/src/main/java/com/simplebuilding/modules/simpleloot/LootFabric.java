package com.simplebuilding.modules.simpleloot;

import net.fabricmc.api.ModInitializer;

public final class LootFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleLoot.configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        SimpleLoot.load();
        // CMD24: /simpleloot config for every option; save() normalises, writes and syncs the clients (Fabric entry: SimpleLib is bundled here).
        com.simplelib.api.config.ConfigCommands.server(SimpleLoot.MOD_ID).gson(LootConfig.class, () -> SimpleLoot.CONFIG, c -> { c.normalize(); SimpleLoot.CONFIG = c; },
                () -> { LootConfig d = new LootConfig(); d.normalize(); return d; }).save(SimpleLoot::save).register();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> LootCommands.register(d));
    }
}
