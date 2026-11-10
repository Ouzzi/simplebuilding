package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "electro" (BACK22, owned by its level branch): one method per {@code electro_*} test of {@code Band.tests}. */
public final class ElectroGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void electroRegistered(GameTestHelper h) {
        LayersTests.ALL.get("electro_registered").accept(h);
    }
}
