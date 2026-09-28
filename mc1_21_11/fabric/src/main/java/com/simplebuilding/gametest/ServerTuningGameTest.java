package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the "Server & Modpack Tuning" tab ({@link ServerTuningTests}). No test logic;
 * class and method names are load bearing (Fabric derives the test id from them), registered through
 * the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 */
public final class ServerTuningGameTest {

    @GameTest
    public void theServerValueWinsOverTheClientFile(GameTestHelper helper) {
        ServerTuningTests.theServerValueWinsOverTheClientFile(helper);
    }

    @GameTest
    public void everySpeedAndRangeOptionIsClamped(GameTestHelper helper) {
        ServerTuningTests.everySpeedAndRangeOptionIsClamped(helper);
    }

    @GameTest
    public void theAirJumpSwitchRefusesAirJumps(GameTestHelper helper) {
        ServerTuningTests.theAirJumpSwitchRefusesAirJumps(helper);
    }

    @GameTest
    public void chunkLoadersIdleWhileTheirOwnerIsOffline(GameTestHelper helper) {
        ServerTuningTests.chunkLoadersIdleWhileTheirOwnerIsOffline(helper);
    }

    @GameTest
    public void disabledFeaturesLoseTheirRecipes(GameTestHelper helper) {
        ServerTuningTests.disabledFeaturesLoseTheirRecipes(helper);
    }

    @GameTest
    public void theLootMultiplierAndStructureSwitchesShapeTheModLoot(GameTestHelper helper) {
        ServerTuningTests.theLootMultiplierAndStructureSwitchesShapeTheModLoot(helper);
    }

    @GameTest
    public void theLaserSwitchesStopWhatTheBeamIgnites(GameTestHelper helper) {
        ServerTuningTests.theLaserSwitchesStopWhatTheBeamIgnites(helper);
    }

    @GameTest
    public void tuningValuesReachTheToolsAndMachines(GameTestHelper helper) {
        ServerTuningTests.tuningValuesReachTheToolsAndMachines(helper);
    }
}
