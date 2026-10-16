package com.simplebuilding.modules.simplelib;

import net.neoforged.bus.api.IEventBus;

/** Client-only; referenced only on the client dist. Nothing to register since the crucible screens moved to Simple Crucibles (split stage 5). */
final class SimpleLibNeoForgeClient {
    static void init(IEventBus bus) {
    }

    private SimpleLibNeoForgeClient() {}
}
