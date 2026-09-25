package com.simplebuilding.loot;

import com.simplebuilding.version.LootNumbers;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;

/**
 * Die Pools, die die Mod an Vanilla-Kisten, Vaults und die Angel-Schatztabelle haengt.
 *
 * <p>Die Gewichte folgen {@code docs/LOOT-BALANCE.md}. Faustregel: in Kisten, von denen eine
 * Struktur viele hat (Mine, Mansion, Ancient City, Bastion), bringt ein Mod-Pool im Schnitt
 * hoechstens ein halbes Mod-Item; seltene Einzelkisten (Stronghold-Bibliothek, Buried Treasure,
 * Bastion-Schatzraum, Iglu) duerfen mehr geben. Kerne haben einen Netherstern im Rezept und
 * bleiben deshalb ueberall selten.
 */
public final class ModLootTableModifications {
    public interface Editor {
        void addPool(LootPool.Builder pool);
        void addBuiltPool(LootPool pool);
    }

    private ModLootTableModifications() {
    }

    // HolderGetter.Provider, not HolderLookup.Provider: NeoForge 26.3 hands its loot event a plain
    // getter provider; every HolderLookup.Provider is one as well.
    public static void apply(ResourceKey<LootTable> key, Editor editor, HolderGetter.Provider registry) {
        if (!Simplebuilding.getConfig().worldGen.enableLootTableChanges) {
            return;
        }

        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

        // 1. STRONGHOLD LIBRARY - Bau-Buecher (eine bis zwei Kisten pro Stronghold)
        if (BuiltInLootTables.STRONGHOLD_LIBRARY.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.RANGE, 2, enchantments, 4))
                    .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.VERSATILITY, 1, enchantments, 4))
                    .add(enchantedBook(ModEnchantments.VERSATILITY, 2, enchantments, 2))
                    .add(EmptyLootItem.emptyItem().setWeight(12)));
        }

        // 2. END CITY TREASURE - Enderit-Fortschritt, End-Rohstoffe, Endgame-Buecher
        if (BuiltInLootTables.END_CITY_TREASURE.equals(key)) {
            // Scrap (selten)
            editor.addBuiltPool(LootPool.lootPool()
                    .add(LootItem.lootTableItem(ModItems.ENDERITE_SCRAP))
                    .setRolls(LootNumbers.binomial(1, 0.15f)) // 15% pro Kiste
                    .build());

            // Template: 30% pro Kiste - bei vier bis acht Kisten pro Stadt meist eins bis zwei
            editor.addBuiltPool(LootPool.lootPool()
                    .add(LootItem.lootTableItem(ModItems.ENDERITE_UPGRADE_TEMPLATE))
                    .setRolls(LootNumbers.binomial(1, 0.3f))
                    .build());

            // End-Rohstoffe: genau ein Wurf, rund 60% Treffer
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.exactly(1))
                    .add(counted(ModItems.RAW_ENDERITE, 4, 1, 2))
                    .add(counted(ModItems.ENDERITE_NUGGET, 6, 2, 5))
                    .add(counted(ModItems.ASTRALIT_DUST, 6, 2, 6))
                    .add(counted(ModItems.NIHILITH_SHARD, 6, 1, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(14)));

            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 3))
                    .add(enchantedBook(ModEnchantments.RANGE, 3, enchantments, 4))
                    .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.OVERRIDE, 2, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.DOUBLE_JUMP, 2, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.VERSATILITY, 1, enchantments, 6))
                    .add(enchantedBook(ModEnchantments.VERSATILITY, 2, enchantments, 3))
                    .add(LootItem.lootTableItem(ModItems.DIAMOND_BUILDING_WAND).setWeight(6).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(LootItem.lootTableItem(ModItems.DIAMOND_SLEDGEHAMMER).setWeight(8).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(item(ModItems.ENDERITE_APPLE, 3))
                    .add(item(ModItems.ENCHANTED_ENDERITE_APPLE, 1))
                    .add(EmptyLootItem.emptyItem().setWeight(40)));
        }

        // 3. ANCIENT CITY - viele Kisten pro Stadt, daher 0-2 Wuerfe mit viel Leere
        if (BuiltInLootTables.ANCIENT_CITY.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.DEEP_POCKETS, 2, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.RADIUS, 1, enchantments, 4))
                    .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(5).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(item(ModItems.DIAMOND_SLEDGEHAMMER, 3))
                    .add(LootItem.lootTableItem(ModItems.QUIVER).setWeight(3).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(item(ModItems.NETHERITE_APPLE, 2))
                    .add(item(ModItems.ENCHANTED_NETHERITE_APPLE, 1))
                    .add(counted(ModItems.NETHERITE_NUGGET, 4, 1, 3))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 6, 2, 5))
                    .add(EmptyLootItem.emptyItem().setWeight(25)));
        }

        // 4. BASTION - ein gemeinsamer Pool fuer jede Bastion-Kiste ...
        if (BuiltInLootTables.BASTION_TREASURE.equals(key) || BuiltInLootTables.BASTION_OTHER.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 5))
                    .add(item(ModItems.GOLD_SLEDGEHAMMER, 6))
                    .add(item(ModItems.GOLD_CORE, 1))
                    .add(counted(ModItems.NETHERITE_NUGGET, 12, 1, 4))
                    .add(counted(ModItems.NETHERITE_CARROT, 6, 1, 2))
                    .add(EmptyLootItem.emptyItem().setWeight(25)));
        }
        // ... und der Schatzraum (eine Kiste pro Schatz-Bastion) zusaetzlich die grossen Sachen
        if (BuiltInLootTables.BASTION_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.exactly(1))
                    .add(item(ModItems.NETHERITE_CORE, 2))
                    .add(item(ModItems.NETHERITE_APPLE, 4))
                    .add(item(ModItems.ENCHANTED_NETHERITE_APPLE, 2))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 2, enchantments, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(7)));
        }

        // 5. NETHER BRIDGE
        if (BuiltInLootTables.NETHER_BRIDGE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 6))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 2, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 2))
                    .add(item(ModItems.GOLD_CORE, 1))
                    .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(3).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(counted(ModItems.NETHERITE_NUGGET, 6, 1, 3))
                    .add(counted(ModItems.NETHERITE_CARROT, 3, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(14)));
        }

        // 6. PILLAGER OUTPOST - eine Kiste pro Aussenposten
        if (BuiltInLootTables.PILLAGER_OUTPOST.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 6))
                    .add(enchantedBook(ModEnchantments.COVER, 1, enchantments, 8))
                    .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 8))
                    .add(item(ModItems.OCTANT, 5))
                    .add(item(ModItems.QUIVER, 5))
                    .add(item(ModItems.COPPER_CHISEL, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(20)));
        }

        // 7. WOODLAND MANSION - sehr viele Kisten, daher sparsam; Vein Miner V bleibt der Jackpot
        if (BuiltInLootTables.WOODLAND_MANSION.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.COVER, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 5, enchantments, 1))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 3))
                    .add(item(ModItems.IRON_BUILDING_WAND, 4))
                    .add(item(ModItems.IRON_CORE, 1))
                    .add(item(ModItems.QUIVER, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(30)));
        }

        // 8. BURIED TREASURE - Einzelkiste, darf grosszuegig sein
        if (BuiltInLootTables.BURIED_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 2))
                    .add(item(ModItems.GOLD_CHISEL, 10))
                    .add(item(ModItems.DIAMOND_CHISEL, 6))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 10, 2, 6))
                    .add(EmptyLootItem.emptyItem().setWeight(30)));
        }

        // 9. SIMPLE DUNGEON
        if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 8))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 8))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 3, enchantments, 8))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 2, enchantments, 12))
                    .add(item(ModItems.REINFORCED_BUNDLE, 8))
                    .add(item(ModItems.BASIC_UPGRADE_TEMPLATE, 2))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 6, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(40)));
        }

        // 10. SHIPWRECK TREASURE
        if (BuiltInLootTables.SHIPWRECK_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 1))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 10))
                    .add(item(ModItems.REINFORCED_BUNDLE, 8))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 10, 1, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(20)));
        }

        // 11. IGLOO
        if (BuiltInLootTables.IGLOO_CHEST.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 1))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 3))
                    .add(item(ModItems.DIAMOND_CHISEL, 6))
                    .add(EmptyLootItem.emptyItem().setWeight(8)));
        }

        // 12. ABANDONED MINESHAFT - sehr viele Kisten, Bergbau-Buecher als Hauptquelle
        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 2))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 8))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 3, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 3, enchantments, 4))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 3))
                    .add(LootItem.lootTableItem(ModItems.REINFORCED_BUNDLE).setWeight(6).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 8, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(30)));
        }

        // 13. VAULT (Trial Chambers)
        if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON.equals(key) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 1))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 2))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 3, 2, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(12)));
        }
        if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.equals(key) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 1))
                    .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 10))
                    .add(enchantedBook(ModEnchantments.DOUBLE_JUMP, 1, enchantments, 7))
                    .add(item(ModItems.DIAMOND_CORE, 2))
                    .add(item(ModItems.NETHERITE_APPLE, 2))
                    .add(item(ModItems.ENCHANTED_NETHERITE_APPLE, 1))
                    .add(EmptyLootItem.emptyItem().setWeight(35)));
        }

        // 14. RUINED PORTAL - kleiner Nether-Vorgeschmack an der Oberflaeche
        if (BuiltInLootTables.RUINED_PORTAL.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.between(0, 1))
                    .add(counted(ModItems.NETHERITE_NUGGET, 3, 1, 2))
                    .add(item(ModItems.GOLD_CHISEL, 3))
                    .add(item(ModItems.NETHERITE_CARROT, 2))
                    .add(EmptyLootItem.emptyItem().setWeight(12)));
        }

        // 15. ANGELN (Schatz-Kategorie) - jeder Schatzfang wuerfelt hier einmal zusaetzlich
        if (BuiltInLootTables.FISHING_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(LootNumbers.exactly(1))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.DEEP_POCKETS, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 2))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 4, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(20)));
        }
    }

    private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolEntryContainer.Builder<?> counted(ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item).setWeight(weight)
                .apply(SetItemCountFunction.setCount(LootNumbers.between(min, max)));
    }

    private static LootPoolEntryContainer.Builder<?> enchantedBook(
            ResourceKey<Enchantment> enchantKey,
            int level,
            HolderGetter<Enchantment> registry,
            int weight) {

        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        builder.upgrade(registry.getOrThrow(enchantKey), level);
        ItemEnchantments component = builder.toImmutable();

        return LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                .setWeight(weight)
                .apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS, component));
    }
}
