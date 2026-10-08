package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for the crucible parts (Crucible P5). Every method delegates to {@link CrucibleTests}; names are load bearing. */
public final class CrucibleGameTest {

    @GameTest
    public void cauldronWorldCatalogMatchesTheRules(GameTestHelper helper) {
        CrucibleTests.cauldronWorldCatalogMatchesTheRules(helper);
    }

    @GameTest
    public void milkAndReinforcedCauldronsShowTheirContents(GameTestHelper helper) {
        CrucibleTests.milkAndReinforcedCauldronsShowTheirContents(helper);
    }

    @GameTest
    public void jadeReportsHeatSlotsAndShortestRemainingTime(GameTestHelper helper) {
        CrucibleTests.jadeReportsHeatSlotsAndShortestRemainingTime(helper);
    }

    @GameTest
    public void recipeHeatAndCatalogMatchCookingRules(GameTestHelper helper) {
        CrucibleTests.recipeHeatAndCatalogMatchCookingRules(helper);
    }

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
    public void soulBurnBitesTwiceAsOftenOnColdGround(GameTestHelper helper) {
        CrucibleTests.soulBurnBitesTwiceAsOftenOnColdGround(helper);
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
    public void copperBucketFullyOxidizedScoopsNothing(GameTestHelper helper) {
        CrucibleTests.copperBucketFullyOxidizedScoopsNothing(helper);
    }

    @GameTest
    public void ceramicBucketHoldsWaterAndWearsOutAfterThirtyTwoUses(GameTestHelper helper) {
        CrucibleTests.ceramicBucketHoldsWaterAndWearsOutAfterThirtyTwoUses(helper);
    }

    @GameTest
    public void ceramicLavaBucketScoopsPoursAndWears(GameTestHelper helper) {
        CrucibleTests.ceramicLavaBucketScoopsPoursAndWears(helper);
    }

    @GameTest
    public void ceramicBucketWearsThroughStagesAndKeepsItsFilling(GameTestHelper helper) {
        CrucibleTests.ceramicBucketWearsThroughStagesAndKeepsItsFilling(helper);
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

    @GameTest
    public void vanillaCauldronTakesCopperAndEnderiteBuckets(GameTestHelper helper) {
        CrucibleTests.vanillaCauldronTakesCopperAndEnderiteBuckets(helper);
    }

    @GameTest
    public void crucibleCollisionAllowsWalkingInAndKeepsTheInteriorEmpty(GameTestHelper helper) {
        CrucibleTests.crucibleCollisionAllowsWalkingInAndKeepsTheInteriorEmpty(helper);
    }

    @GameTest
    public void crucibleStepOnHurtsOnlyOnHighHeatAndNotWhenSneaking(GameTestHelper helper) {
        CrucibleTests.crucibleStepOnHurtsOnlyOnHighHeatAndNotWhenSneaking(helper);
    }

    @GameTest
    public void aBarrelAttachedToTheEnderiteCrucibleHoldsItsDoubleStacks(GameTestHelper helper) {
        CrucibleTests.aBarrelAttachedToTheEnderiteCrucibleHoldsItsDoubleStacks(helper);
    }

    @GameTest
    public void modBucketsStayInTheHandWhenPouringAndScooping(GameTestHelper helper) {
        CrucibleTests.modBucketsStayInTheHandWhenPouringAndScooping(helper);
    }
}
