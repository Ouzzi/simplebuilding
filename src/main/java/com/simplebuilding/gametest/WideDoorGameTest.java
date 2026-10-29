package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class WideDoorGameTest {
    @GameTest public void registeredAndCraftable(GameTestHelper helper) { WideDoorTests.registeredAndCraftable(helper); }
    @GameTest public void placesEveryCell(GameTestHelper helper) { WideDoorTests.placesEveryCell(helper); }
    @GameTest(maxTicks = 100) public void openAndCloseByHand(GameTestHelper helper) { WideDoorTests.openAndCloseByHand(helper); }
    @GameTest(maxTicks = 100) public void blockedSwingAndOpening(GameTestHelper helper) { WideDoorTests.blockedSwingAndOpening(helper); }
    @GameTest public void halfDoorsSwingInsideTheirBlock(GameTestHelper helper) { WideDoorTests.halfDoorsSwingInsideTheirBlock(helper); }
    @GameTest public void openLeafShape(GameTestHelper helper) { WideDoorTests.openLeafShape(helper); }
    @GameTest(maxTicks = 100) public void followRedstone(GameTestHelper helper) { WideDoorTests.followRedstone(helper); }
    @GameTest(maxTicks = 100) public void redstoneAtTheOpeningStillWorks(GameTestHelper helper) { WideDoorTests.redstoneAtTheOpeningStillWorks(helper); }
    @GameTest(maxTicks = 100) public void breakAsAUnit(GameTestHelper helper) { WideDoorTests.breakAsAUnit(helper); }
    @GameTest(maxTicks = 60) public void pistonsLeaveDoorsAlone(GameTestHelper helper) { WideDoorTests.pistonsLeaveDoorsAlone(helper); }
    @GameTest public void switchOffKeepsDoorsShut(GameTestHelper helper) { WideDoorTests.switchOffKeepsDoorsShut(helper); }
}
