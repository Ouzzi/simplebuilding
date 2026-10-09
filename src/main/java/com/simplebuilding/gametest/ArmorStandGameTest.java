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
    public void theSmallStandTakesEveryArmorPieceAndAnimalArmor(GameTestHelper helper) {
        ArmorStandTests.theSmallStandTakesEveryArmorPieceAndAnimalArmor(helper);
    }

    @GameTest
    public void theSmallStandHoldsExactlyOneItem(GameTestHelper helper) {
        ArmorStandTests.theSmallStandHoldsExactlyOneItem(helper);
    }

    @GameTest
    public void theSmallStandDropsItselfAndItsItem(GameTestHelper helper) {
        ArmorStandTests.theSmallStandDropsItselfAndItsItem(helper);
    }

    @GameTest
    public void aDispenserPutsArmorOnTheSmallStand(GameTestHelper helper) {
        ArmorStandTests.aDispenserPutsArmorOnTheSmallStand(helper);
    }

    @GameTest
    public void anOldMediumStandBecomesASmallStand(GameTestHelper helper) {
        ArmorStandTests.anOldMediumStandBecomesASmallStand(helper);
    }

    @GameTest
    public void theTestCentreStocksTheSmallStand(GameTestHelper helper) {
        ArmorStandTests.theTestCentreStocksTheSmallStand(helper);
    }
}
