package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.custom.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.simplebuilding.items.custom.BuildingWandItem.*;
import static com.simplebuilding.items.custom.OctantItem.DURABILITY_OCTANT;
import static com.simplebuilding.items.custom.SledgehammerItem.*;
import static net.minecraft.util.Rarity.UNCOMMON;

/**
 * Verwaltet die Registrierung aller benutzerdefinierten Gegenstände (Items) des Simplemoney Mods.
 * Definiert die Eigenschaften der Währungskomponenten und des endgültigen Geldscheins.
 */
public class ModItems {

    // --- Durability & Settings Constants ---
    private static final int DURABILITY_WOOD_STONE = 48*4;
    private static final int DURABILITY_IRON = 64*4;
    private static final int DURABILITY_GOLD = 32*4;
    private static final int DURABILITY_DIAMOND = 98*4;
    private static final int DURABILITY_NETHERITE = 128*4;

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
    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);
    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);
    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);
    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);
    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);
    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);

    // Spatulas (Backward)
    public static final ChiselItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);
    public static final ChiselItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);
    public static final ChiselItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);
    public static final ChiselItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);
    public static final ChiselItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);
    public static final ChiselItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);


    // Diamond Items
    public static final Item DIAMOND_PEBBLE = registerItem("diamond_pebble", settings -> new Item(settings));
    public static final Item CRACKED_DIAMOND = registerItem("cracked_diamond", settings -> new Item(settings));
    public static final Item CRACKED_DIAMOND_BLOCK = registerItem("cracked_diamond_block", settings -> new BlockItem(ModBlocks.CRACKED_DIAMOND_BLOCK, settings)); // todo: wie diamond_block nur härter

    // Building Cores
    public static final Item COPPER_CORE = registerItem("copper_core", s -> new Item(s.maxCount(16)));
    public static final Item IRON_CORE = registerItem("iron_core", s -> new Item(s.maxCount(16)));
    public static final Item GOLD_CORE = registerItem("gold_core", s -> new Item(s.maxCount(16)));
    public static final Item DIAMOND_CORE = registerItem("diamond_core", s -> new Item(s.maxCount(16)));
    public static final Item NETHERITE_CORE = registerItem("netherite_core", s -> new Item(s.maxCount(16)));

    // Wands
    public static final BuildingWandItem COPPER_BUILDING_WAND = registerBuildingWand("copper_building_wand", DURABILITY_COPPER_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_COPPER, ENCHANTABILITY_COPPER);
    public static final BuildingWandItem IRON_BUILDING_WAND = registerBuildingWand("iron_building_wand", DURABILITY_IRON_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_IRON, ENCHANTABILITY_IRON);
    public static final BuildingWandItem GOLD_BUILDING_WAND = registerBuildingWand("gold_building_wand", DURABILITY_GOLD_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_GOLD, ENCHANTABILITY_GOLD);
    public static final BuildingWandItem DIAMOND_BUILDING_WAND = registerBuildingWand("diamond_building_wand", DURABILITY_DIAMOND_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final BuildingWandItem NETHERITE_BUILDING_WAND = registerBuildingWand("netherite_building_wand", DURABILITY_NETHERITE_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_NETHERITE, ENCHANTABILITY_NETHERITE);

    // Sledgehammer
    public static final SledgehammerItem STONE_SLEDGEHAMMER = registerSledgehammer("stone_sledgehammer", DURABILITY_STONE_SLEDGEHAMMER, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE, SledgehammerItem.STONE_ATTACK_DAMAGE, SledgehammerItem.STONE_ATTACK_SPEED);
    public static final SledgehammerItem COPPER_SLEDGEHAMMER = registerSledgehammer("copper_sledgehammer", DURABILITY_COPPER_SLEDGEHAMMER, ENCHANTABILITY_COPPER, ToolMaterial.COPPER, SledgehammerItem.COPPER_ATTACK_DAMAGE, SledgehammerItem.COPPER_ATTACK_SPEED);
    public static final SledgehammerItem IRON_SLEDGEHAMMER = registerSledgehammer("iron_sledgehammer", DURABILITY_IRON_SLEDGEHAMMER, ENCHANTABILITY_IRON, ToolMaterial.IRON, SledgehammerItem.IRON_ATTACK_DAMAGE, SledgehammerItem.IRON_ATTACK_SPEED);
    public static final SledgehammerItem GOLD_SLEDGEHAMMER = registerSledgehammer("gold_sledgehammer", DURABILITY_GOLD_SLEDGEHAMMER, ENCHANTABILITY_GOLD, ToolMaterial.GOLD, SledgehammerItem.GOLD_ATTACK_DAMAGE, SledgehammerItem.GOLD_ATTACK_SPEED);
    public static final SledgehammerItem DIAMOND_SLEDGEHAMMER = registerSledgehammer("diamond_sledgehammer", DURABILITY_DIAMOND_SLEDGEHAMMER, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND, SledgehammerItem.DIAMOND_ATTACK_DAMAGE, SledgehammerItem.DIAMOND_ATTACK_SPEED);
    public static final SledgehammerItem NETHERITE_SLEDGEHAMMER = registerSledgehammer("netherite_sledgehammer", DURABILITY_NETHERITE_SLEDGEHAMMER, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE, SledgehammerItem.NETHERITE_ATTACK_DAMAGE, SledgehammerItem.NETHERITE_ATTACK_SPEED);

    // Octants
    public static final OctantItem OCTANT = (OctantItem) registerItem("octant",
            settings -> new OctantItem(settings.maxDamage(DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), null));

    public static final Map<DyeColor, OctantItem> COLORED_OCTANT_ITEMS = new HashMap<>();

    // Speedometer
    public static final Item VELOCITY_GAUGE = registerItem("velocity-gauge", settings -> new Item(settings.maxCount(1)));
    public static final Item ORE_DETECTOR = registerItem("ore_detector", settings -> new OreDetectorItem(settings.maxDamage(512))); // Haltbarkeit ist optional, aber nett für Balance
    public static final Item MAGNET = registerItem("magnet", settings -> new MagnetItem(settings.maxCount(1).rarity(UNCOMMON)));

    // Reinforced Items
    public static final Item REINFORCED_BUNDLE = registerItem("reinforced_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1)));
    public static final Item NETHERITE_BUNDLE = registerItem("netherite_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1).fireproof()));
    public static final Item QUIVER = registerItem("quiver", settings -> new QuiverItem(settings.maxCount(1)));
    public static final Item NETHERITE_QUIVER = registerItem("netherite_quiver", settings -> new QuiverItem(settings.maxCount(1).fireproof()));

    // Block Items
    public static final Item CONSTRUCTION_LIGHT = registerItem("construction_light", s -> new BlockItem(ModBlocks.CONSTRUCTION_LIGHT, s));

    // Hoppers
    public static final Item REINFORCED_HOPPER = registerItem("reinforced_hopper", s -> new BlockItem(ModBlocks.REINFORCED_HOPPER, s));
    public static final Item NETHERITE_HOPPER = registerItem("netherite_hopper", s -> new BlockItem(ModBlocks.NETHERITE_HOPPER, s.fireproof()));

    // Chests Todo:
    // public static final Item REINFORCED_CHEST = registerItem("reinforced_chest", s -> new BlockItem(ModBlocks.REINFORCED_CHEST, s));
    // public static final Item NETHERITE_CHEST = registerItem("netherite_chest", s -> new BlockItem(ModBlocks.NETHERITE_CHEST, s.fireproof()));

    // Pistons
    public static final Item REINFORCED_PISTON = registerItem("reinforced_piston", s -> new BlockItem(ModBlocks.REINFORCED_PISTON, s));
    public static final Item NETHERITE_PISTON = registerItem("netherite_piston", s -> new BlockItem(ModBlocks.NETHERITE_PISTON, s.fireproof()));

    // Furnaces
    // --- FURNACES (Cooking) ---
    // Blast Furnace
    public static final Item REINFORCED_BLAST_FURNACE = registerItem("reinforced_blast_furnace", s -> new BlockItem(ModBlocks.REINFORCED_BLAST_FURNACE, s));
    public static final Item NETHERITE_BLAST_FURNACE = registerItem("netherite_blast_furnace", s -> new BlockItem(ModBlocks.NETHERITE_BLAST_FURNACE, s.fireproof()));

    // Standard Furnace (NEU)
    public static final Item REINFORCED_FURNACE = registerItem("reinforced_furnace", s -> new BlockItem(ModBlocks.REINFORCED_FURNACE, s));
    public static final Item NETHERITE_FURNACE = registerItem("netherite_furnace", s -> new BlockItem(ModBlocks.NETHERITE_FURNACE, s.fireproof()));

    // Smoker (NEU)
    public static final Item REINFORCED_SMOKER = registerItem("reinforced_smoker", s -> new BlockItem(ModBlocks.REINFORCED_SMOKER, s));
    public static final Item NETHERITE_SMOKER = registerItem("netherite_smoker", s -> new BlockItem(ModBlocks.NETHERITE_SMOKER, s.fireproof()));

    // TODO: use lang files for text components
    // Trim Templates
    public static final Item GLOWING_TRIM_TEMPLATE = registerItem("glowing_trim_template", settings -> new SmithingTemplateItem(
            Text.literal("Add Radiance").formatted(Formatting.GRAY),
            Text.literal("Glowing Material").formatted(Formatting.GRAY),
            Text.literal("Apply to Armor").formatted(Formatting.GRAY),
            Text.literal("Add Glow Ink").formatted(Formatting.GRAY),
            java.util.List.of(Identifier.ofVanilla("container/slot/helmet"), Identifier.ofVanilla("container/slot/chestplate"), Identifier.ofVanilla("container/slot/leggings"), Identifier.ofVanilla("container/slot/boots")),
            java.util.List.of(Identifier.ofVanilla("container/slot/ingot"), Identifier.ofVanilla("container/slot/lapis_lazuli"), Identifier.ofVanilla("container/slot/redstone_dust")),
            settings.maxCount(64)
    ));
    public static final Item EMITTING_TRIM_TEMPLATE = registerItem("emitting_trim_template", settings -> new SmithingTemplateItem(
            Text.literal("Light-source").formatted(Formatting.GOLD),
            Text.literal("Light function").formatted(Formatting.GRAY),
            Text.literal("Emits light").formatted(Formatting.GRAY),
            Text.literal("Add Material").formatted(Formatting.GRAY),
            List.of(Identifier.ofVanilla("container/slot/helmet"), Identifier.ofVanilla("container/slot/chestplate"), Identifier.ofVanilla("container/slot/leggings"), Identifier.ofVanilla("container/slot/boots")),
            java.util.List.of(Identifier.ofVanilla("container/slot/ingot"), Identifier.ofVanilla("container/slot/lapis_lazuli"), Identifier.ofVanilla("container/slot/redstone_dust")),
            settings.maxCount(64)
    ));

    public static final Item BASIC_UPGRADE_TEMPLATE = registerItem("basic_upgrade_template", settings -> new Item(settings.maxCount(64)));

    // =================================================================================
    // HILFSMETHODEN
    // =================================================================================

    private static ChiselItem registerChisel(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {
        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, settings.maxDamage(maxDamage).enchantable(enchantability)));
        chisel.setCooldownTicks(cooldownTicks);
        return chisel;
    }

    private static ChiselItem registerSpatula(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {
        ChiselItem spatula = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, settings.maxDamage(maxDamage).enchantable(enchantability)));
        spatula.setCooldownTicks(cooldownTicks);
        spatula.setChiselSound(SoundEvents.BLOCK_SAND_FALL);
        spatula.setChiselDirectionCycle(ChiselItem.Direction.BACKWARD);
        spatula.setAsDedicatedSpatula(true);
        return spatula;
    }

    private static BuildingWandItem registerBuildingWand(String name, int maxDamage, int wandSquareDiameter, int enchantability) {
        BuildingWandItem wand = (BuildingWandItem) registerItem(name, settings -> new BuildingWandItem(settings.maxDamage(maxDamage).enchantable(enchantability)));
        wand.setWandSquareDiameter(wandSquareDiameter);
        return wand;
    }

    private static SledgehammerItem registerSledgehammer(String name, int durability, int enchantability, ToolMaterial toolMaterial, int attackDamage, float attackSpeed) {
        return (SledgehammerItem) registerItem(name, settings -> new SledgehammerItem(toolMaterial, attackDamage, attackSpeed, durability, settings.maxDamage(durability).enchantable(enchantability)));
    }

    private static Item registerItem(String name, Function<Item.Settings, Item> function) {
        return Registry.register(Registries.ITEM, Identifier.of(Simplebuilding.MOD_ID, name),
                function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Simplebuilding.MOD_ID, name)))));
    }

    public static void registerModItems() {
        Simplebuilding.LOGGER.info("Registering Mod Items for " + Simplebuilding.MOD_ID);

        // Farbige Octanten registrieren
        for (DyeColor color : DyeColor.values()) {
            String name = "octant_" + color.getId();
            OctantItem coloredItem = (OctantItem) registerItem(name,
                    settings -> new OctantItem(settings.maxDamage(DURABILITY_NETHERITE * DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), color));
            COLORED_OCTANT_ITEMS.put(color, coloredItem);
        }

    }
}