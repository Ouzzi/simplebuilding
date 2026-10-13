package com.simplebuilding.modules.simplemobmanners;

import net.neoforged.fml.common.Mod;

@Mod("simplemobmanners")
public final class MobMannersNeoForge {
    public MobMannersNeoForge(net.neoforged.bus.api.IEventBus bus) {
        SimpleMobManners.configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        SimpleMobManners.load();
        ModuleNeoTests.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> MobMannersCommands.register(e.getDispatcher()));
    }
}
