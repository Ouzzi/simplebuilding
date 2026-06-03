package com.simplebuilding.neoforge;

import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.common.SimplebuildingStartupPlan;

public final class SimplebuildingNeoForgeEntrypoint {
    public SimplebuildingNeoForgeEntrypoint() {
        System.out.println(SimplebuildingBootstrap.initialize("neoforge", buildStartupPlan()));
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
            .configure(this::registerNeoForgeHooks)
            .build();
    }

    private void registerNeoForgeHooks() {
        // TODO: wire NeoForge event bus and @Mod bootstrap here.
    }
}
