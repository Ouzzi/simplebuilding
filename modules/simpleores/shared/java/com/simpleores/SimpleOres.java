package com.simpleores;

import com.simpleores.config.OresConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-A-RG30-2026-10-30.md, owner choice ABN30-10). Loader entry points call
 * {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleOres {
    public static final String MOD_ID = "simpleores";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Ores");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Super-mod switch "Enable Simple Ores" (framework SubMods, mirrored by SimpleBuilding) and the own config. Off = no new
     * ore veins and no creative entries; blocks and items that exist keep working.
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && OresConfig.enabled;
    }

    public static void init(Path configDir) {
        OresConfig.load(configDir);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "ores", "simpleores.testcentre.station", OresRegistry.itemIds());
    }

    private SimpleOres() {}
}
