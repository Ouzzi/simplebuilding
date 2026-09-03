package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the armour trim benefits.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link TrimEffectTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TrimEffectGameTest {

    @GameTest
    public void trimCountsFollowThePatternAndMaterialMatching(GameTestHelper helper) {
        TrimEffectTests.trimCountsFollowThePatternAndMaterialMatching(helper);
    }

    @GameTest
    public void damageReductionFollowsThePatternAndKeepsItsFloor(GameTestHelper helper) {
        TrimEffectTests.damageReductionFollowsThePatternAndKeepsItsFloor(helper);
    }

    @GameTest
    public void utilityBonusesAreNeutralUntilTheMatchingTrimIsWorn(GameTestHelper helper) {
        TrimEffectTests.utilityBonusesAreNeutralUntilTheMatchingTrimIsWorn(helper);
    }

    @GameTest
    public void benefitGateSwitchesEveryTrimEffectOff(GameTestHelper helper) {
        TrimEffectTests.benefitGateSwitchesEveryTrimEffectOff(helper);
    }

    @GameTest
    public void astralitJumpBoostCrossesItsThresholdsOnTick(GameTestHelper helper) {
        TrimEffectTests.astralitJumpBoostCrossesItsThresholdsOnTick(helper);
    }

    @GameTest
    public void nihilithPullsDownTheSneakingAirbornePlayer(GameTestHelper helper) {
        TrimEffectTests.nihilithPullsDownTheSneakingAirbornePlayer(helper);
    }

    @GameTest
    public void trimBonusesReachThePlayerThroughTheMixins(GameTestHelper helper) {
        TrimEffectTests.trimBonusesReachThePlayerThroughTheMixins(helper);
    }
}
