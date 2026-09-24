package com.simplebuilding.datagen;

public final class ModTradeOffers {
    private ModTradeOffers() {
    }

    public static void registerModTradeOffers() {
        // Villager trades come from the data-driven JSON under data/simplebuilding/villager_trade/, as on
        // NeoForge; the simplebuilding:weighted_enchant function they use is registered in
        // ForgeRegistryBootstrap. Config gating enableVillagerTrades/enableWanderingTrades: next to
        // fabric:load_conditions and neoforge:conditions the jsons carry "forge:condition" of type
        // simplebuilding:config - the ICondition is com.simplebuilding.forge.ConfigLoadCondition,
        // registered in ForgeModRegistries.CONDITION_CODECS (forge:condition_codecs).
    }
}
