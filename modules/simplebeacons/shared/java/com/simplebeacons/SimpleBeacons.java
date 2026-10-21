package com.simplebeacons;

import com.simplebeacons.config.BeaconsConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (docs/ai/KONZEPT-STRONG-BEACON-2026-10-13.md, plan docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md).
 * Loader entry points call {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleBeacons {
    public static final String MOD_ID = "simplebeacons";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Beacons");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Beacons" (framework SubMods, mirrored by SimpleBuilding) and the own config: off = inert. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && BeaconsConfig.enabled;
    }

    public static void init(Path configDir) {
        BeaconsConfig.load(configDir);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "beacons", "simplebeacons.testcentre.station",
                java.util.List.of("simplebeacons:strong_upgrade_template", "simplebeacons:strong_upgrade_shard", "simplebeacons:strong_beacon",
                        "simplebeacons:strong_conduit", "simplebeacons:guide_book"));
    }

    private SimpleBeacons() {}
}
