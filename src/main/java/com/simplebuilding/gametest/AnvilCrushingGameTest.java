package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class AnvilCrushingGameTest {
    @GameTest(maxTicks = 120)
    public void anvilsConsumeOnce(GameTestHelper helper) {
        AnvilCrushingTests.anvilsConsumeOnce(helper);
    }

    @GameTest
    public void catalogAndRecipesConserveDiamonds(GameTestHelper helper) {
        AnvilCrushingTests.catalogAndRecipesConserveDiamonds(helper);
    }

    @GameTest(maxTicks = 80)
    public void invalidFallsDoNotCrush(GameTestHelper helper) {
        AnvilCrushingTests.invalidFallsDoNotCrush(helper);
    }
}
