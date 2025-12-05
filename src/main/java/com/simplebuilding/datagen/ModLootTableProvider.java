package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetComponentsLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

// List all Loot Table types:
// 1. STRONGHOLD LIBRARY CHEST: (RANGE, QUIVER, MASTER_BUILDER, BRIDGE)
// 2. END CITY: (RANGE, QUIVER, MASTER_BUILDER, IGNORE_BLOCK_TYPES, BRIDGE), (DIAMOND_CHISEL_ENCHANTED, DIAMOND_SPATULA_ENCHANTED,DIAMOND_BUILDING_WAND_ENCHANTED, DIAMOND_SLEDGEHAMMER_ENCHANTED, DIAMOND_BUILDING_CORE)
// 4. ANCIENT CITY: (DEEP POCKETS, RADIUS), (OCTANT_ENCHANTED, DIAMOND_SLEDGEHAMMER)
// 5. BASTION: (FUNNEL, BREAK THROUGH, SWIFT_RIDE, HORSE_JUMP), (GOLD_SLEDGEHAMMER, GOLD_BUILDING_CORE, NETHERITE_BUILDING_CORE)
// 6. NETHER BRIDGE: (FUNNEL, BREAK THROUGH, STRIP_MINER, SWIFT_RIDE), (GOLD_BUILDING_CORE, OCTANT_ENCHANTED)
// 7. PILLAGER OUTPOST: (COLOR PALETTE, SURFACE PLACE, LINE PLACE), (OCTANT)
// 8. WOODLAND MANSION: (COLOR PALETTE, SURFACE PLACE, LINE PLACE), (IRON_BUILDING_WAND, IRON_BUILDING_CORE)
// 9. BURIED TREASURE: (CONSTRUCTORS TOUCH, FAST CHISEL), (GOLD_CHISEL, DIAMOND_SPATULA)
// 10. SIMPLE DUNGEON: (FAST CHISEL, FUNNEL, BREAK THROUGH), (REINFORCED_BUNDLE)
// 11. SHIPWRECK TREASURE: (FAST CHISEL), (REINFORCED_BUNDLE)
// 12. IGLOO: (CONSTRUCTORS TOUCH, FAST CHISEL), (DIAMOND_CHISEL, IRON_SPATULA)
// 13. ABANDONED MINESHAFT: (FAST CHISEL, STRIP_MINER I), (REINFORCED_BUNDLE_ENCHANTED)
// 14. VAULT: (CONSTRUCTORS TOUCH, FAST_CHISEL, HORSE_JUMP, SWIFT_RIDE), (DIAMOND_BUILDING_CORE)

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        RegistryWrapper.Impl<Enchantment> impl = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
    }

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) -> {

            if (!Simplebuilding.getConfig().worldGen.enableLootTableChanges) {
                return;
            }

            var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);

            // 1. STRONGHOLD LIBRARY (RANGE, QUIVER, MASTER_BUILDER, BRIDGE)
            if (LootTables.STRONGHOLD_LIBRARY_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.RANGE, 1, enchantments, 5))
                        .with(enchantedBook(ModEnchantments.QUIVER, 1, enchantments, 5))
                        .with(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 2))
                        .with(enchantedBook(ModEnchantments.BRIDGE, 1, enchantments, 5));
                tableBuilder.pool(pool);
            }

            // 2. END CITY (RANGE, QUIVER, MASTER_BUILDER, IGNORE_BLOCK_TYPES, BRIDGE)
            // + DIAMOND CHISEL/SPATULA
            if (LootTables.END_CITY_TREASURE_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 2))
                        .with(enchantedBook(ModEnchantments.IGNORE_BLOCK_TYPE, 2, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 5))
                        .with(ItemEntry.builder(ModItems.DIAMOND_CHISEL).weight(5))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SPATULA).weight(5))
                        .with(ItemEntry.builder(ModItems.DIAMOND_BUILDING_WAND).weight(2))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SLEDGEHAMMER).weight(2))
                        .with(ItemEntry.builder(ModItems.DIAMOND_BUILDING_CORE).weight(2));
                tableBuilder.pool(pool);
            }

            // 4. ANCIENT CITY
            if (LootTables.ANCIENT_CITY_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.DEEP_POCKETS, 2, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.RADIUS, 1, enchantments, 5))
                        .with(ItemEntry.builder(ModItems.OCTANT).weight(10))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SLEDGEHAMMER).weight(5));
                tableBuilder.pool(pool);
            }

            // 5. BASTION (FUNNEL, BREAK THROUGH)
            // + GOLD SLEDGEHAMMER, CORES
            if (LootTables.BASTION_TREASURE_CHEST.equals(key) || LootTables.BASTION_OTHER_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 2))
                        .with(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 15))
                        .with(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.SWIFT_RIDE, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.HORSE_JUMP, 1, enchantments, 5))
                        .with(ItemEntry.builder(ModItems.GOLD_SLEDGEHAMMER).weight(10))
                        .with(ItemEntry.builder(ModItems.GOLD_BUILDING_CORE).weight(10))
                        .with(ItemEntry.builder(ModItems.NETHERITE_BUILDING_CORE).weight(1)); // Sehr selten
                tableBuilder.pool(pool);
            }

            // 6. NETHER BRIDGE (FUNNEL, BREAK THROUGH, STRIP_MINER, SWIFT_RIDE)
            if (LootTables.NETHER_BRIDGE_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.STRIP_MINER, 2, enchantments, 5))
                        .with(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 15))
                        .with(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.SWIFT_RIDE, 1, enchantments, 10))
                        .with(ItemEntry.builder(ModItems.GOLD_BUILDING_CORE).weight(5))
                        .with(ItemEntry.builder(ModItems.OCTANT).weight(5));
                tableBuilder.pool(pool);
            }

            // 7. PILLAGER OUTPOST (COLOR PALETTE, SURFACE PLACE, LINE PLACE)
            if (LootTables.PILLAGER_OUTPOST_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 15))
                        .with(enchantedBook(ModEnchantments.SURFACE_PLACE, 1, enchantments, 15))
                        .with(enchantedBook(ModEnchantments.LINE_PLACE, 1, enchantments, 15))
                        .with(ItemEntry.builder(ModItems.OCTANT).weight(10));
                tableBuilder.pool(pool);
            }

            // 8. WOODLAND MANSION
            if (LootTables.WOODLAND_MANSION_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(1, 2))
                        .with(enchantedBook(ModEnchantments.SURFACE_PLACE, 1, enchantments, 20))
                        .with(enchantedBook(ModEnchantments.LINE_PLACE, 1, enchantments, 20))
                        .with(ItemEntry.builder(ModItems.IRON_BUILDING_WAND).weight(10))
                        .with(ItemEntry.builder(ModItems.IRON_BUILDING_CORE).weight(10));
                tableBuilder.pool(pool);
            }

            // 9. BURIED TREASURE (CONSTRUCTORS TOUCH, FAST CHISEL)
            if (LootTables.BURIED_TREASURE_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 10))
                        .with(ItemEntry.builder(ModItems.GOLD_CHISEL).weight(15))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SPATULA).weight(5));
                tableBuilder.pool(pool);
            }

            // 10. SIMPLE DUNGEON / 13. MINESHAFT / 11. SHIPWRECK / 12. IGLOO
            if (LootTables.SIMPLE_DUNGEON_CHEST.equals(key) || LootTables.ABANDONED_MINESHAFT_CHEST.equals(key)
                    || LootTables.SHIPWRECK_TREASURE_CHEST.equals(key) || LootTables.IGLOO_CHEST_CHEST.equals(key)) {

                LootPool.Builder pool = LootPool.builder().rolls(UniformLootNumberProvider.create(0, 1));

                // Common Stuff
                pool.with(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 20));
                pool.with(ItemEntry.builder(ModItems.REINFORCED_BUNDLE).weight(10));

                if (LootTables.ABANDONED_MINESHAFT_CHEST.equals(key)) {
                    pool.with(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 10));
                }
                if (LootTables.IGLOO_CHEST_CHEST.equals(key)) {
                    pool.with(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 10));
                    pool.with(ItemEntry.builder(ModItems.DIAMOND_CHISEL).weight(5));
                    pool.with(ItemEntry.builder(ModItems.IRON_SPATULA).weight(5));
                }
                if (LootTables.SIMPLE_DUNGEON_CHEST.equals(key)) {
                    pool.with(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 10));
                    pool.with(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 10));
                }

                tableBuilder.pool(pool);
            }

            // 14. VAULT (Trial Chambers)
            if (LootTables.TRIAL_CHAMBERS_REWARD_COMMON_CHEST.equals(key) || LootTables.TRIAL_CHAMBERS_REWARD_RARE_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.builder().rolls(UniformLootNumberProvider.create(0, 1))
                        .with(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.HORSE_JUMP, 1, enchantments, 10))
                        .with(enchantedBook(ModEnchantments.SWIFT_RIDE, 1, enchantments, 10))
                        .with(ItemEntry.builder(ModItems.DIAMOND_BUILDING_CORE).weight(2));
                tableBuilder.pool(pool);
            }
        });
    }

    /**
     * Helper Methode um ein Enchanted Book mit einem bestimmten Enchantment zu erstellen.
     */
    private static LeafEntry.Builder<?> enchantedBook(
            RegistryKey<Enchantment> enchantKey,
            int level,
            RegistryWrapper<Enchantment> registry,
            int weight) {

        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        builder.add(registry.getOrThrow(enchantKey), level);
        ItemEnchantmentsComponent component = builder.build();

        return ItemEntry.builder(Items.ENCHANTED_BOOK)
                .weight(weight)
                .apply(SetComponentsLootFunction.builder(DataComponentTypes.STORED_ENCHANTMENTS, component));
    }
}