package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the chisel and the spatulas.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ChiselTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ChiselGameTest {

    @GameTest
    public void spatulaRunsForwardWhileSneakingAndChiselRunsBackward(GameTestHelper helper) {
        ChiselTests.spatulaRunsForwardWhileSneakingAndChiselRunsBackward(helper);
    }

    @GameTest
    public void tierTablesAreInheritedUpwardsAndSharedInPairs(GameTestHelper helper) {
        ChiselTests.tierTablesAreInheritedUpwardsAndSharedInPairs(helper);
    }

    @GameTest
    public void netheriteTierCyclesTheNetherBrickFamily(GameTestHelper helper) {
        ChiselTests.netheriteTierCyclesTheNetherBrickFamily(helper);
    }

    @GameTest
    public void cooldownTicksFollowTheTierTable(GameTestHelper helper) {
        ChiselTests.cooldownTicksFollowTheTierTable(helper);
    }

    @GameTest
    public void sharedPropertiesSurviveAndSelfMappingsReorient(GameTestHelper helper) {
        ChiselTests.sharedPropertiesSurviveAndSelfMappingsReorient(helper);
    }

    @GameTest
    public void intuitiveOrientationDerivesTheEdgeDirection(GameTestHelper helper) {
        ChiselTests.intuitiveOrientationDerivesTheEdgeDirection(helper);
    }

    @GameTest
    public void chiselledPillarsAndStairsTakeTheClickOrientation(GameTestHelper helper) {
        ChiselTests.chiselledPillarsAndStairsTakeTheClickOrientation(helper);
    }

    @GameTest
    public void chiselMinesAtHalfMaterialSpeed(GameTestHelper helper) {
        ChiselTests.chiselMinesAtHalfMaterialSpeed(helper);
    }

    @GameTest
    public void lastTargetIsStoredAndShownInTheTooltip(GameTestHelper helper) {
        ChiselTests.lastTargetIsStoredAndShownInTheTooltip(helper);
    }

    @GameTest
    public void smithingUpgradesCarryWearNameAndEnchantments(GameTestHelper helper) {
        ChiselTests.smithingUpgradesCarryWearNameAndEnchantments(helper);
    }
}
