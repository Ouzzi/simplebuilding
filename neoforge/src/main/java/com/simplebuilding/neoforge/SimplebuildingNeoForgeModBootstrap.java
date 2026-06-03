package com.simplebuilding.neoforge;

import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.common.SimplebuildingLoader;
import com.simplebuilding.common.SimplebuildingStartupPlan;

public final class SimplebuildingNeoForgeModBootstrap {
    public String bootstrap() {
        return SimplebuildingBootstrap.initialize(SimplebuildingLoader.NEOFORGE, buildStartupPlan());
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
            .configure(this::registerNeoForgeHooks)
            .build();
    }

    private void registerNeoForgeHooks() {
        // TODO: replace with real NeoForge @Mod class and event bus wiring.
    }
}