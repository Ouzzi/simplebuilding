package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the ore detector.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link OreDetectorTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class OreDetectorGameTest {

    @GameTest
    public void detectorReportsTheNearestTargetInsideItsBudget(GameTestHelper helper) {
        OreDetectorTests.detectorReportsTheNearestTargetInsideItsBudget(helper);
    }

    @GameTest
    public void detectorModesMatchTheirOreTags(GameTestHelper helper) {
        OreDetectorTests.detectorModesMatchTheirOreTags(helper);
    }

    @GameTest
    public void denseBlocksShortenTheBeamMoreThanSoftOnes(GameTestHelper helper) {
        OreDetectorTests.denseBlocksShortenTheBeamMoreThanSoftOnes(helper);
    }

    @GameTest
    public void constructorsTouchDoublesTheReachThroughSolidRock(GameTestHelper helper) {
        OreDetectorTests.constructorsTouchDoublesTheReachThroughSolidRock(helper);
    }

    @GameTest
    public void sneakClickingCalibratesTheDetectorAndPlainClicksDoNot(GameTestHelper helper) {
        OreDetectorTests.sneakClickingCalibratesTheDetectorAndPlainClicksDoNot(helper);
    }

    @GameTest
    public void modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable(GameTestHelper helper) {
        OreDetectorTests.modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable(helper);
    }

    @GameTest
    public void tooltipNamesEveryModeWithItsPowerAndTarget(GameTestHelper helper) {
        OreDetectorTests.tooltipNamesEveryModeWithItsPowerAndTarget(helper);
    }
}
