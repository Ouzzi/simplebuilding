package com.simplebuilding.modules.simplecontainers;

import net.neoforged.fml.common.Mod;

/** NeoForge entry: only the GameTests; the style itself is client mixins plus a lazily loaded config. */
@Mod("simplecontainers")
public final class ContainersNeoForge {
    public ContainersNeoForge(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
    }
}
