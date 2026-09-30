package com.simplebuilding.modules.simpletweaks;
import net.fabricmc.api.ModInitializer;
public final class FabricSimpleTweaks implements ModInitializer {
    @Override public void onInitialize() {
        LegacyDeed.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::start);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(com.simplebuilding.modules.simpletweaks.claims.Claims::stop);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((d,r,e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(d));
    }
}
