package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric entries for {@link ColorBrushTests}. */
public final class ColorBrushGameTest {
    @GameTest
    public void loadsAndConsumesOneDye(GameTestHelper helper) {
        ColorBrushTests.loadsAndConsumesOneDye(helper);
    }

    @GameTest
    public void paintsConcreteAndPreservesGlassPaneState(GameTestHelper helper) {
        ColorBrushTests.paintsConcreteAndPreservesGlassPaneState(helper);
    }

    @GameTest
    public void pipetteAndWash(GameTestHelper helper) {
        ColorBrushTests.pipetteAndWash(helper);
    }

    @GameTest
    public void keepsContentsAndBedHalvesButLeavesWood(GameTestHelper helper) {
        ColorBrushTests.keepsContentsAndBedHalvesButLeavesWood(helper);
    }
}
