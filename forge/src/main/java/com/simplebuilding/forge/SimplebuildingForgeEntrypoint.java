package com.simplebuilding.forge;

import com.simplebuilding.common.SimplebuildingBootstrap;

public final class SimplebuildingForgeEntrypoint {
    public SimplebuildingForgeEntrypoint() {
        System.out.println(SimplebuildingBootstrap.initialize("forge", this::registerForgeHooks));
    }

    private void registerForgeHooks() {
        // TODO: wire Forge event bus and @Mod bootstrap here.
    }
}
