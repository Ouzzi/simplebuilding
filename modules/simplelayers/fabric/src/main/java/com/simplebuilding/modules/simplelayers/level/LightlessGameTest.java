package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "lightless" (BACK22, owned by its level branch): one method per {@code lightless_*} test of {@code Band.tests}. */
public final class LightlessGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void lightlessRegistered(GameTestHelper h) {
        LayersTests.ALL.get("lightless_registered").accept(h);
    }
}
