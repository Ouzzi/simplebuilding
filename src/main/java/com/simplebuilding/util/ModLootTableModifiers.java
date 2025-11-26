package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.loot.provider.number.LootNumberProvider; // Importieren wir für sauberen Code

/**
 * Utility-Klasse zur Modifikation von Vanilla Loot Tables, um die Custom Currency (Money Bills)
 * in verschiedenen Truhen als gelegentliche Beute zu platzieren.
 */
public class ModLootTableModifiers {

    /**
     * Registriert einen Listener auf das LootTableEvents.MODIFY-Event, um benutzerdefinierte
     * Loot-Pools für die Money Bills zu allen relevanten Truhen hinzuzufügen.
     */
    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) -> {

            // === DEFINITION DER ZU MODIFIZIERENDEN LOOT TABLES ===

            // 1. Igloo Chest (Niedriger/Mittelwert Truhe)
            generateMoneyLootPool(key, LootTables.IGLOO_CHEST_CHEST, tableBuilder, 0.30f,1, 1);

            // 2. Simple Dungeon Chest (Mittelwert Truhe, breite Spanne)
            generateMoneyLootPool(key, LootTables.SIMPLE_DUNGEON_CHEST, tableBuilder, 0.20f, 2, 4);

            // 3. End City Treasure (Hoher Wert, aber niedrige Chance pro Kiste)
            generateMoneyLootPool(key, LootTables.END_CITY_TREASURE_CHEST, tableBuilder, 0.20f,1, 6);

            // 4. Abandoned Mineshaft Chest (Mittelwert, Standard-Truhe)
            generateMoneyLootPool(key, LootTables.ABANDONED_MINESHAFT_CHEST, tableBuilder, 0.35f, 1, 2);

            // 5. Shipwreck Treasure Chest (Häufigere Truhe, geringer Wert)
            generateMoneyLootPool(key, LootTables.SHIPWRECK_TREASURE_CHEST, tableBuilder, 0.30f,1, 2);

            // 6. Stronghold Library Chest (Sehr selten, aber sehr hoher Wert)
            generateMoneyLootPool(key, LootTables.STRONGHOLD_LIBRARY_CHEST, tableBuilder, 0.25f,8, 16  );

            // 7. Buried Treasure Chest (Garantiert hochwertiges Loot)
            generateMoneyLootPool(key, LootTables.BURIED_TREASURE_CHEST, tableBuilder, 0.3f,1, 4);

        });
    }

    /**
     * Hilfsmethode, die einen Loot Pool für die Custom Currency (ModItems.MONEY_BILL) erstellt
     * und ihn dem gegebenen LootTable.Builder hinzufügt, wenn die Schlüssel übereinstimmen.
     *
     * @param currentKey  Die RegistryKey<LootTable> der aktuell vom Event geladenen Loot Table.
     * @param targetKey   Die RegistryKey<LootTable> des Ziel-Loot-Tisches (z.B. Mineshaft, Dungeon), den wir modifizieren möchten.
     * @param tableBuilder Der LootTable.Builder, zu dem der neue Loot Pool hinzugefügt wird.
     * @param dropChance  Die Wahrscheinlichkeit (0.0f bis 1.0f), mit der dieser spezifische Loot Pool aktiviert wird.
     * @param minAmount   Die minimale Anzahl an Money Bills, die im Pool gefunden werden kann.
     * @param maxAmount   Die maximale Anzahl an Money Bills, die im Pool gefunden werden kann.
     */
    public static void generateMoneyLootPool(
            RegistryKey<LootTable> currentKey,
            RegistryKey<LootTable> targetKey,
            LootTable.Builder tableBuilder,
            float dropChance,
            int minAmount,
            int maxAmount)
    {
        // Überprüft, ob der Schlüssel der geladenen Loot Table mit dem Zielschlüssel übereinstimmt.
        if (targetKey.equals(currentKey)) {

            // Definiert den Provider für die Item-Anzahl: konstant, wenn min=max, sonst uniform.
            LootNumberProvider countProvider = (minAmount == maxAmount)
                    ? ConstantLootNumberProvider.create(minAmount)
                    : UniformLootNumberProvider.create(minAmount, maxAmount);

            // Erstellt den Loot Pool Builder.
            LootPool.Builder poolBuilder = LootPool.builder()
                    // Stellt sicher, dass der Pool nur einmal versucht, Items zu generieren.
                    .rolls(ConstantLootNumberProvider.create(1))

                    // Fügt die Wahrscheinlichkeitsbedingung hinzu (ob der Pool droppt).
                    .conditionally(RandomChanceLootCondition.builder(dropChance))

                    // Fügt den Mod-Geldschein als potenziellen Loot-Eintrag hinzu.
                    .with(ItemEntry.builder(ModItems.GOLD_CHISEL))

                    // Wendet die Funktion an, um die definierte Item-Anzahl festzulegen.
                    .apply(SetCountLootFunction.builder(countProvider));

            // Fügt den konfigurierten Pool zum Loot Table Builder hinzu (Injection).
            tableBuilder.pool(poolBuilder.build());
        }
    }
}