package com.simplebuilding.command;

import com.simplebuilding.Simplebuilding;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Register the shared root before extensions, independent of jar scan order. */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModCommands {
    private ModCommands() {}

    @SubscribeEvent(priority = Priority.HIGH)
    public static void registerCommands(RegisterCommandsEvent event) {
        SimplebuildingCommand.register(event.getDispatcher());
    }
}
