package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the placed smithing templates ({@link PlacedTemplateTests}). No logic here; class
 * and method names are load bearing, Fabric derives the test id from them. Registered via the
 * {@code fabric-gametest} entrypoint in fabric.mod.json.
 */
public final class PlacedTemplateGameTest {

    @GameTest
    public void sneakUsePlacesTemplatesOnTheFloorAgainstTheWallAndUnderTheCeiling(GameTestHelper helper) {
        PlacedTemplateTests.sneakUsePlacesTemplatesOnTheFloorAgainstTheWallAndUnderTheCeiling(helper);
    }

    @GameTest
    public void withoutSneakingTheTemplateKeepsItsNormalBehaviour(GameTestHelper helper) {
        PlacedTemplateTests.withoutSneakingTheTemplateKeepsItsNormalBehaviour(helper);
    }

    @GameTest(maxTicks = PlacedTemplateTests.WATER_MAX_TICKS)
    public void placedTemplatesSurviveWaterAndDropThemselvesWithTheirData(GameTestHelper helper) {
        PlacedTemplateTests.placedTemplatesSurviveWaterAndDropThemselvesWithTheirData(helper);
    }

    @GameTest
    public void placedTrimTemplatesNeedThreeHammerHits(GameTestHelper helper) {
        PlacedTemplateTests.placedTrimTemplatesNeedThreeHammerHits(helper);
    }

    @GameTest(maxTicks = 200)
    public void placedTrimTemplateUpgradesOnRightClickOnly(GameTestHelper helper) {
        PlacedTemplateTests.placedTrimTemplateUpgradesOnRightClickOnly(helper);
    }

    @GameTest
    public void hintSparksOnlyShowNearPlayersHoldingGlowstoneOrGlowInk(GameTestHelper helper) {
        PlacedTemplateTests.hintSparksOnlyShowNearPlayersHoldingGlowstoneOrGlowInk(helper);
    }

    @GameTest
    public void placedTemplatesCarryTheNameOfTheirTemplate(GameTestHelper helper) {
        PlacedTemplateTests.placedTemplatesCarryTheNameOfTheirTemplate(helper);
    }

    @GameTest
    public void theHitboxCoversOnlyThePixelsOfThePlate(GameTestHelper helper) {
        PlacedTemplateTests.theHitboxCoversOnlyThePixelsOfThePlate(helper);
    }

    @GameTest
    public void blueprintsArePlacedLikeTemplatesAndDropThemselves(GameTestHelper helper) {
        PlacedTemplateTests.blueprintsArePlacedLikeTemplatesAndDropThemselves(helper);
    }

    @GameTest(maxTicks = PlacedTemplateTests.ATTRACTOR_MAX_TICKS)
    public void placedAttractorsPullLooseItemsTowardThemselves(GameTestHelper helper) {
        PlacedTemplateTests.placedAttractorsPullLooseItemsTowardThemselves(helper);
    }

    @GameTest
    public void lockedOctantsArePlacedAndRightClickTogglesTheOutlinePerPlayer(GameTestHelper helper) {
        PlacedTemplateTests.lockedOctantsArePlacedAndRightClickTogglesTheOutlinePerPlayer(helper);
    }

    @GameTest
    public void smallPartsLieDownAndTheServerOptionsGateThem(GameTestHelper helper) {
        PlacedTemplateTests.smallPartsLieDownAndTheServerOptionsGateThem(helper);
    }

    @GameTest
    public void placedEggsGoBackWithSilkTouchAndHatchLikeThrownEggs(GameTestHelper helper) {
        PlacedTemplateTests.placedEggsGoBackWithSilkTouchAndHatchLikeThrownEggs(helper);
    }

    @GameTest
    public void smallPartsStackUpToFourInAnyMixAndTheFifthIsRefused(GameTestHelper helper) {
        PlacedTemplateTests.smallPartsStackUpToFourInAnyMixAndTheFifthIsRefused(helper);
    }

    @GameTest
    public void brokenPilesHatchEachEggLikeThrownEggsAndSilkTouchReturnsThem(GameTestHelper helper) {
        PlacedTemplateTests.brokenPilesHatchEachEggLikeThrownEggsAndSilkTouchReturnsThem(helper);
    }

    @GameTest
    public void oldPlacedEggsAndLyingSmallPartsTurnIntoPilesWhenPartsAreAdded(GameTestHelper helper) {
        PlacedTemplateTests.oldPlacedEggsAndLyingSmallPartsTurnIntoPilesWhenPartsAreAdded(helper);
    }

    @GameTest
    public void theEggModelMatchesTheEggHitbox(GameTestHelper helper) {
        PlacedTemplateTests.theEggModelMatchesTheEggHitbox(helper);
    }

    @GameTest
    public void theNewSmallPartsLieDownMixAndGlowingOnesHaveParticles(GameTestHelper helper) {
        PlacedTemplateTests.theNewSmallPartsLieDownMixAndGlowingOnesHaveParticles(helper);
    }

    @GameTest
    public void candlesAndSeaPicklesMixWithSmallPartsOnlyWhenMixed(GameTestHelper helper) {
        PlacedTemplateTests.candlesAndSeaPicklesMixWithSmallPartsOnlyWhenMixed(helper);
    }

    @GameTest
    public void mixedCandlesLightAndGoOutAndPicklesGlowOnlyUnderWater(GameTestHelper helper) {
        PlacedTemplateTests.mixedCandlesLightAndGoOutAndPicklesGlowOnlyUnderWater(helper);
    }

    @GameTest
    public void fireChipsLightAndIceChipsPutOutPiledCandles(GameTestHelper helper) {
        PlacedTemplateTests.fireChipsLightAndIceChipsPutOutPiledCandles(helper);
    }

    @GameTest
    public void oldPilesLoadUnlitAndTheCandleModelsAreTheVanillaOnes(GameTestHelper helper) {
        PlacedTemplateTests.oldPilesLoadUnlitAndTheCandleModelsAreTheVanillaOnes(helper);
    }

    @GameTest
    public void severalAttractorsShareOneCentreAndFloatingItemsHover(GameTestHelper helper) {
        PlacedTemplateTests.severalAttractorsShareOneCentreAndFloatingItemsHover(helper);
    }
}
