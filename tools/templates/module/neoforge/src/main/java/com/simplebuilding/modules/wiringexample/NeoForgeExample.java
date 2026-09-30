package com.simplebuilding.modules.__MODID__;
import net.neoforged.fml.common.Mod;
@Mod("__MODID__")
public final class NeoForgeExample {
    public NeoForgeExample(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) ExampleItems.register();
        });
    }
}
