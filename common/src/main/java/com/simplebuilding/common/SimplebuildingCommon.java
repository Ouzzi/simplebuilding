package com.simplebuilding.common;

public final class SimplebuildingCommon {
    public static final String MOD_ID = "simplebuilding";

    private SimplebuildingCommon() {
    }

    public static synchronized String initialize(SimplebuildingLoader loader) {
        return SimplebuildingBootstrap.initialize(loader, SimplebuildingStartupPlan.builder().build());
    }

    public static synchronized String initialize(String loader) {
        return SimplebuildingBootstrap.initialize(loader, SimplebuildingStartupPlan.builder().build());
    }
}
