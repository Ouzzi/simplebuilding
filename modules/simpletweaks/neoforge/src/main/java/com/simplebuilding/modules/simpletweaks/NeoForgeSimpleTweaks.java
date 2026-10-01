package com.simplebuilding.modules.simpletweaks;
import net.neoforged.fml.common.Mod;
@Mod("simpletweaks")
public final class NeoForgeSimpleTweaks {
    public NeoForgeSimpleTweaks(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
        var events=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        events.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.start(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e)->com.simplebuilding.modules.simpletweaks.claims.Claims.stop(e.getServer()));
        events.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->com.simplebuilding.modules.simpletweaks.claims.ClaimCommands.register(e.getDispatcher()));
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) LegacyDeed.register();
        });
    }
}
