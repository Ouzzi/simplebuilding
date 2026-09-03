package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the end ore features and the item frame hooks.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link OreGenAndItemFrameTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class OreGenAndItemFrameGameTest {

    @GameTest
    public void endOreFeaturesCarryTheRightOreBlockAndVeinSize(GameTestHelper helper) {
        OreGenAndItemFrameTests.endOreFeaturesCarryTheRightOreBlockAndVeinSize(helper);
    }

    @GameTest
    public void endOrePlacementDiffersBetweenAstralitAndNihilith(GameTestHelper helper) {
        OreGenAndItemFrameTests.endOrePlacementDiffersBetweenAstralitAndNihilith(helper);
    }

    @GameTest
    public void bothEndOresReachTheEndBiomesAndStayOutOfTheOverworld(GameTestHelper helper) {
        OreGenAndItemFrameTests.bothEndOresReachTheEndBiomesAndStayOutOfTheOverworld(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void glassPaneLocksTheFrameAndTheLockSurvivesTheSaveRoundTrip(GameTestHelper helper) {
        OreGenAndItemFrameTests.glassPaneLocksTheFrameAndTheLockSurvivesTheSaveRoundTrip(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void shearsHideTheFrameAndTheLockTakesPriorityOverThem(GameTestHelper helper) {
        OreGenAndItemFrameTests.shearsHideTheFrameAndTheLockTakesPriorityOverThem(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void survivalPlayersPayForTheLockAndCannotBreakTheLockedFrame(GameTestHelper helper) {
        OreGenAndItemFrameTests.survivalPlayersPayForTheLockAndCannotBreakTheLockedFrame(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void constructorsTouchMagnetTakesItsFilterFromTheFramedItem(GameTestHelper helper) {
        OreGenAndItemFrameTests.constructorsTouchMagnetTakesItsFilterFromTheFramedItem(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void brushRevealIsWiredToAnInterfaceNothingImplements(GameTestHelper helper) {
        OreGenAndItemFrameTests.brushRevealIsWiredToAnInterfaceNothingImplements(helper);
    }
}
