package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the in-world machine upgrade with the sledgehammer.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link SledgehammerUpgradeTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class SledgehammerUpgradeGameTest {

    @GameTest
    public void everyMachineClimbsFromReinforcedToNetheriteToEnderite(GameTestHelper helper) {
        SledgehammerUpgradeTests.everyMachineClimbsFromReinforcedToNetheriteToEnderite(helper);
    }

    @GameTest(maxTicks = SledgehammerUpgradeTests.FURNACE_PACE_MAX_TICKS)
    public void upgradedFurnaceKeepsCookingTheSameItemAtTheNewPace(GameTestHelper helper) {
        SledgehammerUpgradeTests.upgradedFurnaceKeepsCookingTheSameItemAtTheNewPace(helper);
    }

    @GameTest
    public void upgradedHopperKeepsItsItemsFilterAndMode(GameTestHelper helper) {
        SledgehammerUpgradeTests.upgradedHopperKeepsItsItemsFilterAndMode(helper);
    }

    @GameTest
    public void creativeUpgradeKeepsTheNuggetAndTheHammer(GameTestHelper helper) {
        SledgehammerUpgradeTests.creativeUpgradeKeepsTheNuggetAndTheHammer(helper);
    }

    @GameTest(maxTicks = SledgehammerUpgradeTests.REFUSAL_MAX_TICKS)
    public void refusedUpgradesNeverStartTheHammer(GameTestHelper helper) {
        SledgehammerUpgradeTests.refusedUpgradesNeverStartTheHammer(helper);
    }

    @GameTest
    public void interruptedUpgradesConsumeNoNugget(GameTestHelper helper) {
        SledgehammerUpgradeTests.interruptedUpgradesConsumeNoNugget(helper);
    }

    @GameTest
    public void netheriteMachineRecipesAndTheirUnlocksAreGone(GameTestHelper helper) {
        SledgehammerUpgradeTests.netheriteMachineRecipesAndTheirUnlocksAreGone(helper);
    }
}
