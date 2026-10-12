package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class RedstoneLinesGameTest {
    @GameTest public void linesStaySeparateSideBySide(GameTestHelper helper) { RedstoneLinesTests.linesStaySeparateSideBySide(helper); }
    @GameTest public void linesStaySeparateThroughABlock(GameTestHelper helper) { RedstoneLinesTests.linesStaySeparateThroughABlock(helper); }
    @GameTest public void vanillaSourcesFeedAstralAndNihil(GameTestHelper helper) { RedstoneLinesTests.vanillaSourcesFeedAstralAndNihil(helper); }
    @GameTest(maxTicks = 60) public void astralPowersLampAndPiston(GameTestHelper helper) { RedstoneLinesTests.astralPowersLampAndPiston(helper); }
    @GameTest(maxTicks = 60) public void bridgeLinksAllLines(GameTestHelper helper) { RedstoneLinesTests.bridgeLinksAllLines(helper); }
    @GameTest public void removedSwitchesAndLampsBecomeVanilla(GameTestHelper helper) { RedstoneLinesTests.removedSwitchesAndLampsBecomeVanilla(helper); }
}
