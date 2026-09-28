package com.simplebuilding.loot;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Die Pools, die die Mod an Vanilla-Kisten, Vaults und die Angel-Schatztabelle haengt.
 *
 * <p>Die Gewichte folgen {@code docs/LOOT-BALANCE.md}. Faustregel: in Kisten, von denen eine
 * Struktur viele hat (Mine, Mansion, Ancient City, Bastion), bringt ein Mod-Pool im Schnitt
 * hoechstens ein halbes Mod-Item; seltene Einzelkisten (Stronghold-Bibliothek, Buried Treasure,
 * Bastion-Schatzraum, Iglu) duerfen mehr geben.
 *
 * <p>Baukerne (Besitzer 2026-09-27: "sehr selten", Enderit-Kern besonders): jeder Kern haengt in
 * einem eigenen Pool mit genau einer Chance pro Kiste ({@link #rareCore}), statt als Gewicht in
 * einem Pool mit vielen Wuerfen - so steht die Wahrscheinlichkeit pro Kiste direkt im Code. Der
 * Kupferkern liegt in keiner Kiste (billig, beim Steinmetz-Dorfbewohner zu kaufen), der
 * Enderit-Kern nur in der End City.
 */
public final class ModLootTableModifications {
    public interface Editor {
        void addPool(LootPool.Builder pool);
        void addBuiltPool(LootPool pool);
    }

    private ModLootTableModifications() {
    }

    /** Pool fuer charged_creeper/root: ein Lohenkopf, nur wenn das Opfer ({@code this}) eine Lohe ist. */
    public static LootPool.Builder blazeHeadPool(HolderLookup.Provider registry) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(TweaksItems.BLAZE_HEAD))
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(
                                EntityTypePredicate.of(registry.lookupOrThrow(Registries.ENTITY_TYPE), EntityType.BLAZE))));
    }
    /** Pool fuer charged_creeper/root: ein Endermankopf, nur wenn das Opfer ein Enderman ist (Spawn-Teleporter I). */
    public static LootPool.Builder endermanHeadPool(HolderLookup.Provider registry) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(TweaksItems.ENDERMAN_HEAD))
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                        EntityPredicate.Builder.entity().entityType(
                                EntityTypePredicate.of(registry.lookupOrThrow(Registries.ENTITY_TYPE), EntityType.ENDERMAN))));
    }
    /** Eisenkern pro Waldanwesen-Kiste. */
    public static final float IRON_CORE_CHANCE = 0.008f;
    /** Goldkern pro Bastion-Kiste (alle Bastion-Kisten, auch der Schatzraum). */
    public static final float GOLD_CORE_BASTION_CHANCE = 0.006f;
    /** Goldkern pro Netherfestungs-Kiste. */
    public static final float GOLD_CORE_FORTRESS_CHANCE = 0.008f;
    /** Diamantkern pro unheilvollem (und seltenem) Vault. */
    public static final float DIAMOND_CORE_CHANCE = 0.008f;
    /** Netheritkern pro Bastion-Schatzraum (eine Kiste pro Schatz-Bastion). */
    public static final float NETHERITE_CORE_CHANCE = 0.04f;
    /** Enderit-Kern pro End-City-Kiste - der seltenste Kern. */
    public static final float ENDERITE_CORE_CHANCE = 0.0025f;

    public static void apply(ResourceKey<LootTable> key, Editor editor, HolderLookup.Provider registry) {
        // 0. Lohenkopf: wie Vanillas Mob-Koepfe aus charged_creeper/root - toetet eine geladene
        // Creeper-Explosion eine Lohe, faellt ihr Kopf (Creeper#killedEntity rollt die Tabelle nur
        // einmal je Explosion). Eigener Pool, der nur fuer Lohen greift. Unabhaengig vom Schalter
        // enableLootTableChanges: der Kopf ist die einzige Quelle fuer das Trank-Pad.
        if (BuiltInLootTables.CHARGED_CREEPER.equals(key)) {
            editor.addPool(blazeHeadPool(registry));
            // Endermankopf genauso (2026-09-28): einzige Quelle fuer den Spawn-Teleporter I.
            editor.addPool(endermanHeadPool(registry));
        }

        if (!Simplebuilding.getConfig().worldGen.enableLootTableChanges) {
            return;
        }

        var enchantments = registry.lookupOrThrow(Registries.ENCHANTMENT);

        // 1. STRONGHOLD LIBRARY - Bau-Buecher (eine bis zwei Kisten pro Stronghold)
        if (BuiltInLootTables.STRONGHOLD_LIBRARY.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(BinomialDistributionGenerator.binomial(1, 0.15f)) // 15% pro Kiste
                    .build());

            // Enderit-Kern: extrem selten (0,25 % pro Kiste, etwa 1-2 % pro Stadt)
            rareCore(editor, ModItems.ENDERITE_CORE, ENDERITE_CORE_CHANCE);

            // Template: 30% pro Kiste - bei vier bis acht Kisten pro Stadt meist eins bis zwei
            editor.addBuiltPool(LootPool.lootPool()
                    .add(LootItem.lootTableItem(ModItems.ENDERITE_UPGRADE_TEMPLATE))
                    .setRolls(BinomialDistributionGenerator.binomial(1, 0.3f))
                    .build());

            // End-Rohstoffe: genau ein Wurf, rund 60% Treffer
            editor.addPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(counted(ModItems.RAW_ENDERITE, 4, 1, 2))
                    .add(counted(ModItems.ENDERITE_NUGGET, 6, 2, 5))
                    .add(counted(ModItems.ASTRALIT_DUST, 6, 2, 6))
                    .add(counted(ModItems.NIHILITH_SHARD, 6, 1, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(14)));

            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 3))
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
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(UniformGenerator.between(0, 2))
                    .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 5))
                    .add(item(ModItems.GOLD_SLEDGEHAMMER, 6))
                    .add(counted(ModItems.NETHERITE_NUGGET, 12, 1, 4))
                    .add(counted(ModItems.NETHERITE_CARROT, 6, 1, 2))
                    .add(EmptyLootItem.emptyItem().setWeight(25)));
            rareCore(editor, ModItems.GOLD_CORE, GOLD_CORE_BASTION_CHANCE);
        }
        // ... und der Schatzraum (eine Kiste pro Schatz-Bastion) zusaetzlich die grossen Sachen
        if (BuiltInLootTables.BASTION_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(item(ModItems.NETHERITE_APPLE, 4))
                    .add(item(ModItems.ENCHANTED_NETHERITE_APPLE, 2))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 2, enchantments, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(7)));
            rareCore(editor, ModItems.NETHERITE_CORE, NETHERITE_CORE_CHANCE);
        }

        // 5. NETHER BRIDGE
        if (BuiltInLootTables.NETHER_BRIDGE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 2))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 1, enchantments, 6))
                    .add(enchantedBook(ModEnchantments.STRIP_MINER, 2, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FUNNEL, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.BREAK_THROUGH, 1, enchantments, 2))
                    .add(LootItem.lootTableItem(ModItems.OCTANT).setWeight(3).apply(EnchantRandomlyFunction.randomEnchantment()))
                    .add(counted(ModItems.NETHERITE_NUGGET, 6, 1, 3))
                    .add(counted(ModItems.NETHERITE_CARROT, 3, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(14)));
            rareCore(editor, ModItems.GOLD_CORE, GOLD_CORE_FORTRESS_CHANCE);
        }

        // 6. PILLAGER OUTPOST - eine Kiste pro Aussenposten
        if (BuiltInLootTables.PILLAGER_OUTPOST.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(UniformGenerator.between(0, 2))
                    .add(enchantedBook(ModEnchantments.COLOR_PALETTE, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.COVER, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 5))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 5, enchantments, 1))
                    .add(enchantedBook(ModEnchantments.VEIN_MINER, 4, enchantments, 3))
                    .add(item(ModItems.IRON_BUILDING_WAND, 4))
                    .add(item(ModItems.QUIVER, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(30)));
            rareCore(editor, ModItems.IRON_CORE, IRON_CORE_CHANCE);
        }

        // 8. BURIED TREASURE - Einzelkiste, darf grosszuegig sein
        if (BuiltInLootTables.BURIED_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(UniformGenerator.between(0, 1))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 10))
                    .add(item(ModItems.REINFORCED_BUNDLE, 8))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 10, 1, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(20)));
        }

        // 11. IGLOO
        if (BuiltInLootTables.IGLOO_CHEST.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 1))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 3))
                    .add(item(ModItems.DIAMOND_CHISEL, 6))
                    .add(EmptyLootItem.emptyItem().setWeight(8)));
        }

        // 12. ABANDONED MINESHAFT - sehr viele Kisten, Bergbau-Buecher als Hauptquelle
        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 2))
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
                    .setRolls(UniformGenerator.between(0, 1))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 2, enchantments, 2))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 3, 2, 4))
                    .add(EmptyLootItem.emptyItem().setWeight(12)));
        }
        if (BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.equals(key) || BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 1))
                    .add(enchantedBook(ModEnchantments.MASTER_BUILDER, 1, enchantments, 10))
                    .add(enchantedBook(ModEnchantments.DOUBLE_JUMP, 1, enchantments, 7))
                    .add(item(ModItems.NETHERITE_APPLE, 2))
                    .add(item(ModItems.ENCHANTED_NETHERITE_APPLE, 1))
                    .add(EmptyLootItem.emptyItem().setWeight(35)));
            rareCore(editor, ModItems.DIAMOND_CORE, DIAMOND_CORE_CHANCE);
        }

        // 14. RUINED PORTAL - kleiner Nether-Vorgeschmack an der Oberflaeche
        if (BuiltInLootTables.RUINED_PORTAL.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(UniformGenerator.between(0, 1))
                    .add(counted(ModItems.NETHERITE_NUGGET, 3, 1, 2))
                    .add(item(ModItems.GOLD_CHISEL, 3))
                    .add(item(ModItems.NETHERITE_CARROT, 2))
                    .add(EmptyLootItem.emptyItem().setWeight(12)));
        }

        // 15. ANGELN (Schatz-Kategorie) - jeder Schatzfang wuerfelt hier einmal zusaetzlich
        if (BuiltInLootTables.FISHING_TREASURE.equals(key)) {
            editor.addPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(enchantedBook(ModEnchantments.FAST_CHISELING, 1, enchantments, 3))
                    .add(enchantedBook(ModEnchantments.CONSTRUCTORS_TOUCH, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.DEEP_POCKETS, 1, enchantments, 2))
                    .add(enchantedBook(ModEnchantments.LINEAR, 1, enchantments, 2))
                    .add(counted(ModItems.DIAMOND_PEBBLE, 4, 1, 3))
                    .add(EmptyLootItem.emptyItem().setWeight(20)));
        }
    }

    /**
     * Ein Kern in einem eigenen Pool: genau ein Wurf mit Wahrscheinlichkeit {@code chance}, also
     * {@code chance} Kerne pro Kiste im Mittel und nie mehr als einer.
     */
    private static void rareCore(Editor editor, ItemLike core, float chance) {
        float scaled = coreChance(chance);
        if (scaled <= 0.0f) {
            return;
        }
        editor.addPool(LootPool.lootPool()
                .setRolls(BinomialDistributionGenerator.binomial(1, scaled))
                .add(item(core, 1)));
    }

    /**
     * Kern-Chance mit dem Faktor {@code worldGen.buildingCoreLootChanceMultiplier} (Standard 1),
     * hoechstens 1 je Kiste; 0 = kein Kern-Pool. Gelesen beim Laden der Datenpakete.
     */
    public static float coreChance(float base) {
        com.simplebuilding.config.SimplebuildingConfig config = Simplebuilding.getConfig();
        double factor = config == null ? 1.0
                : com.simplebuilding.config.SimplebuildingConfig.nonNegative(config.worldGen.buildingCoreLootChanceMultiplier, 1.0);
        return (float) Math.min(1.0, base * factor);
    }

    private static LootPoolSingletonContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> counted(ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item).setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
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
