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
 * (IG31-12) and are a required sub-mod of SimpleBuilding now. Stage 2 (docs/ai/PLAN-B-MACHINES02-2026-11-03.md): the piston upgrades
 * ({@link com.simplemachines.upgrade.PistonUpgrades}). Stage 3 (docs/ai/PLAN-B-MACHINES03-2026-11-03.md): the mod hoppers and hopper
 * carts ({@link com.simplemachines.hopper.Hoppers}). Stage 4 (docs/ai/PLAN-B-MACH32S4-2026-11-10.md): the Auto Smither
 * ({@link com.simplemachines.smither.Smithers}). Vanilla and SimpleLib ingredients, runs alone.
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

    /** Stage 2: the crafted piston upgrades' recipes (the breakers come from the hammer upgrade, no recipe). */
    public static final List<String> MOVED_PISTON_RECIPES = List.of("reinforced_piston", "reinforced_sticky_piston");

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
        // Their ways into the world (recipes, anvil crushing): the cracked diamond of the Reinforced Hopper and Piston recipes, the
        // netherite nugget of the hammer upgrades (SimpleLib registers both always).
        com.simplelib.api.SimpleLibApi.requestMaterial("cracked_diamond");
        com.simplelib.api.SimpleLibApi.requestMaterial("netherite_nugget");
        // The shipped upgrade files (data/simplemachines/in_world_upgrades) apply before datapack files of other namespaces,
        // so a modpack's "remove" takes a piston or hopper step out (like Simple Ovens).
        com.simplelib.api.InWorldUpgrades.registerDefaults(MOD_ID);
        // Switch off = the recipe goes with it (SimpleLib RecipeFilters, read live; /reload applies a changed config).
        com.simplelib.api.RecipeFilters.register(MOD_ID, p -> (!MachinesConfig.doublePiston && p.equals("double_piston"))
                || (!MachinesConfig.signalDampener && p.equals("signal_dampener")));
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "machines", "simplemachines.testcentre.station",
                List.of(MOD_ID + ":reinforced_piston", MOD_ID + ":reinforced_sticky_piston", MOD_ID + ":netherite_piston",
                        MOD_ID + ":enderite_piston", MOD_ID + ":double_piston", MOD_ID + ":signal_dampener",
                        MOD_ID + ":reinforced_hopper", MOD_ID + ":netherite_hopper", MOD_ID + ":enderite_hopper",
                        MOD_ID + ":reinforced_hopper_minecart", MOD_ID + ":netherite_hopper_minecart", MOD_ID + ":enderite_hopper_minecart",
                        MOD_ID + ":auto_smither", MOD_ID + ":guide_book"));
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
        // Stage 2 (03.11.): the piston upgrades leave SimpleBuilding; block states (facing, extended, wear, wear_step) are the same.
        for (String path : com.simplemachines.upgrade.PistonUpgrades.PISTONS) {
            move(Registries.ITEM, CORE_NAMESPACE, path);
            move(Registries.BLOCK, CORE_NAMESPACE, path);
        }
        for (String path : com.simplemachines.upgrade.PistonUpgrades.HEADS) move(Registries.BLOCK, CORE_NAMESPACE, path);
        for (String path : MOVED_PISTON_RECIPES) {
            move(Registries.RECIPE, CORE_NAMESPACE, path);
            move(Registries.ADVANCEMENT, CORE_NAMESPACE, "recipes/redstone/" + path);
        }
        // Stage 3 (03.11.): the mod hoppers and hopper carts; block states, block entity data (Items, FilterMode, TransferCooldown)
        // and cart data (Enabled, FilterMode) are the same, only the block entity type gets a shorter name.
        for (com.simplemachines.hopper.HopperTier tier : com.simplemachines.hopper.HopperTier.values()) {
            move(Registries.ITEM, CORE_NAMESPACE, tier.hopperPath());
            move(Registries.BLOCK, CORE_NAMESPACE, tier.hopperPath());
            move(Registries.ITEM, CORE_NAMESPACE, tier.cartPath());
            move(Registries.ENTITY_TYPE, CORE_NAMESPACE, tier.cartPath());
        }
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(CORE_NAMESPACE, com.simplemachines.hopper.Hoppers.OLD_BLOCK_ENTITY),
                id(com.simplemachines.hopper.Hoppers.BLOCK_ENTITY_ID));
        com.simplemachines.hopper.Hoppers.RECIPES.forEach((path, folder) -> {
            move(Registries.RECIPE, CORE_NAMESPACE, path);
            move(Registries.ADVANCEMENT, CORE_NAMESPACE, "recipes/" + folder + "/" + path);
        });
        // Stage 4 (10.11.): the Auto Smither; block state (facing, triggered, crafting) and block entity data (Items,
        // crafting_ticks_remaining) are the same, the block entity type keeps its path.
        String smither = com.simplemachines.smither.Smithers.ID;
        move(Registries.ITEM, CORE_NAMESPACE, smither);
        move(Registries.BLOCK, CORE_NAMESPACE, smither);
        move(Registries.BLOCK_ENTITY_TYPE, CORE_NAMESPACE, smither);
        move(Registries.RECIPE, CORE_NAMESPACE, smither);
        move(Registries.ADVANCEMENT, CORE_NAMESPACE, "recipes/" + com.simplemachines.smither.Smithers.RECIPE_FOLDER + "/" + smither);
    }

    private static void move(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String from, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(from, path), id(path));
    }

    private SimpleMachines() {}
}
