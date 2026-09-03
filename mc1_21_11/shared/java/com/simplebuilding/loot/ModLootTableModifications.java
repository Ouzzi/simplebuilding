package com.simplebuilding.loot;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class ModLootTableModifications {
    public interface Editor {
        void addPool(LootPool.Builder pool);
        void addBuiltPool(LootPool pool);
    }

    private ModLootTableModifications() {
    }

    public static void apply(ResourceKey<LootTable> key, Editor editor, HolderLookup.Provider registry) {
if (!Simplebuilding.getConfig().worldGen.enableLootTableChanges) {
                return;
            }

            var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

            // 1. STRONGHOLD LIBRARY CHEST
            // (RANGE, QUIVER, MASTER_BUILDER)
            if (BuiltInLootTables.STRONGHOLD_LIBRARY.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 5))
                        .add(enchantedBook(ModEnchantments.RANGE, 2, enchantments, 2))
                        .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 1))
                        .add(enchantedBook(ModEnchantments.VERSATILITY, 1, enchantments, 1))
                        .add(enchantedBook(ModEnchantments.VERSATILITY, 2, enchantments, 1))
                        .add(EmptyLootItem.emptyItem().setWeight(10));
                editor.addPool(pool);
            }

            // 2. END CITY TREASURE CHEST
            // (RANGE, QUIVER, MASTER_BUILDER, OVERRIDES, DOUBLE_JUMP)
            // (DIAMOND_CHISEL_ENCHANTED, DIAMOND_SPATULA_ENCHANTED, DIAMOND_BUILDING_WAND_ENCHANTED, DIAMOND_SLEDGEHAMMER_ENCHANTED, diamond_core)
            if (BuiltInLootTables.END_CITY_TREASURE.equals(key)) {

                // Pool für Scrap (selten)
                editor.addBuiltPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.ENDERITE_SCRAP))
                        .setRolls(BinomialDistributionGenerator.binomial(1, 0.15f)) // 15% Chance
                        .build());

                // Pool für Template (garantiert oder sehr häufig)
                editor.addBuiltPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.ENDERITE_UPGRADE_TEMPLATE))
                        .setRolls(BinomialDistributionGenerator.binomial(1, 0.5f)) // 50% Chance pro Kiste
                        .build());

                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 4))
                        // Books
                        .add(enchantedBook(ModEnchantments.RANGE, 3, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.OVERRIDE, 2, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.DOUBLE_JUMP, 2, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.VERSATILITY, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.VERSATILITY, 2, enchantments, 10))
                        // Items
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_BUILDING_WAND).setWeight(10).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_SLEDGEHAMMER).setWeight(20).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(LootItem.lootTableItem(ModItems.ENCHANTED_ENDERITE_APPLE).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(40));
                editor.addPool(pool);
            }

            // 4. ANCIENT CITY CHEST
            // (DEEP POCKETS, RADIUS)
            // (OCTANT_ENCHANTED, DIAMOND_SLEDGEHAMMER, QUIVER_ENCHANTED)
            if (BuiltInLootTables.ANCIENT_CITY.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 3))
                        .add(enchantedBook(ModEnchantments.DEEP_POCKETS, 2, enchantments, 5))
                        .add(enchantedBook(ModEnchantments.RADIUS, 1, enchantments, 4))
                        .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(6).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_SLEDGEHAMMER).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.QUIVER).setWeight(3).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_APPLE).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.ENCHANTED_NETHERITE_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_NUGGET).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(EmptyLootItem.emptyItem().setWeight(3));
                editor.addPool(pool);
            }

            // 5. BASTION (Treasure & Other)
            // (FUNNEL, BREAK THROUGH)
            // (GOLD_SLEDGEHAMMER, gold_core, NETHERITE_CORE)
            if (BuiltInLootTables.BASTION_TREASURE.equals(key) || BuiltInLootTables.BASTION_OTHER.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 3))
                        .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 6))
                        .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 8))
                        .add(LootItem.lootTableItem(ModItems.GOLD_SLEDGEHAMMER).setWeight(6))
                        .add(LootItem.lootTableItem(ModItems.GOLD_CORE).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_CORE).setWeight(1))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_NUGGET).setWeight(15).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_CARROT).setWeight(5))
                        .add(LootItem.lootTableItem(ModItems.ENCHANTED_NETHERITE_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(ModItems.GOLD_SLEDGEHAMMER).setWeight(8))
                        .add(EmptyLootItem.emptyItem().setWeight(20));
                editor.addPool(pool);
            }

            // 6. NETHER BRIDGE
            // (FUNNEL, BREAK THROUGH, STRIP_MINER)
            // (gold_core, OCTANT_ENCHANTED)
            if (BuiltInLootTables.NETHER_BRIDGE.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 2))
                        .add(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 6))
                        .add(enchantedBook(ModEnchantments.STRIP_MINER, 2, enchantments, 3))
                        .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 2))
                        .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 2))
                        .add(LootItem.lootTableItem(ModItems.GOLD_CORE).setWeight(1))
                        .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(3).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_NUGGET).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_CARROT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
                        .add(EmptyLootItem.emptyItem().setWeight(3));
                editor.addPool(pool);
            }

            // 7. PILLAGER OUTPOST
            // (COLOR PALETTE, SURFACE PLACE/COVER, LINE PLACE/LINEAR)
            // (OCTANT, QUIVER)
            if (BuiltInLootTables.PILLAGER_OUTPOST.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1, 3))
                        .add(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.COVER, 1, enchantments, 15))
                        .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 15))
                        .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(4))
                        .add(LootItem.lootTableItem(ModItems.QUIVER).setWeight(4))
                        .add(EmptyLootItem.emptyItem().setWeight(30));
                editor.addPool(pool);
            }

            // 8. WOODLAND MANSION
            // (COLOR PALETTE, COVER, LINEAR, VEIN_MINER IV)
            // (IRON_BUILDING_WAND, iron_core, QUIVER)
            if (BuiltInLootTables.WOODLAND_MANSION.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1, 4))
                        .add(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 3))
                        .add(enchantedBook(ModEnchantments.COVER, 1, enchantments, 7))
                        .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 7))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 5, enchantments, 8))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 2))
                        .add(LootItem.lootTableItem(ModItems.IRON_BUILDING_WAND).setWeight(15))
                        .add(LootItem.lootTableItem(ModItems.IRON_CORE).setWeight(10))
                        .add(LootItem.lootTableItem(ModItems.QUIVER).setWeight(3))
                        .add(EmptyLootItem.emptyItem().setWeight(5));
                editor.addPool(pool);
            }

            // 9. BURIED TREASURE
            // (CONSTRUCTORS TOUCH, FAST CHISEL)
            // (GOLD_CHISEL, DIAMOND_SPATULA)
            if (BuiltInLootTables.BURIED_TREASURE.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 2))
                        .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 2))
                        .add(LootItem.lootTableItem(ModItems.GOLD_CHISEL).setWeight(15))
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_CHISEL).setWeight(12))
                        .add(EmptyLootItem.emptyItem().setWeight(50));
                editor.addPool(pool);
            }

            // 10. SIMPLE DUNGEON
            // (FAST CHISEL, FUNNEL, BREAK THROUGH, VEIN_MINER II)
            // (REINFORCED_BUNDLE)
            if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 2))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 5))
                        .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 5))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 3, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 2, enchantments, 15))
                        .add(LootItem.lootTableItem(ModItems.REINFORCED_BUNDLE).setWeight(10))
                        .add(LootItem.lootTableItem(ModItems.BASIC_UPGRADE_TEMPLATE).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(30));
                editor.addPool(pool);
            }

            // 11. SHIPWRECK TREASURE
            // (FAST CHISEL)
            // (REINFORCED_BUNDLE)
            if (BuiltInLootTables.SHIPWRECK_TREASURE.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 1))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 15))
                        .add(LootItem.lootTableItem(ModItems.REINFORCED_BUNDLE).setWeight(8))
                        .add(EmptyLootItem.emptyItem().setWeight(20));
                editor.addPool(pool);
            }

            // 12. IGLOO CHEST
            // (CONSTRUCTORS TOUCH, FAST CHISEL)
            // (DIAMOND_CHISEL, IRON_SPATULA)
            if (BuiltInLootTables.IGLOO_CHEST.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 1))
                        .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 2))
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_CHISEL).setWeight(10))
                        .add(EmptyLootItem.emptyItem().setWeight(5));
                editor.addPool(pool);
            }

            // 13. ABANDONED MINESHAFT
            // (FAST CHISEL, STRIP_MINER I, VEIN_MINER III)
            // (REINFORCED_BUNDLE_ENCHANTED)
            if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 2))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 2))
                        .add(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.STRIP_MINER, 3, enchantments, 5))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 3, enchantments, 2))
                        .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 5))
                        .add(LootItem.lootTableItem(ModItems.REINFORCED_BUNDLE).setWeight(10).apply(EnchantRandomlyFunction.randomEnchantment()))
                        .add(EmptyLootItem.emptyItem().setWeight(20));
                editor.addPool(pool);
            }

            // 14. VAULT (Trial Chambers)
            // (CONSTRUCTORS TOUCH, FAST_CHISEL, DOUBLE_JUMP I)
            // (diamond_core)
            if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON.equals(key) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 1))
                        .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 2))
                        .add(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 1))
                        .add(EmptyLootItem.emptyItem().setWeight(10));
                editor.addPool(pool);
            }
            if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.equals(key) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.equals(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0, 1))
                        .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 10))
                        .add(enchantedBook(ModEnchantments.DOUBLE_JUMP, 1, enchantments, 7))
                        .add(LootItem.lootTableItem(ModItems.DIAMOND_CORE).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.NETHERITE_APPLE).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.ENCHANTED_NETHERITE_APPLE).setWeight(1))
                        .add(EmptyLootItem.emptyItem().setWeight(35));
                editor.addPool(pool);
            }
    }

    private static LootPoolSingletonContainer.Builder<?> enchantedBook(
            ResourceKey<Enchantment> enchantKey,
            int level,
            HolderLookup<Enchantment> registry,
            int weight) {

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        builder.upgrade(registry.getOrThrow(enchantKey), level);
        ItemEnchantments component = builder.toImmutable();

        return LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                .setWeight(weight)
                .apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS, component));
    }
}
