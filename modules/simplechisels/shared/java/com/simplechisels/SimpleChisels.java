package com.simplechisels;

import com.simplechisels.config.ChiselConfig;
import com.simplelib.api.IdMoves;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Chisels (WE25-7, docs/ai/PLAN-B-CHISELS07-2026-11-07.md): the seven chisels that move a block one step along its
 * family (sneak: one step back), their datapack transformation tables and the enchantment Fast Chiseling, moved out of
 * SimpleBuilding. Other mods add chains through {@code data/<namespace>/chisel_transformations/} files and plug their
 * chisel ways in through SimpleLib's {@code ChiselActions}; nobody imports this mod. Loader entry points call {@link #init}
 * once, before any registry event, so the id moves are known in time.
 */
public final class SimpleChisels {
    public static final String MOD_ID = "simplechisels";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Chisels");
    /** The mod the chisels came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";
    /** The six spatulas SimpleBuilding renamed into chisels in 2026-09 (old saves may still hold them). */
    public static final List<String> SPATULA_TIERS = List.of("stone", "copper", "iron", "gold", "diamond", "netherite");
    /** The chisel enchantment (data driven, {@code data/simplechisels/enchantment}). */
    public static final String FAST_CHISELING = "fast_chiseling";
    /** Recipes (recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>}. */
    public static final Map<String, String> RECIPES = Map.ofEntries(
            Map.entry("stone_chisel", "tools"), Map.entry("copper_chisel", "tools"), Map.entry("iron_chisel", "tools"),
            Map.entry("gold_chisel", "tools"), Map.entry("diamond_chisel", "tools"), Map.entry("netherite_chisel_smithing", "tools"),
            Map.entry("enderite_chisel_smithing", "combat"),
            Map.entry("upgrade_copper_chisel_to_iron_chisel", "misc"),
            Map.entry("upgrade_iron_chisel_to_gold_chisel", "misc"),
            Map.entry("upgrade_gold_chisel_to_diamond_chisel", "misc"));

    private static Predicate<String> loaded = id -> false;
    private static boolean movesRegistered;

    private SimpleChisels() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Chisels" (framework SubMods, set by SimpleBuilding) and the own config: off = no reshaping. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && ChiselConfig.enabled;
    }

    /** Whether another mod is loaded (the loader's mod list, set by {@link #init}). */
    public static boolean isLoaded(String modId) {
        return loaded.test(modId);
    }

    public static void init(Path configDir, Predicate<String> modLoaded) {
        loaded = modLoaded;
        ChiselConfig.load(configDir);
        // CMD24: /simplechisels config for every option.
        com.simplelib.api.config.ConfigCommands.server(MOD_ID).json(ChiselConfig::toJson, ChiselConfig::apply)
                .file(configDir, ChiselConfig.FILE).register();
        registerIdMoves();
        // The tables of every chisel for partners (SimpleBuilding's wiki export, recipe viewer, test centre).
        com.simplelib.api.ChiselActions.publishTools(com.simplechisels.registry.ChiselRegistry::publishedTools);
        com.simplechisels.compat.ChiselsViewer.register();
        // Test centre station (framework contract, no import of the test centre): the chisels and the guide by id.
        List<String> items = new ArrayList<>();
        for (ChiselTier tier : ChiselTier.values()) items.add(MOD_ID + ":" + tier.id());
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "chisels", "simplechisels.testcentre.station", items);
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (ChiselTier tier : ChiselTier.values()) {
            IdMoves.move(Registries.ITEM, old(tier.id()), id(tier.id()));
        }
        // The legacy spatulas: SimpleBuilding turned them into the chisel of their tier on join; the move does it everywhere.
        for (String tier : SPATULA_TIERS) {
            IdMoves.move(Registries.ITEM, old(tier + "_spatula"), id(tier + "_chisel"));
        }
        IdMoves.move(Registries.ENCHANTMENT, old(FAST_CHISELING), id(FAST_CHISELING));
        RECIPES.forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
    }
}
