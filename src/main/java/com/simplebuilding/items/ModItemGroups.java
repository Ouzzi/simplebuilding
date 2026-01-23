package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BUILDING_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Simplebuilding.MOD_ID, "building_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.IRON_SPATULA))
                    .displayName(Text.translatable("itemgroup.simplebuilding.building_items"))
                    .entries((displayContext, entries) -> {
                        // -- Resources ---
                        entries.add(ModItems.DIAMOND_PEBBLE);
                        entries.add(ModItems.CRACKED_DIAMOND);
                        entries.add(ModItems.CRACKED_DIAMOND_BLOCK);
                        entries.add(ModItems.NETHERITE_NUGGET);
                        entries.add(ModItems.NETHERITE_APPLE);
                        entries.add(ModItems.NETHERITE_CARROT);

                        // --- Functional Blocks ---
                        entries.add(ModItems.CONSTRUCTION_LIGHT);

                        // --- Machines & Storage (NEU) ---
                        // todo chest:
                            //  entries.add(ModItems.REINFORCED_CHEST);
                            // entries.add(ModItems.NETHERITE_CHEST);
                        entries.add(ModItems.REINFORCED_HOPPER);
                        entries.add(ModItems.REINFORCED_PISTON);
                        entries.add(ModItems.NETHERITE_PISTON);
                        entries.add(ModItems.NETHERITE_HOPPER);
                        entries.add(ModItems.REINFORCED_FURNACE);
                        entries.add(ModItems.NETHERITE_FURNACE);
                        entries.add(ModItems.REINFORCED_SMOKER);
                        entries.add(ModItems.NETHERITE_SMOKER);
                        entries.add(ModItems.REINFORCED_BLAST_FURNACE);
                        entries.add(ModItems.NETHERITE_BLAST_FURNACE);

                        // --- Tools ---
                        entries.add(ModItems.STONE_CHISEL);
                        entries.add(ModItems.COPPER_CHISEL);
                        entries.add(ModItems.IRON_CHISEL);
                        entries.add(ModItems.GOLD_CHISEL);
                        entries.add(ModItems.DIAMOND_CHISEL);
                        entries.add(ModItems.NETHERITE_CHISEL);

                        entries.add(ModItems.STONE_SPATULA);
                        entries.add(ModItems.COPPER_SPATULA);
                        entries.add(ModItems.IRON_SPATULA);
                        entries.add(ModItems.GOLD_SPATULA);
                        entries.add(ModItems.DIAMOND_SPATULA);
                        entries.add(ModItems.NETHERITE_SPATULA);

                        // --- Building Cores ---
                        entries.add(ModItems.COPPER_CORE);
                        entries.add(ModItems.IRON_CORE);
                        entries.add(ModItems.GOLD_CORE);
                        entries.add(ModItems.DIAMOND_CORE);
                        entries.add(ModItems.NETHERITE_CORE);

                        // --- Wands ---
                        entries.add(ModItems.COPPER_BUILDING_WAND);
                        entries.add(ModItems.IRON_BUILDING_WAND);
                        entries.add(ModItems.GOLD_BUILDING_WAND);
                        entries.add(ModItems.DIAMOND_BUILDING_WAND);
                        entries.add(ModItems.NETHERITE_BUILDING_WAND);

                        // --- Sledgehammers ---
                        entries.add(ModItems.STONE_SLEDGEHAMMER);
                        entries.add(ModItems.COPPER_SLEDGEHAMMER);
                        entries.add(ModItems.IRON_SLEDGEHAMMER);
                        entries.add(ModItems.GOLD_SLEDGEHAMMER);
                        entries.add(ModItems.DIAMOND_SLEDGEHAMMER);
                        entries.add(ModItems.NETHERITE_SLEDGEHAMMER);

                        // --- Rangefinders ---
                        entries.add(ModItems.OCTANT);
                        for (DyeColor color : DyeColor.values()) {
                            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
                            if (coloredItem != null) {
                                entries.add(coloredItem);
                            }
                        }

                        // --- Speedometer ---
                        entries.add(ModItems.VELOCITY_GAUGE);
                        entries.add(ModItems.ORE_DETECTOR);
                        entries.add(ModItems.MAGNET);
                        entries.add(ModItems.ROTATOR);

                        // --- Storage ---
                        entries.add(ModItems.REINFORCED_BUNDLE);
                        entries.add(ModItems.NETHERITE_BUNDLE);
                        entries.add(ModItems.QUIVER);
                        entries.add(ModItems.NETHERITE_QUIVER);

                        // --- Enchanted Books ---
                        RegistryWrapper.WrapperLookup lookup = displayContext.lookup();
                        RegistryWrapper<Enchantment> enchantmentRegistry = lookup.getOrThrow(RegistryKeys.ENCHANTMENT);

                        // 1. Tool Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.FAST_CHISELING, 2);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.RANGE, 3);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.STRIP_MINER, 3);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.VEIN_MINER, 5);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.VERSATILITY, 2);

                        // 2. Sledgehammer Specific
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.BREAK_THROUGH, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.RADIUS, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.OVERRIDE, 2);

                        // 3. Bundle/Container Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.DEEP_POCKETS, 2);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.FUNNEL, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.DRAWER, 8);

                        // 4. Wand/Construction Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.MASTER_BUILDER, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.COLOR_PALETTE, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.COVER, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.BRIDGE, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.LINEAR, 1);

                        // 5. Armor Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.DOUBLE_JUMP, 2);

                        // 6. Miscellaneous
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.KINETIC_PROTECTION, 4);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.DRAWER, 1);

                        // 7. Armor Trim
                        entries.add(ModItems.GLOWING_TRIM_TEMPLATE);
                        entries.add(ModItems.EMITTING_TRIM_TEMPLATE);

                        entries.add(ModItems.BASIC_UPGRADE_TEMPLATE);

                    }).build());

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for " + Simplebuilding.MOD_ID);
    }

    /**
     * Helper method to add an Enchanted Book to the Creative Tab.
     * Checks if the enchantment exists in the registry wrapper before adding.
     *
     * @param entries The ItemGroup entries list
     * @param registry The Enchantment Registry Wrapper
     * @param key The Registry Key of the custom enchantment
     * @param level The level of the enchantment to display on the book
     */
    private static void addEnchant(ItemGroup.Entries entries, RegistryWrapper<Enchantment> registry, RegistryKey<Enchantment> key, int level) {
        registry.getOptional(key).ifPresent(enchantmentEntry -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            builder.add(enchantmentEntry, level);
            book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
            entries.add(book);
        });
    }
}
