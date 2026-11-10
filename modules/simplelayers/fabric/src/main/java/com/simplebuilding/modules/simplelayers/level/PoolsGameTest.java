package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "pools" (BACK22, owned by its level branch): one method per {@code pools_*} test of {@code Band.tests}. */
public final class PoolsGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void poolsRegistered(GameTestHelper h) {
        LayersTests.ALL.get("pools_registered").accept(h);
    }
}
