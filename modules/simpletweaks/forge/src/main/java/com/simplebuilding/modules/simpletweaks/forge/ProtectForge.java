package com.simplebuilding.modules.simpletweaks.forge;

import com.simplebuilding.modules.simpletweaks.protect.*;

/** Forge wiring of the protection features: lifecycle and commands (rule packet on the deed channel, GameTests via {@link ModuleForgeTests}). */
final class ProtectForge {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> Protect.start(e.getServer()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> Protect.stop(e.getServer()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> ProtectCommands.register(e.getDispatcher()));
    }
    private ProtectForge() {}
}
