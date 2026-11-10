package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "suburb" (BACK22, owned by its level branch): one method per {@code suburb_*} test of {@code Band.tests}. */
public final class SuburbGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void suburbRegistered(GameTestHelper h) {
        LayersTests.ALL.get("suburb_registered").accept(h);
    }
}
