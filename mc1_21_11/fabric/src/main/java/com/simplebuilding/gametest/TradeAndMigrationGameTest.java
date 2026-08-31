package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the trade and migration tests (MC 1.21.11 line). No logic here; see
 * {@link TradeAndMigrationTests}.
 */
public final class TradeAndMigrationGameTest {

    @GameTest
    public void modTradesAreMergedIntoTheVillagerTradePools(GameTestHelper helper) {
        TradeAndMigrationTests.modTradesAreMergedIntoTheVillagerTradePools(helper);
    }

    @GameTest
    public void modTradesAreMergedIntoTheWanderingTraderPools(GameTestHelper helper) {
        TradeAndMigrationTests.modTradesAreMergedIntoTheWanderingTraderPools(helper);
    }

    @GameTest
    public void tradeDefinitionsProduceTheExpectedOffers(GameTestHelper helper) {
        TradeAndMigrationTests.tradeDefinitionsProduceTheExpectedOffers(helper);
    }

    @GameTest(maxTicks = TradeAndMigrationTests.MASON_VILLAGER_MAX_TICKS)
    public void masonVillagerCanRollAModTrade(GameTestHelper helper) {
        TradeAndMigrationTests.masonVillagerCanRollAModTrade(helper);
    }

    @GameTest(maxTicks = TradeAndMigrationTests.WANDERING_TRADER_MAX_TICKS)
    public void wanderingTraderCanRollAModTrade(GameTestHelper helper) {
        TradeAndMigrationTests.wanderingTraderCanRollAModTrade(helper);
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
