package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.ModTags;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.effect.AttributeEnchantmentEffect;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/*
    Enchantments:
            1 - constructors touch, (Treasure enchantment, very rare), [chisel, spatula, rangefinder]
                > (custom chiel maps for each tier netherite obsidian to crying obsidian; diamond stone to cobble; iron cobble to mossy cobble; stone log to stripped log, ...;
            2 - fast chisel, [chisel, spatula]
                > (faster cooldown for chisel-tools)
            3 - range, (Treasure enchantment, very rare), [chisel, spatula, pickaxe, axe, shulker, bundle]
                > (bigger range for mining and chisel and building tools (bundle, shulker)
            4 - Quiver enchant, (Treasure enchantment, rare), [shulker, bundle]
                > (Pfeile aus bundle oder shulker in den Bogen laden) (entweder quiver enchant oder master builder enchant) (nicht kombinierbar mit color palette)
            5 - master builder, (Treasure enchantment, very rare), [shulker, bundle, building wand]
                > (places first block of shulker/bundle by right-clicking) (building wand allows to pick from other master-builder enchanted shulkers or bundles)
            6 - color palette, (Treasure enchantment, rare), [shulker, bundle, building wand]
                > (changes picking order first block to random but with weighted probability)
            7 - deep pockets, (Treasure enchantment, rare), [bundle]
                > (increases bundle capacity: I = 128, II = 256)
            8 - funnel, (Treasure enchantment, common), [bundle, shulker]
                > (automatically picks up items while sneaking and in hand))
            9 - break trough, (Treasure enchantment, rare), [slegehammer]
                > (automatically picks up items while sneaking and in hand))


 */

public class ModEnchantments {
    // Keys
    public static final RegistryKey<Enchantment> FAST_CHISELING =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "fast_chiseling"));
    public static final RegistryKey<Enchantment> CONSTRUCTORS_TOUCH =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "constructors_touch"));
    public static final RegistryKey<Enchantment> RANGE =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "range"));
    public static final RegistryKey<Enchantment> DEEP_POCKETS =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "deep_pockets"));
    public static final RegistryKey<Enchantment> MASTER_BUILDER =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "master_builder"));
    public static final RegistryKey<Enchantment> COLOR_PALETTE =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "color_palette"));
    public static final RegistryKey<Enchantment> QUIVER =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "quiver"));
    public static final RegistryKey<Enchantment> FUNNEL =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "funnel"));
    public static final RegistryKey<Enchantment> BREAK_THROUGH = 
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "break_through"));



    public static void bootstrap(Registerable<Enchantment> registerable) {
        var items = registerable.getRegistryLookup(RegistryKeys.ITEM);

        // Fast Chiseling (Max Level III, Common) [CHISEL, SPATULA]
        register(registerable, FAST_CHISELING, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS),
                        5, // Weight (Common)
                        2, // Max Level
                        Enchantment.leveledCost(1, 10),
                        Enchantment.leveledCost(20, 10),
                        2,
                        AttributeModifierSlot.MAINHAND
                )));

        // Constructor's Touch (Max Level I, Treasure, Very Rare) [CHISEL, SPATULA]
        register(registerable, CONSTRUCTORS_TOUCH, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE),
                        1, // Sehr seltene Rarity (oder 10 für selten)
                        1, // Max Level I
                        Enchantment.leveledCost(20, 10),
                        Enchantment.leveledCost(50, 10),
                        1,
                        AttributeModifierSlot.MAINHAND
                )));

        // Range (Max Level III, Treasure, Very Rare) [CHISEL, SPATULA, MINING_TOOLS]
        register(registerable, RANGE, Enchantment.builder(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        1, // Weight (Very Rare)
                        3, // Max Level
                        Enchantment.leveledCost(15, 9),
                        Enchantment.leveledCost(65, 9),
                        4,
                        AttributeModifierSlot.MAINHAND
                ))
                .addEffect(
                        EnchantmentEffectComponentTypes.ATTRIBUTES,
                        new AttributeEnchantmentEffect(
                                Identifier.of(Simplebuilding.MOD_ID, "enchantment.range"),
                                EntityAttributes.BLOCK_INTERACTION_RANGE,
                                EnchantmentLevelBasedValue.linear(2.0f, 4.0f),
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )
                ));

        // Deep Pockets (Max Level II -> Bundle 128 items, II -> Bundle 256 items, Treasure, Rare) [BUNDLE]
        register(registerable, DEEP_POCKETS, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight (Rare)
                2, // Max Level
                Enchantment.leveledCost(15, 10),
                Enchantment.leveledCost(65, 10),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // Master Builder (Max Level I, allows bundle and shulker to place blocks, Treasure, Very Rare) [BUNDLE, SHULKER, BUILDING_WAND]
        register(registerable, MASTER_BUILDER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                1, // Weight (Very Rare)
                1, // Max Level
                Enchantment.leveledCost(25, 25),
                Enchantment.leveledCost(75, 25),
                8, // Sehr teuer im Amboss
                AttributeModifierSlot.MAINHAND
        )));

        // Color Palette (Max Level I, allows changing block colors when placing from bundle or shulker, Treasure, Rare) [BUNDLE, SHULKER, BUILDING_WAND]
        register(registerable, COLOR_PALETTE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(65, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 7. QUIVER (Rare/Treasure, Max Level 1, Treasure, Rare) [BUNDLE]
        register(registerable, QUIVER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE), // Nur Bundles
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(20, 20),
                Enchantment.leveledCost(70, 20),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 8. FUNNEL (Common, Max Level 1, Treasure, Rare) [BUNDLE, SHULKER]
        register(registerable, FUNNEL, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE), // Nur Bundles/Shulker
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight rare
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(55, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));
    }

    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}