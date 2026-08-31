package com.simplebuilding.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimplebuildingCommon {
    public static final String MOD_ID = "simplebuilding";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SimplebuildingCommon() {
    }

    public static synchronized String initialize(SimplebuildingLoader loader) {
        return SimplebuildingBootstrap.initialize(loader, SimplebuildingStartupPlan.builder().build());
    }

    public static synchronized String initialize(String loader) {
        return SimplebuildingBootstrap.initialize(loader, SimplebuildingStartupPlan.builder().build());
    }
}
