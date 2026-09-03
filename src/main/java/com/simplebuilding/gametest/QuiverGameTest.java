package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the quivers.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link QuiverTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class QuiverGameTest {

    @GameTest
    public void rightClicksDoNothingEvenWithMasterBuilder(GameTestHelper helper) {
        QuiverTests.rightClicksDoNothingEvenWithMasterBuilder(helper);
    }

    @GameTest
    public void arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt(GameTestHelper helper) {
        QuiverTests.arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt(helper);
    }

    @GameTest
    public void capacityDropsTheBundleBonusAndFollowsTierAndEnchantments(GameTestHelper helper) {
        QuiverTests.capacityDropsTheBundleBonusAndFollowsTierAndEnchantments(helper);
    }

    @GameTest
    public void barWidthFollowsTheSameCapacityTheFillingUses(GameTestHelper helper) {
        QuiverTests.barWidthFollowsTheSameCapacityTheFillingUses(helper);
    }

    @GameTest
    public void bowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack(GameTestHelper helper) {
        QuiverTests.bowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack(helper);
    }

    @GameTest
    public void bowConsumesOneArrowFromTheQuiverThatSuppliedIt(GameTestHelper helper) {
        QuiverTests.bowConsumesOneArrowFromTheQuiverThatSuppliedIt(helper);
    }

    @GameTest
    public void bowShootsFromTheQuiverAndBillsItOutsideCreativeOnly(GameTestHelper helper) {
        QuiverTests.bowShootsFromTheQuiverAndBillsItOutsideCreativeOnly(helper);
    }

    @GameTest
    public void netheriteQuiverBurnsInAnExplosionWhileTheNetheriteBundleSurvives(GameTestHelper helper) {
        QuiverTests.netheriteQuiverBurnsInAnExplosionWhileTheNetheriteBundleSurvives(helper);
    }
}
