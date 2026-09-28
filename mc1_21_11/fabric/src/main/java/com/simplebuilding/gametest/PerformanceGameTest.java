package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the performance shortcuts ({@link PerformanceTests}, docs/PERFORMANCE.md). No
 * logic here; class and method names are load bearing, Fabric derives the test id from them.
 * Registered via the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PerformanceGameTest {

    @GameTest
    public void playerScanFindsExactlyThePlayersTheSectionSearchFinds(GameTestHelper helper) {
        PerformanceTests.playerScanFindsExactlyThePlayersTheSectionSearchFinds(helper);
    }

    @GameTest
    public void anIdleSpawnTeleporterStopsTrackingOncePlayersLeave(GameTestHelper helper) {
        PerformanceTests.anIdleSpawnTeleporterStopsTrackingOncePlayersLeave(helper);
    }

    @GameTest
    public void theCachedOctantSurfaceMatchesThePerFrameScanItReplaced(GameTestHelper helper) {
        PerformanceTests.theCachedOctantSurfaceMatchesThePerFrameScanItReplaced(helper);
    }
}
