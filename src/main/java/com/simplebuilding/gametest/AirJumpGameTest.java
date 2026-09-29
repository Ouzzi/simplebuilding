package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the air jump cooldown and its contextual bar ({@link AirJumpTests}). No test
 * logic; class and method names are load bearing (Fabric derives the test id from them), registered
 * through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 */
public final class AirJumpGameTest {

    @GameTest
    public void theCooldownIsTwentySecondsAtLevelOneAndTenAtLevelTwo(GameTestHelper helper) {
        AirJumpTests.theCooldownIsTwentySecondsAtLevelOneAndTenAtLevelTwo(helper);
    }

    @GameTest
    public void theBarFollowsVanillasExperienceOverLocatorRule(GameTestHelper helper) {
        AirJumpTests.theBarFollowsVanillasExperienceOverLocatorRule(helper);
    }

    @GameTest
    public void theBarFillsUpWhileTheAirJumpRecharges(GameTestHelper helper) {
        AirJumpTests.theBarFillsUpWhileTheAirJumpRecharges(helper);
    }
}
