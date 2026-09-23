package com.simplebuilding.datagen;

public final class ModTradeOffers {
    private ModTradeOffers() {
    }

    public static void registerModTradeOffers() {
        // Villager trades come from the data-driven JSON under data/simplebuilding/villager_trade/, as on
        // NeoForge; the simplebuilding:weighted_enchant function they use is registered in
        // ForgeRegistryBootstrap.
    }
}
