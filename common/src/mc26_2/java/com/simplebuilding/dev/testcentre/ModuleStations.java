package com.simplebuilding.dev.testcentre;

import java.util.List;

/** 26.2 twin of the 26.3 bridge to {@code framework.api.TestCentreStations}: no framework here, so no stations. */
public final class ModuleStations {
    public static final boolean SUPPORTED = false;

    public record Station(String owner, String id, String titleKey, List<String> items) {}

    private ModuleStations() {
    }

    public static List<Station> all() {
        return List.of();
    }

    public static void register(String owner, String id, String titleKey, List<String> items) {
    }

    public static void unregister(String owner, String id) {
    }
}
