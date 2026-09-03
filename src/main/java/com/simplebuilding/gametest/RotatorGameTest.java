package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the rotator.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link RotatorTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class RotatorGameTest {

    @GameTest
    public void logAxisCyclesThroughAllThreeAxesAndIgnoresSneaking(GameTestHelper helper) {
        RotatorTests.logAxisCyclesThroughAllThreeAxesAndIgnoresSneaking(helper);
    }

    @GameTest
    public void rimIsTheOuterEighthOfEveryFaceAndNowhereInside(GameTestHelper helper) {
        RotatorTests.rimIsTheOuterEighthOfEveryFaceAndNowhereInside(helper);
    }

    @GameTest
    public void facingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart(GameTestHelper helper) {
        RotatorTests.facingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart(helper);
    }

    @GameTest
    public void rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue(GameTestHelper helper) {
        RotatorTests.rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue(helper);
    }

    @GameTest
    public void sixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim(GameTestHelper helper) {
        RotatorTests.sixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim(helper);
    }

    @GameTest
    public void wearsOutAtItsRatedDurabilityAndTakesDurabilityEnchantments(GameTestHelper helper) {
        RotatorTests.wearsOutAtItsRatedDurabilityAndTakesDurabilityEnchantments(helper);
    }

    @GameTest
    public void craftingTakesFiveIronAndOneEnderPearlInThatShape(GameTestHelper helper) {
        RotatorTests.craftingTakesFiveIronAndOneEnderPearlInThatShape(helper);
    }
}
