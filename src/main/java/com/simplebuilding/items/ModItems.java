package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BuildingBundleItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.ChiselItem.*;
import com.simplebuilding.items.custom.SpatulaItem;
import com.simplebuilding.items.custom.SpatulaItem.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.
 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.
 */
public class ModItems {
    // --- Durability Tiers (Angelehnt an Vanilla Tools) ---
    private static final int DURABILITY_WOOD_STONE = 32;
    private static final int DURABILITY_IRON = 64; // Erhöht
    private static final int DURABILITY_GOLD = 32;  // Erhöht
    private static final int DURABILITY_DIAMOND = 256;
    private static final int DURABILITY_NETHERITE = 512;

    private static final int COOLDOWN_TICKS_WOOD_STONE = 30;
    private static final int COOLDOWN_TICKS_IRON = 25;
    private static final int COOLDOWN_TICKS_GOLD = 20;
    private static final int COOLDOWN_TICKS_DIAMOND = 10;
    private static final int COOLDOWN_TICKS_NETHERITE = 5;

    private static Map<Block, Block> mergeMaps(Map<Block, Block> destination, Map<Block, Block> source) {
        Map<Block, Block> result = new HashMap<>(destination);
        result.putAll(source);
        return result;
    }

    private static final Map<Block, Block> CHISEL_MAP_STONE = Map.of(
            Blocks.STONE, Blocks.STONE_BRICKS,
            Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
            Blocks.SMOOTH_STONE, Blocks.STONE,
            Blocks.POLISHED_ANDESITE, Blocks.ANDESITE
    );
    private static final Map<Block, Block> CHISEL_MAP_IRON_BASE = Map.of(
            Blocks.IRON_BLOCK, Blocks.IRON_BARS,
            Blocks.QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK,
            Blocks.BRICKS, Blocks.CRACKED_STONE_BRICKS // Einfache Rissbildung
    );
    private static final Map<Block, Block> CHISEL_MAP_DIAMOND_BASE = Map.of(
            Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
            Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS,
            Blocks.BASALT, Blocks.POLISHED_BASALT
    );
    private static final Map<Block, Block> CHISEL_MAP_NETHERITE_BASE = Map.of(
            Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE,
            Blocks.DEEPSLATE, Blocks.DEEPSLATE_BRICKS,
            Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS,
            Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE

    );
    private static final Map<Block, Block> CHISEL_MAP_IRON = mergeMaps(CHISEL_MAP_STONE, CHISEL_MAP_IRON_BASE);
    private static final Map<Block, Block> CHISEL_MAP_DIAMOND = mergeMaps(CHISEL_MAP_IRON, CHISEL_MAP_DIAMOND_BASE);
    private static final Map<Block, Block> CHISEL_MAP_NETHERITE = mergeMaps(CHISEL_MAP_DIAMOND, CHISEL_MAP_NETHERITE_BASE);

    private static final Map<Block, Block> SPATULA_MAP_STONE = Map.of(
            Blocks.STONE_BRICKS, Blocks.STONE,
            Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS
    );
    private static final Map<Block, Block> SPATULA_MAP_IRON_BASE = Map.of(
            Blocks.IRON_BARS, Blocks.IRON_BLOCK,
            Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK,
            Blocks.POLISHED_ANDESITE, Blocks.ANDESITE
    );
    private static final Map<Block, Block> SPATULA_MAP_DIAMOND_BASE = Map.of(
            Blocks.CRYING_OBSIDIAN, Blocks.OBSIDIAN,
            Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE
    );
    private static final Map<Block, Block> SPATULA_MAP_NETHERITE_BASE = Map.of(
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BASALT, Blocks.BASALT
    );

    private static final Map<Block, Block> SPATULA_MAP_IRON = mergeMaps(SPATULA_MAP_STONE, SPATULA_MAP_IRON_BASE);
    private static final Map<Block, Block> SPATULA_MAP_DIAMOND = mergeMaps(SPATULA_MAP_IRON, SPATULA_MAP_DIAMOND_BASE);
    private static final Map<Block, Block> SPATULA_MAP_NETHERITE = mergeMaps(SPATULA_MAP_DIAMOND, SPATULA_MAP_NETHERITE_BASE);


    // --- 3. ITEM REGISTRIERUNGEN ---

    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, CHISEL_MAP_STONE, COOLDOWN_TICKS_WOOD_STONE);
    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, CHISEL_MAP_IRON, COOLDOWN_TICKS_IRON);
    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, CHISEL_MAP_IRON, COOLDOWN_TICKS_IRON);
    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, CHISEL_MAP_IRON, COOLDOWN_TICKS_GOLD);
    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, CHISEL_MAP_DIAMOND, COOLDOWN_TICKS_DIAMOND);
    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, CHISEL_MAP_NETHERITE, COOLDOWN_TICKS_NETHERITE);

    public static final SpatulaItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, SPATULA_MAP_STONE, COOLDOWN_TICKS_WOOD_STONE);
    public static final SpatulaItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, SPATULA_MAP_IRON, COOLDOWN_TICKS_IRON);
    public static final SpatulaItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, SPATULA_MAP_IRON, COOLDOWN_TICKS_IRON);
    public static final SpatulaItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, SPATULA_MAP_IRON, COOLDOWN_TICKS_GOLD);
    public static final SpatulaItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, SPATULA_MAP_DIAMOND, COOLDOWN_TICKS_DIAMOND);
    public static final SpatulaItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, SPATULA_MAP_NETHERITE, COOLDOWN_TICKS_NETHERITE);


    public static final Item BUILDING_BUNDLE = registerItem("building_bundle",settings -> new BuildingBundleItem(settings.maxCount(1)));


    // --- HILFSMETHODEN ---

    /**
     * Registriert und mappt eine ChiselItem Instanz.
     * @param cooldownTicks Die Dauer des Cooldowns (in Ticks).
     */
    private static ChiselItem registerChisel(String name, int maxDamage, Map<Block, Block> transformationMap, int cooldownTicks) {
        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(settings.maxDamage(maxDamage)));
        chisel.setTransformationMap(transformationMap);
        chisel.setCooldownTicks(cooldownTicks); // NEU: Cooldown setzen
        return chisel;
    }

    /**
     * Registriert und mappt eine SpatulaItem Instanz.
     * @param cooldownTicks Die Dauer des Cooldowns (in Ticks).
     */
    private static SpatulaItem registerSpatula(String name, int maxDamage, Map<Block, Block> transformationMap, int cooldownTicks) {
        SpatulaItem spatula = (SpatulaItem) registerItem(name, settings -> new SpatulaItem(settings.maxDamage(maxDamage)));
        spatula.setTransformationMap(transformationMap);
        spatula.setCooldownTicks(cooldownTicks); // NEU: Cooldown setzen
        return spatula;
    }


    /**
     * Hilfsmethode zur Registrierung eines Items unter Verwendung einer Funktion,
     * die die Item.Settings-Konfiguration anwendet.
     * @param name Der Bezeichner des Items.
     * @param function Die Funktion, die die Item.Settings verarbeitet und das Item erstellt.
     * @return Das registrierte Item.
     */
    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(Simplebuilding.MOD_ID, name),
                function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Simplebuilding.MOD_ID, name)))));
    }

    /**
     * Registriert alle Items des Mods.
     * Führt die eigentliche Registrierung durch und fügt die Items zu den entsprechenden Item Groups hinzu.
     */
    public static void registerModItems() {
        Simplebuilding.LOGGER.info("Registering Mod Items for " + Simplebuilding.MOD_ID);
    }

}