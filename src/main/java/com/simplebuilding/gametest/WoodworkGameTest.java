package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the woodwork family (hollow logs, sheets, wooden cauldrons, crates, carved wood). No test logic
 * here: every method delegates to {@link WoodworkTests}. Class and method names are load bearing.
 */
public final class WoodworkGameTest {

    @GameTest
    public void hollowLogsLetSmallMobsAndCrawlersThrough(GameTestHelper helper) {
        WoodworkTests.hollowLogsLetSmallMobsAndCrawlersThrough(helper);
    }

    @GameTest
    public void sheetsConnectAndLetLightThrough(GameTestHelper helper) {
        WoodworkTests.sheetsConnectAndLetLightThrough(helper);
    }

    @GameTest(maxTicks = WoodworkTests.CAULDRON_MAX_TICKS)
    public void woodenCauldronHoldsWaterAndBurnsWithLava(GameTestHelper helper) {
        WoodworkTests.woodenCauldronHoldsWaterAndBurnsWithLava(helper);
    }

    @GameTest(maxTicks = WoodworkTests.CAULDRON_MAX_TICKS)
    public void cratesStoreFoodForHoppersAndComparators(GameTestHelper helper) {
        WoodworkTests.cratesStoreFoodForHoppersAndComparators(helper);
    }

    @GameTest
    public void chiselCarvesSherdMotifsAndHollowsLogs(GameTestHelper helper) {
        WoodworkTests.chiselCarvesSherdMotifsAndHollowsLogs(helper);
    }

    @GameTest
    public void woodworkRecipesExistForEveryWood(GameTestHelper helper) {
        WoodworkTests.woodworkRecipesExistForEveryWood(helper);
    }
}
