package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter for the traps (N24: sculk jaw, sulfur cube easter egg); the logic lives in {@link TrapTests}. */
public final class TrapGameTest {

    @GameTest(maxTicks = TrapTests.JAW_MAX_TICKS)
    public void sculkJawBitesAndReopensSilently(GameTestHelper helper) {
        TrapTests.sculkJawBitesAndReopensSilently(helper);
    }

    @GameTest
    public void workstationsAreHeavySulfurCubeFood(GameTestHelper helper) {
        TrapTests.workstationsAreHeavySulfurCubeFood(helper);
    }
}
