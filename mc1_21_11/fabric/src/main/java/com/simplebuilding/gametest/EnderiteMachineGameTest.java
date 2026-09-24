package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the enderite hopper, furnace, smoker and blast furnace.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link EnderiteMachineTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class EnderiteMachineGameTest {

    @GameTest(maxTicks = EnderiteMachineTests.HOPPER_MAX_TICKS)
    public void enderiteHopperMovesAnItemEveryTick(GameTestHelper helper) {
        EnderiteMachineTests.enderiteHopperMovesAnItemEveryTick(helper);
    }

    @GameTest(maxTicks = EnderiteMachineTests.FURNACE_MAX_TICKS)
    public void enderiteFurnacesCookEightTimesAsFastAsVanilla(GameTestHelper helper) {
        EnderiteMachineTests.enderiteFurnacesCookEightTimesAsFastAsVanilla(helper);
    }

    @GameTest
    public void enderiteMachineItemsAreFireResistantEpicAndVoidProtected(GameTestHelper helper) {
        EnderiteMachineTests.enderiteMachineItemsAreFireResistantEpicAndVoidProtected(helper);
    }

    @GameTest(maxTicks = EnderiteMachineTests.DROP_MAX_TICKS)
    public void enderiteHopperAndPistonDropThemselvesWhenBroken(GameTestHelper helper) {
        EnderiteMachineTests.enderiteHopperAndPistonDropThemselvesWhenBroken(helper);
    }

    @GameTest
    public void enderiteMachinesFitTheirBlockEntityTypesAndTitles(GameTestHelper helper) {
        EnderiteMachineTests.enderiteMachinesFitTheirBlockEntityTypesAndTitles(helper);
    }
}
