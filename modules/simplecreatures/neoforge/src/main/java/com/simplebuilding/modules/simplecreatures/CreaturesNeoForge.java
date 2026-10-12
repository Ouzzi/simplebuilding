package com.simplebuilding.modules.simplecreatures;

import net.neoforged.fml.common.Mod;

@Mod("simplecreatures")
public final class CreaturesNeoForge {
    public CreaturesNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleCreatures.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleCreatures.load();
        ModuleNeoTests.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> CreaturesCommands.register(e.getDispatcher()));
    }
}
