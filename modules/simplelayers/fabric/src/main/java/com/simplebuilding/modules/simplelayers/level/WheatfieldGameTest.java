package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "wheatfield" (BACK22, owned by its level branch): one method per {@code wheatfield_*} test of {@code Band.tests}. */
public final class WheatfieldGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void wheatfieldRegistered(GameTestHelper h) {
        LayersTests.ALL.get("wheatfield_registered").accept(h);
    }
}
