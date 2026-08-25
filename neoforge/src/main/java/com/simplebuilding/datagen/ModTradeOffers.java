package com.simplebuilding.datagen;

public final class ModTradeOffers {
    private ModTradeOffers() {
    }

    public static void registerModTradeOffers() {
        // Villager-/Wandering-Trades kommen datengetrieben aus data/simplebuilding/villager_trade/
        // (geteilte Ressourcen mit dem Fabric-Modul). Die benötigte Loot-Funktion
        // simplebuilding:weighted_enchant registriert NeoForgeRegistryBootstrap.
        // Offen (Folge-Punkt): Config-Gating enableVillagerTrades/enableWanderingTrades —
        // die fabric:load_conditions in den JSONs werden auf NeoForge ignoriert.
    }
}
