package com.simplebuilding.items;



import com.simplebuilding.Simplebuilding;

import com.simplebuilding.blocks.ModBlocks;

import com.simplebuilding.items.custom.*;

import com.simplebuilding.trim.ModTrimMaterials;

import net.minecraft.ChatFormatting;

import net.minecraft.core.Registry;

import net.minecraft.core.component.DataComponents;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.registries.Registries;

import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;

import net.minecraft.resources.ResourceKey;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;

import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.food.FoodProperties;

import net.minecraft.world.item.BlockItem;

import net.minecraft.world.item.DyeColor;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemUseAnimation;

import net.minecraft.world.item.Rarity;

import net.minecraft.world.item.SmithingTemplateItem;

import net.minecraft.world.item.ToolMaterial;

import net.minecraft.world.item.component.Consumable;

import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import net.minecraft.world.item.equipment.ArmorMaterial;

import net.minecraft.world.item.equipment.ArmorType;



import java.util.EnumMap;
import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.function.Function;



import static com.simplebuilding.items.custom.BuildingWandItem.*;

import static com.simplebuilding.items.custom.OctantItem.DURABILITY_OCTANT;

import static com.simplebuilding.items.custom.SledgehammerItem.*;

import static net.minecraft.world.item.Rarity.RARE;

import static net.minecraft.world.item.Rarity.UNCOMMON;



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

    public static final Item BASIC_UPGRADE_TEMPLATE = registerItem("basic_upgrade_template", settings -> new Item(settings.stacksTo(64).rarity(UNCOMMON)));

    public static final Item ENDERITE_UPGRADE_TEMPLATE = registerItem("enderite_upgrade_template", s -> new Item(s));



    // Materials and Blocks Components

    public static final Item DIAMOND_PEBBLE = registerItem("diamond_pebble", settings -> new Item(settings));

    public static final Item CRACKED_DIAMOND = registerItem("cracked_diamond", settings -> new Item(settings));

    public static final Item CRACKED_DIAMOND_BLOCK = registerItem("cracked_diamond_block", settings -> new BlockItem(ModBlocks.CRACKED_DIAMOND_BLOCK, settings)); // todo: wie diamond_block nur härter

    public static final Item NETHERITE_NUGGET = registerItem("netherite_nugget", settings -> new Item(settings));

    public static final Item ENDERITE_NUGGET = registerItem("enderite_nugget", settings -> new Item(settings.fireResistant()));



        public static final Item NIHILITH_SHARD = registerItem("nihilith_shard", s -> new Item(s

            .trimMaterial(ModTrimMaterials.NIHILITH)));

        public static final Item ASTRALIT_DUST = registerItem("astralit_dust", s -> new Item(s

            .trimMaterial(ModTrimMaterials.ASTRALIT)));

    public static final Item RAW_ENDERITE = registerItem("raw_enderite", s -> new Item(s));     // Fix: s nutzen!

    public static final Item ENDERITE_SCRAP = registerItem("enderite_scrap", s -> new Item(s.fireResistant()));

        public static final Item ENDERITE_INGOT = registerItem("enderite_ingot", s -> new Item(s

            .fireResistant()

            .trimMaterial(ModTrimMaterials.ENDERITE)));



    public static final Item ENDERITE_BLOCK_ITEM = registerItem("enderite_block", s -> new BlockItem(ModBlocks.ENDERITE_BLOCK, s));

    public static final Item NIHILITH_ORE_ITEM = registerItem("nihilith_ore", s -> new BlockItem(ModBlocks.NIHILITH_ORE, s));

    public static final Item ASTRALIT_ORE_ITEM = registerItem("astralit_ore", s -> new BlockItem(ModBlocks.ASTRALIT_ORE, s));



    public static final Item CONSTRUCTION_LIGHT = registerItem("construction_light", s -> new BlockItem(ModBlocks.CONSTRUCTION_LIGHT, s));





    // Building Cores

    public static final Item COPPER_CORE = registerItem("copper_core", s -> new Item(s.stacksTo(16)));

    public static final Item IRON_CORE = registerItem("iron_core", s -> new Item(s.stacksTo(16)));

    public static final Item GOLD_CORE = registerItem("gold_core", s -> new Item(s.stacksTo(16)));

    public static final Item DIAMOND_CORE = registerItem("diamond_core", s -> new Item(s.stacksTo(16)));

    public static final Item NETHERITE_CORE = registerItem("netherite_core", s -> new Item(s.stacksTo(16)));

    public static final Item ENDERITE_CORE = registerItem("enderite_core", s -> new Item(s.stacksTo(16)));



    // =================================================================================

    // TOOLS

    // =================================================================================

    public static final Item ENDERITE_SWORD = registerItem("enderite_sword", s -> new Item(s.fireResistant().sword(ModToolMaterials.ENDERITE, 3.0F, -2.4F)));

    public static final Item ENDERITE_SPEAR = registerItem("enderite_spear", s -> new Item(s

            .fireResistant()

            // Mirror vanilla spear setup so the item receives actual spear mechanics/components.

            .spear(ModToolMaterials.ENDERITE, 1.15F, 1.2F, 0.4F, 2.5F, 7.0F, 5.5F, 5.1F, 8.75F, 4.6F)));

    public static final Item ENDERITE_PICKAXE = registerItem("enderite_pickaxe", s -> new Item(s.fireResistant().pickaxe(ModToolMaterials.ENDERITE, 1.0F, -2.8F)));

    public static final Item ENDERITE_AXE = registerItem("enderite_axe", s -> new Item(s.fireResistant().axe(ModToolMaterials.ENDERITE, 5.0F, -3.0F)));

    public static final Item ENDERITE_SHOVEL = registerItem("enderite_shovel", s -> new Item(s.fireResistant().shovel(ModToolMaterials.ENDERITE, 1.5F, -3.0F)));

    public static final Item ENDERITE_HOE = registerItem("enderite_hoe", s -> new Item(s.fireResistant().hoe(ModToolMaterials.ENDERITE, -4.0F, 0.0F)));

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

    public static final Item VELOCITY_GAUGE = registerItem("velocity-gauge", settings -> new Item(settings.stacksTo(1)));

    public static final Item ORE_DETECTOR = registerItem("ore_detector", settings -> new OreDetectorItem(settings.durability(512).rarity(RARE)));

    public static final Item MAGNET = registerItem("magnet", settings -> new MagnetItem(settings.stacksTo(1).rarity(UNCOMMON)));

    public static final Item ROTATOR = registerItem("rotator", settings -> new RotatorItem(settings.durability(1024).stacksTo(1)));

    public static final OctantItem OCTANT = (OctantItem) registerItem("octant", settings -> new OctantItem(settings.durability(DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), null));

    // EnumMap, not HashMap: datagen iterates this to build the octants_enchantable tag, and a
    // HashMap keyed by an enum orders by identity hash -- i.e. differently on every JVM run,
    // which made the generated tag churn on every datagen run.
    public static final Map<DyeColor, OctantItem> COLORED_OCTANT_ITEMS = new EnumMap<>(DyeColor.class);



    // =================================================================================

    // Reinforced Items

    // =================================================================================

    public static final Item REINFORCED_BUNDLE = registerItem("reinforced_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1)));

    public static final Item NETHERITE_BUNDLE = registerItem("netherite_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1).fireResistant().rarity(UNCOMMON)));

    public static final Item QUIVER = registerItem("quiver", settings -> new QuiverItem(settings.stacksTo(1)));

    public static final Item NETHERITE_QUIVER = registerItem("netherite_quiver", settings -> new QuiverItem(settings.stacksTo(1).fireResistant().rarity(UNCOMMON)));

    public static final Item ENDERITE_BUNDLE = registerItem("enderite_bundle", settings -> new ReinforcedBundleItem(settings.stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final Item ENDERITE_QUIVER = registerItem("enderite_quiver", settings -> new QuiverItem(settings.stacksTo(1).fireResistant().rarity(Rarity.EPIC)));



    // Reinforced Block Items

    // Todo: public static final Item REINFORCED_CHEST = registerItem("reinforced_chest", s -> new BlockItem(ModBlocks.REINFORCED_CHEST, s));

    // Todo: public static final Item NETHERITE_CHEST = registerItem("netherite_chest", s -> new BlockItem(ModBlocks.NETHERITE_CHEST, s.fireResistant()));

    public static final Item REINFORCED_HOPPER = registerItem("reinforced_hopper", s -> new BlockItem(ModBlocks.REINFORCED_HOPPER, s));

    public static final Item NETHERITE_HOPPER = registerItem("netherite_hopper", s -> new BlockItem(ModBlocks.NETHERITE_HOPPER, s.fireResistant()));

    public static final Item REINFORCED_PISTON = registerItem("reinforced_piston", s -> new BlockItem(ModBlocks.REINFORCED_PISTON, s));

    public static final Item NETHERITE_PISTON = registerItem("netherite_piston", s -> new BlockItem(ModBlocks.NETHERITE_PISTON, s.fireResistant()));

    public static final Item REINFORCED_BLAST_FURNACE = registerItem("reinforced_blast_furnace", s -> new BlockItem(ModBlocks.REINFORCED_BLAST_FURNACE, s));

    public static final Item NETHERITE_BLAST_FURNACE = registerItem("netherite_blast_furnace", s -> new BlockItem(ModBlocks.NETHERITE_BLAST_FURNACE, s.fireResistant()));

    public static final Item REINFORCED_FURNACE = registerItem("reinforced_furnace", s -> new BlockItem(ModBlocks.REINFORCED_FURNACE, s));

    public static final Item NETHERITE_FURNACE = registerItem("netherite_furnace", s -> new BlockItem(ModBlocks.NETHERITE_FURNACE, s.fireResistant()));

    public static final Item REINFORCED_SMOKER = registerItem("reinforced_smoker", s -> new BlockItem(ModBlocks.REINFORCED_SMOKER, s));

    public static final Item NETHERITE_SMOKER = registerItem("netherite_smoker", s -> new BlockItem(ModBlocks.NETHERITE_SMOKER, s.fireResistant()));



    // =================================================================================

    // TODO: use lang files for text components

    // Trim Templates

    // =================================================================================

    public static final Item GLOWING_TRIM_TEMPLATE = registerItem("glowing_trim_template", settings -> new SmithingTemplateItem(

            Component.literal("Add Radiance").withStyle(ChatFormatting.GRAY),

            Component.literal("Glowing Material").withStyle(ChatFormatting.GRAY),

            Component.literal("Apply to Armor").withStyle(ChatFormatting.GRAY),

            Component.literal("Add Glow Ink").withStyle(ChatFormatting.GRAY),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots")),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/ingot"), Identifier.withDefaultNamespace("container/slot/lapis_lazuli"), Identifier.withDefaultNamespace("container/slot/redstone_dust")),

            settings.stacksTo(64).rarity(RARE)

    ));

    public static final Item EMITTING_TRIM_TEMPLATE = registerItem("emitting_trim_template", settings -> new SmithingTemplateItem(

            Component.literal("Light-source").withStyle(ChatFormatting.GOLD),

            Component.literal("Light function").withStyle(ChatFormatting.GRAY),

            Component.literal("Emits light").withStyle(ChatFormatting.GRAY),

            Component.literal("Add Material").withStyle(ChatFormatting.GRAY),

            List.of(Identifier.withDefaultNamespace("container/slot/helmet"), Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"), Identifier.withDefaultNamespace("container/slot/boots")),

            java.util.List.of(Identifier.withDefaultNamespace("container/slot/ingot"), Identifier.withDefaultNamespace("container/slot/lapis_lazuli"), Identifier.withDefaultNamespace("container/slot/redstone_dust")),

            settings.stacksTo(64).rarity(RARE)

    ));









    // =================================================================================

    // ARMOR (Nur mit 'Item' und Components via Helper)

    // =================================================================================



    public static final Item ENDERITE_HELMET = registerArmor("enderite_helmet", ModArmorMaterials.ENDERITE, ArmorType.HELMET, 42);

    public static final Item ENDERITE_CHESTPLATE = registerArmor("enderite_chestplate", ModArmorMaterials.ENDERITE, ArmorType.CHESTPLATE, 42);

    public static final Item ENDERITE_LEGGINGS = registerArmor("enderite_leggings", ModArmorMaterials.ENDERITE, ArmorType.LEGGINGS, 42);

    public static final Item ENDERITE_BOOTS = registerArmor("enderite_boots", ModArmorMaterials.ENDERITE, ArmorType.BOOTS, 42);





    // =================================================================================

    // Food

    // =================================================================================

    public static final FoodProperties NETHERITE_CARROT_FOOD = new FoodProperties(6, 1.2f, true);

    public static final FoodProperties NETHERITE_APPLE_FOOD = new FoodProperties(8, 1.5f, true);

    public static final FoodProperties ENDERITE_CARROT_FOOD = new FoodProperties(12, 2.4f, true);

    public static final FoodProperties ENDERITE_APPLE_FOOD = new FoodProperties(14, 3.0f, true);



    public static Consumable createNetheriteFoodEffects(boolean isApple) {

        Consumable.Builder builder = Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f);

        if (isApple) {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 4800, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1), 1.0f));

        } else {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2400, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0), 1.0f));

        }



        return builder.build();

    }



    public static Consumable createEnderiteFoodEffects(boolean isApple) {

        Consumable.Builder builder = Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f);

        if (isApple) {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 3000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 2400, 2), 1.0f));

        } else {

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 1200, 1), 1.0f));

            builder.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0), 1.0f));

        }

        return builder.build();

    }



    public static Consumable createEnchantedNetheriteFoodEffects() {

        return Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f)

                // Deutlich stärkere Effekte als der normale Netherite Apfel

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 800, 1), 1.0f)) // Regeneration II (40s)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0), 1.0f))  // Resistance I (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0f)) // Fire Res (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0f)) // Absorption IV (2min, 8 extra Herzen)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.STRENGTH, 2400, 1), 1.0f))   // Strength II (2min)

                .build();

    }



    public static Consumable createEnchantedEnderiteFoodEffects() {

        return Consumable.builder().animation(ItemUseAnimation.EAT).consumeSeconds(1.6f)

                // Massive Effekte für das Endgame

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2), 1.0f)) // Regeneration III (60s)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 1), 1.0f))  // Resistance II (5min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 12000, 0), 1.0f)) // Fire Res (10min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 3600, 4), 1.0f)) // Absorption V (3min, 10 extra Herzen)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 3600, 4), 1.0f)) // Health Boost V (3min)

                .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0), 1.0f)) // Night Vision (5min)

                .build();

    }





    public static final Item NETHERITE_CARROT = registerItem("netherite_carrot", settings -> new Item(settings.food(NETHERITE_CARROT_FOOD, createNetheriteFoodEffects(false)).fireResistant()));

    public static final Item NETHERITE_APPLE = registerItem("netherite_apple", settings -> new Item(settings.food(NETHERITE_APPLE_FOOD, createNetheriteFoodEffects(true)).fireResistant()));

    public static final Item ENDERITE_CARROT = registerItem("enderite_carrot", settings -> new Item(settings.food(ENDERITE_CARROT_FOOD, createEnderiteFoodEffects(false)).fireResistant()));

    public static final Item ENDERITE_APPLE = registerItem("enderite_apple", settings -> new Item(settings.food(ENDERITE_APPLE_FOOD, createEnderiteFoodEffects(true)).fireResistant()));



    public static final Item ENCHANTED_NETHERITE_APPLE = registerItem("enchanted_netherite_apple", settings -> new Item(settings

            .food(NETHERITE_APPLE_FOOD, createEnchantedNetheriteFoodEffects())

            .fireResistant()

            .rarity(Rarity.EPIC)

            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))); // Aktiviert den Schimmer



    public static final Item ENCHANTED_ENDERITE_APPLE = registerItem("enchanted_enderite_apple", settings -> new Item(settings

            .food(ENDERITE_APPLE_FOOD, createEnchantedEnderiteFoodEffects())

            .fireResistant()

            .rarity(Rarity.EPIC)

            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));







    // =================================================================================

    // HILFSMETHODEN

    // =================================================================================



    private static Item registerArmor(String name, ArmorMaterial material, ArmorType type, int durabilityMultiplier) {

        return registerItem(name, settings -> new Item(settings

                .fireResistant()

                .durability(type.getDurability(durabilityMultiplier))

                .repairable(material.repairIngredient())

                .humanoidArmor(material, type)));

    }



    private static ChiselItem registerChisel(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {

        ChiselItem chisel = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, settings.durability(maxDamage).enchantable(enchantability)));

        chisel.setCooldownTicks(cooldownTicks);

        return chisel;

    }



    private static ChiselItem registerSpatula(String name, int maxDamage, int cooldownTicks, int enchantability, ToolMaterial tier) {

        ChiselItem spatula = (ChiselItem) registerItem(name, settings -> new ChiselItem(tier, settings.durability(maxDamage).enchantable(enchantability)));

        spatula.setCooldownTicks(cooldownTicks);

        spatula.setChiselSound(SoundEvents.SAND_FALL);

        spatula.setChiselDirectionCycle(ChiselItem.Direction.BACKWARD);

        spatula.setAsDedicatedSpatula(true);

        return spatula;

    }



    private static BuildingWandItem registerBuildingWand(String name, int maxDamage, int wandSquareDiameter, int enchantability) {

        BuildingWandItem wand = (BuildingWandItem) registerItem(name, settings -> new BuildingWandItem(settings.durability(maxDamage).enchantable(enchantability)));

        wand.setWandSquareDiameter(wandSquareDiameter);

        return wand;

    }



    private static SledgehammerItem registerSledgehammer(String name, int durability, int enchantability, ToolMaterial toolMaterial, int attackDamage, float attackSpeed) {

        return (SledgehammerItem) registerItem(name, settings -> new SledgehammerItem(toolMaterial, attackDamage, attackSpeed, durability, settings.durability(durability).enchantable(enchantability)));

    }



    private static Item registerItem(String name, Function<Item.Properties, Item> function) {

        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);

        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        return Registry.register(BuiltInRegistries.ITEM, id,

                function.apply(new Item.Properties().setId(key)));

    }



    public static void registerModItems() {

        Simplebuilding.LOGGER.info("Registering Mod Items for " + Simplebuilding.MOD_ID);



        // Farbige Octanten registrieren

        for (DyeColor color : DyeColor.values()) {

            String name = "octant_" + color.getName();

            OctantItem coloredItem = (OctantItem) registerItem(name,

                    settings -> new OctantItem(settings.durability(DURABILITY_NETHERITE * DURABILITY_OCTANT).enchantable(ENCHANTABILITY_NETHERITE), color));

            COLORED_OCTANT_ITEMS.put(color, coloredItem);

        }



    }

}

