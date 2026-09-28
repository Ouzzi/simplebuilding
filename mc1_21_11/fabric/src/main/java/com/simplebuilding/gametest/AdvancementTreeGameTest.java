package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the advancement tree and the mod's own trigger ({@link AdvancementTreeTests}).
 * No logic here; class and method names are load bearing, Fabric derives the test id from them.
 * Registered via the {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class AdvancementTreeGameTest {

    @GameTest
    public void theTreeLoadsCompletelyAndEveryEntryIsTranslated(GameTestHelper helper) {
        AdvancementTreeTests.theTreeLoadsCompletelyAndEveryEntryIsTranslated(helper);
    }

    @GameTest
    public void everyFeatureGrantsTheAdvancementsThatWaitForIt(GameTestHelper helper) {
        AdvancementTreeTests.everyFeatureGrantsTheAdvancementsThatWaitForIt(helper);
    }

    @GameTest
    public void itemAdvancementsFollowTheInventory(GameTestHelper helper) {
        AdvancementTreeTests.itemAdvancementsFollowTheInventory(helper);
    }

    @GameTest
    public void markingOneCornerWithTheOctantEarnsMeasureTwice(GameTestHelper helper) {
        AdvancementTreeTests.markingOneCornerWithTheOctantEarnsMeasureTwice(helper);
    }

    @GameTest
    public void everyRecipeUnlockHandsOutAnExistingRecipe(GameTestHelper helper) {
        AdvancementTreeTests.everyRecipeUnlockHandsOutAnExistingRecipe(helper);
    }
}
