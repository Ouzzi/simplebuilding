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

/*
    Enchantments:
            1 constructors touch I, (Trial, Treasure, very rare), [chisel, spatula, octant]
                Trial I, Burried Tresure I
                > (custom chiel maps for each tier netherite obsidian to crying obsidian; diamond stone to cobble; iron cobble to mossy cobble; stone log to stripped log, ...;
            2 fast chisel II,  (Trial/haste, Treasure, very rare), [chisel, spatula]
                Buried Treasure II, Spawner II, Villager I
                > (faster  for chisel-tools)
            3 range III, (End, Treasure, very rare), [chisel, spatula, pickaxe, axe, shulker, bundle]
                End City III, Library II, Villager I
                > (bigger range for mining and chisel and building tools (bundle, shulker)
            4 Quiver I, (End, Treasure, rare), [shulker, bundle]
                End City I, Library I
                > (Pfeile aus bundle oder shulker in den Bogen laden) (entweder quiver enchant oder master builder enchant) (nicht kombinierbar mit color palette)
            5 master builder I, (End, Treasure, very rare), [shulker, bundle, building wand]
                End City I, Library I, Villager I
                > (places first block of shulker/bundle by right-clicking) (building wand allows to pick from other master-builder enchanted shulkers or bundles)
            6 color palette I, (Pilage, Treasure, rare), [shulker, bundle, building wand]
                Pillager Outpost I, Villager I
                > (changes picking order first block to random but with weighted probability)
            7 deep pockets II, (Deep Dark, Treasure, rare), [bundle]
                Deep Dark II
                > (increases bundle capacity: I = 128, II = 256)
            8 funnel I, (Nether/Spawner, Treasure, common), [bundle, shulker]
                Bastion I, Fortres I
                > (automatically picks up items while sneaking and in hand))
            9 break trough I, (Nether, Treasure, rare), [slegehammer]
                Bastion I
                > (automatically picks up items while sneaking and in hand))
            10 radius I, (Deep Dark, Treasure, rare), [slegehammer]
                deep dark I
                > (5x5 instead of 3x3, not compatible with brak_trough)
            11 ignore blocktype II, (End, Treasure, rare), [slegehammer]
                 End Ship II, End City I
                > (ignores blocktypes while destroying blocks, when not supported block double durrability cost, lvl 1 only supportrd blocks, lvl2 also not supported blocks)
            12 strip miner III (Nether, Treasure, very rare), [pickaxe]
                Fortress III, villager I
                > (I mines 2 blocks in a row, II mines 3 blocks in a row, III mines 5 blochs in a row)
            13 surface place I, (Pillage, Treasure, rare), [building wand]
                Pillager Outpost I, Woodlin Mantion, Villager I
                > (places blocks on surface instead of plane)
                    // base block normal,
                    // surrounding blocks maximal depth diviation from base block 2,
                    // next ring maximum diviation from previous is 1
                    //> not with bridge compatible
            14 bridge I, (end, Treasure, rare), [building wand]
                End City I, Library I
                > (if placing a block place on the side of the edge of that block a line of blocks)
                    // if targeting the front edge, place on the front side, if right the right side and so on
                    //> not with surface place compatible
            15 line place I, (Pillage, Treasure, rare), [building wand]
                Pillager Outpost I, Woodlin Mantion, Villager I
                > (places a line of blocks in one axis direction depending on player looking direction)
                    // deafault horizontal to player
                    // when near the block edge front or back change direction to front/back
                    // with surface place same logic but only the line instead a sqare

 */

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
    public static final RegistryKey<Enchantment> IGNORE_BLOCK_TYPE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "ignore_block_type"));
    public static final RegistryKey<Enchantment> STRIP_MINER = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "strip_miner"));
    public static final RegistryKey<Enchantment> SURFACE_PLACE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "surface_place"));
    public static final RegistryKey<Enchantment> BRIDGE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "bridge"));
    public static final RegistryKey<Enchantment> LINE_PLACE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(Simplebuilding.MOD_ID, "line_place"));

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

}

    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}