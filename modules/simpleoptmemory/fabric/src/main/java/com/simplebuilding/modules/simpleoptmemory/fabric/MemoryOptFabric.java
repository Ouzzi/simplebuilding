package com.simplebuilding.modules.simpleoptmemory.fabric;

import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Loader adapter: config, switch and detection log (no area in stage 1). */
public final class MemoryOptFabric implements ModInitializer {
    @Override public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        MemoryOpt.boot(loader.getConfigDir(), loader::isModLoaded);
    }
}
