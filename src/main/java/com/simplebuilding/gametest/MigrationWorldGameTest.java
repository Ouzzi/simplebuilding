package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the migration world tests ({@link MigrationWorldTests}, split plan stage 3b). No test logic here;
 * class and method names are load bearing: Fabric derives the test id from them.
 */
public final class MigrationWorldGameTest {

    @GameTest
    public void fixturesAreBuildable(GameTestHelper helper) {
        MigrationWorldTests.fixturesAreBuildable(helper);
    }

    @GameTest
    public void worldLoadsUnderMovedIds(GameTestHelper helper) {
        MigrationWorldTests.worldLoadsUnderMovedIds(helper);
    }
}
