package com.simplebuilding.modules.simpleoptimization;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleoptimization.json). */
public final class OptimizationFabric implements ModInitializer {
    @Override public void onInitialize() { SimpleOptimization.load(FabricLoader.getInstance().getConfigDir()); }
}
