package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class SilentDandelionGameTest {
    @GameTest(maxTicks = 100)
    public void creativeOffhandUseDoesNotConsume(GameTestHelper helper) {
        SilentDandelionTests.creativeOffhandUseDoesNotConsume(helper);
    }
    @GameTest
    public void playersStandsBossesAndDeadMobsAreUnchanged(GameTestHelper helper) {
        SilentDandelionTests.playersStandsBossesAndDeadMobsAreUnchanged(helper);
    }
    @GameTest
    public void pottingAndUnpottingUseVanillaInteraction(GameTestHelper helper) {
        SilentDandelionTests.pottingAndUnpottingUseVanillaInteraction(helper);
    }
    @GameTest
    public void recipesUseFourStringOrAnyWoolAndEightYarn(GameTestHelper helper) {
        SilentDandelionTests.recipesUseFourStringOrAnyWoolAndEightYarn(helper);
    }
    @GameTest
    public void toggledSilenceSurvivesVanillaSaveAndLoad(GameTestHelper helper) {
        SilentDandelionTests.toggledSilenceSurvivesVanillaSaveAndLoad(helper);
    }
    @GameTest(maxTicks = 100)
    public void useTogglesMobsWithVanillaCooldownAndConsumption(GameTestHelper helper) {
        SilentDandelionTests.useTogglesMobsWithVanillaCooldownAndConsumption(helper);
    }
    @GameTest
    public void yarnPlacesInSmallPartsAndDropsItself(GameTestHelper helper) {
        SilentDandelionTests.yarnPlacesInSmallPartsAndDropsItself(helper);
    }
}
