package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the brewing wave 2026-10-09 (new potions and effects, warden tendril, Storage Crafting Table).
 * No test logic here: every method delegates to {@link BrewingTests}. Class and method names are load bearing.
 */
public final class BrewingGameTest {

    @GameTest
    public void newPotionsBrewFromTheirIngredients(GameTestHelper helper) {
        BrewingTests.newPotionsBrewFromTheirIngredients(helper);
    }

    @GameTest
    public void drinkingTheNewPotionsAppliesTheirEffects(GameTestHelper helper) {
        BrewingTests.drinkingTheNewPotionsAppliesTheirEffects(helper);
    }

    @GameTest
    public void mirageKeepsTheSizeClassAndIsStable(GameTestHelper helper) {
        BrewingTests.mirageKeepsTheSizeClassAndIsStable(helper);
    }

    @GameTest
    public void wardenDropsTendrils(GameTestHelper helper) {
        BrewingTests.wardenDropsTendrils(helper);
    }

    @GameTest
    public void storageCraftingTableKeepsItsGrid(GameTestHelper helper) {
        BrewingTests.storageCraftingTableKeepsItsGrid(helper);
    }

    @GameTest
    public void storageCraftingTableSharesItsGrid(GameTestHelper helper) {
        BrewingTests.storageCraftingTableSharesItsGrid(helper);
    }

    @GameTest
    public void storageCraftingTableDropsItsGridAndIgnoresHoppers(GameTestHelper helper) {
        BrewingTests.storageCraftingTableDropsItsGridAndIgnoresHoppers(helper);
    }
}
