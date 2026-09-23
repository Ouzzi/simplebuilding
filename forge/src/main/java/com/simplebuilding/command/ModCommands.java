package com.simplebuilding.command;

import com.simplebuilding.Simplebuilding;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-Haken fuer {@link SimplebuildingCommand}; der Baum selbst liegt im gemeinsamen Code. */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCommands {

    private ModCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SimplebuildingCommand.register(event.getDispatcher());
    }
}
