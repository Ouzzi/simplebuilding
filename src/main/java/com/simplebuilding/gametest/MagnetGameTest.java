package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the magnet.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link MagnetTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class MagnetGameTest {

    @GameTest
    public void magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking(GameTestHelper helper) {
        MagnetTests.magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking(helper);
    }

    @GameTest(maxTicks = MagnetTests.OFF_HAND_MAX_TICKS)
    public void magnetInTheOffHandDragsLooseItemsIntoTheInventory(GameTestHelper helper) {
        MagnetTests.magnetInTheOffHandDragsLooseItemsIntoTheInventory(helper);
    }

    @GameTest
    public void magnetPullFollowsTheAccelerationAndBrakingCurve(GameTestHelper helper) {
        MagnetTests.magnetPullFollowsTheAccelerationAndBrakingCurve(helper);
    }

    @GameTest
    public void magnetReachIsFourBlocksAndConstructorsTouchWidensIt(GameTestHelper helper) {
        MagnetTests.magnetReachIsFourBlocksAndConstructorsTouchWidensIt(helper);
    }

    @GameTest
    public void magnetFilterMatchesTheFullRegistryIdAndNothingElse(GameTestHelper helper) {
        MagnetTests.magnetFilterMatchesTheFullRegistryIdAndNothingElse(helper);
    }

    @GameTest
    public void sneakRightClickClearsTheFilterAndTheTooltipFollows(GameTestHelper helper) {
        MagnetTests.sneakRightClickClearsTheFilterAndTheTooltipFollows(helper);
    }

    @GameTest
    public void theMagnetRecipeStillCraftsFromItsDocumentedPattern(GameTestHelper helper) {
        MagnetTests.theMagnetRecipeStillCraftsFromItsDocumentedPattern(helper);
    }
}
