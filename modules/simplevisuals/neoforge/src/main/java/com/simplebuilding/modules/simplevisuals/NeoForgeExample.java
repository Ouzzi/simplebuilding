package com.simplebuilding.modules.simplevisuals;
import net.neoforged.fml.common.Mod;
@Mod("simplevisuals")
public final class NeoForgeExample {
    public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
        com.simplevisuals.Visuals.initialize();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event)->com.simplevisuals.VisualsCommands.register(event.getDispatcher()));
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
