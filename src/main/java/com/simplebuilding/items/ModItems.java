package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.RangefinderItem;
import com.simplebuilding.items.custom.SpatulaItem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.simplebuilding.items.custom.BuildingWandItem.*;
import static com.simplebuilding.items.custom.ChiselItem.*;
import static com.simplebuilding.items.custom.RangefinderItem.DURABILITY_MULTIPLAYER_RANGEFINDER;

/**
 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.
 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.
 */
public class ModItems {
    // --- Durability Tiers (Angelehnt an Vanilla Tools) ---
    private static final int DURABILITY_WOOD_STONE = 48;
    private static final int DURABILITY_IRON = 64; // Erhöht
    private static final int DURABILITY_GOLD = 32;  // Erhöht
    private static final int DURABILITY_DIAMOND = 98;
    private static final int DURABILITY_NETHERITE = 128;


    private static final int COOLDOWN_TICKS_WOOD_STONE = 30;
    private static final int COOLDOWN_TICKS_IRON = 25;
    private static final int COOLDOWN_TICKS_GOLD = 20;
    private static final int COOLDOWN_TICKS_DIAMOND = 10;
    private static final int COOLDOWN_TICKS_NETHERITE = 5;

    private static final int ENCHANTABILITY_WOOD_STONE = 15;
    private static final int ENCHANTABILITY_COPPER = 18;
    private static final int ENCHANTABILITY_IRON = 14;
    private static final int ENCHANTABILITY_GOLD = 22;
    private static final int ENCHANTABILITY_DIAMOND = 10;
    private static final int ENCHANTABILITY_NETHERITE = 15;


    // =================================================================================
    // 4. ITEM REGISTRIERUNGEN
    // =================================================================================

    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, CHISEL_MAP_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE);
    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, CHISEL_MAP_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER);
    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, CHISEL_MAP_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON);
    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, CHISEL_MAP_IRON, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD);
    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, CHISEL_MAP_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, CHISEL_MAP_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE);

    public static final SpatulaItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, SPATULA_MAP_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE);
    public static final SpatulaItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, SPATULA_MAP_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER);
    public static final SpatulaItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, SPATULA_MAP_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON);
    public static final SpatulaItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, SPATULA_MAP_IRON, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD);
    public static final SpatulaItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, SPATULA_MAP_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final SpatulaItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, SPATULA_MAP_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE);

    public static final Item COPPER_BUILDING_CORE = registerItem("copper_building_core",settings -> new Item(settings.maxCount(16)));
    public static final Item IRON_BUILDING_CORE = registerItem("iron_building_core",settings -> new Item(settings.maxCount(16)));
    public static final Item GOLD_BUILDING_CORE = registerItem("gold_building_core",settings -> new Item(settings.maxCount(16)));
    public static final Item DIAMOND_BUILDING_CORE = registerItem("diamond_building_core",settings -> new Item(settings.maxCount(16)));
    public static final Item NETHERITE_BUILDING_CORE = registerItem("netherite_building_core",settings -> new Item(settings.maxCount(16)));

    public static final BuildingWandItem COPPER_BUILDING_WAND = registerBuildingWand("copper_building_wand", DURABILITY_WOOD_STONE* DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_COPPER, ENCHANTABILITY_COPPER);
    public static final BuildingWandItem IRON_BUILDING_WAND = registerBuildingWand("iron_building_wand", DURABILITY_IRON* DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_IRON, ENCHANTABILITY_IRON);
    public static final BuildingWandItem GOLD_BUILDING_WAND = registerBuildingWand("gold_building_wand", DURABILITY_GOLD* DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_GOLD, ENCHANTABILITY_GOLD);
    public static final BuildingWandItem DIAMOND_BUILDING_WAND = registerBuildingWand("diamond_building_wand", DURABILITY_DIAMOND* DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final BuildingWandItem NETHERITE_BUILDING_WAND = registerBuildingWand("netherite_building_wand", DURABILITY_NETHERITE* DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_NETHERITE, ENCHANTABILITY_NETHERITE);

    public static final RangefinderItem RANGEFINDER_ITEM = (RangefinderItem) registerItem("rangefinder",
            settings -> new RangefinderItem(settings.maxDamage(DURABILITY_NETHERITE * DURABILITY_MULTIPLAYER_RANGEFINDER).enchantable(ENCHANTABILITY_NETHERITE), null));

    // 2. Map für die farbigen Rangefinder (für schnellen Zugriff)
    public static final Map<DyeColor, RangefinderItem> COLORED_RANGEFINDERS = new HashMap<>();

    // --- HILFSMETHODEN ---

    /**
     * Registriert und mappt eine ChiselItem Instanz.
     * @param cooldownTicks Die Dauer des Cooldowns (in Ticks).
     */
    private static ChiselItem registerChisel(String name, int maxDamage, Map<Block, Block> transformationMap, int cooldownTicks, int enchantability) {
        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(settings.maxDamage(maxDamage).enchantable(enchantability)));
        chisel.setTransformationMap(transformationMap);
        chisel.setCooldownTicks(cooldownTicks); // NEU: Cooldown setzen
        return chisel;
    }

    /**
     * Registriert und mappt eine SpatulaItem Instanz.
     * @param cooldownTicks Die Dauer des Cooldowns (in Ticks).
     */
    private static SpatulaItem registerSpatula(String name, int maxDamage, Map<Block, Block> transformationMap, int cooldownTicks, int enchantability) {
        SpatulaItem spatula = (SpatulaItem) registerItem(name, settings -> new SpatulaItem(settings.maxDamage(maxDamage).enchantable(enchantability)));
        spatula.setTransformationMap(transformationMap);
        spatula.setCooldownTicks(cooldownTicks); // NEU: Cooldown setzen
        return spatula;
    }

    
    private static BuildingWandItem registerBuildingWand(String name, int maxDamage, int wandSquareDiameter, int enchantability) {
        BuildingWandItem wand = (BuildingWandItem) registerItem(name, settings -> new BuildingWandItem(settings.maxDamage(maxDamage).enchantable(enchantability)));
        wand.setWandSquareDiameter(wandSquareDiameter);
        return wand;
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

        for (DyeColor color : DyeColor.values()) {
            // FIX: Verwende color.getId() (oder color.asString())
            String name = "rangefinder_" + color.getId();

            // Registrieren mit der jeweiligen Farbe im Konstruktor
            RangefinderItem coloredItem = (RangefinderItem) registerItem(name,
                    settings -> new RangefinderItem(settings.maxDamage(DURABILITY_NETHERITE * DURABILITY_MULTIPLAYER_RANGEFINDER).enchantable(ENCHANTABILITY_NETHERITE), color));

            // In die Map packen (optional, aber nützlich für ItemGroups)
            COLORED_RANGEFINDERS.put(color, coloredItem);
        }
    }

}