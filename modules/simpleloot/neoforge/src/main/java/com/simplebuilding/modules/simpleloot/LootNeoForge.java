package com.simplebuilding.modules.simpleloot;

import net.neoforged.fml.common.Mod;

@Mod("simpleloot")
public final class LootNeoForge {
    public LootNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleLoot.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleLoot.load();
        ModuleNeoTests.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> LootCommands.register(e.getDispatcher()));
    }
}
