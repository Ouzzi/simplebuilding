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
    public void enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(GameTestHelper helper) {
        CrucibleTests.enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(helper);
    }

    @GameTest
    public void sledgehammerBuildsTheIronCrucible(GameTestHelper helper) {
        CrucibleTests.sledgehammerBuildsTheIronCrucible(helper);
    }

    @GameTest
    public void hammerUpgradeGoesStrikeByStrike(GameTestHelper helper) {
        CrucibleTests.hammerUpgradeGoesStrikeByStrike(helper);
    }

    @GameTest
    public void brokenMidUpgradeReturnsTheUnits(GameTestHelper helper) {
        CrucibleTests.brokenMidUpgradeReturnsTheUnits(helper);
    }

    @GameTest
    public void sledgehammerUpgradesCostDouble(GameTestHelper helper) {
        CrucibleTests.sledgehammerUpgradesCostDouble(helper);
    }

    @GameTest
    public void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        CrucibleTests.soulLavaHeatsExtremeFlowingHigh(helper);
    }

    @GameTest
    public void sledgehammerCrushesQuartzBlockIntoFourQuartz(GameTestHelper helper) {
        CrucibleTests.sledgehammerCrushesQuartzBlockIntoFourQuartz(helper);
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
    public void aLooseEnderiteBarrelKeepsDoubleStacksInMenuAndHopper(GameTestHelper helper) {
        CrucibleTests.aLooseEnderiteBarrelKeepsDoubleStacksInMenuAndHopper(helper);
    }

    @GameTest
    public void attachedBarrelHitboxFollowsItsModel(GameTestHelper helper) {
        CrucibleTests.attachedBarrelHitboxFollowsItsModel(helper);
    }

    @GameTest
    public void attachedBarrelDoesNotOccludeItsNeighbours(GameTestHelper helper) {
        CrucibleTests.attachedBarrelDoesNotOccludeItsNeighbours(helper);
    }

    @GameTest
    public void blazingObsidianHeatsTheCrucibleHigh(GameTestHelper helper) {
        CrucibleTests.blazingObsidianHeatsTheCrucibleHigh(helper);
    }
}
