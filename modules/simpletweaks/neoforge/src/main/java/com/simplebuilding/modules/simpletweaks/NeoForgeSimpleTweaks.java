package com.simplebuilding.modules.simpletweaks;
import net.neoforged.fml.common.Mod;
@Mod("simpletweaks")
public final class NeoForgeSimpleTweaks {
    public NeoForgeSimpleTweaks(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
            if (event.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) LegacyDeed.register();
        });
    }
}
