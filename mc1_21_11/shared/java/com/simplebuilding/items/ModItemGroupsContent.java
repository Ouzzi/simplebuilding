package com.simplebuilding.items;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ModItemGroupsContent {
    private ModItemGroupsContent() {}

    public static void populate(CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        HolderLookup<Enchantment> enchantmentRegistry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
// -- Resources ---
                        entries.accept(ModItems.DIAMOND_PEBBLE);
                        entries.accept(ModItems.CRACKED_DIAMOND);
                        entries.accept(ModItems.CRACKED_DIAMOND_BLOCK);
                        entries.accept(ModItems.NETHERITE_NUGGET);
                        entries.accept(ModItems.ENDERITE_NUGGET);
                        entries.accept(ModItems.NETHERITE_APPLE);
                        entries.accept(ModItems.NETHERITE_CARROT);

                        entries.accept(ModItems.NIHILITH_SHARD);
                        entries.accept(ModItems.ASTRALIT_DUST);
                        entries.accept(ModItems.RAW_ENDERITE);
                        entries.accept(ModItems.ENDERITE_SCRAP);
                        entries.accept(ModItems.ENDERITE_INGOT);

                        // -- Food (New) --
                        entries.accept(ModItems.ENDERITE_APPLE);
                        entries.accept(ModItems.ENDERITE_CARROT);

                        // --- Ores & Raw Blocks ---
                        entries.accept(ModItems.NIHILITH_ORE_ITEM);
                        entries.accept(ModItems.ASTRALIT_ORE_ITEM);
                        entries.accept(ModItems.ENDERITE_BLOCK_ITEM);

                        // --- Functional Blocks ---
                        entries.accept(ModItems.CONSTRUCTION_LIGHT);

                        // --- Decoration Blocks (New) ---
                        entries.accept(ModItems.POLISHED_END_STONE);
                        entries.accept(ModItems.PURPUR_QUARTZ_CHECKER);
                        entries.accept(ModItems.LAPIS_QUARTZ_CHECKER);
                        entries.accept(ModItems.BLACKSTONE_QUARTZ_CHECKER);
                        entries.accept(ModItems.RESIN_QUARTZ_CHECKER);

                        // --- Astral / Nihil Blocks ---
                        entries.accept(ModItems.ASTRAL_PURPUR_BLOCK);
                        entries.accept(ModItems.NIHIL_PURPUR_BLOCK);
                        entries.accept(ModItems.ASTRAL_END_STONE);
                        entries.accept(ModItems.NIHIL_END_STONE);

                        // --- Gravity Blocks ---
                        entries.accept(ModItems.SUSPENDED_SAND);
                        entries.accept(ModItems.SUSPENDED_GRAVEL);
                        entries.accept(ModItems.LEVITATING_SAND);
                        entries.accept(ModItems.LEVITATING_GRAVEL);

                        // --- Machines & Storage ---
                        // todo chest: entries.accept(ModItems.REINFORCED_CHEST);
                        entries.accept(ModItems.REINFORCED_HOPPER);
                        entries.accept(ModItems.REINFORCED_PISTON);
                        entries.accept(ModItems.NETHERITE_PISTON);
                        entries.accept(ModItems.NETHERITE_HOPPER);
                        entries.accept(ModItems.REINFORCED_FURNACE);
                        entries.accept(ModItems.NETHERITE_FURNACE);
                        entries.accept(ModItems.REINFORCED_SMOKER);
                        entries.accept(ModItems.NETHERITE_SMOKER);
                        entries.accept(ModItems.REINFORCED_BLAST_FURNACE);
                        entries.accept(ModItems.NETHERITE_BLAST_FURNACE);

                        // --- Tools ---
                        entries.accept(ModItems.STONE_CHISEL);
                        entries.accept(ModItems.COPPER_CHISEL);
                        entries.accept(ModItems.IRON_CHISEL);
                        entries.accept(ModItems.GOLD_CHISEL);
                        entries.accept(ModItems.DIAMOND_CHISEL);
                        entries.accept(ModItems.NETHERITE_CHISEL);
                        entries.accept(ModItems.ENDERITE_CHISEL);

                        // --- NEW: Enderite Tools ---
                        entries.accept(ModItems.ENDERITE_SWORD);
                        entries.accept(ModItems.ENDERITE_SPEAR);
                        entries.accept(ModItems.ENDERITE_PICKAXE);
                        entries.accept(ModItems.ENDERITE_AXE);
                        entries.accept(ModItems.ENDERITE_SHOVEL);
                        entries.accept(ModItems.ENDERITE_HOE);

                        // --- NEW: Enderite Armor ---
                        entries.accept(ModItems.ENDERITE_HELMET);
                        entries.accept(ModItems.ENDERITE_CHESTPLATE);
                        entries.accept(ModItems.ENDERITE_LEGGINGS);
                        entries.accept(ModItems.ENDERITE_BOOTS);

                        // --- Building Cores ---
                        entries.accept(ModItems.COPPER_CORE);
                        entries.accept(ModItems.IRON_CORE);
                        entries.accept(ModItems.GOLD_CORE);
                        entries.accept(ModItems.DIAMOND_CORE);
                        entries.accept(ModItems.NETHERITE_CORE);
                        entries.accept(ModItems.ENDERITE_CORE);

                        // --- Wands ---
                        entries.accept(ModItems.COPPER_BUILDING_WAND);
                        entries.accept(ModItems.IRON_BUILDING_WAND);
                        entries.accept(ModItems.GOLD_BUILDING_WAND);
                        entries.accept(ModItems.DIAMOND_BUILDING_WAND);
                        entries.accept(ModItems.NETHERITE_BUILDING_WAND);
                        entries.accept(ModItems.ENDERITE_BUILDING_WAND);

                        // --- Sledgehammers ---
                        entries.accept(ModItems.STONE_SLEDGEHAMMER);
                        entries.accept(ModItems.COPPER_SLEDGEHAMMER);
                        entries.accept(ModItems.IRON_SLEDGEHAMMER);
                        entries.accept(ModItems.GOLD_SLEDGEHAMMER);
                        entries.accept(ModItems.DIAMOND_SLEDGEHAMMER);
                        entries.accept(ModItems.NETHERITE_SLEDGEHAMMER);
                        entries.accept(ModItems.ENDERITE_SLEDGEHAMMER);

                        // --- Rangefinders ---
                        entries.accept(ModItems.OCTANT);
                        for (DyeColor color : DyeColor.values()) {
                            Item coloredItem = ModItems.COLORED_OCTANT_ITEMS.get(color);
                            if (coloredItem != null) {
                                entries.accept(coloredItem);
                            }
                        }

                        // --- Speedometer ---
                        entries.accept(ModItems.VELOCITY_GAUGE);
                        entries.accept(ModItems.ORE_DETECTOR);
                        entries.accept(ModItems.MAGNET);
                        entries.accept(ModItems.ROTATOR);

                        // --- Storage ---
                        entries.accept(ModItems.REINFORCED_BUNDLE);
                        entries.accept(ModItems.NETHERITE_BUNDLE);
                        entries.accept(ModItems.ENDERITE_BUNDLE);
                        entries.accept(ModItems.QUIVER);
                        entries.accept(ModItems.NETHERITE_QUIVER);
                        entries.accept(ModItems.ENDERITE_QUIVER);

                        // --- Enchanted Books ---
                        // 1. Tool Utilities
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.FAST_CHISELING);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.CONSTRUCTORS_TOUCH);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.RANGE);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.STRIP_MINER);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.VEIN_MINER);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.VERSATILITY);

                        // 2. Sledgehammer Specific
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.BREAK_THROUGH);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.RADIUS);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.OVERRIDE);

                        // 3. Bundle/Container Utilities
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.DEEP_POCKETS);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.FUNNEL);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.DRAWER);

                        // 4. Wand/Construction Utilities
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.MASTER_BUILDER);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.COLOR_PALETTE);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.COVER);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.BRIDGE);
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.LINEAR);

                        // 5. Armor Utilities
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.DOUBLE_JUMP);

                        // 6. Miscellaneous
                        addEnchantAtMax(entries, enchantmentRegistry, ModEnchantments.KINETIC_PROTECTION);

                        // 7. Armor Trim
                        entries.accept(ModItems.GLOWING_TRIM_TEMPLATE);
                        entries.accept(ModItems.EMITTING_TRIM_TEMPLATE);

                        entries.accept(ModItems.BASIC_UPGRADE_TEMPLATE);
                        entries.accept(ModItems.ENDERITE_UPGRADE_TEMPLATE); // NEW


                    
    }

    private static void addEnchantAtMax(CreativeModeTab.Output entries, HolderLookup<Enchantment> registry, ResourceKey<Enchantment> key) {
        registry.get(key).ifPresent(enchantmentEntry -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            builder.upgrade(enchantmentEntry, enchantmentEntry.value().getMaxLevel());
            book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
            entries.accept(book);
        });
    }
}
