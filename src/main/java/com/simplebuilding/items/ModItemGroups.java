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
                        entries.add(ModItems.COPPER_BUILDING_CORE);
                        entries.add(ModItems.IRON_BUILDING_CORE);
                        entries.add(ModItems.GOLD_BUILDING_CORE);
                        entries.add(ModItems.DIAMOND_BUILDING_CORE);
                        entries.add(ModItems.NETHERITE_BUILDING_CORE);

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
                        entries.add(ModItems.RANGEFINDER_ITEM);
                        for (DyeColor color : DyeColor.values()) {
                            Item coloredItem = ModItems.COLORED_RANGEFINDERS.get(color);
                            if (coloredItem != null) {
                                entries.add(coloredItem);
                            }
                        }

                        // --- Storage ---
                        entries.add(ModItems.REINFORCED_BUNDLE);

                        // --- Enchanted Books ---
                        RegistryWrapper.WrapperLookup lookup = displayContext.lookup();

                        // FIX: Use .getOrThrow() (Matches the interface you pasted)
                        RegistryWrapper<Enchantment> enchantmentRegistry = lookup.getOrThrow(RegistryKeys.ENCHANTMENT);

                        // 1. Tool Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.FAST_CHISELING, 2);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.CONSTRUCTORS_TOUCH, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.RANGE, 3);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.STRIP_MINER, 3);

                        // 2. Sledgehammer Specific
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.BREAK_THROUGH, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.RADIUS, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.IGNORE_BLOCK_TYPE, 2);

                        // 3. Bundle/Container Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.DEEP_POCKETS, 2);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.FUNNEL, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.QUIVER, 1);

                        // 4. Wand/Construction Utilities
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.MASTER_BUILDER, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.COLOR_PALETTE, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.SURFACE_PLACE, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.BRIDGE, 1);
                        addEnchant(entries, enchantmentRegistry, ModEnchantments.LINE_PLACE, 1);

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
            // 1. Create the Book Item
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

            // 2. Use ItemEnchantmentsComponent to build the stored enchantments
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            builder.add(enchantmentEntry, level);

            // 3. Set the STORED_ENCHANTMENTS component
            book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());

            // 4. Add to tab
            entries.add(book);
        });
    }
}
