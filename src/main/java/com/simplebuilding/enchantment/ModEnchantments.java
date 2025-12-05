package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.datagen.ModEnchantmentTagProvider;
import com.simplebuilding.util.ModTags;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.AttributeEnchantmentEffect;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    // Keys
    public static final RegistryKey<Enchantment> FAST_CHISELING = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "fast_chiseling"));
    public static final RegistryKey<Enchantment> CONSTRUCTORS_TOUCH = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "constructors_touch"));
    public static final RegistryKey<Enchantment> RANGE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "range"));
    public static final RegistryKey<Enchantment> DEEP_POCKETS = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "deep_pockets"));
    public static final RegistryKey<Enchantment> MASTER_BUILDER = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "master_builder"));
    public static final RegistryKey<Enchantment> COLOR_PALETTE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "color_palette"));
    public static final RegistryKey<Enchantment> QUIVER = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "quiver"));
    public static final RegistryKey<Enchantment> FUNNEL = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "funnel"));
    public static final RegistryKey<Enchantment> BREAK_THROUGH = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "break_through"));
    public static final RegistryKey<Enchantment> RADIUS = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "radius"));
    public static final RegistryKey<Enchantment> IGNORE_BLOCK_TYPE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "ignore_block_type")); // TODO New name
    public static final RegistryKey<Enchantment> STRIP_MINER = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "strip_miner"));
    public static final RegistryKey<Enchantment> SURFACE_PLACE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "surface_place")); // TODO New name
    public static final RegistryKey<Enchantment> BRIDGE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "bridge"));
    public static final RegistryKey<Enchantment> LINE_PLACE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "line_place")); // TODO New name
    public static final RegistryKey<Enchantment> SWIFT_RIDE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "swift_ride")); // TODO New name
    public static final RegistryKey<Enchantment> HORSE_JUMP = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "horse_jump")); // TODO New name

    public static void bootstrap(Registerable<Enchantment> registerable) {
        var items = registerable.getRegistryLookup(RegistryKeys.ITEM);

        var enchantmentsLookup = registerable.getRegistryLookup(RegistryKeys.ENCHANTMENT);

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
                8, // Teuer
                AttributeModifierSlot.MAINHAND
        ))
        // FIX: Variable 'enchantmentsLookup' statt 'entries'
        .exclusiveSet(enchantmentsLookup.getOrThrow(ModEnchantmentTagProvider.QUIVER_EXCLUSIVE_SET)));

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
        ))
        // FIX: Variable 'enchantmentsLookup'
        .exclusiveSet(enchantmentsLookup.getOrThrow(ModEnchantmentTagProvider.QUIVER_EXCLUSIVE_SET)));

        // 7. QUIVER (Max Level 1, Treasure, Rare) [BUNDLE]
        register(registerable, QUIVER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE), // Nur Bundles
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(20, 20),
                Enchantment.leveledCost(70, 20),
                4,
                AttributeModifierSlot.MAINHAND
        ))
        // FIX: Variable 'enchantmentsLookup'
        .exclusiveSet(enchantmentsLookup.getOrThrow(ModEnchantmentTagProvider.BUILDER_EXCLUSIVE_SET)));

        // 8. FUNNEL (Max Level 1, Treasure, Rare) [BUNDLE, SHULKER]
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
        
        // 9. BREAK_TROUGH (Max Level 1, Treasure, Rare) [SLEDGEHAMMER]
        register(registerable, BREAK_THROUGH, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE), // Nur Bundles/Shulker
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                2, // Weight rare
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(55, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )).exclusiveSet(enchantmentsLookup.getOrThrow(ModEnchantmentTagProvider.RADIUS_EXCLUSIVE_SET)));

        // 10. RADIUS (Max Level 1, Treasure, Very Rare, 5x5 mining) [SLEDGEHAMMER]
        register(registerable, RADIUS, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                1, // Weight (Very Rare)
                1, // Max Level
                Enchantment.leveledCost(25, 20),
                Enchantment.leveledCost(75, 20),
                8, // Teurer
                AttributeModifierSlot.MAINHAND
        )).exclusiveSet(enchantmentsLookup.getOrThrow(ModEnchantmentTagProvider.BREAK_THROUGH_EXCLUSIVE_SET)));

        // 11. IGNORE_BLOCKTYPE (Max Level 2, Treasure, Rare) [SLEDGEHAMMER]
        register(registerable, IGNORE_BLOCK_TYPE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                2, // Weight (Rare)
                2, // Max Level
                Enchantment.leveledCost(20, 15),
                Enchantment.leveledCost(70, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 12. STRIP_MINER (Max Level 3, Treasure, Very Rare, mines multiple blocks in a row) [PICKAXE]
        register(registerable, STRIP_MINER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ItemTags.PICKAXES),
                items.getOrThrow(ItemTags.PICKAXES),
                1, // Weight (Very Rare)
                3, // Max Level
                Enchantment.leveledCost(20, 10),
                Enchantment.leveledCost(60, 10),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 13. SURFACE_PLACE (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, SURFACE_PLACE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(55, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 14. BRIDGE (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, BRIDGE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(55, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 15. LINE_PLACE (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, LINE_PLACE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.leveledCost(15, 15),
                Enchantment.leveledCost(55, 15),
                4,
                AttributeModifierSlot.MAINHAND
        )));

        // 16. SWIFT_RIDE (Max Level III, Common) [SADDLE]
        register(registerable, SWIFT_RIDE, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.SADDLE_ENCHANTABLE), // Ziel: Saddle
                        items.getOrThrow(ModTags.Items.SADDLE_ENCHANTABLE),
                        2, // Weight (Rare)
                        3, // Max Level
                        Enchantment.leveledCost(15, 10),
                        Enchantment.leveledCost(65, 10),
                        4,
                        AttributeModifierSlot.ARMOR
                )));

        // 17. HORSE_JUMP (Max Level III, Common) [SADDLE]
        register(registerable, HORSE_JUMP, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.HORSE_ARMOR_ENCHANTABLE), // Ziel: Saddle
                        items.getOrThrow(ModTags.Items.HORSE_ARMOR_ENCHANTABLE),
                        3, // Weight (Uncommon)
                        3, // Max Level
                        Enchantment.leveledCost(20, 10),
                        Enchantment.leveledCost(70, 10),
                        4,
                        AttributeModifierSlot.ARMOR
                )));

}

    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}