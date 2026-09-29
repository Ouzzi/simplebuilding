package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the beginner's guide and the topic books ({@link GuideBookTests}). No logic here;
 * class and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class GuideBookGameTest {

    @GameTest
    public void theFirstJoinGivesTheGuideOnceAndHonoursTheConfig(GameTestHelper helper) {
        GuideBookTests.theFirstJoinGivesTheGuideOnceAndHonoursTheConfig(helper);
    }

    @GameTest
    public void everyTopicBookRecipeTakesBookOrGuideAndTheGuideStays(GameTestHelper helper) {
        GuideBookTests.everyTopicBookRecipeTakesBookOrGuideAndTheGuideStays(helper);
    }

    @GameTest
    public void everyGuidePageUsesTranslationKeysThatExistInEnglishAndGerman(GameTestHelper helper) {
        GuideBookTests.everyGuidePageUsesTranslationKeysThatExistInEnglishAndGerman(helper);
    }

    @GameTest
    public void guideBooksReadLikeWrittenBooks(GameTestHelper helper) {
        GuideBookTests.guideBooksReadLikeWrittenBooks(helper);
    }

    @GameTest
    public void everyGuideChapterIconAndRecipeResolves(GameTestHelper helper) {
        GuideBookTests.everyGuideChapterIconAndRecipeResolves(helper);
    }

    @GameTest
    public void theGuidesExplainEveryEnchantmentAndTheWaveItems(GameTestHelper helper) {
        GuideBookTests.theGuidesExplainEveryEnchantmentAndTheWaveItems(helper);
    }

    @GameTest
    public void theAdminGuideNamesOnlyCommandsAndOptionsThatExist(GameTestHelper helper) {
        GuideBookTests.theAdminGuideNamesOnlyCommandsAndOptionsThatExist(helper);
    }

    @GameTest
    public void everyGuidePageFitsTheBookInEnglishAndGerman(GameTestHelper helper) {
        GuideBookTests.everyGuidePageFitsTheBookInEnglishAndGerman(helper);
    }

    @GameTest
    public void readingTheGuideDoesNotPauseTheGame(GameTestHelper helper) {
        GuideBookTests.readingTheGuideDoesNotPauseTheGame(helper);
    }

    @GameTest
    public void onlyOperatorsCraftTheAdminGuide(GameTestHelper helper) {
        GuideBookTests.onlyOperatorsCraftTheAdminGuide(helper);
    }

    @GameTest
    public void theEnchantmentsGuideCoversEveryModEnchantment(GameTestHelper helper) {
        GuideBookTests.theEnchantmentsGuideCoversEveryModEnchantment(helper);
    }
}
