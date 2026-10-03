package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the chest tiers. No test logic: every method delegates to
 * {@link TieredChestTests}; class and method names are load bearing (Fabric derives the test id).
 */
public final class TieredChestGameTest {

    @GameTest
    public void trappedCapacityComparatorAndPropertiesMatchNormalTiers(GameTestHelper helper) {
        TieredChestTests.trappedCapacityComparatorAndPropertiesMatchNormalTiers(helper);
    }

    @GameTest
    public void trappedDoubleChestsRequireTheSameKind(GameTestHelper helper) {
        TieredChestTests.trappedDoubleChestsRequireTheSameKind(helper);
    }

    @GameTest
    public void trappedRecipesUseTheMatchingChestAndHook(GameTestHelper helper) {
        TieredChestTests.trappedRecipesUseTheMatchingChestAndHook(helper);
    }

    @GameTest
    public void trappedSignalCountsViewersAndUpdatesNeighbors(GameTestHelper helper) {
        TieredChestTests.trappedSignalCountsViewersAndUpdatesNeighbors(helper);
    }

    @GameTest
    public void trappedUpgradesKeepBothHalvesAndContents(GameTestHelper helper) {
        TieredChestTests.trappedUpgradesKeepBothHalvesAndContents(helper);
    }

    @GameTest
    public void singleChestClimbsFromCopperToEnderiteKeepingItsContents(GameTestHelper helper) {
        TieredChestTests.singleChestClimbsFromCopperToEnderiteKeepingItsContents(helper);
    }

    @GameTest
    public void doubleChestUpgradesBothHalvesTogether(GameTestHelper helper) {
        TieredChestTests.doubleChestUpgradesBothHalvesTogether(helper);
    }

    @GameTest
    public void slotCountsAndStackLimitsFollowTheTier(GameTestHelper helper) {
        TieredChestTests.slotCountsAndStackLimitsFollowTheTier(helper);
    }

    @GameTest
    public void doubleChestsFormOnlyFromEqualTiers(GameTestHelper helper) {
        TieredChestTests.doubleChestsFormOnlyFromEqualTiers(helper);
    }

    @GameTest(maxTicks = TieredChestTests.HOPPER_MAX_TICKS)
    public void vanillaHoppersFillAndEmptyOversizedSlots(GameTestHelper helper) {
        TieredChestTests.vanillaHoppersFillAndEmptyOversizedSlots(helper);
    }

    @GameTest
    public void comparatorReadsOversizedSlotsAgainstTheTierLimit(GameTestHelper helper) {
        TieredChestTests.comparatorReadsOversizedSlotsAgainstTheTierLimit(helper);
    }

    @GameTest
    public void oversizedStacksSurviveSavingAndLoading(GameTestHelper helper) {
        TieredChestTests.oversizedStacksSurviveSavingAndLoading(helper);
    }

    @GameTest
    public void chestItemsFollowTheFamilyScheme(GameTestHelper helper) {
        TieredChestTests.chestItemsFollowTheFamilyScheme(helper);
    }
}
