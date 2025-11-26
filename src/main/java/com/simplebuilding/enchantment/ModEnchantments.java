package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.custom.LightningStrikerEnchantmentEffect;
import com.simplebuilding.util.ModTags;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.EnchantmentEffectTarget;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    public static final RegistryKey<Enchantment> LIGHTNING_STRIKER =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "lightning_striker"));

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

    public static void bootstrap(Registerable<Enchantment> registerable) {
        var enchantments = registerable.getRegistryLookup(RegistryKeys.ENCHANTMENT);
        var items = registerable.getRegistryLookup(RegistryKeys.ITEM);

        // Fast Chiseling (Max Level III) [CHISEL, SPATULA]
        register(registerable, FAST_CHISELING, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS),
                        5, // Common Rarity
                        2, // Max Level II
                        Enchantment.leveledCost(1, 7), // Minimum Cost
                        Enchantment.leveledCost(15, 9), // Maximum Cost
                        2,
                        AttributeModifierSlot.MAINHAND
                )));

        // Constructor's Touch (Max Level I, Sehr selten) [CHISEL, SPATULA]
        register(registerable, CONSTRUCTORS_TOUCH, Enchantment.builder(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS),
                        1, // Sehr seltene Rarity (oder 10 für selten)
                        1, // Max Level I
                        Enchantment.leveledCost(20, 10),
                        Enchantment.leveledCost(50, 10),
                        1,
                        AttributeModifierSlot.MAINHAND
                )));

        // Range (Max Level III) [CHISEL, SPATULA, MINING_TOOLS]
        register(registerable, RANGE, Enchantment.builder(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        3,
                        3,
                        Enchantment.leveledCost(5, 5),
                        Enchantment.leveledCost(30, 8),
                        2,
                        AttributeModifierSlot.MAINHAND
                )));

        // Deep Pockets (Max Level II, I -> Bundle 128 items, II -> Bundle 256 items, treasure enchantment)
        register(registerable, DEEP_POCKETS, Enchantment.builder(Enchantment.definition(
            items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
            items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
            5,
            2,
            Enchantment.leveledCost(10, 6),
            Enchantment.leveledCost(40, 8),
            2,
            AttributeModifierSlot.MAINHAND)));

        // Master Builder (Max Level I, allows bundle and shulker to place blocks, treasure enchantment)
        register(registerable, MASTER_BUILDER, Enchantment.builder(Enchantment.definition(
            items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
            items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
            2,
            1,
            Enchantment.leveledCost(15, 8),
            Enchantment.leveledCost(50, 10),
            1,
            AttributeModifierSlot.MAINHAND)));

        // Color Palette (Max Level I, allows changing block colors when placing from bundle or shulker, treasure enchantment)
        register(registerable, COLOR_PALETTE, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                2,
                1,
                Enchantment.leveledCost(15, 8),
                Enchantment.leveledCost(50, 10),
                1,
                AttributeModifierSlot.MAINHAND)));

        // Lightning Striker (Max Level 2, I -> small chance, II -> higher chance, exclusive with other damage enchants)
        register(registerable, LIGHTNING_STRIKER, Enchantment.builder(Enchantment.definition(
                items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                5,
                2,
                Enchantment.leveledCost(5, 7),
                Enchantment.leveledCost(25, 9),
                2,
                AttributeModifierSlot.MAINHAND))
                .exclusiveSet(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE_SET))
                .addEffect(EnchantmentEffectComponentTypes.POST_ATTACK,
                EnchantmentEffectTarget.ATTACKER, EnchantmentEffectTarget.VICTIM,
                new LightningStrikerEnchantmentEffect()));
    }


    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}
