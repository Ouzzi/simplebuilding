package com.simplebuilding;

import com.simplebuilding.common.SimplebuildingCommon;
import com.simplebuilding.config.SimplebuildingConfig;

public final class Simplebuilding {
    public static final String MOD_ID = SimplebuildingCommon.MOD_ID;
    public static final org.slf4j.Logger LOGGER = SimplebuildingCommon.LOGGER;

    private static SimplebuildingConfig CONFIG = new SimplebuildingConfig();

    private Simplebuilding() {
    }

    public static void setConfig(SimplebuildingConfig config) {
        CONFIG = config != null ? config : new SimplebuildingConfig();
    }

    public static SimplebuildingConfig getConfig() {
        return CONFIG;
    }
}
