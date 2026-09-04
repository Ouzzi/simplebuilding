package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the sledgehammer.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link SledgehammerTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class SledgehammerGameTest {

    @GameTest
    public void sledgehammerFieldSkipsAirGapsAndUnbreakableBlocks(GameTestHelper helper) {
        SledgehammerTests.sledgehammerFieldSkipsAirGapsAndUnbreakableBlocks(helper);
    }

    @GameTest
    public void sledgehammerRefusesTheWholeFieldWhenTheOriginIsOutOfReach(GameTestHelper helper) {
        SledgehammerTests.sledgehammerRefusesTheWholeFieldWhenTheOriginIsOutOfReach(helper);
    }

    @GameTest
    public void sledgehammerBillsOneDurabilityPerBlockAndTwoForTheWrongTool(GameTestHelper helper) {
        SledgehammerTests.sledgehammerBillsOneDurabilityPerBlockAndTwoForTheWrongTool(helper);
    }

    @GameTest
    public void sledgehammerStopsTheSwingWhenTheHammerBreaks(GameTestHelper helper) {
        SledgehammerTests.sledgehammerStopsTheSwingWhenTheHammerBreaks(helper);
    }

    @GameTest
    public void sledgehammerFieldPlaneAndDepthFollowTheLookDirection(GameTestHelper helper) {
        SledgehammerTests.sledgehammerFieldPlaneAndDepthFollowTheLookDirection(helper);
    }

    @GameTest
    public void sledgehammerRightClickChargesOnlyOnBlocksItCanReshape(GameTestHelper helper) {
        SledgehammerTests.sledgehammerRightClickChargesOnlyOnBlocksItCanReshape(helper);
    }

    @GameTest
    public void sledgehammerReshapesFullBlocksStairsAndSlabs(GameTestHelper helper) {
        SledgehammerTests.sledgehammerReshapesFullBlocksStairsAndSlabs(helper);
    }

    @GameTest
    public void sledgehammerSpeedAndBlockCountScaleWithItsEnchantments(GameTestHelper helper) {
        SledgehammerTests.sledgehammerSpeedAndBlockCountScaleWithItsEnchantments(helper);
    }

    @GameTest
    public void sledgehammerChargeTimeShortensWithMaterialAndEfficiency(GameTestHelper helper) {
        SledgehammerTests.sledgehammerChargeTimeShortensWithMaterialAndEfficiency(helper);
    }

    @GameTest
    public void sledgehammerTurnsFramedTrimTemplatesGlowing(GameTestHelper helper) {
        SledgehammerTests.sledgehammerTurnsFramedTrimTemplatesGlowing(helper);
    }
}
