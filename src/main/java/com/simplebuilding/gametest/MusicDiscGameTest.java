package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class MusicDiscGameTest {
    @GameTest
    public void discsCarryLoadedSongsWithRegisteredSounds(GameTestHelper helper) {
        MusicDiscTests.discsCarryLoadedSongsWithRegisteredSounds(helper);
    }
    @GameTest
    public void discsLieInTheirStructureChests(GameTestHelper helper) {
        MusicDiscTests.discsLieInTheirStructureChests(helper);
    }
    @GameTest
    public void hammerFlipsPlacedDiscBackAndForth(GameTestHelper helper) {
        MusicDiscTests.hammerFlipsPlacedDiscBackAndForth(helper);
    }
    @GameTest
    public void speakersBoostOnlyTheirOwnSource(GameTestHelper helper) {
        MusicDiscTests.speakersBoostOnlyTheirOwnSource(helper);
    }
    @GameTest
    public void speakerRangeAndConfigCaps(GameTestHelper helper) {
        MusicDiscTests.speakerRangeAndConfigCaps(helper);
    }
    @GameTest
    public void speakerRecipesUseAnyPlanks(GameTestHelper helper) {
        MusicDiscTests.speakerRecipesUseAnyPlanks(helper);
    }
}
