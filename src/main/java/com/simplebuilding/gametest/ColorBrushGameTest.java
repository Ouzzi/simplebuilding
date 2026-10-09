package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric entries for {@link ColorBrushTests}. */
public final class ColorBrushGameTest {
    @GameTest
    public void dyesAreUsedInBowOrder(GameTestHelper helper) {
        ColorBrushTests.dyesAreUsedInBowOrder(helper);
    }

    @GameTest
    public void offHandComesFirst(GameTestHelper helper) {
        ColorBrushTests.offHandComesFirst(helper);
    }

    @GameTest
    public void creativeUsesNothing(GameTestHelper helper) {
        ColorBrushTests.creativeUsesNothing(helper);
    }

    @GameTest
    public void paletteIsRandom(GameTestHelper helper) {
        ColorBrushTests.paletteIsRandom(helper);
    }

    @GameTest
    public void paletteTakesOnlyDyes(GameTestHelper helper) {
        ColorBrushTests.paletteTakesOnlyDyes(helper);
    }

    @GameTest
    public void paintsConcreteAndPreservesGlassPaneState(GameTestHelper helper) {
        ColorBrushTests.paintsConcreteAndPreservesGlassPaneState(helper);
    }

    @GameTest
    public void keepsContentsAndBedHalvesButLeavesWood(GameTestHelper helper) {
        ColorBrushTests.keepsContentsAndBedHalvesButLeavesWood(helper);
    }
}
