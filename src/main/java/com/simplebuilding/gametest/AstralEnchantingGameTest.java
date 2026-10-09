package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the Astral Enchanting Table (queue N27). No test logic here: every method delegates to
 * {@link AstralEnchantingTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class AstralEnchantingGameTest {

    @GameTest
    public void budgetAndCostRules(GameTestHelper helper) {
        AstralEnchantingTests.budgetAndCostRules(helper);
    }

    @GameTest
    public void shelvesAndFloorSetTheTier(GameTestHelper helper) {
        AstralEnchantingTests.shelvesAndFloorSetTheTier(helper);
    }

    @GameTest(maxTicks = AstralEnchantingTests.MAX_TICKS)
    public void hammerTurnsTheEnchantingTableAstral(GameTestHelper helper) {
        AstralEnchantingTests.hammerTurnsTheEnchantingTableAstral(helper);
    }

    @GameTest
    public void breakingDropsTableNuggetAndStock(GameTestHelper helper) {
        AstralEnchantingTests.breakingDropsTableNuggetAndStock(helper);
    }

    @GameTest
    public void stockStaysInTheTable(GameTestHelper helper) {
        AstralEnchantingTests.stockStaysInTheTable(helper);
    }

    @GameTest
    public void enchantingUsesUpWhatTheRulesSay(GameTestHelper helper) {
        AstralEnchantingTests.enchantingUsesUpWhatTheRulesSay(helper);
    }

    @GameTest
    public void recipesAreLoaded(GameTestHelper helper) {
        AstralEnchantingTests.recipesAreLoaded(helper);
    }
}
