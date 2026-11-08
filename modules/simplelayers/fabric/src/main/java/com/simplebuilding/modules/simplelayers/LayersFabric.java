package com.simplebuilding.modules.simplelayers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class LayersFabric implements ModInitializer {
    @Override public void onInitialize() {
        SimpleLayers.init(FabricLoader.getInstance().getConfigDir());
        ExampleItems.register();
    }
}
