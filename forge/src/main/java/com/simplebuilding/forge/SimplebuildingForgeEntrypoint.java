package com.simplebuilding.forge;

import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.common.SimplebuildingStartupPlan;

public final class SimplebuildingForgeEntrypoint {
    public SimplebuildingForgeEntrypoint() {
        System.out.println(SimplebuildingBootstrap.initialize("forge", buildStartupPlan()));
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
            .configure(this::registerForgeHooks)
            .build();
    }

    private void registerForgeHooks() {
        // TODO: wire Forge event bus and @Mod bootstrap here.
    }
}
