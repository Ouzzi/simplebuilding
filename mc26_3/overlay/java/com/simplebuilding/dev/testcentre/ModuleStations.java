package com.simplebuilding.dev.testcentre;

import com.simplebuilding.framework.api.TestCentreStations;
import java.util.List;

/**
 * 26.3: the test centre's view of {@link TestCentreStations} (module split 3c) - stations other modules register.
 * The 26.2 twin (common/src/mc26_2/java) has none (no framework on that line).
 */
public final class ModuleStations {
    public static final boolean SUPPORTED = true;

    public record Station(String owner, String id, String titleKey, List<String> items) {}

    private ModuleStations() {
    }

    public static List<Station> all() {
        return TestCentreStations.all().stream().map(s -> new Station(s.owner(), s.id(), s.titleKey(), s.items())).toList();
    }

    public static void register(String owner, String id, String titleKey, List<String> items) {
        TestCentreStations.register(owner, id, titleKey, items);
    }

    public static void unregister(String owner, String id) {
        TestCentreStations.unregister(owner, id);
    }
}
