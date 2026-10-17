package com.simplebuilding.modules.simpletweaks.forge;

import com.simplebuilding.modules.simpletweaks.play.*;

/** Forge wiring of the player/world/event features: lifecycle and commands (GameTests via {@link ModuleForgeTests}). */
final class PlayForge {
    static void init(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        Mods.check = net.minecraftforge.fml.ModList::isLoaded;
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(e -> Play.start(e.getServer()));
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> Play.stop(e.getServer()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(e -> PlayCommands.register(e.getDispatcher()));
    }
    private PlayForge() {}
}
