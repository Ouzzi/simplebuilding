package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the smithing table recipe book and the Auto Smither. No test logic here: every method delegates
 * to {@link WorkstationTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class WorkstationGameTest {

    @GameTest
    public void autoSmitherOutputRejectsInsertion(GameTestHelper helper) {
        WorkstationTests.autoSmitherOutputRejectsInsertion(helper);
    }

    @GameTest
    public void autoSmitherOutputCapacityAndRecipeError(GameTestHelper helper) {
        WorkstationTests.autoSmitherOutputCapacityAndRecipeError(helper);
    }

    @GameTest
    public void smithingRecipeBookHidesDummyDisplays(GameTestHelper helper) {
        WorkstationTests.smithingRecipeBookHidesDummyDisplays(helper);
    }

    @GameTest
    public void smithingRecipeBookPlacesDamagedGear(GameTestHelper helper) {
        WorkstationTests.smithingRecipeBookPlacesDamagedGear(helper);
    }

    @GameTest(maxTicks = 200)
    public void autoSmitherSmithsOncePerPulse(GameTestHelper helper) {
        WorkstationTests.autoSmitherSmithsOncePerPulse(helper);
    }

    @GameTest
    public void autoSmitherSortsHopperInput(GameTestHelper helper) {
        WorkstationTests.autoSmitherSortsHopperInput(helper);
    }

    @GameTest(maxTicks = WorkstationTests.CRAFTER_MAX_TICKS)
    public void autonomousCrafterCraftsOnlyAboveHoppers(GameTestHelper helper) {
        WorkstationTests.autonomousCrafterCraftsOnlyAboveHoppers(helper);
    }

    @GameTest(maxTicks = WorkstationTests.CRAFTER_MAX_TICKS)
    public void autonomousCrafterStopsOnRedstone(GameTestHelper helper) {
        WorkstationTests.autonomousCrafterStopsOnRedstone(helper);
    }

    @GameTest(maxTicks = WorkstationTests.CRAFTER_MAX_TICKS)
    public void autonomousCrafterFilterKeepsTheRecipeItems(GameTestHelper helper) {
        WorkstationTests.autonomousCrafterFilterKeepsTheRecipeItems(helper);
    }
}
