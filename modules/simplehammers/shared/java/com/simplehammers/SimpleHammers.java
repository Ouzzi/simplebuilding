package com.simplehammers;

import com.simplehammers.config.HammerConfig;
import com.simplelib.api.HammerActions;
import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
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
 * Simple Hammers (split stage 7, docs/ai/PLAN-S7-HAMMERS-2026-10-18.md): the seven sledgehammers, their engine (area
 * breaking, reshaping, carving octets, crushing diamond and quartz blocks, in-world upgrades strike by strike) and the
 * hammer enchantments Break Through, Override and Radius, moved out of SimpleBuilding. Other mods plug their hammer ways
 * in through SimpleLib's {@code HammerActions} and never import this mod. Loader entry points call {@link #init} once in
 * their constructor / initializer, before any registry event, so the id moves are known in time (Forge aliases).
 */
public final class SimpleHammers {
    public static final String MOD_ID = "simplehammers";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Hammers");
    /** The mod the hammers came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";
    /** Tiers in tab order; the item ids are {@code <tier>_sledgehammer}. */
    public static final List<String> TIERS = List.of("stone", "copper", "iron", "gold", "diamond", "netherite", "enderite");
    /** The hammer enchantments (data driven, {@code data/simplehammers/enchantment}). */
    public static final List<String> ENCHANTMENTS = List.of("break_through", "override", "radius");
    /** Recipes (recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>}. */
    public static final Map<String, String> RECIPES = Map.ofEntries(
            Map.entry("stone_sledgehammer", "tools"), Map.entry("copper_sledgehammer", "tools"),
            Map.entry("iron_sledgehammer", "tools"), Map.entry("gold_sledgehammer", "tools"),
            Map.entry("diamond_sledgehammer", "tools"), Map.entry("netherite_sledgehammer_smithing", "tools"),
            Map.entry("enderite_sledgehammer_smithing", "combat"),
            Map.entry("upgrade_copper_sledgehammer_to_iron_sledgehammer", "misc"),
            Map.entry("upgrade_iron_sledgehammer_to_gold_sledgehammer", "misc"),
            Map.entry("upgrade_gold_sledgehammer_to_diamond_sledgehammer", "misc"));
    /** SimpleLib materials the hammers are made from, crush into or upgrade with (their recipes and sources go on). */
    public static final List<String> MATERIALS = List.of("cracked_diamond", "netherite_nugget", "enderite", "upgrade_templates");
    /**
     * Owner rule F3 (PLAN-MODULFAMILIE §5): Enderite tiers of the sub-mods are unlocked only with Simple Enderite. That mod
     * does not exist yet, so the Enderite sledgehammer stays registered and craftable as before.
     * TODO(Simple Enderite, stage P4): set to true and gate the Enderite smithing recipe on {@code simpleenderite}.
     */
    public static final boolean ENDERITE_NEEDS_SIMPLE_ENDERITE = false;

    private static Predicate<String> loaded = id -> false;
    private static boolean movesRegistered;

    private SimpleHammers() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Hammers" (framework SubMods, set by SimpleBuilding) and the own config: off = a plain pickaxe. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && HammerConfig.enabled;
    }

    /** Whether another mod is loaded (the loader's mod list, set by {@link #init}). */
    public static boolean isLoaded(String modId) {
        return loaded.test(modId);
    }

    public static void init(Path configDir, Predicate<String> modLoaded) {
        loaded = modLoaded;
        HammerConfig.load(configDir);
        registerIdMoves();
        MATERIALS.forEach(SimpleLibApi::requestMaterial);
        // Principle 5a / split plan §2.6: with a hammer mod the in-world ways run with the hammer only, the Vanilla axe ways go off.
        SimpleLibApi.disableAxeWays();
        HammerActions.setUpgradeTakesClick(com.simplehammers.engine.SledgehammerUpgrades::shouldSkipBlockUse);
        // Test centre station (framework contract, no import of the test centre): the hammers and the guide by id.
        List<String> items = new ArrayList<>();
        TIERS.forEach(tier -> items.add(MOD_ID + ":" + tier + "_sledgehammer"));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "hammers", "simplehammers.testcentre.station", items);
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String tier : TIERS) {
            IdMoves.move(Registries.ITEM, old(tier + "_sledgehammer"), id(tier + "_sledgehammer"));
        }
        for (String enchantment : ENCHANTMENTS) {
            IdMoves.move(Registries.ENCHANTMENT, old(enchantment), id(enchantment));
        }
        RECIPES.forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
    }

    /** Whether in-world upgrades are SimpleLib's own load (no partner like SimpleBuilding reads them). */
    public static boolean libLoadsUpgrades() {
        return !InWorldUpgrades.isLoadedByPartner();
    }
}
