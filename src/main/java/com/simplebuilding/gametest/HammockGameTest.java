package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the hammock. No test logic here: every method delegates to {@link HammockTests}. Class and method
 * names are load bearing: Fabric derives the test id from them.
 */
public final class HammockGameTest {

    @GameTest
    public void hangsOnlyBetweenTwoAnchorsTwoOrThreeApart(GameTestHelper helper) {
        HammockTests.hangsOnlyBetweenTwoAnchorsTwoOrThreeApart(helper);
    }

    @GameTest
    public void losingAnAnchorDropsTheHammockOnce(GameTestHelper helper) {
        HammockTests.losingAnAnchorDropsTheHammockOnce(helper);
    }

    @GameTest
    public void breakingOnePartDropsOnceExceptInCreative(GameTestHelper helper) {
        HammockTests.breakingOnePartDropsOnceExceptInCreative(helper);
    }

    @GameTest
    public void restingByDayKeepsThePhantomTimerAndSpeedsTheClock(GameTestHelper helper) {
        HammockTests.restingByDayKeepsThePhantomTimerAndSpeedsTheClock(helper);
    }

    @GameTest
    public void clockSpeedsUpOnlyByDayWithEnoughResters(GameTestHelper helper) {
        HammockTests.clockSpeedsUpOnlyByDayWithEnoughResters(helper);
    }

    @GameTest
    public void timeFactorIsCappedOnTheServer(GameTestHelper helper) {
        HammockTests.timeFactorIsCappedOnTheServer(helper);
    }
}
