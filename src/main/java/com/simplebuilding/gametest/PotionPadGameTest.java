package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the potion pad and the blaze head ({@link PotionPadTests}). No logic here; class
 * and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PotionPadGameTest {

    @GameTest
    public void splashPotionsLandingOnThePadAreStoredAndReplaced(GameTestHelper helper) {
        PotionPadTests.splashPotionsLandingOnThePadAreStoredAndReplaced(helper);
    }

    @GameTest
    public void standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds(GameTestHelper helper) {
        PotionPadTests.standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds(helper);
    }

    @GameTest
    public void instantEffectsApplyOnceAtTheThreeSecondMark(GameTestHelper helper) {
        PotionPadTests.instantEffectsApplyOnceAtTheThreeSecondMark(helper);
    }

    @GameTest
    public void aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration(GameTestHelper helper) {
        PotionPadTests.aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration(helper);
    }

    @GameTest
    public void aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain(GameTestHelper helper) {
        PotionPadTests.aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain(helper);
    }

    @GameTest
    public void everyPadItemStacksToOne(GameTestHelper helper) {
        PotionPadTests.everyPadItemStacksToOne(helper);
    }

    @GameTest
    public void potionPadRecipesCoverAllThreeTiers(GameTestHelper helper) {
        PotionPadTests.potionPadRecipesCoverAllThreeTiers(helper);
    }

    @GameTest
    public void chargedCreeperExplosionsDropOneBlazeHeadEach(GameTestHelper helper) {
        PotionPadTests.chargedCreeperExplosionsDropOneBlazeHeadEach(helper);
    }

    @GameTest
    public void blazesKilledOtherwiseDropNoHead(GameTestHelper helper) {
        PotionPadTests.blazesKilledOtherwiseDropNoHead(helper);
    }

    @GameTest
    public void potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToAComparator(GameTestHelper helper) {
        PotionPadTests.potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToAComparator(helper);
    }

    @GameTest(maxTicks = PotionPadTests.AUTOMATION_MAX_TICKS)
    public void aDispenserFillsThePotionPadButAHopperCannot(GameTestHelper helper) {
        PotionPadTests.aDispenserFillsThePotionPadButAHopperCannot(helper);
    }
}
