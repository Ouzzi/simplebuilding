package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the test centre ({@code /sbtestcentre}).
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TestCentreTests}. Registered through the {@code fabric-gametest} entrypoint in
 * {@code fabric.mod.json}. Class and method names are load bearing: Fabric derives the test id
 * from them.
 */
public final class TestCentreGameTest {

    @GameTest
    public void everyModItemAndBlockHasItsPlaceInTheTestCentre(GameTestHelper helper) {
        TestCentreTests.everyModItemAndBlockHasItsPlaceInTheTestCentre(helper);
    }

    @GameTest
    public void theWholeCentreBuildsAndMatchesItsPlan(GameTestHelper helper) {
        TestCentreTests.theWholeCentreBuildsAndMatchesItsPlan(helper);
    }
}
