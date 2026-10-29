package com.simplebuilding.modules.simpleoptserver.fabric;

import com.simplebuilding.modules.simpleoptserver.ServerOpt;
import com.simplebuilding.modules.simpleoptserver.ServerOptRuntime;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Loader adapter: mod detection via FabricLoader.isModLoaded, S5a on the server events, the status command. */
public final class ServerOptFabric implements ModInitializer {
    @Override public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        ServerOpt.boot(loader.getConfigDir(), loader::isModLoaded);
        // CMD24: /simpleoptserver config (every option needs a restart).
        com.simplelib.api.config.ConfigCommands.optSettings(com.simplelib.api.config.ConfigCommands.server(ServerOpt.MOD_ID), loader.getConfigDir(), ServerOpt.FILE, ServerOpt.defaults()).register();
        ServerLifecycleEvents.SERVER_STARTED.register(ServerOptRuntime::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> ServerOptRuntime.stopped());
        ServerTickEvents.END_SERVER_TICK.register(ServerOptRuntime::tick);
        CommandRegistrationCallback.EVENT.register((d, r, e) -> ServerOptRuntime.register(d));
    }
}
