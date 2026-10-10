package com.simplebuilding.modules.simpleinterfaces;

import net.neoforged.fml.common.Mod;

/** NeoForge entry: only the GameTests; the style itself is client mixins plus a lazily loaded config. */
@Mod("simpleinterfaces")
public final class ContainersNeoForge {
    public ContainersNeoForge(net.neoforged.bus.api.IEventBus bus) {
        ModuleNeoTests.register(bus);
    }
}
