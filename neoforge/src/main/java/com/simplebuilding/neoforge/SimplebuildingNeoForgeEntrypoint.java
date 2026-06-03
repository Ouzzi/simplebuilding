package com.simplebuilding.neoforge;

import com.simplebuilding.common.SimplebuildingBootstrap;

public final class SimplebuildingNeoForgeEntrypoint {
    public SimplebuildingNeoForgeEntrypoint() {
        System.out.println(SimplebuildingBootstrap.initialize("neoforge", this::registerNeoForgeHooks));
    }

    private void registerNeoForgeHooks() {
        // TODO: wire NeoForge event bus and @Mod bootstrap here.
    }
}
