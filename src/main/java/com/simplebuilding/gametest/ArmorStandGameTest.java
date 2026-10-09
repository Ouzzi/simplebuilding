package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the armor stands (armor swap, arms, medium and small stands). No test logic here: every method
 * delegates to {@link ArmorStandTests}. Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ArmorStandGameTest {

    @GameTest
    public void sneakingEmptyHandSwapsTheWholeArmour(GameTestHelper helper) {
        ArmorStandTests.sneakingEmptyHandSwapsTheWholeArmour(helper);
    }

    @GameTest
    public void plainRightClicksKeepTheVanillaSingleSlot(GameTestHelper helper) {
        ArmorStandTests.plainRightClicksKeepTheVanillaSingleSlot(helper);
    }

    @GameTest
    public void aPoweredStandSwapsOnEveryRightClick(GameTestHelper helper) {
        ArmorStandTests.aPoweredStandSwapsOnEveryRightClick(helper);
    }

    @GameTest
    public void bindingCurseAndTheSwitchStopTheSwap(GameTestHelper helper) {
        ArmorStandTests.bindingCurseAndTheSwitchStopTheSwap(helper);
    }

    @GameTest
    public void placedStandsHaveArms(GameTestHelper helper) {
        ArmorStandTests.placedStandsHaveArms(helper);
    }

    @GameTest
    public void partialStandsHoldOnlyTheirSlots(GameTestHelper helper) {
        ArmorStandTests.partialStandsHoldOnlyTheirSlots(helper);
    }

    @GameTest
    public void partialStandsDropTheirOwnItem(GameTestHelper helper) {
        ArmorStandTests.partialStandsDropTheirOwnItem(helper);
    }

    @GameTest
    public void theTestCentreStocksThePartialStands(GameTestHelper helper) {
        ArmorStandTests.theTestCentreStocksThePartialStands(helper);
    }
}
