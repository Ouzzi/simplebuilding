package com.simpleanimals;

import com.simpleanimals.config.AnimalsConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md, owner choice ABN29b-3). Loader entry points call
 * {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleAnimals {
    public static final String MOD_ID = "simpleanimals";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Animals");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Super-mod switch "Enable Simple Animals" (framework SubMods, mirrored by SimpleBuilding) and the own config. Off =
     * no new natural spawns; animals that already live in the world stay and behave as before.
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && AnimalsConfig.enabled;
    }

    public static void init(Path configDir) {
        AnimalsConfig.load(configDir);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "animals", "simpleanimals.testcentre.station",
                java.util.List.of("simpleanimals:deer_spawn_egg", "simpleanimals:boar_spawn_egg", "simpleanimals:hedgehog_spawn_egg",
                        "simpleanimals:crab_spawn_egg", "simpleanimals:zebra_spawn_egg", "simpleanimals:raw_crab",
                        "simpleanimals:cooked_crab", "simpleanimals:guide_book"));
    }

    private SimpleAnimals() {}
}
