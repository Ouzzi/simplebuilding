package com.simplebuilding.modules.simplefoundry;

import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.SimpleFoundry;
import com.simplefoundry.config.FoundryConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. The tab holds the items alone (IG31-3, CreativeTabPolicy). */
public final class FoundryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleFoundry.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simplefoundry config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleFoundry.MOD_ID).json(FoundryConfig::toJson, FoundryConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), FoundryConfig.FILE).register();
        FoundryRegistry.registerAll();
    }
}
