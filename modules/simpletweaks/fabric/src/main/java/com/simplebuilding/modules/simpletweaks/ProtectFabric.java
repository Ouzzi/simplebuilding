package com.simplebuilding.modules.simpletweaks;

import com.simplebuilding.modules.simpletweaks.protect.*;

/** Fabric wiring of the protection features (lifecycle, commands, rule packet); the rest comes from shared mixins. */
final class ProtectFabric {
    static void init() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(ProtectRulesPayload.TYPE, ProtectRulesPayload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(Protect::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Protect.stop(server);
            // REV24-F-31: drop all framework protection providers of the stopped server so the registry does not pin it.
            com.simplebuilding.framework.api.Protection.clear(server);
        });
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d, r, e) -> ProtectCommands.register(d));
    }
    private ProtectFabric() {}
}
