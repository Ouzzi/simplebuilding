package com.simplebuilding.datagen;

public final class ModTradeOffers {
    private ModTradeOffers() {
    }

    public static void registerModTradeOffers() {
        // Villager-/Wandering-Trades kommen datengetrieben aus data/simplebuilding/villager_trade/
        // (geteilte Ressourcen mit dem Fabric-Modul). Die benötigte Loot-Funktion
        // simplebuilding:weighted_enchant registriert NeoForgeRegistryBootstrap.
        // Config-Gating enableVillagerTrades/enableWanderingTrades: Die JSONs tragen neben
        // fabric:load_conditions zusätzlich neoforge:conditions mit dem Typ simplebuilding:config.
        // Die zugehörige ICondition ist ConfigLoadCondition, registriert über
        // NeoForgeModRegistries.CONDITION_CODECS (neoforge:condition_codecs).
    }
}
