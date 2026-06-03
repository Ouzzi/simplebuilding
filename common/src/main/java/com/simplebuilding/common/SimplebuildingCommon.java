package com.simplebuilding.common;

public final class SimplebuildingCommon {
    private static boolean initialized;

    private SimplebuildingCommon() {
    }

    public static synchronized String initialize(String loader) {
        if (initialized) {
            return "Simplebuilding common already initialized (" + loader + ")";
        }
        initialized = true;
        return "Simplebuilding common initialized for " + loader;
    }
}
