package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the rare structure finds (better chests, rare shulkers, tier shells). No test logic: every method
 * delegates to {@link RareStructureFindsTests}; class and method names are load bearing (Fabric derives the test id).
 */
public final class RareStructureFindsGameTest {

    @GameTest
    public void betterChestTablesRateAndDoubleChestRule(GameTestHelper helper) {
        RareStructureFindsTests.betterChestTablesRateAndDoubleChestRule(helper);
    }

    @GameTest
    public void bastionTemplateChestBecomesNetheriteChest(GameTestHelper helper) {
        RareStructureFindsTests.bastionTemplateChestBecomesNetheriteChest(helper);
    }

    @GameTest
    public void placedEndCityChestBecomesEnderiteChestWithDoubleLoot(GameTestHelper helper) {
        RareStructureFindsTests.placedEndCityChestBecomesEnderiteChestWithDoubleLoot(helper);
    }

    @GameTest
    public void rareShulkersHaveTieredHealthAndDropTheirShells(GameTestHelper helper) {
        RareStructureFindsTests.rareShulkersHaveTieredHealthAndDropTheirShells(helper);
    }

    @GameTest
    public void placedShellsUpgradeOneTierPerNugget(GameTestHelper helper) {
        RareStructureFindsTests.placedShellsUpgradeOneTierPerNugget(helper);
    }

    @GameTest
    public void shellRecipesAndConfigCaps(GameTestHelper helper) {
        RareStructureFindsTests.shellRecipesAndConfigCaps(helper);
    }

    @GameTest
    public void rareShulkerCallsFourEndermitesOnceWhenPlayersComeNear(GameTestHelper helper) {
        RareStructureFindsTests.rareShulkerCallsFourEndermitesOnceWhenPlayersComeNear(helper);
    }

    @GameTest
    public void livingShulkersClimbTheTiersButUpgradedOnesDropNoShells(GameTestHelper helper) {
        RareStructureFindsTests.livingShulkersClimbTheTiersButUpgradedOnesDropNoShells(helper);
    }
}
