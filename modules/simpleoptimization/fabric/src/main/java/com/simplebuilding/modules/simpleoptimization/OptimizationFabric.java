package com.simplebuilding.modules.simpleoptimization;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
/** Super-mod entry: no content, only the sub-mod switches (config/simpleoptimization.json). */
public final class OptimizationFabric implements ModInitializer {
    @Override public void onInitialize() {
        SimpleOptimization.load(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleoptimization config; the sub-mods read their switch while the game starts, so a change needs a restart.
        com.simplelib.api.config.ConfigCommands.server(SimpleOptimization.MOD_ID).gson(SimpleOptimization.Config.class, SimpleOptimization::config, SimpleOptimization::restore, SimpleOptimization.Config::new)
                .file(FabricLoader.getInstance().getConfigDir(), SimpleOptimization.FILE).restart("enableSimpleOptRender", "enableSimpleOptMemory", "enableSimpleOptServer").register();
    }
}
