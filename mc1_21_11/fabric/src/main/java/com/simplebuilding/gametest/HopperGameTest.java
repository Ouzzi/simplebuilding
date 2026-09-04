package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the reinforced and netherite hoppers.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link HopperTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class HopperGameTest {

    @GameTest(maxTicks = HopperTests.REDSTONE_LOCK_MAX_TICKS)
    public void redstonePowerStopsEveryHopperTransfer(GameTestHelper helper) {
        HopperTests.redstonePowerStopsEveryHopperTransfer(helper);
    }

    @GameTest
    public void filterItemsAreStoredAsSingleCountCopiesAndCanBeCleared(GameTestHelper helper) {
        HopperTests.filterItemsAreStoredAsSingleCountCopiesAndCanBeCleared(helper);
    }

    @GameTest
    public void theModeDelegateReadsAndWritesTheFilterMode(GameTestHelper helper) {
        HopperTests.theModeDelegateReadsAndWritesTheFilterMode(helper);
    }

    @GameTest
    public void theFilterLearnsItsGhostFromTheFirstItemThatIsPlaced(GameTestHelper helper) {
        HopperTests.theFilterLearnsItsGhostFromTheFirstItemThatIsPlaced(helper);
    }

    @GameTest
    public void hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(GameTestHelper helper) {
        HopperTests.hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(helper);
    }

    @GameTest
    public void theUpdateTagCarriesModeAndFilterItemsToTheClient(GameTestHelper helper) {
        HopperTests.theUpdateTagCarriesModeAndFilterItemsToTheClient(helper);
    }

    @GameTest
    public void hopperMenuOpensOnUseAndFilterClicksNeverStoreTheItem(GameTestHelper helper) {
        HopperTests.hopperMenuOpensOnUseAndFilterClicksNeverStoreTheItem(helper);
    }

    @GameTest
    public void hopperBlocksCarryTheirRegisteredStrengthSoundAndTags(GameTestHelper helper) {
        HopperTests.hopperBlocksCarryTheirRegisteredStrengthSoundAndTags(helper);
    }

    @GameTest
    public void hopperRecipesCraftFromTheirDocumentedPatterns(GameTestHelper helper) {
        HopperTests.hopperRecipesCraftFromTheirDocumentedPatterns(helper);
    }

    @GameTest(maxTicks = HopperTests.HOPPER_DROP_MAX_TICKS)
    public void bothHoppersDropThemselvesWhenBroken(GameTestHelper helper) {
        HopperTests.bothHoppersDropThemselvesWhenBroken(helper);
    }
}
