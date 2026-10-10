package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the tiered vehicles (Queue N19/N23). No test logic here: every method delegates to
 * {@link VehicleTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class VehicleGameTest {

    @GameTest
    public void chestVehiclesHoldTheTierSlotsAndKeepOversizedStacks(GameTestHelper helper) {
        VehicleTests.chestVehiclesHoldTheTierSlotsAndKeepOversizedStacks(helper);
    }

    @GameTest
    public void furnaceCartsBurnLongerAndRunFaster(GameTestHelper helper) {
        VehicleTests.furnaceCartsBurnLongerAndRunFaster(helper);
    }

    @GameTest(maxTicks = VehicleTests.MOVE_MAX_TICKS)
    public void enderiteFurnaceCartOutrunsVanilla(GameTestHelper helper) {
        VehicleTests.enderiteFurnaceCartOutrunsVanilla(helper);
    }

    @GameTest
    public void hopperCartsTakeInAtTheHopperSpeed(GameTestHelper helper) {
        VehicleTests.hopperCartsTakeInAtTheHopperSpeed(helper);
    }

    @GameTest
    public void hopperCartFiltersLikeTheHopperBlock(GameTestHelper helper) {
        VehicleTests.hopperCartFiltersLikeTheHopperBlock(helper);
    }

    @GameTest
    public void brokenVehiclesDropTheirTierItemAndContents(GameTestHelper helper) {
        VehicleTests.brokenVehiclesDropTheirTierItemAndContents(helper);
    }

    @GameTest
    public void vehicleRecipesCraftAndUpgrade(GameTestHelper helper) {
        VehicleTests.vehicleRecipesCraftAndUpgrade(helper);
    }
}
