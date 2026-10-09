package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the Straw Armor Stand and the Training Dummy. No test logic here: every method delegates to
 * {@link TrainingDummyTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class TrainingDummyGameTest {

    @GameTest
    public void pumpkinTurnsTheStrawStandIntoTrainingDummy(GameTestHelper helper) {
        TrainingDummyTests.pumpkinTurnsTheStrawStandIntoTrainingDummy(helper);
    }

    @GameTest
    public void onlySneakingHitsPickTheDummyUp(GameTestHelper helper) {
        TrainingDummyTests.onlySneakingHitsPickTheDummyUp(helper);
    }

    @GameTest
    public void headsMakeEnchantmentsSeeTheirMob(GameTestHelper helper) {
        TrainingDummyTests.headsMakeEnchantmentsSeeTheirMob(helper);
    }

    @GameTest
    public void armourAndImmunitiesShapeTheNumber(GameTestHelper helper) {
        TrainingDummyTests.armourAndImmunitiesShapeTheNumber(helper);
    }

    @GameTest(maxTicks = 120)
    public void numbersCooldownCritsAndTheSummary(GameTestHelper helper) {
        TrainingDummyTests.numbersCooldownCritsAndTheSummary(helper);
    }

    @GameTest
    public void theArcheryStationHasEveryArrowAndItsDummies(GameTestHelper helper) {
        TrainingDummyTests.theArcheryStationHasEveryArrowAndItsDummies(helper);
    }

    @GameTest(maxTicks = 120)
    public void everyHitShowsItsNumberAfterTheCooldown(GameTestHelper helper) {
        TrainingDummyTests.everyHitShowsItsNumberAfterTheCooldown(helper);
    }

    @GameTest
    public void scarecrowKeepsMobsFromTramplingFarmland(GameTestHelper helper) {
        TrainingDummyTests.scarecrowKeepsMobsFromTramplingFarmland(helper);
    }

    @GameTest
    public void meleeHitsCountThroughTheRealPath(GameTestHelper helper) {
        TrainingDummyTests.meleeHitsCountThroughTheRealPath(helper);
    }

    @GameTest
    public void aFallingChargedMeleeHitShowsTheCrit(GameTestHelper helper) {
        TrainingDummyTests.aFallingChargedMeleeHitShowsTheCrit(helper);
    }

    @GameTest
    public void aSweptMeleeHitReachesTheNeighbourDummy(GameTestHelper helper) {
        TrainingDummyTests.aSweptMeleeHitReachesTheNeighbourDummy(helper);
    }

    @GameTest
    public void sneakingMeleeHitsPickTheDummyUpThroughTheRealPath(GameTestHelper helper) {
        TrainingDummyTests.sneakingMeleeHitsPickTheDummyUpThroughTheRealPath(helper);
    }

    @GameTest
    public void spearThrustsCountAndBreakOnlyWhileSneaking(GameTestHelper helper) {
        TrainingDummyTests.spearThrustsCountAndBreakOnlyWhileSneaking(helper);
    }

    @GameTest
    public void spearChargeWhileRunningHitsTheDummy(GameTestHelper helper) {
        TrainingDummyTests.spearChargeWhileRunningHitsTheDummy(helper);
    }

    @GameTest
    public void arrowsTickIntoTheDummyAndShowTheirNumber(GameTestHelper helper) {
        TrainingDummyTests.arrowsTickIntoTheDummyAndShowTheirNumber(helper);
    }

    @GameTest(maxTicks = 120)
    public void anExplosionBreaksTheStrawStandButOnlyNumbersTheDummy(GameTestHelper helper) {
        TrainingDummyTests.anExplosionBreaksTheStrawStandButOnlyNumbersTheDummy(helper);
    }

    @GameTest
    public void rightClicksDressAndUndressTheDummy(GameTestHelper helper) {
        TrainingDummyTests.rightClicksDressAndUndressTheDummy(helper);
    }

    @GameTest
    public void twoFastPlayerHitsBreakTheStrawStand(GameTestHelper helper) {
        TrainingDummyTests.twoFastPlayerHitsBreakTheStrawStand(helper);
    }

    @GameTest
    public void creativePlayerHitsBreakTheStandsWithoutDrops(GameTestHelper helper) {
        TrainingDummyTests.creativePlayerHitsBreakTheStandsWithoutDrops(helper);
    }

    @GameTest
    public void genericKillRemovesBothStands(GameTestHelper helper) {
        TrainingDummyTests.genericKillRemovesBothStands(helper);
    }
}
