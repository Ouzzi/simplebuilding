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
        // CMD24: /simplebeacons config for every option.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(BeaconsConfig::toJson, BeaconsConfig::apply).file(configDir, BeaconsConfig.FILE).register();
        // WF25-10 (07.11.): the Strong Upgrade template and shard moved to SimpleLib (the Wither drop and its recipe viewer
        // entry with them); old worlds find them under their old ids. The Nether Heart recipes belong to Simple Mobs now.
        com.simplelib.api.SimpleLibApi.requestMaterial("strong_upgrade");
        idMoves();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "beacons", "simplebeacons.testcentre.station",
                java.util.List.of("simplelib:strong_upgrade_template", "simplebeacons:evil_upgrade_template", "simplelib:strong_upgrade_shard",
                        "simplebeacons:strong_beacon", "simplebeacons:mighty_beacon", "simplebeacons:evil_beacon", "simplebeacons:mighty_evil_beacon",
                        "simplebeacons:strong_conduit", "simplebeacons:mighty_conduit", "simplebeacons:evil_conduit",
                        "simplebeacons:mighty_evil_conduit", "simplebeacons:guide_book"));
    }

    /** Ids that left Simple Beacons on 07.11. (WF25-10): items + shard recipe to SimpleLib, the Nether Heart recipes to Simple Mobs. */
    public static final java.util.Map<String, String> MOVED = java.util.Map.of(
            "strong_upgrade_template", "simplelib", "strong_upgrade_shard", "simplelib");
    public static final java.util.Map<String, String> MOVED_RECIPES = java.util.Map.of(
            "strong_upgrade_template_from_shards", "simplelib", "strong_upgrade_template", "simplemobs", "strong_upgrade_template_duplication", "simplemobs");

    static void idMoves() {
        MOVED.forEach((path, to) -> com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ITEM, id(path), Identifier.fromNamespaceAndPath(to, path)));
        MOVED_RECIPES.forEach((path, to) -> {
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.RECIPE, id(path), Identifier.fromNamespaceAndPath(to, path));
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ADVANCEMENT, id("recipes/misc/" + path),
                    Identifier.fromNamespaceAndPath(to, "recipes/misc/" + path));
        });
    }

    private SimpleBeacons() {}
}
