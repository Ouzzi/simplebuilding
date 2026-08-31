package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the trade and migration tests.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TradeAndMigrationTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TradeAndMigrationGameTest {

    @GameTest
    public void allModTradesAreLoadedIntoTheDatapackRegistry(GameTestHelper helper) {
        TradeAndMigrationTests.allModTradesAreLoadedIntoTheDatapackRegistry(helper);
    }

    @GameTest
    public void modTradesAreMergedIntoTheVanillaTradePools(GameTestHelper helper) {
        TradeAndMigrationTests.modTradesAreMergedIntoTheVanillaTradePools(helper);
    }

    @GameTest
    public void professionTradeSetsResolveTheModTrades(GameTestHelper helper) {
        TradeAndMigrationTests.professionTradeSetsResolveTheModTrades(helper);
    }

    @GameTest
    public void tradeDefinitionsProduceTheExpectedOffers(GameTestHelper helper) {
        TradeAndMigrationTests.tradeDefinitionsProduceTheExpectedOffers(helper);
    }

    @GameTest(maxTicks = TradeAndMigrationTests.MASON_VILLAGER_MAX_TICKS)
    public void masonVillagerCanRollAModTrade(GameTestHelper helper) {
        TradeAndMigrationTests.masonVillagerCanRollAModTrade(helper);
    }

    @GameTest
    public void legacySpatulasInPlayerInventoryBecomeChisels(GameTestHelper helper) {
        TradeAndMigrationTests.legacySpatulasInPlayerInventoryBecomeChisels(helper);
    }

    @GameTest(maxTicks = TradeAndMigrationTests.LEGACY_ITEM_ENTITY_MAX_TICKS)
    public void legacySpatulaItemEntityIsRewrittenInPlace(GameTestHelper helper) {
        TradeAndMigrationTests.legacySpatulaItemEntityIsRewrittenInPlace(helper);
    }
}
