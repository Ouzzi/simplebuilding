package com.simpleovens;

import com.simplelib.api.IdMoves;
import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simpleovens.config.OvensConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Ovens (split plan stage 4, docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md): the Reinforced, Netherite and Enderite
 * furnace, smoker and blast furnace and the three furnace minecarts, moved out of SimpleBuilding. Loader entry points
 * call {@link #init} once in their constructor / initializer, before any registry event, so the id moves out of
 * SimpleBuilding are known in time (Forge aliases).
 */
public final class SimpleOvens {
    public static final String MOD_ID = "simpleovens";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Ovens");
    /** The mod the furnaces came from; old worlds hold its ids. */
    public static final String OLD_NAMESPACE = "simplebuilding";

    /** Blocks and their items that moved (paths unchanged). */
    public static final List<String> MOVED_BLOCKS = List.of("reinforced_furnace", "netherite_furnace", "enderite_furnace",
            "reinforced_smoker", "netherite_smoker", "enderite_smoker",
            "reinforced_blast_furnace", "netherite_blast_furnace", "enderite_blast_furnace");
    /** Furnace minecarts: entity types and items (paths unchanged). */
    public static final List<String> MOVED_CARTS = List.of("reinforced_furnace_minecart", "netherite_furnace_minecart", "enderite_furnace_minecart");
    /** Block entity types: SimpleBuilding's path -> the path here. */
    public static final Map<String, String> MOVED_BLOCK_ENTITIES = Map.of("mod_furnace", "furnace", "mod_smoker", "smoker",
            "mod_blast_furnace", "blast_furnace");
    /** Recipes (recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>}. */
    public static final Map<String, String> MOVED_RECIPES = Map.of("reinforced_furnace", "redstone", "reinforced_smoker", "redstone",
            "reinforced_blast_furnace", "redstone", "reinforced_furnace_minecart", "transportation",
            "netherite_furnace_minecart", "transportation", "netherite_furnace_minecart_smithing", "transportation",
            "enderite_furnace_minecart", "transportation", "enderite_furnace_minecart_smithing", "transportation");
    /** SimpleLib materials the furnaces are built and upgraded with (their recipes and sources go on). */
    public static final List<String> MATERIALS = List.of("cracked_diamond", "netherite_nugget", "enderite", "upgrade_templates");

    private static boolean movesRegistered;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(OLD_NAMESPACE, path);
    }

    /** Super-mod switch "Enable Simple Ovens" (framework SubMods, set by SimpleBuilding) and the own config: off = inert. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID) && OvensConfig.enabled;
    }

    public static void init(Path configDir) {
        OvensConfig.load(configDir);
        registerIdMoves();
        MATERIALS.forEach(SimpleLibApi::requestMaterial);
        // The shipped upgrade file (data/simpleovens/in_world_upgrades) applies before datapack files of other namespaces.
        InWorldUpgrades.registerDefaults(MOD_ID);
        // Test centre station (framework contract, no import of the test centre): the module's items by id.
        java.util.List<String> items = new java.util.ArrayList<>();
        MOVED_BLOCKS.forEach(path -> items.add(MOD_ID + ":" + path));
        MOVED_CARTS.forEach(path -> items.add(MOD_ID + ":" + path));
        items.add(MOD_ID + ":guide_book");
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "ovens", "simpleovens.testcentre.station", items);
    }

    /** Old {@code simplebuilding:} ids resolve to the new ones (SimpleLib {@link IdMoves}; the next save writes the new id). */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : MOVED_BLOCKS) {
            IdMoves.move(Registries.BLOCK, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
        }
        for (String path : MOVED_CARTS) {
            IdMoves.move(Registries.ENTITY_TYPE, old(path), id(path));
            IdMoves.move(Registries.ITEM, old(path), id(path));
        }
        MOVED_BLOCK_ENTITIES.forEach((from, to) -> IdMoves.move(Registries.BLOCK_ENTITY_TYPE, old(from), id(to)));
        MOVED_RECIPES.forEach((path, folder) -> {
            IdMoves.move(Registries.RECIPE, old(path), id(path));
            IdMoves.move(Registries.ADVANCEMENT, old("recipes/" + folder + "/" + path), id("recipes/" + folder + "/" + path));
        });
    }

    private SimpleOvens() {}
}
