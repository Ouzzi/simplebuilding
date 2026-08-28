package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.condition.ConfigResourceCondition;
import com.simplebuilding.loot.ModLootFunctions;

/**
 * Villager-/Wandering-Trades sind seit Minecraft 26.1 datengetrieben:
 * - Trades: data/simplebuilding/villager_trade/(librarian|mason|toolsmith|wandering_trader)/...
 * - Pool-Anbindung: Tag-Merge in data/minecraft/tags/villager_trade/...
 * Hier wird nur noch registriert, was die Daten benötigen: die gewichtete
 * Verzauberungs-Loot-Funktion und die Config-Bedingung für enableVillagerTrades/enableWanderingTrades.
 */
public class ModTradeOffers {
    public static void registerModTradeOffers() {
        Simplebuilding.LOGGER.info("Registering Custom Trade Offers for " + Simplebuilding.MOD_ID);
        ModLootFunctions.registerLootFunctions();
        ConfigResourceCondition.register();
    }
}
