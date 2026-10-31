package com.simplebuilding.modules.simplelivemap.fabric;

import com.simplebuilding.modules.simplelivemap.LivemapCommands;
import com.simplebuilding.modules.simplelivemap.LivemapRuntime;
import com.simplebuilding.modules.simplelivemap.SimpleLivemap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Loader adapter: config, the server events (start, tick, stop) and the commands. No mixins, no registry content. */
public final class LivemapFabric implements ModInitializer {
    @Override public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        SimpleLivemap.init(loader.getConfigDir(), loader::isModLoaded);
        ServerLifecycleEvents.SERVER_STARTED.register(LivemapRuntime::started);
        ServerLifecycleEvents.SERVER_STOPPING.register(LivemapRuntime::stopping);
        ServerTickEvents.END_SERVER_TICK.register(LivemapRuntime::tick);
        CommandRegistrationCallback.EVENT.register((d, r, e) -> LivemapCommands.register(d));
    }
}
