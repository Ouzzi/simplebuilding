package com.simplebuilding.common;

import java.util.HashSet;
import java.util.Set;

public final class SimplebuildingBootstrap {
    private static final Set<String> INITIALIZED_LOADERS = new HashSet<>();

    private SimplebuildingBootstrap() {
    }

    public static synchronized String initialize(String loader, Runnable loaderHook) {
        return initialize(loader, SimplebuildingStartupPlan.builder()
            .configure(loaderHook)
            .build());
    }

    public static synchronized String initialize(String loader, SimplebuildingStartupPlan startupPlan) {
        boolean firstInitializationForLoader = INITIALIZED_LOADERS.add(loader);
        if (firstInitializationForLoader && startupPlan != null) {
            startupPlan.run();
        }

        if (firstInitializationForLoader) {
            return "Simplebuilding common initialized for " + loader;
        }

        return "Simplebuilding common already initialized (" + loader + ")";
    }
}