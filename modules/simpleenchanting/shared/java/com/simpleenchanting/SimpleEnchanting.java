package com.simpleenchanting;

import com.simpleenchanting.config.EnchantingConfig;
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
 * Simple Enchanting (ENCH01/ENCH02, docs/ai/PLAN-B-ENCH02-2026-11-03.md): the two enchanters above Vanilla's table. The
 * Dark Enchanter (until 03.11. SimpleBuilding's Astral Enchanting Table, {@code simplebuilding:astral_enchanting_table}) and the
 * Astral Enchanter (new, End stage). Required sub-mod of SimpleBuilding, runs alone: SimpleBuilding owns the building blocks the
 * tables stand on and around (Blazing and Nihil Obsidian, blazewood planks and bookshelves); without SimpleBuilding this module
 * registers its own twins of them ({@link EnchantingRegistry#ownBuildingBlocks()}). The loader entry calls {@link #init} once.
 */
public final class SimpleEnchanting {
    public static final String MOD_ID = "simpleenchanting";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Enchanting");
    /** SimpleBuilding's namespace: the old table id and the owner of the building blocks. */
    public static final String CORE_NAMESPACE = "simplebuilding";
    /** The id the Dark Enchanter had in SimpleBuilding (block, item, block entity type). */
    public static final String OLD_TABLE = "astral_enchanting_table";
    /** Building blocks SimpleBuilding owns; this module registers twins of them only when SimpleBuilding is not installed. */
    public static final List<String> BUILDING_BLOCKS = List.of("blazing_obsidian", "nihil_obsidian", "crimson_blazewood_planks",
            "warped_blazewood_planks", "crimson_blazewood_bookshelf", "warped_blazewood_bookshelf", "chiseled_crimson_blazewood_bookshelf",
            "chiseled_warped_blazewood_bookshelf");
    /**
     * Items of other mods shown in this module's tab when installed (DW04C-3, by id, no class import): SimpleBuilding's Blaze and
     * Nihilit Books, Simple End's End bookshelves (worth {@link EnchantingRules#BLAZE_SHELF} points, tag {@code end_shelves}).
     */
    public static final List<String> PARTNER_ITEMS = List.of("simplebuilding:blaze_book", "simplebuilding:nihilith_book",
            "simpleend:ashwood_bookshelf", "simpleend:duskwood_bookshelf", "simpleend:chiseled_ashwood_bookshelf",
            "simpleend:chiseled_duskwood_bookshelf");
    /** SimpleLib materials the tables are upgraded with (8 Netherite nuggets for the Dark Enchanter). */
    public static final List<String> MATERIALS = List.of("netherite_nugget");

    private static boolean movesRegistered;
    private static boolean simpleBuilding;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Enchanting" (framework SubMods, set by SimpleBuilding): off = no tab entries, no guide. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** Whether SimpleBuilding is installed (set by the loader entry before anything registers). */
    public static boolean simpleBuilding() {
        return simpleBuilding;
    }

    /** Namespace of the building blocks in this installation: SimpleBuilding's, or this module's twins without it. */
    public static String buildingNamespace() {
        return simpleBuilding ? CORE_NAMESPACE : MOD_ID;
    }

    public static void init(Path configDir, boolean simpleBuildingLoaded) {
        simpleBuilding = simpleBuildingLoaded;
        EnchantingConfig.load(configDir);
        registerIdMoves();
        MATERIALS.forEach(SimpleLibApi::requestMaterial);
        // The shipped upgrade file (data/simpleenchanting/in_world_upgrades) applies before datapack files of other namespaces.
        InWorldUpgrades.registerDefaults(MOD_ID);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new java.util.ArrayList<>(List.of(MOD_ID + ":dark_enchanter", MOD_ID + ":astral_enchanter"));
        for (String path : BUILDING_BLOCKS) items.add(buildingNamespace() + ":" + path);
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "enchanting", "simpleenchanting.testcentre.station", items);
    }

    /**
     * Old ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). The table: SimpleBuilding's
     * Astral Enchanting Table is the Dark Enchanter now (block, item; its block entity type is the shared {@code enchanter}).
     * The building blocks move with the installation: with SimpleBuilding this module's twins become SimpleBuilding's blocks
     * (a world played with Simple Enchanting alone keeps them), without SimpleBuilding its blocks become the twins.
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        IdMoves.move(Registries.BLOCK, old(OLD_TABLE), id("dark_enchanter"));
        IdMoves.move(Registries.ITEM, old(OLD_TABLE), id("dark_enchanter"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(OLD_TABLE), id("enchanter"));
        for (String path : BUILDING_BLOCKS) {
            Identifier sb = old(path), own = id(path);
            if (simpleBuilding) {
                IdMoves.move(Registries.BLOCK, own, sb);
                IdMoves.move(Registries.ITEM, own, sb);
            } else {
                IdMoves.move(Registries.BLOCK, sb, own);
                IdMoves.move(Registries.ITEM, sb, own);
            }
        }
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(CORE_NAMESPACE, path);
    }

    private SimpleEnchanting() {}
}
