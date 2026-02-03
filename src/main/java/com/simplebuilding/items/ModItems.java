package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.items.custom.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
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
import static net.minecraft.util.Rarity.RARE;
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
    private static final int DURABILITY_ENDERITE = 150*4;

    private static final int COOLDOWN_TICKS_WOOD_STONE = 30;
    private static final int COOLDOWN_TICKS_IRON = 25;
    private static final int COOLDOWN_TICKS_GOLD = 20;
    private static final int COOLDOWN_TICKS_DIAMOND = 10;
    private static final int COOLDOWN_TICKS_NETHERITE = 5;
    private static final int COOLDOWN_TICKS_ENDERITE = 4;

    private static final int ENCHANTABILITY_WOOD_STONE = 15;
    private static final int ENCHANTABILITY_COPPER = 18;
    private static final int ENCHANTABILITY_IRON = 14;
    private static final int ENCHANTABILITY_GOLD = 22;
    private static final int ENCHANTABILITY_DIAMOND = 10;
    private static final int ENCHANTABILITY_NETHERITE = 15;
    private static final int ENCHANTABILITY_ENDERITE = 18;

    // =================================================================================
    // ITEM REGISTRIERUNGEN
    // =================================================================================


    // --- BLOCK ITEMS (Automatisch registrieren oder hier manuell) ---
    // Falls du eine Loop hast, gut. Sonst manuell:
    public static final Item POLISHED_END_STONE = registerItem("polished_end_stone", s -> new BlockItem(ModBlocks.POLISHED_END_STONE, s));
    public static final Item PURPUR_QUARTZ_CHECKER = registerItem("purpur_quartz_checker", s -> new BlockItem(ModBlocks.PURPUR_QUARTZ_CHECKER, s));
    public static final Item LAPIS_QUARTZ_CHECKER = registerItem("lapis_quartz_checker", s -> new BlockItem(ModBlocks.LAPIS_QUARTZ_CHECKER, s));
    public static final Item BLACKSTONE_QUARTZ_CHECKER = registerItem("blackstone_quartz_checker", s -> new BlockItem(ModBlocks.BLACKSTONE_QUARTZ_CHECKER, s));
    public static final Item RESIN_QUARTZ_CHECKER = registerItem("resin_quartz_checker", s -> new BlockItem(ModBlocks.RESIN_QUARTZ_CHECKER, s));

    public static final Item ASTRAL_PURPUR_BLOCK = registerItem("astral_purpur_block", s -> new BlockItem(ModBlocks.ASTRAL_PURPUR_BLOCK, s));
    public static final Item NIHIL_PURPUR_BLOCK = registerItem("nihil_purpur_block", s -> new BlockItem(ModBlocks.NIHIL_PURPUR_BLOCK, s));
    public static final Item ASTRAL_END_STONE = registerItem("astral_end_stone", s -> new BlockItem(ModBlocks.ASTRAL_END_STONE, s));
    public static final Item NIHIL_END_STONE = registerItem("nihil_end_stone", s -> new BlockItem(ModBlocks.NIHIL_END_STONE, s));

    public static final Item SUSPENDED_SAND = registerItem("suspended_sand", s -> new BlockItem(ModBlocks.SUSPENDED_SAND, s));
    public static final Item SUSPENDED_GRAVEL = registerItem("suspended_gravel", s -> new BlockItem(ModBlocks.SUSPENDED_GRAVEL, s));
    public static final Item LEVITATING_SAND = registerItem("levitating_sand", s -> new BlockItem(ModBlocks.LEVITATING_SAND, s));
    public static final Item LEVITATING_GRAVEL = registerItem("levitating_gravel", s -> new BlockItem(ModBlocks.LEVITATING_GRAVEL, s));

    // Upgrade Templates
    public static final Item BASIC_UPGRADE_TEMPLATE = registerItem("basic_upgrade_template", settings -> new Item(settings.maxCount(64).rarity(UNCOMMON)));
    public static final Item ENDERITE_UPGRADE_TEMPLATE = registerItem("enderite_upgrade_template", s -> new Item(s));

    // Materials and Blocks Components
    public static final Item DIAMOND_PEBBLE = registerItem("diamond_pebble", settings -> new Item(settings));
    public static final Item CRACKED_DIAMOND = registerItem("cracked_diamond", settings -> new Item(settings));
    public static final Item CRACKED_DIAMOND_BLOCK = registerItem("cracked_diamond_block", settings -> new BlockItem(ModBlocks.CRACKED_DIAMOND_BLOCK, settings)); // todo: wie diamond_block nur härter
    public static final Item NETHERITE_NUGGET = registerItem("netherite_nugget", settings -> new Item(settings));

    public static final Item NIHILITH_SHARD = registerItem("nihilith_shard", s -> new Item(s)); // Fix: s nutzen!
    public static final Item ASTRALIT_DUST = registerItem("astralit_dust", s -> new Item(s));   // Fix: s nutzen!
    public static final Item RAW_ENDERITE = registerItem("raw_enderite", s -> new Item(s));     // Fix: s nutzen!
    public static final Item ENDERITE_SCRAP = registerItem("enderite_scrap", s -> new Item(s.fireproof()));
    public static final Item ENDERITE_INGOT = registerItem("enderite_ingot", s -> new Item(s.fireproof()));

    public static final Item ENDERITE_BLOCK_ITEM = registerItem("enderite_block", s -> new BlockItem(ModBlocks.ENDERITE_BLOCK, s));
    public static final Item NIHILITH_ORE_ITEM = registerItem("nihilith_ore", s -> new BlockItem(ModBlocks.NIHILITH_ORE, s));
    public static final Item ASTRALIT_ORE_ITEM = registerItem("astralit_ore", s -> new BlockItem(ModBlocks.ASTRALIT_ORE, s));

    public static final Item CONSTRUCTION_LIGHT = registerItem("construction_light", s -> new BlockItem(ModBlocks.CONSTRUCTION_LIGHT, s));


    // Building Cores
    public static final Item COPPER_CORE = registerItem("copper_core", s -> new Item(s.maxCount(16)));
    public static final Item IRON_CORE = registerItem("iron_core", s -> new Item(s.maxCount(16)));
    public static final Item GOLD_CORE = registerItem("gold_core", s -> new Item(s.maxCount(16)));
    public static final Item DIAMOND_CORE = registerItem("diamond_core", s -> new Item(s.maxCount(16)));
    public static final Item NETHERITE_CORE = registerItem("netherite_core", s -> new Item(s.maxCount(16)));
    public static final Item ENDERITE_CORE = registerItem("enderite_core", s -> new Item(s.maxCount(16)));

    // =================================================================================
    // TOOLS
    // =================================================================================
    public static final Item ENDERITE_SWORD = registerItem("enderite_sword", s -> new Item(ModToolMaterials.ENDERITE.applySwordSettings(s.fireproof(), 3.0F, -2.4F)));
    public static final Item ENDERITE_PICKAXE = registerItem("enderite_pickaxe", s -> new Item(ModToolMaterials.ENDERITE.applyToolSettings(s.fireproof(), BlockTags.PICKAXE_MINEABLE, 1.0F, -2.8F, 0.0F)));
    public static final Item ENDERITE_AXE = registerItem("enderite_axe", s -> new Item(ModToolMaterials.ENDERITE.applyToolSettings(s.fireproof(), BlockTags.AXE_MINEABLE, 5.0F, -3.0F, 0.0F)));
    public static final Item ENDERITE_SHOVEL = registerItem("enderite_shovel", s -> new Item(ModToolMaterials.ENDERITE.applyToolSettings(s.fireproof(), BlockTags.SHOVEL_MINEABLE, 1.5F, -3.0F, 0.0F)));
    public static final Item ENDERITE_HOE = registerItem("enderite_hoe", s -> new Item(ModToolMaterials.ENDERITE.applyToolSettings(s.fireproof(), BlockTags.HOE_MINEABLE, -4.0F, 0.0F, 0.0F)));
    // Wands
    public static final BuildingWandItem COPPER_BUILDING_WAND = registerBuildingWand("copper_building_wand", DURABILITY_COPPER_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_COPPER, ENCHANTABILITY_COPPER);
    public static final BuildingWandItem IRON_BUILDING_WAND = registerBuildingWand("iron_building_wand", DURABILITY_IRON_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_IRON, ENCHANTABILITY_IRON);
    public static final BuildingWandItem GOLD_BUILDING_WAND = registerBuildingWand("gold_building_wand", DURABILITY_GOLD_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_GOLD, ENCHANTABILITY_GOLD);
    public static final BuildingWandItem DIAMOND_BUILDING_WAND = registerBuildingWand("diamond_building_wand", DURABILITY_DIAMOND_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_DIAMOND, ENCHANTABILITY_DIAMOND);
    public static final BuildingWandItem NETHERITE_BUILDING_WAND = registerBuildingWand("netherite_building_wand", DURABILITY_NETHERITE_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_NETHERITE, ENCHANTABILITY_NETHERITE);
    public static final BuildingWandItem ENDERITE_BUILDING_WAND = registerBuildingWand("enderite_building_wand", DURABILITY_ENDERITE_SLEDGEHAMMER * 2, BUILDING_WAND_SQUARE_ENDERITE, ENCHANTABILITY_ENDERITE);
    // Sledgehammer
    public static final SledgehammerItem STONE_SLEDGEHAMMER = registerSledgehammer("stone_sledgehammer", DURABILITY_STONE_SLEDGEHAMMER, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE, SledgehammerItem.STONE_ATTACK_DAMAGE, SledgehammerItem.STONE_ATTACK_SPEED);
    public static final SledgehammerItem COPPER_SLEDGEHAMMER = registerSledgehammer("copper_sledgehammer", DURABILITY_COPPER_SLEDGEHAMMER, ENCHANTABILITY_COPPER, ToolMaterial.COPPER, SledgehammerItem.COPPER_ATTACK_DAMAGE, SledgehammerItem.COPPER_ATTACK_SPEED);
    public static final SledgehammerItem IRON_SLEDGEHAMMER = registerSledgehammer("iron_sledgehammer", DURABILITY_IRON_SLEDGEHAMMER, ENCHANTABILITY_IRON, ToolMaterial.IRON, SledgehammerItem.IRON_ATTACK_DAMAGE, SledgehammerItem.IRON_ATTACK_SPEED);
    public static final SledgehammerItem GOLD_SLEDGEHAMMER = registerSledgehammer("gold_sledgehammer", DURABILITY_GOLD_SLEDGEHAMMER, ENCHANTABILITY_GOLD, ToolMaterial.GOLD, SledgehammerItem.GOLD_ATTACK_DAMAGE, SledgehammerItem.GOLD_ATTACK_SPEED);
    public static final SledgehammerItem DIAMOND_SLEDGEHAMMER = registerSledgehammer("diamond_sledgehammer", DURABILITY_DIAMOND_SLEDGEHAMMER, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND, SledgehammerItem.DIAMOND_ATTACK_DAMAGE, SledgehammerItem.DIAMOND_ATTACK_SPEED);
    public static final SledgehammerItem NETHERITE_SLEDGEHAMMER = registerSledgehammer("netherite_sledgehammer", DURABILITY_NETHERITE_SLEDGEHAMMER, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE, SledgehammerItem.NETHERITE_ATTACK_DAMAGE, SledgehammerItem.NETHERITE_ATTACK_SPEED);
    public static final SledgehammerItem ENDERITE_SLEDGEHAMMER = registerSledgehammer("enderite_sledgehammer", DURABILITY_ENDERITE_SLEDGEHAMMER, ENCHANTABILITY_ENDERITE, ModToolMaterials.ENDERITE, SledgehammerItem.ENDERITE_ATTACK_DAMAGE, SledgehammerItem.ENDERITE_ATTACK_SPEED);
    // Chisels (Forward)
    public static final ChiselItem STONE_CHISEL = registerChisel("stone_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);
    public static final ChiselItem COPPER_CHISEL = registerChisel("copper_chisel", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);
    public static final ChiselItem IRON_CHISEL = registerChisel("iron_chisel", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);
    public static final ChiselItem GOLD_CHISEL = registerChisel("gold_chisel", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);
    public static final ChiselItem DIAMOND_CHISEL = registerChisel("diamond_chisel", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);
    public static final ChiselItem NETHERITE_CHISEL = registerChisel("netherite_chisel", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);
    public static final ChiselItem ENDERITE_CHISEL = registerChisel("enderite_chisel", DURABILITY_ENDERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_ENDERITE, ModToolMaterials.ENDERITE);
    // Spatulas (Backward)
    public static final ChiselItem STONE_SPATULA = registerSpatula("stone_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_WOOD_STONE, ENCHANTABILITY_WOOD_STONE, ToolMaterial.STONE);
    public static final ChiselItem COPPER_SPATULA = registerSpatula("copper_spatula", DURABILITY_WOOD_STONE, COOLDOWN_TICKS_IRON, ENCHANTABILITY_COPPER, ToolMaterial.COPPER);
    public static final ChiselItem IRON_SPATULA = registerSpatula("iron_spatula", DURABILITY_IRON, COOLDOWN_TICKS_IRON, ENCHANTABILITY_IRON, ToolMaterial.IRON);
    public static final ChiselItem GOLD_SPATULA = registerSpatula("gold_spatula", DURABILITY_GOLD, COOLDOWN_TICKS_GOLD, ENCHANTABILITY_GOLD, ToolMaterial.GOLD);
    public static final ChiselItem DIAMOND_SPATULA = registerSpatula("diamond_spatula", DURABILITY_DIAMOND, COOLDOWN_TICKS_DIAMOND, ENCHANTABILITY_DIAMOND, ToolMaterial.DIAMOND);
    public static final ChiselItem NETHERITE_SPATULA = registerSpatula("netherite_spatula", DURABILITY_NETHERITE, COOLDOWN_TICKS_NETHERITE, ENCHANTABILITY_NETHERITE, ToolMaterial.NETHERITE);
    // Gadgets
    public static final Item VELOCITY_GAUGE = registerItem("velocity-gauge", settings -> new Item(settings.maxCount(1)));
    public static final Item ORE_DETECTOR = registerItem("ore_detector", settings -> new OreDetectorItem(settings.maxDamage(512).rarity(RARE)));
    public static final Item MAGNET = registerItem("magnet", settings -> new MagnetItem(settings.maxCount(1).rarity(UNCOMMON)));
    public static final Item ROTATOR = registerItem("rotator", settings -> new RotatorItem(settings.maxDamage(1024).maxCount(1)));
    public static final OctantItem OCTANT = (OctantItem) registerItem("octant", settings -> new OctantItem(settings.maxDamage(DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), null));
    public static final Map<DyeColor, OctantItem> COLORED_OCTANT_ITEMS = new HashMap<>();

    // =================================================================================
    // Reinforced Items
    // =================================================================================
    public static final Item REINFORCED_BUNDLE = registerItem("reinforced_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1)));
    public static final Item NETHERITE_BUNDLE = registerItem("netherite_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1).fireproof().rarity(UNCOMMON)));
    public static final Item QUIVER = registerItem("quiver", settings -> new QuiverItem(settings.maxCount(1)));
    public static final Item NETHERITE_QUIVER = registerItem("netherite_quiver", settings -> new QuiverItem(settings.maxCount(1).fireproof().rarity(UNCOMMON)));
    public static final Item ENDERITE_BUNDLE = registerItem("enderite_bundle", settings -> new ReinforcedBundleItem(settings.maxCount(1).fireproof().rarity(net.minecraft.util.Rarity.EPIC)));
    public static final Item ENDERITE_QUIVER = registerItem("enderite_quiver", settings -> new QuiverItem(settings.maxCount(1).fireproof().rarity(net.minecraft.util.Rarity.EPIC)));

    // Reinforced Block Items
    // Todo: public static final Item REINFORCED_CHEST = registerItem("reinforced_chest", s -> new BlockItem(ModBlocks.REINFORCED_CHEST, s));
    // Todo: public static final Item NETHERITE_CHEST = registerItem("netherite_chest", s -> new BlockItem(ModBlocks.NETHERITE_CHEST, s.fireproof()));
    public static final Item REINFORCED_HOPPER = registerItem("reinforced_hopper", s -> new BlockItem(ModBlocks.REINFORCED_HOPPER, s));
    public static final Item NETHERITE_HOPPER = registerItem("netherite_hopper", s -> new BlockItem(ModBlocks.NETHERITE_HOPPER, s.fireproof()));
    public static final Item REINFORCED_PISTON = registerItem("reinforced_piston", s -> new BlockItem(ModBlocks.REINFORCED_PISTON, s));
    public static final Item NETHERITE_PISTON = registerItem("netherite_piston", s -> new BlockItem(ModBlocks.NETHERITE_PISTON, s.fireproof()));
    public static final Item REINFORCED_BLAST_FURNACE = registerItem("reinforced_blast_furnace", s -> new BlockItem(ModBlocks.REINFORCED_BLAST_FURNACE, s));
    public static final Item NETHERITE_BLAST_FURNACE = registerItem("netherite_blast_furnace", s -> new BlockItem(ModBlocks.NETHERITE_BLAST_FURNACE, s.fireproof()));
    public static final Item REINFORCED_FURNACE = registerItem("reinforced_furnace", s -> new BlockItem(ModBlocks.REINFORCED_FURNACE, s));
    public static final Item NETHERITE_FURNACE = registerItem("netherite_furnace", s -> new BlockItem(ModBlocks.NETHERITE_FURNACE, s.fireproof()));
    public static final Item REINFORCED_SMOKER = registerItem("reinforced_smoker", s -> new BlockItem(ModBlocks.REINFORCED_SMOKER, s));
    public static final Item NETHERITE_SMOKER = registerItem("netherite_smoker", s -> new BlockItem(ModBlocks.NETHERITE_SMOKER, s.fireproof()));

    // =================================================================================
    // TODO: use lang files for text components
    // Trim Templates
    // =================================================================================
    public static final Item GLOWING_TRIM_TEMPLATE = registerItem("glowing_trim_template", settings -> new SmithingTemplateItem(
            Text.literal("Add Radiance").formatted(Formatting.GRAY),
            Text.literal("Glowing Material").formatted(Formatting.GRAY),
            Text.literal("Apply to Armor").formatted(Formatting.GRAY),
            Text.literal("Add Glow Ink").formatted(Formatting.GRAY),
            java.util.List.of(Identifier.ofVanilla("container/slot/helmet"), Identifier.ofVanilla("container/slot/chestplate"), Identifier.ofVanilla("container/slot/leggings"), Identifier.ofVanilla("container/slot/boots")),
            java.util.List.of(Identifier.ofVanilla("container/slot/ingot"), Identifier.ofVanilla("container/slot/lapis_lazuli"), Identifier.ofVanilla("container/slot/redstone_dust")),
            settings.maxCount(64).rarity(RARE)
    ));
    public static final Item EMITTING_TRIM_TEMPLATE = registerItem("emitting_trim_template", settings -> new SmithingTemplateItem(
            Text.literal("Light-source").formatted(Formatting.GOLD),
            Text.literal("Light function").formatted(Formatting.GRAY),
            Text.literal("Emits light").formatted(Formatting.GRAY),
            Text.literal("Add Material").formatted(Formatting.GRAY),
            List.of(Identifier.ofVanilla("container/slot/helmet"), Identifier.ofVanilla("container/slot/chestplate"), Identifier.ofVanilla("container/slot/leggings"), Identifier.ofVanilla("container/slot/boots")),
            java.util.List.of(Identifier.ofVanilla("container/slot/ingot"), Identifier.ofVanilla("container/slot/lapis_lazuli"), Identifier.ofVanilla("container/slot/redstone_dust")),
            settings.maxCount(64).rarity(RARE)
    ));




    // =================================================================================
    // ARMOR (Nur mit 'Item' und Components via Helper)
    // =================================================================================

    public static final Item ENDERITE_HELMET = registerArmor("enderite_helmet", ModArmorMaterials.ENDERITE, EquipmentType.HELMET, 42);
    public static final Item ENDERITE_CHESTPLATE = registerArmor("enderite_chestplate", ModArmorMaterials.ENDERITE, EquipmentType.CHESTPLATE, 42);
    public static final Item ENDERITE_LEGGINGS = registerArmor("enderite_leggings", ModArmorMaterials.ENDERITE, EquipmentType.LEGGINGS, 42);
    public static final Item ENDERITE_BOOTS = registerArmor("enderite_boots", ModArmorMaterials.ENDERITE, EquipmentType.BOOTS, 42);


    // =================================================================================
    // Food
    // =================================================================================
    public static final FoodComponent NETHERITE_CARROT_FOOD = new FoodComponent.Builder().nutrition(6).saturationModifier(1.2f).alwaysEdible().build();
    public static final FoodComponent NETHERITE_APPLE_FOOD = new FoodComponent.Builder().nutrition(8).saturationModifier(1.5f).alwaysEdible().build();
    public static final FoodComponent ENDERITE_CARROT_FOOD = new FoodComponent.Builder().nutrition(10).saturationModifier(1.8f).alwaysEdible().build();
    public static final FoodComponent ENDERITE_APPLE_FOOD = new FoodComponent.Builder().nutrition(12).saturationModifier(2.0f).alwaysEdible().build();

    public static ConsumableComponent createNetheriteFoodEffects(boolean isApple) {
        ConsumableComponent.Builder builder = ConsumableComponent.builder().useAction(UseAction.EAT).consumeSeconds(1.6f);
        if (isApple) {
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 4800, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1), 1.0f));
        } else {
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 2400, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 2400, 0), 1.0f));
        }

        return builder.build();
    }

    public static ConsumableComponent createEnderiteFoodEffects(boolean isApple) {
        ConsumableComponent.Builder builder = ConsumableComponent.builder().useAction(UseAction.EAT).consumeSeconds(1.6f);
        if (isApple) {
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 3000, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3000, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 2400, 2), 1.0f));
        } else {
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200, 1), 1.0f));
            builder.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1, 0), 1.0f));
        }
        return builder.build();
    }

    public static ConsumableComponent createEnchantedNetheriteFoodEffects() {
        return ConsumableComponent.builder().useAction(UseAction.EAT).consumeSeconds(1.6f)
                // Deutlich stärkere Effekte als der normale Netherite Apfel
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 800, 1), 1.0f)) // Regeneration II (40s)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0), 1.0f))  // Resistance I (5min)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)) // Fire Res (5min)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3), 1.0f)) // Absorption IV (2min, 8 extra Herzen)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 2400, 1), 1.0f))   // Strength II (2min)
                .build();
    }

    public static ConsumableComponent createEnchantedEnderiteFoodEffects() {
        return ConsumableComponent.builder().useAction(UseAction.EAT).consumeSeconds(1.6f)
                // Massive Effekte für das Endgame
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 2), 1.0f)) // Regeneration III (60s)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1), 1.0f))  // Resistance II (5min)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0), 1.0f)) // Fire Res (10min)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 3600, 4), 1.0f)) // Absorption V (3min, 10 extra Herzen)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 3600, 4), 1.0f)) // Health Boost V (3min)
                .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0), 1.0f)) // Night Vision (5min)
                .build();
    }


    public static final Item NETHERITE_CARROT = registerItem("netherite_carrot", settings -> new Item(settings.food(NETHERITE_CARROT_FOOD, createNetheriteFoodEffects(false)).fireproof()));
    public static final Item NETHERITE_APPLE = registerItem("netherite_apple", settings -> new Item(settings.food(NETHERITE_APPLE_FOOD, createNetheriteFoodEffects(true)).fireproof()));
    public static final Item ENDERITE_CARROT = registerItem("enderite_carrot", settings -> new Item(settings.food(ENDERITE_CARROT_FOOD, createEnderiteFoodEffects(false)).fireproof()));
    public static final Item ENDERITE_APPLE = registerItem("enderite_apple", settings -> new Item(settings.food(ENDERITE_APPLE_FOOD, createEnderiteFoodEffects(true)).fireproof()));

    public static final Item ENCHANTED_NETHERITE_APPLE = registerItem("enchanted_netherite_apple", settings -> new Item(settings
            .food(NETHERITE_APPLE_FOOD, createEnchantedNetheriteFoodEffects())
            .fireproof()
            .rarity(net.minecraft.util.Rarity.EPIC)
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))); // Aktiviert den Schimmer

    public static final Item ENCHANTED_ENDERITE_APPLE = registerItem("enchanted_enderite_apple", settings -> new Item(settings
            .food(ENDERITE_APPLE_FOOD, createEnchantedEnderiteFoodEffects())
            .fireproof()
            .rarity(net.minecraft.util.Rarity.EPIC)
            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));



    // =================================================================================
    // HILFSMETHODEN
    // =================================================================================

    private static Item registerArmor(String name, ArmorMaterial material, EquipmentType type, int durabilityMultiplier) {
        return registerItem(name, settings -> {
            // 1. Basis Settings
            settings.fireproof();
            settings.maxDamage(type.getMaxDamage(durabilityMultiplier));
            settings.repairable(material.repairIngredient()); // Reparatur-Item setzen

            // 2. Attribute (Rüstungsschutz etc.)
            // Wir nutzen die Methode aus deinem ArmorMaterial Record
            settings.attributeModifiers(material.createAttributeModifiers(type));

            // 3. Equippable Component (Damit man es anziehen kann)
            // Das ist der Ersatz für die ArmorItem-Logik in 1.21.3+
            settings.component(DataComponentTypes.EQUIPPABLE,
                    EquippableComponent.builder(type.getEquipmentSlot())
                            .equipSound(material.equipSound())
                            .model(material.assetId()) // Verweist auf den EquipmentAsset Key
                            .build()
            );

            return new Item(settings);
        });
    }

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