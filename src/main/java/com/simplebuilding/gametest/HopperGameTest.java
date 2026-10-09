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

    @GameTest(maxTicks = HopperTests.KEEP_ONE_MAX_TICKS)
    public void aFilteredHopperKeepsOneFilterItemInEverySlot(GameTestHelper helper) {
        HopperTests.aFilteredHopperKeepsOneFilterItemInEverySlot(helper);
    }

    @GameTest
    public void theModeDelegateReadsAndWritesTheFilterMode(GameTestHelper helper) {
        HopperTests.theModeDelegateReadsAndWritesTheFilterMode(helper);
    }

    @GameTest
    public void automationOnlyTopsUpMatchingSlots(GameTestHelper helper) {
        HopperTests.automationOnlyTopsUpMatchingSlots(helper);
    }

    @GameTest
    public void hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(GameTestHelper helper) {
        HopperTests.hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(helper);
    }

    @GameTest
    public void theUpdateTagCarriesTheFilterModeToTheClient(GameTestHelper helper) {
        HopperTests.theUpdateTagCarriesTheFilterModeToTheClient(helper);
    }

    @GameTest
    public void hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem(GameTestHelper helper) {
        HopperTests.hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem(helper);
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

    @GameTest
    public void filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra(GameTestHelper helper) {
        HopperTests.filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra(helper);
    }

    @GameTest(maxTicks = HopperTests.ITEM_AUTOMATION_MAX_TICKS)
    public void modHoppersFallBackToTheLoaderTransferApiWithoutContainer(GameTestHelper helper) {
        HopperTests.modHoppersFallBackToTheLoaderTransferApiWithoutContainer(helper);
    }
}
