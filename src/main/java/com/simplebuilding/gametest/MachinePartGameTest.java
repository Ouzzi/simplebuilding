package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter for {@link MachinePartTests} (Double Piston, Signal Dampener; IG31-12/-25/-26). Names are load bearing. */
public final class MachinePartGameTest {
    @GameTest public void registered(GameTestHelper helper) { MachinePartTests.registered(helper); }
    @GameTest public void oldIdsMove(GameTestHelper helper) { MachinePartTests.oldIdsMove(helper); }
    @GameTest public void recipesCraft(GameTestHelper helper) { MachinePartTests.recipesCraft(helper); }
    @GameTest(maxTicks = 100) public void dampenerSubtractsTheStage(GameTestHelper helper) { MachinePartTests.dampenerSubtractsTheStage(helper); }
    @GameTest(maxTicks = 60) public void dampenerWaitsOneRedstoneTick(GameTestHelper helper) { MachinePartTests.dampenerWaitsOneRedstoneTick(helper); }
    @GameTest public void dampenerStageCyclesOnRightClick(GameTestHelper helper) { MachinePartTests.dampenerStageCyclesOnRightClick(helper); }
    @GameTest(maxTicks = 40) public void dampenerIgnoresSideInputs(GameTestHelper helper) { MachinePartTests.dampenerIgnoresSideInputs(helper); }
    @GameTest public void dampenerSwitchOffOutputsNothing(GameTestHelper helper) { MachinePartTests.dampenerSwitchOffOutputsNothing(helper); }
    @GameTest public void doublePistonPlacesOneCell(GameTestHelper helper) { MachinePartTests.doublePistonPlacesOneCell(helper); }
    @GameTest(maxTicks = 100) public void doublePistonStageOneForWeakSignal(GameTestHelper helper) { MachinePartTests.doublePistonStageOneForWeakSignal(helper); }
    @GameTest(maxTicks = 100) public void doublePistonStageTwoForStrongSignal(GameTestHelper helper) { MachinePartTests.doublePistonStageTwoForStrongSignal(helper); }
    @GameTest(maxTicks = 140) public void doublePistonPushesAndRetracts(GameTestHelper helper) { MachinePartTests.doublePistonPushesAndRetracts(helper); }
    @GameTest(maxTicks = 160) public void doublePistonStepsBackOnWeakerSignal(GameTestHelper helper) { MachinePartTests.doublePistonStepsBackOnWeakerSignal(helper); }
    @GameTest(maxTicks = 120) public void doublePistonPushLimit(GameTestHelper helper) { MachinePartTests.doublePistonPushLimit(helper); }
    @GameTest(maxTicks = 60) public void doublePistonStoppedByObsidian(GameTestHelper helper) { MachinePartTests.doublePistonStoppedByObsidian(helper); }
    @GameTest(maxTicks = 40) public void doublePistonCannotBePushed(GameTestHelper helper) { MachinePartTests.doublePistonCannotBePushed(helper); }
    @GameTest(maxTicks = 120) public void doublePistonFallsAsUnit(GameTestHelper helper) { MachinePartTests.doublePistonFallsAsUnit(helper); }
    @GameTest public void doublePistonSwitchOffKeepsItStill(GameTestHelper helper) { MachinePartTests.doublePistonSwitchOffKeepsItStill(helper); }
}
