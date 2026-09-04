package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the reinforced bundle item.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ReinforcedBundleTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ReinforcedBundleGameTest {

    @GameTest
    public void insertionStopsAtTheBrimAndWeighsByStackSize(GameTestHelper helper) {
        ReinforcedBundleTests.insertionStopsAtTheBrimAndWeighsByStackSize(helper);
    }

    @GameTest
    public void insertionTurnsAwayWhatCannotGoIntoContainerItems(GameTestHelper helper) {
        ReinforcedBundleTests.insertionTurnsAwayWhatCannotGoIntoContainerItems(helper);
    }

    @GameTest
    public void insertionMergesEqualStacksAndPushesThemToTheTop(GameTestHelper helper) {
        ReinforcedBundleTests.insertionMergesEqualStacksAndPushesThemToTheTop(helper);
    }

    @GameTest
    public void drawerCapsTheBundleAtFiveKinds(GameTestHelper helper) {
        ReinforcedBundleTests.drawerCapsTheBundleAtFiveKinds(helper);
    }

    @GameTest
    public void theSelectedEntryIsTheOneThatComesOut(GameTestHelper helper) {
        ReinforcedBundleTests.theSelectedEntryIsTheOneThatComesOut(helper);
    }

    @GameTest
    public void rightClickThrowsTheSelectedStackButNeverBlocks(GameTestHelper helper) {
        ReinforcedBundleTests.rightClickThrowsTheSelectedStackButNeverBlocks(helper);
    }

    @GameTest
    public void masterBuilderPlacesFromTheBundleAndColorPaletteScattersIt(GameTestHelper helper) {
        ReinforcedBundleTests.masterBuilderPlacesFromTheBundleAndColorPaletteScattersIt(helper);
    }

    @GameTest
    public void capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport(GameTestHelper helper) {
        ReinforcedBundleTests.capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport(helper);
    }

    @GameTest
    public void barAndTooltipReadTheSameCapacityTheFillingUses(GameTestHelper helper) {
        ReinforcedBundleTests.barAndTooltipReadTheSameCapacityTheFillingUses(helper);
    }
}
