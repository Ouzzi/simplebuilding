package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "pipes" (BACK22, owned by its level branch): one method per {@code pipes_*} test of {@code Band.tests}. */
public final class PipesGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void pipesRegistered(GameTestHelper h) {
        LayersTests.ALL.get("pipes_registered").accept(h);
    }
}
