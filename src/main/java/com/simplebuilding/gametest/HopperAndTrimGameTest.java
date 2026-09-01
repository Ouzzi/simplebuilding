package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the hopper filter and armour trim multiplier tests. No logic here; see
 * {@link HopperAndTrimTests}.
 */
public final class HopperAndTrimGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void hopperFilterModesGateWhatMayEnter(GameTestHelper helper) {
        HopperAndTrimTests.hopperFilterModesGateWhatMayEnter(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void hopperPayloadsOnlyActOnAnOpenHopperMenu(GameTestHelper helper) {
        HopperAndTrimTests.hopperPayloadsOnlyActOnAnOpenHopperMenu(helper);
    }

    @GameTest
    public void trimMultiplierFollowsTheExperienceCurveAndTheConfiguredBase(GameTestHelper helper) {
        HopperAndTrimTests.trimMultiplierFollowsTheExperienceCurveAndTheConfiguredBase(helper);
    }
}
