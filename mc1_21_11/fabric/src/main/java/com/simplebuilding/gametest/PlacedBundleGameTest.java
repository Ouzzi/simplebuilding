package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the placed bundles ({@link PlacedBundleTests}). No logic here; class and method
 * names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PlacedBundleGameTest {

    @GameTest
    public void sneakUsePlacesBundlesOnlyOnTopFaces(GameTestHelper helper) {
        PlacedBundleTests.sneakUsePlacesBundlesOnlyOnTopFaces(helper);
    }

    @GameTest(maxTicks = PlacedBundleTests.CYCLE_MAX_TICKS)
    public void sneakingViewersCycleTheTopItemAndRightClickTakesIt(GameTestHelper helper) {
        PlacedBundleTests.sneakingViewersCycleTheTopItemAndRightClickTakesIt(helper);
    }

    @GameTest
    public void placedBundlesDropThemselvesWithTheirContents(GameTestHelper helper) {
        PlacedBundleTests.placedBundlesDropThemselvesWithTheirContents(helper);
    }
}
