package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the villager offers and the spatula migration.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TradeOfferTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TradeOfferGameTest {

    @GameTest
    public void masterBookTradeDrawsEveryEnchantmentInItsPool(GameTestHelper helper) {
        TradeOfferTests.masterBookTradeDrawsEveryEnchantmentInItsPool(helper);
    }

    @GameTest
    public void weightedEnchantHonoursItsSecondChanceSetting(GameTestHelper helper) {
        TradeOfferTests.weightedEnchantHonoursItsSecondChanceSetting(helper);
    }

    @GameTest
    public void weightedEnchantTurnsPlainBooksIntoEnchantedBooks(GameTestHelper helper) {
        TradeOfferTests.weightedEnchantTurnsPlainBooksIntoEnchantedBooks(helper);
    }

    @GameTest
    public void weightedEnchantIgnoresPoolsWithoutAnyWeight(GameTestHelper helper) {
        TradeOfferTests.weightedEnchantIgnoresPoolsWithoutAnyWeight(helper);
    }

    @GameTest
    public void spatulasInContainersSurviveTheWorldScan(GameTestHelper helper) {
        TradeOfferTests.spatulasInContainersSurviveTheWorldScan(helper);
    }

    @GameTest
    public void noRecipeReferencesTheLegacySpatulas(GameTestHelper helper) {
        TradeOfferTests.noRecipeReferencesTheLegacySpatulas(helper);
    }
}
