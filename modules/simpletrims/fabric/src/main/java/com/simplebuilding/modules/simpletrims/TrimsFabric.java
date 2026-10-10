package com.simplebuilding.modules.simpletrims;

import com.simpletrims.SimpleTrims;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry. Stage 1 registers nothing yet; content moves in from SimpleBuilding stage by stage. */
public final class TrimsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleTrims.init(FabricLoader.getInstance().getConfigDir());
    }
}
