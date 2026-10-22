package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class RedstoneLinesGameTest {
    @GameTest public void linesStaySeparateSideBySide(GameTestHelper helper) { RedstoneLinesTests.linesStaySeparateSideBySide(helper); }
    @GameTest public void linesStaySeparateThroughABlock(GameTestHelper helper) { RedstoneLinesTests.linesStaySeparateThroughABlock(helper); }
    @GameTest public void vanillaSourcesFeedAstralAndNihil(GameTestHelper helper) { RedstoneLinesTests.vanillaSourcesFeedAstralAndNihil(helper); }
    @GameTest(maxTicks = 60) public void astralPowersLampAndPiston(GameTestHelper helper) { RedstoneLinesTests.astralPowersLampAndPiston(helper); }
    @GameTest(maxTicks = 60) public void bridgeLinksAllLines(GameTestHelper helper) { RedstoneLinesTests.bridgeLinksAllLines(helper); }
    @GameTest public void oldSwitchesBecomeLeversLampsReturn(GameTestHelper helper) { RedstoneLinesTests.oldSwitchesBecomeLeversLampsReturn(helper); }
    @GameTest(maxTicks = 60) public void lampsToggleOnEveryLine(GameTestHelper helper) { RedstoneLinesTests.lampsToggleOnEveryLine(helper); }
    @GameTest(maxTicks = 60) public void everyDeviceTakesRedstone(GameTestHelper helper) { RedstoneLinesTests.everyDeviceTakesRedstone(helper); }
    @GameTest(maxTicks = 60) public void everyDeviceTakesAstral(GameTestHelper helper) { RedstoneLinesTests.everyDeviceTakesAstral(helper); }
    @GameTest(maxTicks = 60) public void everyDeviceTakesNihil(GameTestHelper helper) { RedstoneLinesTests.everyDeviceTakesNihil(helper); }
    @GameTest(maxTicks = 60) public void everyDeviceTakesBridge(GameTestHelper helper) { RedstoneLinesTests.everyDeviceTakesBridge(helper); }
    @GameTest(maxTicks = 140) public void lineBlocksPowerEveryDevice(GameTestHelper helper) { RedstoneLinesTests.lineBlocksPowerEveryDevice(helper); }
    @GameTest(maxTicks = 60) public void lineBlocksFeedOnlyTheirLine(GameTestHelper helper) { RedstoneLinesTests.lineBlocksFeedOnlyTheirLine(helper); }
    @GameTest public void lineBlockRecipesRoundTrip(GameTestHelper helper) { RedstoneLinesTests.lineBlockRecipesRoundTrip(helper); }
    @GameTest public void lineBlocksHaveOwnSounds(GameTestHelper helper) { RedstoneLinesTests.lineBlocksHaveOwnSounds(helper); }
}
