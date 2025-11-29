package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BuildingWandItem;
import com.simplebuilding.items.custom.ChiselItem;
import com.simplebuilding.items.custom.RangefinderItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.simplebuilding.items.custom.BuildingWandItem.*;
import static com.simplebuilding.items.custom.RangefinderItem.DURABILITY_RANGEFINDER;

/**
 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.
 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.
 */
public class ModItems {

    // --- Durability & Settings Constants ---
    private static final int DURABILITY_WOOD_STONE = 48;
    private static final int DURABILITY_IRON = 64;
    private static final int DURABILITY_GOLD = 32;
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
    // ITEM REGISTRIERUNGEN
    // =================================================================================

    // Chisels (Forward)
    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, "stone");
    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, "copper");
    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, "iron");
    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, "gold");
    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, "diamond");
    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, "netherite");

    // Spatulas (Backward)
    public static final ChiselItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, "stone");
    public static final ChiselItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, "copper");
    public static final ChiselItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, "iron");
    public static final ChiselItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, "gold");
    public static final ChiselItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, "diamond");
    public static final ChiselItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, "netherite");

    // Building Cores
    public static final Item COPPER_BUILDING_CORE = registerItem("copper_building_core", s -> new Item(s.maxCount(16)));
    public static final Item IRON_BUILDING_CORE = registerItem("iron_building_core", s -> new Item(s.maxCount(16)));
    public static final Item GOLD_BUILDING_CORE = registerItem("gold_building_core", s -> new Item(s.maxCount(16)));
    public static final Item DIAMOND_BUILDING_CORE = registerItem("diamond_building_core", s -> new Item(s.maxCount(16)));
    public static final Item NETHERITE_BUILDING_CORE = registerItem("netherite_building_core", s -> new Item(s.maxCount(16)));

    // Wands
    public static final BuildingWandItem COPPER_BUILDING_WAND = registerBuildingWand("copper_building_wand", DURABILITY_WOOD_STONE * DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_COPPER, ENCHANTABILITY_COPPER);
    public static final BuildingWandItem IRON_BUILDING_WAND = registerBuildingWand("iron_building_wand", DURABILITY_IRON * DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_IRON, ENCHANTABILITY_IRON);
    public static final BuildingWandItem GOLD_BUILDING_WAND = registerBuildingWand("gold_building_wand", DURABILITY_GOLD * DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_GOLD, ENCHANTABILITY_GOLD);
    public static final BuildingWandItem DIAMOND_BUILDING_WAND = registerBuildingWand("diamond_building_wand", DURABILITY_DIAMOND * DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final BuildingWandItem NETHERITE_BUILDING_WAND = registerBuildingWand("netherite_building_wand", DURABILITY_NETHERITE * DURABILITY_MULTIPLAYER_WAND, BUILDING_WAND_SQUARE_NETHERITE, ENCHANTABILITY_NETHERITE);

    // Rangefinder
    public static final RangefinderItem RANGEFINDER_ITEM = (RangefinderItem) registerItem("rangefinder",
            settings -> new RangefinderItem(settings.maxDamage(DURABILITY_RANGEFINDER).enchantable(ENCHANTABILITY_NETHERITE), null));

    public static final Map<DyeColor, RangefinderItem> COLORED_RANGEFINDERS = new HashMap<>();

    // Reinforced Bundle
    public static final Item REINFORCED_BUNDLE = registerItem("reinforced_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1)));

    // =================================================================================
    // HILFSMETHODEN
    // =================================================================================

    private static ChiselItem registerChisel(String name, int maxDamage, int cooldownTicks, int enchantability, String tier) {
        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(settings.maxDamage(maxDamage).enchantable(enchantability), tier));
        chisel.setCooldownTicks(cooldownTicks);
        return chisel;
    }

    private static ChiselItem registerSpatula(String name, int maxDamage, int cooldownTicks, int enchantability, String tier) {
        ChiselItem spatula = (ChiselItem) registerItem(name, settings -> new ChiselItem(settings.maxDamage(maxDamage).enchantable(enchantability), tier));
        spatula.setCooldownTicks(cooldownTicks);
        spatula.setChiselSound(SoundEvents.BLOCK_SAND_FALL);
        spatula.setChiselDirectionCycle(ChiselItem.Direction.BACKWARD);
        return spatula;
    }

    private static BuildingWandItem registerBuildingWand(String name, int maxDamage, int wandSquareDiameter, int enchantability) {
        BuildingWandItem wand = (BuildingWandItem) registerItem(name, settings -> new BuildingWandItem(settings.maxDamage(maxDamage).enchantable(enchantability)));
        wand.setWandSquareDiameter(wandSquareDiameter);
        return wand;
    }

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(Simplebuilding.MOD_ID, name),
                function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Simplebuilding.MOD_ID, name)))));
    }

    public static void registerModItems() {
        Simplebuilding.LOGGER.info("Registering Mod Items for " + Simplebuilding.MOD_ID);

        // Farbige Rangefinder registrieren
        for (DyeColor color : DyeColor.values()) {
            String name = "rangefinder_" + color.getId();
            RangefinderItem coloredItem = (RangefinderItem) registerItem(name,
                    settings -> new RangefinderItem(settings.maxDamage(DURABILITY_NETHERITE * DURABILITY_RANGEFINDER).enchantable(ENCHANTABILITY_NETHERITE), color));
            COLORED_RANGEFINDERS.put(color, coloredItem);
        }
    }
}