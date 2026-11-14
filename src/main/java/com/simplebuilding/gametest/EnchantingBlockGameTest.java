package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for SimpleBuilding's side of the enchanting building blocks (moved to Simple Enchanting / SimpleLib in N48). No test logic
 * here: every method delegates to {@link EnchantingBlockTests}. Class and method names are load bearing: Fabric derives the test
 * id from them.
 */
public final class EnchantingBlockGameTest {

    @GameTest
    public void recipesAreLoaded(GameTestHelper helper) {
        EnchantingBlockTests.recipesAreLoaded(helper);
    }
}
