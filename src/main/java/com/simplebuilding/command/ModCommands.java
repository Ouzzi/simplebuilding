package com.simplebuilding.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Fabric-Haken fuer {@link SimplebuildingCommand}; der Baum selbst liegt im gemeinsamen Code. */
public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SimplebuildingCommand.register(dispatcher));
    }

}