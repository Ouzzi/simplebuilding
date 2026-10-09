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
    public void paintBoxIsRandom(GameTestHelper helper) {
        ColorBrushTests.paintBoxIsRandom(helper);
    }

    @GameTest
    public void paintBoxHoldsOneStackPerColourPerTier(GameTestHelper helper) {
        ColorBrushTests.paintBoxHoldsOneStackPerColourPerTier(helper);
    }

    @GameTest
    public void paintBoxScrollSelection(GameTestHelper helper) {
        ColorBrushTests.paintBoxScrollSelection(helper);
    }

    @GameTest
    public void recipesMakeTheBrushAndUpgradeTheBox(GameTestHelper helper) {
        ColorBrushTests.recipesMakeTheBrushAndUpgradeTheBox(helper);
    }

    @GameTest(maxTicks = ColorBrushTests.BRUSHING_MAX_TICKS)
    public void brushesSuspiciousSandLikeTheVanillaBrush(GameTestHelper helper) {
        ColorBrushTests.brushesSuspiciousSandLikeTheVanillaBrush(helper);
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
