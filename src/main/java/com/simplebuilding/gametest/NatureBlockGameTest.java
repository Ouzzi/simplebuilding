package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the nature variants (N24/N25 slabs, ice, nautilus, froglights). No test logic here: every method
 * delegates to {@link NatureBlockTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class NatureBlockGameTest {

    @GameTest
    public void natureVariantsHaveTheirRecipes(GameTestHelper helper) {
        NatureBlockTests.natureVariantsHaveTheirRecipes(helper);
    }

    @GameTest
    public void grassSlabDropsDirtSlabWithoutSilkTouch(GameTestHelper helper) {
        NatureBlockTests.grassSlabDropsDirtSlabWithoutSilkTouch(helper);
    }

    @GameTest(maxTicks = NatureBlockTests.FALLING_SLAB_MAX_TICKS)
    public void sandAndGravelSlabsFallAndMerge(GameTestHelper helper) {
        NatureBlockTests.sandAndGravelSlabsFallAndMerge(helper);
    }

    @GameTest(maxTicks = NatureBlockTests.CRACKED_ICE_MAX_TICKS)
    public void crackedIceMeltsUnderAnEntityAfterSomeSeconds(GameTestHelper helper) {
        NatureBlockTests.crackedIceMeltsUnderAnEntityAfterSomeSeconds(helper);
    }
}
