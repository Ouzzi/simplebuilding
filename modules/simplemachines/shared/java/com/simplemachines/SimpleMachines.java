package com.simplemachines;

import com.simplelib.api.IdMoves;
import com.simplemachines.config.MachinesConfig;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Machines (MACH32-MOD, docs/ai/PLAN-B-MACHINES01-2026-11-01.md): the machine parts of the Simple Machines tab. Stage 1: the
 * Double Piston (one block, two stages) and the Signal Dampener, which lived in the module Simple Redstone (WP-17), then in the core
 * (IG31-12) and are a required sub-mod of SimpleBuilding now. Vanilla ingredients only, runs alone.
 * The loader entry point calls {@link #init} once with its config directory before it registers anything.
 */
public final class SimpleMachines {
    public static final String MOD_ID = "simplemachines";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Machines");

    /** The namespaces the parts came from, in the order they were held: Simple Redstone (WP-17), then SimpleBuilding itself (IG31-12). */
    public static final String REDSTONE_NAMESPACE = "simpleredstone";
    public static final String CORE_NAMESPACE = "simplebuilding";
    /** Items (and recipes, recipe advancements under recipes/redstone/) that moved. */
    public static final List<String> MOVED_ITEMS = List.of("double_piston", "signal_dampener");
    /** Blocks that moved under the same path. */
    public static final List<String> MOVED_BLOCKS = List.of("double_piston", "double_piston_head", "double_piston_arm", "signal_dampener");

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Machines" (framework SubMods, set by SimpleBuilding): off = no tab entries, no guide. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Feature switch {@code features.doublePiston}: off = the Double Piston stays put (placed blocks keep standing). */
    public static boolean doublePistonOn() {
        return on() && MachinesConfig.doublePiston;
    }

    /** Feature switch {@code features.signalDampener}: off = the Dampener outputs nothing. */
    public static boolean signalDampenerOn() {
        return on() && MachinesConfig.signalDampener;
    }

    public static void init(Path configDir) {
        MachinesConfig.load(configDir);
        registerIdMoves();
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "machines", "simplemachines.testcentre.station",
                List.of(MOD_ID + ":double_piston", MOD_ID + ":signal_dampener", MOD_ID + ":guide_book"));
    }

    /**
     * Old ids resolve to the new ones (SimpleLib {@code IdMoves}; the next save writes the new id): {@code simplebuilding:} (31.10.
     * to 01.11.) and {@code simpleredstone:} (before 31.10.) straight to {@code simplemachines:}, one edge per old id. The 1x2 barrel
     * of the oldest piston no longer exists and loads as air.
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String from : List.of(CORE_NAMESPACE, REDSTONE_NAMESPACE)) {
            for (String path : MOVED_ITEMS) {
                move(Registries.ITEM, from, path);
                move(Registries.RECIPE, from, path);
                move(Registries.ADVANCEMENT, from, "recipes/redstone/" + path);
            }
            for (String path : MOVED_BLOCKS) move(Registries.BLOCK, from, path);
        }
        IdMoves.move(Registries.BLOCK, Identifier.fromNamespaceAndPath(REDSTONE_NAMESPACE, "double_piston_barrel"), Identifier.withDefaultNamespace("air"));
        // The old module's own handbook comes back as this module's handbook (IG31-12 had turned it into SimpleBuilding's Machines book).
        move(Registries.ITEM, REDSTONE_NAMESPACE, "guide_book");
        move(Registries.RECIPE, REDSTONE_NAMESPACE, "guide_book");
        move(Registries.ADVANCEMENT, REDSTONE_NAMESPACE, "recipes/misc/guide_book");
    }

    private static void move(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String from, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(from, path), id(path));
    }

    private SimpleMachines() {}
}
