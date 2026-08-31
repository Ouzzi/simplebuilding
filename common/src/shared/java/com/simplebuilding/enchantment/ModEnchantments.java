package com.simplebuilding.enchantment;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantmentTags;
import com.simplebuilding.util.ModTags;
// MC 26.2: Paket net.minecraft.advancements.criterion -> net.minecraft.advancements.predicates
// (reiner Paketumzug, Signaturen von DamageSourcePredicate.Builder und TagPredicate unveraendert)
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;

public class ModEnchantments {
    // Keys
    public static final ResourceKey<Enchantment> FAST_CHISELING = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "fast_chiseling")); // final
    public static final ResourceKey<Enchantment> CONSTRUCTORS_TOUCH = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "constructors_touch")); // final
    public static final ResourceKey<Enchantment> RANGE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "range")); // final
    public static final ResourceKey<Enchantment> DEEP_POCKETS = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "deep_pockets")); // final
    public static final ResourceKey<Enchantment> MASTER_BUILDER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "master_builder")); // final
    public static final ResourceKey<Enchantment> FUNNEL = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "funnel")); // final
    public static final ResourceKey<Enchantment> RADIUS = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "radius")); // final
    public static final ResourceKey<Enchantment> OVERRIDE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "override")); // final
    public static final ResourceKey<Enchantment> STRIP_MINER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "strip_miner")); // final
    public static final ResourceKey<Enchantment> COVER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "cover")); // final
    public static final ResourceKey<Enchantment> BRIDGE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "bridge")); // final
    public static final ResourceKey<Enchantment> LINEAR = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "linear")); // final
    public static final ResourceKey<Enchantment> VEIN_MINER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "vein_miner")); // final
    public static final ResourceKey<Enchantment> KINETIC_PROTECTION = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "kinetic_protection")); // final
    public static final ResourceKey<Enchantment> DRAWER = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "drawer")); // final
    public static final ResourceKey<Enchantment> VERSATILITY = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "versatility"));

    // todo new enchantment names
    public static final ResourceKey<Enchantment> COLOR_PALETTE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "color_palette"));
    public static final ResourceKey<Enchantment> BREAK_THROUGH = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "break_through"));
    public static final ResourceKey<Enchantment> DOUBLE_JUMP = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "double_jump"));

    public static final TagKey<DamageType> KINETIC_DAMAGE_TAG = TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "kinetic_damage"));

    public static void bootstrap(BootstrapContext<Enchantment> registerable) {
        var items = registerable.lookup(Registries.ITEM);
        var enchantmentsLookup = registerable.lookup(Registries.ENCHANTMENT);

        // Fast Chiseling (Max Level III, Common) [CHISEL, SPATULA]
        register(registerable, FAST_CHISELING, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CHISEL_TOOLS),
                        5, // Weight (Common)
                        2, // Max Level
                        Enchantment.dynamicCost(1, 10),
                        Enchantment.dynamicCost(20, 10),
                        2,
                        EquipmentSlotGroup.MAINHAND
                )));

        // Constructor's Touch (Max Level I, Treasure, Very Rare) [CHISEL, SPATULA]
        register(registerable, CONSTRUCTORS_TOUCH, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.CONSTRUCTORS_TOUCH_ENCHANTABLE),
                        1, // Sehr seltene Rarity (oder 10 für selten)
                        1, // Max Level I
                        Enchantment.dynamicCost(20, 10),
                        Enchantment.dynamicCost(50, 10),
                        1,
                        EquipmentSlotGroup.MAINHAND
                )));

        // Range (Max Level III, Treasure, Very Rare) [CHISEL, SPATULA, MINING_TOOLS]
        register(registerable, RANGE, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        items.getOrThrow(ModTags.Items.CHISEL_AND_MINING_TOOLS),
                        1, // Weight (Very Rare)
                        3, // Max Level
                        Enchantment.dynamicCost(15, 9),
                        Enchantment.dynamicCost(65, 9),
                        4,
                        EquipmentSlotGroup.MAINHAND
                ))
                .withEffect(
                        EnchantmentEffectComponents.ATTRIBUTES,
                        new EnchantmentAttributeEffect(
                                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchantment.range"),
                                Attributes.BLOCK_INTERACTION_RANGE,
                                LevelBasedValue.perLevel(2.0f, 4.0f),
                                AttributeModifier.Operation.ADD_VALUE
                        )
                ));

        // Deep Pockets (Max Level II -> Bundle 128 items, II -> Bundle 256 items, Treasure, Rare) [BUNDLE]
        register(registerable, DEEP_POCKETS, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight (Rare)
                2, // Max Level
                Enchantment.dynamicCost(15, 10),
                Enchantment.dynamicCost(65, 10),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // Master Builder (Max Level I, allows bundle and shulker to place blocks, Treasure, Very Rare) [BUNDLE, SHULKER, BUILDING_WAND]
        register(registerable, MASTER_BUILDER, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                1, // Weight (Very Rare)
                1, // Max Level
                Enchantment.dynamicCost(25, 25),
                Enchantment.dynamicCost(75, 25),
                8, // Teuer
                EquipmentSlotGroup.MAINHAND
        )));

        // Color Palette (Max Level I, allows changing block colors when placing from bundle or shulker, Treasure, Rare) [BUNDLE, SHULKER, BUILDING_WAND]
        register(registerable, COLOR_PALETTE, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(65, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // Funnel
        register(registerable, FUNNEL, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE), // Nur Bundles/Shulker
                items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                2, // Weight rare
                2, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(55, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // Drawer (New Feature)
        register(registerable, DRAWER, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                        items.getOrThrow(ModTags.Items.BUNDLE_ENCHANTABLE),
                        1, // Very Rare
                        8, // Max Level
                        Enchantment.dynamicCost(25, 25),
                        Enchantment.dynamicCost(75, 25),
                        8,
                        EquipmentSlotGroup.MAINHAND
                ))
                .exclusiveWith(enchantmentsLookup.getOrThrow(ModEnchantmentTags.BUILDER_EXCLUSIVE_SET)));
        
        // 9. BREAK_TROUGH (Max Level 1, Treasure, Rare) [SLEDGEHAMMER]
        register(registerable, BREAK_THROUGH, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE), // Nur Bundles/Shulker
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                2, // Weight rare
                2, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(55, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // 10. RADIUS (Max Level 1, Treasure, Very Rare, 5x5 mining) [SLEDGEHAMMER]
        register(registerable, RADIUS, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                1, // Weight (Very Rare)
                1, // Max Level
                Enchantment.dynamicCost(25, 20),
                Enchantment.dynamicCost(75, 20),
                8, // Teurer
                EquipmentSlotGroup.MAINHAND
        )));

        // 11. IGNORE_BLOCKTYPE (Max Level 2, Treasure, Rare) [SLEDGEHAMMER]
        register(registerable, OVERRIDE, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.SLEDGEHAMMER_ENCHANTABLE),
                2, // Weight (Rare)
                2, // Max Level
                Enchantment.dynamicCost(20, 15),
                Enchantment.dynamicCost(70, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // 12. STRIP_MINER (Max Level 3, Treasure, Very Rare, mines multiple blocks in a row) [PICKAXE]
        register(registerable, STRIP_MINER, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.PICKAXES),
                items.getOrThrow(ItemTags.PICKAXES),
                1, // Weight (Very Rare)
                3, // Max Level
                Enchantment.dynamicCost(20, 10),
                Enchantment.dynamicCost(60, 10),
                4,
                EquipmentSlotGroup.MAINHAND
        )));

        // 13. COVER (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, COVER, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(55, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        ))
        // IMPLEMENTIERT: Exklusivität gegen Bridge/Linear
        .exclusiveWith(enchantmentsLookup.getOrThrow(ModEnchantmentTags.COVER_EXCLUSIVE_SET)));

        // 14. BRIDGE (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, BRIDGE, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(55, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        ))
        // IMPLEMENTIERT: Exklusivität gegen Cover (kompatibel mit Linear)
        .exclusiveWith(enchantmentsLookup.getOrThrow(ModEnchantmentTags.WAND_MODIFIER_EXCLUSIVE_SET)));

        // 15. LINEAR (Max Level 1, Treasure, Rare) [BUILDING_WAND]
        register(registerable, LINEAR, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                items.getOrThrow(ModTags.Items.BUILDING_WAND_ENCHANTABLE),
                2, // Weight (Rare)
                1, // Max Level
                Enchantment.dynamicCost(15, 15),
                Enchantment.dynamicCost(55, 15),
                4,
                EquipmentSlotGroup.MAINHAND
        ))
        .exclusiveWith(enchantmentsLookup.getOrThrow(ModEnchantmentTags.WAND_MODIFIER_EXCLUSIVE_SET)));

        // 16. DOUBLE_JUMP (Max Level 2, Treasure, Rare) [BOOTS]
        register(registerable, DOUBLE_JUMP, Enchantment.enchantment(Enchantment.definition(
                items.getOrThrow(ItemTags.FOOT_ARMOR), // Target Boots
                items.getOrThrow(ItemTags.FOOT_ARMOR),
                2, // Weight (Rare)
                2, // Max Level
                Enchantment.dynamicCost(20, 15),
                Enchantment.dynamicCost(70, 15),
                4,
                EquipmentSlotGroup.FEET
        )));

        // 17. VEIN_MINER (Max Level 5, Treasure, Very Rare) [PICKAXE, AXE]
        register(registerable, VEIN_MINER, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.VEINMINE_ENCHANTABLE), // Ziel: Tools
                        items.getOrThrow(ModTags.Items.VEINMINE_ENCHANTABLE),
                        1, // Weight (Very Rare)
                        5, // Max Level
                        Enchantment.dynamicCost(20, 10),
                        Enchantment.dynamicCost(70, 10),
                        4,
                        EquipmentSlotGroup.MAINHAND
                ))
                .exclusiveWith(enchantmentsLookup.getOrThrow(ModEnchantmentTags.MINING_EXCLUSIVE_SET)));

        // 18. KINETIC_PROTECTION (Max Level IV, similar to Protection, Weight Rare) [ALL ARMOR]
        register(registerable, KINETIC_PROTECTION, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE), // All Armor
                        items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        5, // Weight
                        4, // Max Level
                        Enchantment.dynamicCost(10, 8),
                        Enchantment.dynamicCost(20, 8),
                        4,
                        EquipmentSlotGroup.ARMOR
                ))
                .exclusiveWith(enchantmentsLookup.getOrThrow(EnchantmentTags.ARMOR_EXCLUSIVE))
                .withEffect(EnchantmentEffectComponents.DAMAGE_PROTECTION, new AddValue(LevelBasedValue.perLevel(2.5f, 2.5f)), DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(KINETIC_DAMAGE_TAG)))));

        register(registerable, VERSATILITY, Enchantment.enchantment(
                Enchantment.definition(
                        items.getOrThrow(ItemTags.MINING_ENCHANTABLE), // Kann auf alle Werkzeuge
                        1,
                        2,
                        Enchantment.dynamicCost(15, 0), // Min Cost
                        Enchantment.dynamicCost(65, 0), // Max Cost
                        4, // Anvil Cost
                        EquipmentSlotGroup.HAND
                )
        ));
    }

    private static void register(BootstrapContext<Enchantment> registry, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.identifier()));
    }
}