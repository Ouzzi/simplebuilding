package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for SimpleBuilding's enchanting building blocks (the tables are Simple Enchanting's since ENCH02). No test logic
 * here: every method delegates to {@link EnchantingBlockTests}. Class and method names are load bearing: Fabric derives the test
 * id from them.
 */
public final class EnchantingBlockGameTest {

    @GameTest
    public void recipesAreLoaded(GameTestHelper helper) {
        EnchantingBlockTests.recipesAreLoaded(helper);
    }

    @GameTest
    public void blazingObsidianKeepsFireBurning(GameTestHelper helper) {
        EnchantingBlockTests.blazingObsidianKeepsFireBurning(helper);
    }

    @GameTest
    public void chiseledBookshelvesStoreBooks(GameTestHelper helper) {
        EnchantingBlockTests.chiseledBookshelvesStoreBooks(helper);
    }

    @GameTest
    public void nihilObsidianIsCraftedAndCountsAsFloor(GameTestHelper helper) {
        EnchantingBlockTests.nihilObsidianIsCraftedAndCountsAsFloor(helper);
    }
}
