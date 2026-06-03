package com.simplebuilding.forge;

import com.simplebuilding.common.SimplebuildingBootstrap;
import com.simplebuilding.common.SimplebuildingLoader;
import com.simplebuilding.common.SimplebuildingStartupPlan;

public final class SimplebuildingForgeModBootstrap {
    public String bootstrap() {
        return SimplebuildingBootstrap.initialize(SimplebuildingLoader.FORGE, buildStartupPlan());
    }

    private SimplebuildingStartupPlan buildStartupPlan() {
        return SimplebuildingStartupPlan.builder()
            .configure(this::registerForgeHooks)
            .build();
    }

    private void registerForgeHooks() {
        // TODO: replace with real Forge @Mod class and event bus wiring.
    }
}