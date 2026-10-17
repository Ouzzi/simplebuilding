package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.play.*;

/** Fabric wiring of the player/world/event features (lifecycle, commands); ticks, joins and chat come from shared mixins. */
final class PlayFabric {
    static void init() {
        Mods.check = net.fabricmc.loader.api.FabricLoader.getInstance()::isModLoaded;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(Play::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(Play::stop);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> PlayCommands.register(d));
    }
    private PlayFabric() {}
}
