package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the Fletching Table and its arrows. No test logic here: every method delegates to
 * {@link FletchingTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class FletchingGameTest {

    @GameTest
    public void eachTipAddsItsDamageAgainstItsTargets(GameTestHelper helper) {
        FletchingTests.eachTipAddsItsDamageAgainstItsTargets(helper);
    }

    @GameTest
    public void shaftsAndFletchingsChangeTheFlight(GameTestHelper helper) {
        FletchingTests.shaftsAndFletchingsChangeTheFlight(helper);
    }

    @GameTest
    public void rodShaftsPierceHitHarderAndResistFire(GameTestHelper helper) {
        FletchingTests.rodShaftsPierceHitHarderAndResistFire(helper);
    }

    @GameTest
    public void theTableMakesFourArrowsFromThreeParts(GameTestHelper helper) {
        FletchingTests.theTableMakesFourArrowsFromThreeParts(helper);
    }

    @GameTest
    public void materialButtonsOnlyMoveItemsTheyFind(GameTestHelper helper) {
        FletchingTests.materialButtonsOnlyMoveItemsTheyFind(helper);
    }

    @GameTest
    public void rightClickingTheFletchingTableOpensTheMenu(GameTestHelper helper) {
        FletchingTests.rightClickingTheFletchingTableOpensTheMenu(helper);
    }
}
