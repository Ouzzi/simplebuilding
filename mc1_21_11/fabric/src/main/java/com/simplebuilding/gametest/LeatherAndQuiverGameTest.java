package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the leather sheet, the reinforced quiver and the container upgrades.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link LeatherAndQuiverTests}; the catalogue in {@link SimpleBuildingGameTests} names the same
 * tests for NeoForge.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class LeatherAndQuiverGameTest {

    @GameTest
    public void leatherSheetTakesExactlyNineLeather(GameTestHelper helper) {
        LeatherAndQuiverTests.leatherSheetTakesExactlyNineLeather(helper);
    }

    @GameTest
    public void reinforcedQuiverCraftsFromThePlainQuiverWithSheetPebbleAndNugget(GameTestHelper helper) {
        LeatherAndQuiverTests.reinforcedQuiverCraftsFromThePlainQuiverWithSheetPebbleAndNugget(helper);
    }

    @GameTest
    public void netheriteQuiverSmithsOnlyFromTheReinforcedQuiver(GameTestHelper helper) {
        LeatherAndQuiverTests.netheriteQuiverSmithsOnlyFromTheReinforcedQuiver(helper);
    }

    @GameTest
    public void upgradesKeepContentsEnchantmentsAndName(GameTestHelper helper) {
        LeatherAndQuiverTests.upgradesKeepContentsEnchantmentsAndName(helper);
    }

    @GameTest
    public void reinforcedQuiverIsAnOrdinaryTierInTheContainerTags(GameTestHelper helper) {
        LeatherAndQuiverTests.reinforcedQuiverIsAnOrdinaryTierInTheContainerTags(helper);
    }
}
