package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the launchpad/chunk loader tiers and the pressure plate upgrades
 * ({@link TweaksTierTests}). No logic here; class and method names are load bearing, Fabric derives
 * the test id from them. Registered via the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class TweaksTierGameTest {

    @GameTest
    public void launchpadTiersHoldFourEightAndSixteenWindCharges(GameTestHelper helper) {
        TweaksTierTests.launchpadTiersHoldFourEightAndSixteenWindCharges(helper);
    }

    @GameTest
    public void sneakingWithWindChargesLoadsTheWholeHandUpToCapacity(GameTestHelper helper) {
        TweaksTierTests.sneakingWithWindChargesLoadsTheWholeHandUpToCapacity(helper);
    }

    @GameTest
    public void launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges(GameTestHelper helper) {
        TweaksTierTests.launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges(helper);
    }

    @GameTest
    public void oldLaunchpadsDropTheChargesTheirTierNoLongerHolds(GameTestHelper helper) {
        TweaksTierTests.oldLaunchpadsDropTheChargesTheirTierNoLongerHolds(helper);
    }

    @GameTest
    public void chunkLoaderTiersForceOneFiveAndNineChunks(GameTestHelper helper) {
        TweaksTierTests.chunkLoaderTiersForceOneFiveAndNineChunks(helper);
    }

    @GameTest
    public void aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross(GameTestHelper helper) {
        TweaksTierTests.aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross(helper);
    }

    @GameTest
    public void padUpgradesPayWithThePressurePlateOfTheirTargetMaterial(GameTestHelper helper) {
        TweaksTierTests.padUpgradesPayWithThePressurePlateOfTheirTargetMaterial(helper);
    }
}
