package com.simplebuilding.modules.simpleoptmemory.fabric;

import com.simplebuilding.modules.simpleoptmemory.MemoryOpt;
import com.simplebuilding.modules.simpleoptmemory.StateTables;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Entry: the areas were decided by the mixin plugin already; logs what O4 shared while the blocks were built. */
public final class MemoryOptFabric implements ModInitializer {
    @Override public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        MemoryOpt.boot(loader.getConfigDir(), loader::isModLoaded);
        // CMD24: /simpleoptmemory config (server) and /simpleclient simpleoptmemory, both on the one file.
        com.simplelib.api.config.ConfigCommands.optSettings(com.simplelib.api.config.ConfigCommands.server(MemoryOpt.MOD_ID), loader.getConfigDir(), MemoryOpt.FILE, MemoryOpt.defaults()).register();
        com.simplelib.api.config.ConfigCommands.optSettings(com.simplelib.api.config.ConfigCommands.client(MemoryOpt.MOD_ID), loader.getConfigDir(), MemoryOpt.FILE, MemoryOpt.defaults()).register();
        if (MemoryOpt.live(MemoryOpt.STATE_TABLES)) MemoryOpt.LOG.info(StateTables.summary());
    }
}
