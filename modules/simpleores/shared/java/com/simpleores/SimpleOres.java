package com.simpleores;

import com.simpleores.config.OresConfig;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
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
        moveBlossomGold();
        OresRegistry.RECIPES_FROM_SIMPLELIB.forEach((path, folder) -> {
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.RECIPE, lib(path), id(path));
            com.simplelib.api.IdMoves.move(net.minecraft.core.registries.Registries.ADVANCEMENT, lib("recipes/" + folder + "/" + path),
                    id("recipes/" + folder + "/" + path));
        });
    }

    /**
     * 05.11.: "Blossom Gold" became "Rose Gold" ({@code blossom_gold_*} to {@code rose_gold_*}); items, block, recipes (recipe book) and
     * recipe advancements (progress) of old worlds move along. Equipment assets and tags are data of this mod and need no move.
     */
    private static void moveBlossomGold() {
        for (String part : List.of("ingot", "pressling", "block", "sword", "helmet", "chestplate", "leggings", "boots", "pickaxe", "axe", "shovel", "hoe", "apple")) {
            moveId(Registries.ITEM, "blossom_gold_" + part, "rose_gold_" + part);
        }
        moveId(Registries.ITEM, "enchanted_blossom_gold_apple", "enchanted_rose_gold_apple");
        moveId(Registries.BLOCK, "blossom_gold_block", "rose_gold_block");
        // recipe name -> folder of its advancement
        Map<String, String> recipes = new LinkedHashMap<>();
        recipes.put("block", "building_blocks");
        for (String part : List.of("sword", "helmet", "chestplate", "leggings", "boots")) recipes.put(part, "combat");
        for (String part : List.of("pickaxe", "axe", "shovel", "hoe")) recipes.put(part, "tools");
        recipes.put("apple", "food");
        recipes.forEach((part, folder) -> moveRecipe("blossom_gold_" + part, "rose_gold_" + part, folder));
        moveRecipe("enchanted_blossom_gold_apple", "enchanted_rose_gold_apple", "food");
        moveRecipe("blossom_gold_pressling_a", "rose_gold_pressling_a", "misc");
        moveRecipe("blossom_gold_ingot_from_blossom_gold_block", "rose_gold_ingot_from_rose_gold_block", "misc");
        moveRecipe("blossom_gold_ingot_from_blasting_blossom_gold_pressling", "rose_gold_ingot_from_blasting_rose_gold_pressling", "misc");
    }

    private static void moveId(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registry, String from, String to) {
        com.simplelib.api.IdMoves.move(registry, id(from), id(to));
    }

    private static void moveRecipe(String from, String to, String folder) {
        com.simplelib.api.IdMoves.move(Registries.RECIPE, id(from), id(to));
        com.simplelib.api.IdMoves.move(Registries.ADVANCEMENT, id("recipes/" + folder + "/" + from), id("recipes/" + folder + "/" + to));
    }

    private static boolean movesRegistered;

    private static Identifier lib(String path) {
        return Identifier.fromNamespaceAndPath("simplelib", path);
    }

    private SimpleOres() {}
}
