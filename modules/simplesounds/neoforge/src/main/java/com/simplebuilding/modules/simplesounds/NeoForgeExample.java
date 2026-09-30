package com.simplebuilding.modules.simplesounds;
import net.neoforged.fml.common.Mod;
@Mod("simplesounds")
public final class NeoForgeExample {
    public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
