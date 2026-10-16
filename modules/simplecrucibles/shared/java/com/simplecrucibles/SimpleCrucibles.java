package com.simplecrucibles;

import com.simplecrucibles.config.CrucibleConfig;
import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Crucibles (split plan stage 5, docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md): the iron, reinforced, netherite and
 * Enderite crucibles, the copper/reinforced/netherite/Enderite barrels that dock onto them, the reinforced cauldron and
 * the village field kitchen, moved out of SimpleLib (and, for the Enderite stage, out of SimpleBuilding). Warm food stays
 * in SimpleLib. Loader entry points call {@link #init} once in their constructor / initializer, before any registry
 * event, so the id moves are known in time (Forge aliases).
 */
public final class SimpleCrucibles {
    public static final String MOD_ID = "simplecrucibles";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Crucibles");
    /** The mods the blocks came from; old worlds hold their ids. */
    public static final String LIB_NAMESPACE = "simplelib";
    public static final String SB_NAMESPACE = "simplebuilding";

    /** Blocks that were SimpleLib's (block and item share the path; the blank has no item). */
    public static final List<String> MOVED_FROM_LIB = List.of("iron_crucible", "reinforced_crucible", "netherite_crucible",
            "copper_barrel", "reinforced_barrel", "netherite_barrel", "reinforced_cauldron");
    /** The Enderite stage, SimpleBuilding's before this stage. */
    public static final List<String> MOVED_FROM_SB = List.of("enderite_crucible", "enderite_barrel");
    /** SimpleLib materials the Enderite stage and the upgrades are built with (their recipes and sources go on). */
    public static final List<String> MATERIALS = List.of("cracked_diamond", "netherite_nugget", "enderite", "upgrade_templates");

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier lib(String path) {
        return Identifier.fromNamespaceAndPath(LIB_NAMESPACE, path);
    }

    public static Identifier sb(String path) {
        return Identifier.fromNamespaceAndPath(SB_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Crucibles" (framework SubMods, set by SimpleBuilding); off = no guide book in the tabs. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    public static void init(Path configDir) {
        CrucibleConfig.load(configDir);
        registerIdMoves();
        MATERIALS.forEach(SimpleLibApi::requestMaterial);
        // The shipped upgrade file (data/simplecrucibles/in_world_upgrades) applies before datapack files of other namespaces.
        InWorldUpgrades.registerDefaults(MOD_ID);
        // The Vanilla axe ways (build, attach, upgrade) take the click instead of the crucible/barrel menu (principle 5a).
        SimpleLibApi.registerToolUse(com.simplecrucibles.crucible.AxeWays::wants);
        // In-world upgrades into a crucible or barrel tier move contents, progress and experience (InWorldUpgrades).
        InWorldUpgrades.registerHandler(com.simplecrucibles.api.SimpleCruciblesApi::upgradesInPlace, (level, pos, to) -> {
            com.simplecrucibles.crucible.CrucibleUpgrades.upgradeInPlace(level, pos, to);
            return 1;
        });
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new java.util.ArrayList<>();
        MOVED_FROM_LIB.forEach(path -> items.add(MOD_ID + ":" + path));
        MOVED_FROM_SB.forEach(path -> items.add(MOD_ID + ":" + path));
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "crucibles", "simplecrucibles.testcentre.station", items);
    }

    /** Old {@code simplelib:} / {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : MOVED_FROM_LIB) {
            IdMoves.move(Registries.BLOCK, lib(path), id(path));
            IdMoves.move(Registries.ITEM, lib(path), id(path));
        }
        IdMoves.move(Registries.BLOCK, lib("crucible_blank"), id("crucible_blank"));
        for (String path : MOVED_FROM_SB) {
            IdMoves.move(Registries.BLOCK, sb(path), id(path));
            IdMoves.move(Registries.ITEM, sb(path), id(path));
        }
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, lib("crucible"), id("crucible"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, lib("barrel"), id("barrel"));
        IdMoves.move(Registries.RECIPE, lib("copper_barrel"), id("copper_barrel"));
        IdMoves.move(Registries.ADVANCEMENT, lib("recipes/misc/copper_barrel"), id("recipes/misc/copper_barrel"));
    }

    private SimpleCrucibles() {}
}
