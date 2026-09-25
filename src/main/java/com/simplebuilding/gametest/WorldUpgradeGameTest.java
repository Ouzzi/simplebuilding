package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the 26.2 -> 26.3 world upgrade tests; the bodies live in
 * {@link WorldUpgradeTests}. Registered through the {@code fabric-gametest} entrypoint in
 * {@code fabric.mod.json}; class and method names are load bearing (test ids).
 */
public final class WorldUpgradeGameTest {

    @GameTest
    public void fixturesAreWhatTwentySixTwoWrites(GameTestHelper helper) {
        WorldUpgradeTests.fixturesAreWhatTwentySixTwoWrites(helper);
    }

    @GameTest
    public void modBlockEntitiesSurviveTheUpgrade(GameTestHelper helper) {
        WorldUpgradeTests.modBlockEntitiesSurviveTheUpgrade(helper);
    }

    @GameTest
    public void modItemsSurviveTheUpgrade(GameTestHelper helper) {
        WorldUpgradeTests.modItemsSurviveTheUpgrade(helper);
    }

    @GameTest
    public void modEntitiesSurviveTheUpgrade(GameTestHelper helper) {
        WorldUpgradeTests.modEntitiesSurviveTheUpgrade(helper);
    }

    @GameTest
    public void playerDataSurvivesTheUpgrade(GameTestHelper helper) {
        WorldUpgradeTests.playerDataSurvivesTheUpgrade(helper);
    }

    @GameTest
    public void savedDataSurvivesTheUpgrade(GameTestHelper helper) {
        WorldUpgradeTests.savedDataSurvivesTheUpgrade(helper);
    }

    @GameTest
    public void wandMidBuildFromAnOlderVersionStopsInsteadOfBuildingOnWithShiftedIds(GameTestHelper helper) {
        WorldUpgradeTests.wandMidBuildFromAnOlderVersionStopsInsteadOfBuildingOnWithShiftedIds(helper);
    }

    @GameTest
    public void backpackWithAnUnreadableEntryKeepsTheRest(GameTestHelper helper) {
        WorldUpgradeTests.backpackWithAnUnreadableEntryKeepsTheRest(helper);
    }
}
