package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the standing rods. No test logic here: every method delegates to {@link StandingRodTests}. Class
 * and method names are load bearing: Fabric derives the test id from them.
 */
public final class StandingRodGameTest {

    @GameTest
    public void everyRodStandsUpOnTopWhenSneaking(GameTestHelper helper) {
        StandingRodTests.everyRodStandsUpOnTopWhenSneaking(helper);
    }

    @GameTest
    public void rodsDropThemselvesAndStackIntoPosts(GameTestHelper helper) {
        StandingRodTests.rodsDropThemselvesAndStackIntoPosts(helper);
    }

    @GameTest
    public void blazeRodGlowsAndRodsHoldWater(GameTestHelper helper) {
        StandingRodTests.blazeRodGlowsAndRodsHoldWater(helper);
    }

    @GameTest
    public void pilesTakeRodsAndTheServerOptionsGateThem(GameTestHelper helper) {
        StandingRodTests.pilesTakeRodsAndTheServerOptionsGateThem(helper);
    }
}
