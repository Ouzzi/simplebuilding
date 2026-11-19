package com.simplebuilding.modules.simpleslabs;

import com.simpleslabs.SimpleSlabs;
import com.simpleslabs.registry.SlabRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class SlabsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleSlabs.init(FabricLoader.getInstance()::isModLoaded);
        SlabRegistry.registerBlocks();
        SlabRegistry.registerItems();
        SlabRegistry.registerFlammability();
        SlabRegistry.registerTab();
    }
}
