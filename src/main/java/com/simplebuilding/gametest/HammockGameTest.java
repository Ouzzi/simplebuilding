package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the hammock. No test logic here: every method delegates to {@link HammockTests}. Class and method
 * names are load bearing: Fabric derives the test id from them.
 */
public final class HammockGameTest {

    @GameTest
    public void hangsOnlyBetweenTwoAnchorsTwoToFourApart(GameTestHelper helper) {
        HammockTests.hangsOnlyBetweenTwoAnchorsTwoToFourApart(helper);
    }

    @GameTest
    public void clothHangsInTheMiddleAtEveryGap(GameTestHelper helper) {
        HammockTests.clothHangsInTheMiddleAtEveryGap(helper);
    }

    @GameTest(maxTicks = 60)
    public void diagonalHammockNeedsTwoClicks(GameTestHelper helper) {
        HammockTests.diagonalHammockNeedsTwoClicks(helper);
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
    public void restingKeepsThePhantomTimerAndSpeedsTheClock(GameTestHelper helper) {
        HammockTests.restingKeepsThePhantomTimerAndSpeedsTheClock(helper);
    }

    @GameTest
    public void clockSpeedsUpWithEnoughRestersDayAndNight(GameTestHelper helper) {
        HammockTests.clockSpeedsUpWithEnoughRestersDayAndNight(helper);
    }

    @GameTest
    public void recipeTakesTwoStringsOneStickAndThreeWool(GameTestHelper helper) {
        HammockTests.recipeTakesTwoStringsOneStickAndThreeWool(helper);
    }

    @GameTest
    public void timeFactorIsCappedOnTheServer(GameTestHelper helper) {
        HammockTests.timeFactorIsCappedOnTheServer(helper);
    }

    @GameTest
    public void clothMiddleSitsBetweenTheAnchorsInEveryDirection(GameTestHelper helper) {
        HammockTests.clothMiddleSitsBetweenTheAnchorsInEveryDirection(helper);
    }

    @GameTest
    public void hangsBetweenStandingRodPosts(GameTestHelper helper) {
        HammockTests.hangsBetweenStandingRodPosts(helper);
    }

    @GameTest
    public void slantedHammocksHangWithTwoClicks(GameTestHelper helper) {
        HammockTests.slantedHammocksHangWithTwoClicks(helper);
    }

    @GameTest(maxTicks = 60)
    public void slantedHammockFallsOnceWhenItsAnchorOrRopeGoes(GameTestHelper helper) {
        HammockTests.slantedHammockFallsOnceWhenItsAnchorOrRopeGoes(helper);
    }
}
