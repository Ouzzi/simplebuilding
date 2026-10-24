package com.simplewanderers;

import com.simplewanderers.config.WandererConfig;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module constants (concept docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md, owner answers section 11).
 * Loader entry points call {@link #init} once with their config directory, before any registry event.
 */
public final class SimpleWanderers {
    public static final String MOD_ID = "simplewanderers";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Wanderers");
    /** One Minecraft day in game ticks; every duration in the config is a number of days. */
    public static final long DAY = 24000L;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Super-mod switch "Enable Simple Wanderers" (framework SubMods, mirrored by SimpleBuilding) and the own config. Off = no new
     * wanderer, no respawn and no rotation; placed wanderers stay plain traders with the offers they have.
     */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && WandererConfig.enabled;
    }

    public static void init(Path configDir) {
        WandererConfig.load(configDir);
        // CMD24: /simplewanderers config for every option.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(WandererConfig::toJson, WandererConfig::apply).file(configDir, WandererConfig.FILE).register();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "wanderers", "simplewanderers.testcentre.station",
                java.util.List.of("simplewanderers:village_wanderer_spawn_egg", "simplewanderers:guide_book"));
    }

    private SimpleWanderers() {}
}
