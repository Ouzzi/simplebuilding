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
        // IG31-13: Simple Ores owns Enderite now - other mods switch their Enderite variants on through SimpleLib's flag.
        com.simplelib.api.SimpleLibApi.provideMaterial("enderite");
        registerIdMoves();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "ores", "simpleores.testcentre.station", OresRegistry.itemIds());
    }

    /**
     * IG31-13: the Enderite materials were SimpleLib's ({@code simplelib:*}); items, the block, their recipes (recipe book)
     * and recipe advancements (progress) move here. SimpleBuilding moves its own older ids (Enderite, Sage Ore, Dimensional
     * Scrap) itself. Idempotent per id (the table refuses a second, different target only).
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : OresRegistry.FROM_SIMPLELIB) {
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ITEM, lib(path), id(path));
        }
        com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.BLOCK, lib("enderite_block"), id("enderite_block"));
        OresRegistry.RECIPES_FROM_SIMPLELIB.forEach((path, folder) -> {
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.RECIPE, lib(path), id(path));
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ADVANCEMENT, lib("recipes/" + folder + "/" + path),
                    id("recipes/" + folder + "/" + path));
        });
    }

    private static boolean movesRegistered;

    private static Identifier lib(String path) {
        return Identifier.fromNamespaceAndPath("simplelib", path);
    }

    private SimpleOres() {}
}
