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
 * Astral Enchanter (new, End stage). Required sub-mod of SimpleBuilding, runs alone. Since N48 (14.11.2026) the building blocks the
 * tables stand on and around are this module's in every installation, no twins: Blazing and Nihil Obsidian, the blazewood planks,
 * the chiseled shelves ({@link EnchantingRegistry#ownBuildingBlocks()}), the blazewood family ({@link BlazewoodFamily}) and the
 * Blaze Book; SimpleBuilding's old ids move here. The plain Crimson/Warped blazewood bookshelves are SimpleLib's
 * ({@code simplelib:*_blazewood_bookshelf}, group {@code nether_bookshelves}, requested in {@link #init}). The loader entry calls
 * {@link #init} once.
 */
public final class SimpleEnchanting {
    public static final String MOD_ID = "simpleenchanting";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Enchanting");
    /** SimpleBuilding's namespace: the old table id and the owner of the building blocks. */
    public static final String CORE_NAMESPACE = "simplebuilding";
    /** The id the Dark Enchanter had in SimpleBuilding (block, item, block entity type). */
    public static final String OLD_TABLE = "astral_enchanting_table";
    /** The building blocks of the tables (full ids, tab order): this module's, and SimpleLib's plain Nether bookshelves. */
    public static final List<String> BUILDING_BLOCKS = List.of("simpleenchanting:blazing_obsidian", "simpleenchanting:nihil_obsidian",
            "simpleenchanting:crimson_blazewood_planks", "simpleenchanting:warped_blazewood_planks",
            "simplelib:crimson_blazewood_bookshelf", "simplelib:warped_blazewood_bookshelf",
            "simpleenchanting:chiseled_crimson_blazewood_bookshelf", "simpleenchanting:chiseled_warped_blazewood_bookshelf");
    /** Of those, the ids SimpleBuilding had: the moved paths of this module (block + item; the shelves go to SimpleLib). */
    public static final List<String> MOVED_OWN = List.of("blazing_obsidian", "nihil_obsidian", "crimson_blazewood_planks", "warped_blazewood_planks",
            "chiseled_crimson_blazewood_bookshelf", "chiseled_warped_blazewood_bookshelf");
    /** The plain Nether bookshelves, SimpleLib's (N48). */
    public static final List<String> SHELVES = List.of("crimson_blazewood_bookshelf", "warped_blazewood_bookshelf");
    /** Recipes SimpleBuilding had, now this module's (same ids, recipe book and advancement progress move; advancement folder). */
    public static final java.util.Map<String, String> MOVED_RECIPES = java.util.Map.ofEntries(
            java.util.Map.entry("crimson_blazewood_planks", "building_blocks"), java.util.Map.entry("warped_blazewood_planks", "building_blocks"),
            java.util.Map.entry("crimson_blazewood_bookshelf", "building_blocks"), java.util.Map.entry("warped_blazewood_bookshelf", "building_blocks"),
            java.util.Map.entry("chiseled_crimson_blazewood_bookshelf", "building_blocks"), java.util.Map.entry("chiseled_warped_blazewood_bookshelf", "building_blocks"),
            java.util.Map.entry("blazing_obsidian", "building_blocks"), java.util.Map.entry("nihil_obsidian", "building_blocks"),
            java.util.Map.entry("blaze_book", "misc"));
    /**
     * Items of other mods shown in this module's tab when installed (DW04C-3, by id, no class import): SimpleBuilding's Nihilit
     * Book, Simple End's End bookshelves (worth {@link EnchantingRules#BLAZE_SHELF} points, tag {@code end_shelves}).
     */
    public static final List<String> PARTNER_ITEMS = List.of("simplebuilding:nihilith_book",
            "simpleend:ashwood_bookshelf", "simpleend:duskwood_bookshelf", "simpleend:chiseled_ashwood_bookshelf",
            "simpleend:chiseled_duskwood_bookshelf");
    /** SimpleLib materials the tables are upgraded with (8 Netherite nuggets for the Dark Enchanter). */
    public static final List<String> MATERIALS = List.of("netherite_nugget", "nether_bookshelves");

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

    public static void init(Path configDir, boolean simpleBuildingLoaded) {
        simpleBuilding = simpleBuildingLoaded;
        EnchantingConfig.load(configDir);
        registerIdMoves();
        MATERIALS.forEach(SimpleLibApi::requestMaterial);
        // The shipped upgrade file (data/simpleenchanting/in_world_upgrades) applies before datapack files of other namespaces.
        InWorldUpgrades.registerDefaults(MOD_ID);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        List<String> items = new java.util.ArrayList<>(List.of(MOD_ID + ":dark_enchanter", MOD_ID + ":astral_enchanter"));
        items.addAll(BUILDING_BLOCKS);
        items.add(MOD_ID + ":blaze_book");
        // PLAN40 simpleenchanting-1: the blazewood family (always this module's ids).
        for (String path : BlazewoodFamily.paths()) items.add(MOD_ID + ":" + path);
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "enchanting", "simpleenchanting.testcentre.station", items);
    }

    /**
     * Old ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). The table: SimpleBuilding's
     * Astral Enchanting Table is the Dark Enchanter now (block, item; its block entity type is the shared {@code enchanter}).
     * N48: SimpleBuilding's Blazing/Nihil Obsidian, blazewood planks, chiseled shelves, Blaze Book and their recipes (recipe book,
     * advancements) are this module's; the plain Nether bookshelves (SimpleBuilding's, and this module's twin of them) are SimpleLib's.
     * SimpleBuilding's shared-material table moves the shelves too (the same edge twice is fine).
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        IdMoves.move(Registries.BLOCK, old(OLD_TABLE), id("dark_enchanter"));
        IdMoves.move(Registries.ITEM, old(OLD_TABLE), id("dark_enchanter"));
        IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(OLD_TABLE), id("enchanter"));
        for (String path : MOVED_OWN) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
        }
        IdMoves.move(Registries.ITEM, old("blaze_book"), id("blaze_book"));
        for (String path : SHELVES) {
            Identifier lib = Identifier.fromNamespaceAndPath("simplelib", path);
            for (var registry : List.of(Registries.BLOCK, Registries.ITEM)) {
                IdMoves.move(registry, old(path), lib);
                IdMoves.move(registry, id(path), lib);
            }
        }
        MOVED_RECIPES.forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(CORE_NAMESPACE, path);
    }

    private SimpleEnchanting() {}
}
