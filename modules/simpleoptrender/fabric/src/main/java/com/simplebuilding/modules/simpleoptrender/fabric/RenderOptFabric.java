package com.simplebuilding.modules.simpleoptrender.fabric;

import com.simplebuilding.modules.simpleoptrender.RenderOpt;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Entry: the areas were decided by the mixin plugin already; this only covers a start where it did not run. */
public final class RenderOptFabric implements ModInitializer {
    @Override public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        RenderOpt.boot(loader.getConfigDir(), loader::isModLoaded);
    }
}
