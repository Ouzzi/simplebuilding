package com.simplebuilding.modules.simplevisuals;
import net.fabricmc.api.ModInitializer;
public final class FabricExample implements ModInitializer {
    @Override public void onInitialize() { ExampleItems.register(); net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, selection)->com.simplevisuals.VisualsCommands.register(dispatcher)); }
}
