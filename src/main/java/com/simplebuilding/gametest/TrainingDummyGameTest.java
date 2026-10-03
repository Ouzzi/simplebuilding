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
}
