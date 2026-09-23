package com.simplebuilding.command;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** NeoForge-Haken fuer {@link SimplebuildingCommand}; der Baum selbst liegt im gemeinsamen Code. */
@EventBusSubscriber(modid = com.simplebuilding.Simplebuilding.MOD_ID)
public final class ModCommands {

    private ModCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SimplebuildingCommand.register(event.getDispatcher());
    }
}
