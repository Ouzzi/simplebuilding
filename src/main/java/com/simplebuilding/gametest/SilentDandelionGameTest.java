package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class SilentDandelionGameTest {
    @GameTest
    public void configIsBoundedAndCanDisableTheArea(GameTestHelper helper) {
        SilentDandelionTests.configIsBoundedAndCanDisableTheArea(helper);
    }
    @GameTest
    public void onlyMobsInsideTheSphereAreSilent(GameTestHelper helper) {
        SilentDandelionTests.onlyMobsInsideTheSphereAreSilent(helper);
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
    public void temporarySilenceIsNeverSavedAndExplicitSilenceSurvives(GameTestHelper helper) {
        SilentDandelionTests.temporarySilenceIsNeverSavedAndExplicitSilenceSurvives(helper);
    }
    @GameTest
    public void yarnPlacesInSmallPartsAndDropsItself(GameTestHelper helper) {
        SilentDandelionTests.yarnPlacesInSmallPartsAndDropsItself(helper);
    }
}
