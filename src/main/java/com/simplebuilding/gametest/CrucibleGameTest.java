package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for the crucible parts (Crucible P5). Every method delegates to {@link CrucibleTests}; names are load bearing. */
public final class CrucibleGameTest {

    @GameTest
    public void enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(GameTestHelper helper) {
        CrucibleTests.enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(helper);
    }

    @GameTest
    public void sledgehammerBuildsTheIronCrucible(GameTestHelper helper) {
        CrucibleTests.sledgehammerBuildsTheIronCrucible(helper);
    }

    @GameTest
    public void sledgehammerUpgradesCostDouble(GameTestHelper helper) {
        CrucibleTests.sledgehammerUpgradesCostDouble(helper);
    }

    @GameTest(maxTicks = 300)
    public void soulLavaFlowsTwoBlocksInTheOverworld(GameTestHelper helper) {
        CrucibleTests.soulLavaFlowsTwoBlocksInTheOverworld(helper);
    }

    @GameTest
    public void soulLavaIsNotReplaceable(GameTestHelper helper) {
        CrucibleTests.soulLavaIsNotReplaceable(helper);
    }

    @GameTest
    public void waterTouchingSoulLavaTurnsToQuartzOrBlackstone(GameTestHelper helper) {
        CrucibleTests.waterTouchingSoulLavaTurnsToQuartzOrBlackstone(helper);
    }

    @GameTest
    public void touchingSoulLavaBurnsLongerAndGivesSoulBurn(GameTestHelper helper) {
        CrucibleTests.touchingSoulLavaBurnsLongerAndGivesSoulBurn(helper);
    }

    @GameTest
    public void fireResistanceOnlyBlocksSoulBurnDamage(GameTestHelper helper) {
        CrucibleTests.fireResistanceOnlyBlocksSoulBurnDamage(helper);
    }

    @GameTest
    public void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        CrucibleTests.soulLavaHeatsExtremeFlowingHigh(helper);
    }

    @GameTest
    public void copperBucketRules(GameTestHelper helper) {
        CrucibleTests.copperBucketRules(helper);
    }

    @GameTest
    public void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper helper) {
        CrucibleTests.ironBucketBreaksOnSoulLavaEnderiteNever(helper);
    }

    @GameTest
    public void reinforcedCauldronHoldsSoulLava(GameTestHelper helper) {
        CrucibleTests.reinforcedCauldronHoldsSoulLava(helper);
    }

    @GameTest
    public void sledgehammerCrushesQuartzBlockIntoFourQuartz(GameTestHelper helper) {
        CrucibleTests.sledgehammerCrushesQuartzBlockIntoFourQuartz(helper);
    }
}
