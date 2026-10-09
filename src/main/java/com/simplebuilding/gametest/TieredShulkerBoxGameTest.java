package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the shulker box tiers. No test logic: every method delegates to
 * {@link TieredShulkerBoxTests}; class and method names are load bearing (Fabric derives the test id).
 */
public final class TieredShulkerBoxGameTest {

    @GameTest
    public void vanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor(GameTestHelper helper) {
        TieredShulkerBoxTests.vanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor(helper);
    }

    @GameTest
    public void contentsAndOversizedStacksSurviveBreakingPlacingAndBurning(GameTestHelper helper) {
        TieredShulkerBoxTests.contentsAndOversizedStacksSurviveBreakingPlacingAndBurning(helper);
    }

    @GameTest
    public void oversizedStacksAndColorSurviveSavingAndLoading(GameTestHelper helper) {
        TieredShulkerBoxTests.oversizedStacksAndColorSurviveSavingAndLoading(helper);
    }

    @GameTest(maxTicks = TieredShulkerBoxTests.RIG_MAX_TICKS)
    public void hoppersAndComparatorsFollowTheTierLimit(GameTestHelper helper) {
        TieredShulkerBoxTests.hoppersAndComparatorsFollowTheTierLimit(helper);
    }

    @GameTest(maxTicks = TieredShulkerBoxTests.RIG_MAX_TICKS)
    public void dispensersPlaceTheBoxes(GameTestHelper helper) {
        TieredShulkerBoxTests.dispensersPlaceTheBoxes(helper);
    }

    @GameTest
    public void boxesDoNotNest(GameTestHelper helper) {
        TieredShulkerBoxTests.boxesDoNotNest(helper);
    }

    @GameTest
    public void boxesAreDyedCraftedAndWashed(GameTestHelper helper) {
        TieredShulkerBoxTests.boxesAreDyedCraftedAndWashed(helper);
    }

    @GameTest
    public void boxItemsFollowTheFamilyScheme(GameTestHelper helper) {
        TieredShulkerBoxTests.boxItemsFollowTheFamilyScheme(helper);
    }

    @GameTest
    public void shulkerShellOpensTheBoxAndRightClickClosesIt(GameTestHelper helper) {
        TieredShulkerBoxTests.shulkerShellOpensTheBoxAndRightClickClosesIt(helper);
    }

    @GameTest
    public void anOpenShulkerBoxKeepsItsStateAsAnItem(GameTestHelper helper) {
        TieredShulkerBoxTests.anOpenShulkerBoxKeepsItsStateAsAnItem(helper);
    }
}
