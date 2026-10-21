package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the in-world shell upgrade on SimpleBuilding's pile. No test logic: every method delegates to
 * {@link RareStructureFindsTests}; class and method names are load bearing (Fabric derives the test id). Since split stage 8b-3
 * the better chests, rare shulkers and shell recipes are Simple Storage's tests ({@code module_game_test_rare_finds_*}).
 */
public final class RareStructureFindsGameTest {

    @GameTest
    public void placedShellsUpgradeOneTierPerNugget(GameTestHelper helper) {
        RareStructureFindsTests.placedShellsUpgradeOneTierPerNugget(helper);
    }
}
