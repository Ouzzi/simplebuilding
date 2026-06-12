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
import net.minecraft.util.registry.Registries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryKeys;
import net.minecraft.util.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BUILDING_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(Simplebuilding.MOD_ID, "building_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.IRON_CHISEL))
                    .displayName(Text.translatable("itemgroup.simplebuilding.building_items"))
                    .entries((displayContext, entries) -> {
                        // -- Resources ---
                        entries.add(ModItems.DIAMOND_PEBBLE);
                        entries.add(ModItems.CRACKED_DIAMOND);
                        entries.add(ModItems.CRACKED_DIAMOND_BLOCK);
                        entries.add(ModItems.NETHERITE_NUGGET);
                        entries.add(ModItems.ENDERITE_NUGGET);
                        entries.add(ModItems.NETHERITE_APPLE);
                        entries.add(ModItems.NETHERITE_CARROT);

                        entries.add(ModItems.NIHILITH_SHARD);
                        entries.add(ModItems.ASTRALIT_DUST);
                        entries.add(ModItems.RAW_ENDERITE);
                        entries.add(ModItems.ENDERITE_SCRAP);
                        entries.add(ModItems.ENDERITE_INGOT);

                        // -- Food (New) --
                        entries.add(ModItems.ENDERITE_APPLE);
                        entries.add(ModItems.ENDERITE_CARROT);

                        // --- Ores & Raw Blocks ---
                        entries.add(ModItems.NIHILITH_ORE_ITEM);
                        entries.add(ModItems.ASTRALIT_ORE_ITEM);
                        entries.add(ModItems.ENDERITE_BLOCK_ITEM);

                        // --- Functional Blocks ---
                        entries.add(ModItems.CONSTRUCTION_LIGHT);

                        // --- Decoration Blocks (New) ---
                        entries.add(ModItems.POLISHED_END_STONE);
                        entries.add(ModItems.PURPUR_QUARTZ_CHECKER);
                        entries.add(ModItems.LAPIS_QUARTZ_CHECKER);
                        entries.add(ModItems.BLACKSTONE_QUARTZ_CHECKER);
                        entries.add(ModItems.RESIN_QUARTZ_CHECKER);

                        // --- Astral / Nihil Blocks ---
                        entries.add(ModItems.ASTRAL_PURPUR_BLOCK);
                        entries.add(ModItems.NIHIL_PURPUR_BLOCK);
                        entries.add(ModItems.ASTRAL_END_STONE);
                        entries.add(ModItems.NIHIL_END_STONE);

                        // --- Gravity Blocks ---
                        entries.add(ModItems.SUSPENDED_SAND);
                        entries.add(ModItems.SUSPENDED_GRAVEL);
                        entries.add(ModItems.LEVITATING_SAND);
                        entries.add(ModItems.LEVITATING_GRAVEL);

                        // --- Machines & Storage ---
                        // todo chest: entries.add(ModItems.REINFORCED_CHEST);
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
                        entries.add(ModItems.ENDERITE_CHISEL);

                        // --- NEW: Enderite Tools ---
                        entries.add(ModItems.ENDERITE_SWORD);
                        entries.add(ModItems.ENDERITE_SPEAR);
                        entries.add(ModItems.ENDERITE_PICKAXE);
                        entries.add(ModItems.ENDERITE_AXE);
                        entries.add(ModItems.ENDERITE_SHOVEL);
                        entries.add(ModItems.ENDERITE_HOE);

                        // --- NEW: Enderite Armor ---
                        entries.add(ModItems.ENDERITE_HELMET);
                        entries.add(ModItems.ENDERITE_CHESTPLATE);
                        entries.add(ModItems.ENDERITE_LEGGINGS);
                        entries.add(ModItems.ENDERITE_BOOTS);

                        // --- Building Cores ---
                        entries.add(ModItems.COPPER_CORE);
                        entries.add(ModItems.IRON_CORE);
                        entries.add(ModItems.GOLD_CORE);
                        entries.add(ModItems.DIAMOND_CORE);
                        entries.add(ModItems.NETHERITE_CORE);
                        entries.add(ModItems.ENDERITE_CORE);

                        // --- Wands ---
                        entries.add(ModItems.COPPER_BUILDING_WAND);
                        entries.add(ModItems.IRON_BUILDING_WAND);
                        entries.add(ModItems.GOLD_BUILDING_WAND);
                        entries.add(ModItems.DIAMOND_BUILDING_WAND);
                        entries.add(ModItems.NETHERITE_BUILDING_WAND);
                        entries.add(ModItems.ENDERITE_BUILDING_WAND);

                        // --- Sledgehammers ---
                        entries.add(ModItems.STONE_SLEDGEHAMMER);
                        entries.add(ModItems.COPPER_SLEDGEHAMMER);
                        entries.add(ModItems.IRON_SLEDGEHAMMER);
                        entries.add(ModItems.GOLD_SLEDGEHAMMER);
                        entries.add(ModItems.DIAMOND_SLEDGEHAMMER);
                        entries.add(ModItems.NETHERITE_SLEDGEHAMMER);
                        entries.add(ModItems.ENDERITE_SLEDGEHAMMER);

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
                        entries.add(ModItems.ENDERITE_BUNDLE);
                        entries.add(ModItems.QUIVER);
                        entries.add(ModItems.NETHERITE_QUIVER);
                        entries.add(ModItems.ENDERITE_QUIVER);

                        // --- Enchanted Books ---
                        RegistryWrapper.WrapperLookup lookup = displayContext.lookup();
                        RegistryWrapper<Enchantment> enchantmentRegistry = lookup.getOrThrow(RegistryKeys.ENCHANTMENT);

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
                        entries.add(ModItems.GLOWING_TRIM_TEMPLATE);
                        entries.add(ModItems.EMITTING_TRIM_TEMPLATE);

                        entries.add(ModItems.BASIC_UPGRADE_TEMPLATE);
                        entries.add(ModItems.ENDERITE_UPGRADE_TEMPLATE); // NEW


                    }).build());

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for " + Simplebuilding.MOD_ID);
    }

    private static void addEnchantAtMax(ItemGroup.Entries entries, RegistryWrapper<Enchantment> registry, RegistryKey<Enchantment> key) {
        registry.getOptional(key).ifPresent(enchantmentEntry -> {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            builder.add(enchantmentEntry, enchantmentEntry.value().getMaxLevel());
            book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
            entries.add(book);
        });
    }
}

