package com.simplebuilding.modules.simplebuildingtools;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter of Simple Building Tools (b-btools07): the module's own checks ({@code BtTests}) and the tool tests that moved
 * with the code from SimpleBuilding (same names and runner parameters as SimpleBuilding's adapters). Names are load bearing.
 */
public final class ModuleGameTest {
    @GameTest
    public void registered(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("registered").accept(helper);
    }

    @GameTest
    public void oldToolIdsMove(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("old_tool_ids_move").accept(helper);
    }

    @GameTest
    public void configTakeover(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("config_takeover").accept(helper);
    }

    @GameTest
    public void subModSwitch(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("sub_mod_switch").accept(helper);
    }

    @GameTest
    public void toolRecipesAreThere(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("tool_recipes_are_there").accept(helper);
    }

    @GameTest
    public void enderiteWandNeedsSimpleOres(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("enderite_wand_needs_simple_ores").accept(helper);
    }

    @GameTest
    public void guideBook(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("guide_book").accept(helper);
    }

    @GameTest
    public void discoverRecipeUnlocks(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("discover_recipe_unlocks").accept(helper);
    }

    @GameTest
    public void discoverBehaviourHints(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("discover_behaviour_hints").accept(helper);
    }

    @GameTest
    public void discoverFirstPickupTips(GameTestHelper helper) {
        com.simplebuildingtools.test.BtTests.ALL.get("discover_first_pickup_tips").accept(helper);
    }

    @GameTest(skyAccess = true)
    public void creativeWandReachesAFarBlockWithoutCost(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.creativeWandReachesAFarBlockWithoutCost(helper);
    }

    @GameTest(skyAccess = true)
    public void creativeBlueprintBuildsFarAndStaysLockedOnceSigned(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.creativeBlueprintBuildsFarAndStaysLockedOnceSigned(helper);
    }

    @GameTest
    public void codeRoundTripsAndParsesTheSpecExamples(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.codeRoundTripsAndParsesTheSpecExamples(helper);
    }

    @GameTest
    public void sizeLimitsCapTheCodeAndMapWandTiers(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.sizeLimitsCapTheCodeAndMapWandTiers(helper);
    }

    @GameTest
    public void codeExtensionsKeepExistingCodesIdentical(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.codeExtensionsKeepExistingCodesIdentical(helper);
    }

    @GameTest
    public void codeShapesFillTheOctantFigures(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.codeShapesFillTheOctantFigures(helper);
    }

    @GameTest
    public void codeHollowShapesKeepOnlyTheShell(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.codeHollowShapesKeepOnlyTheShell(helper);
    }

    @GameTest
    public void codeVariablesComputeCoordinatesSizesAndCounts(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.codeVariablesComputeCoordinatesSizesAndCounts(helper);
    }

    @GameTest
    public void materialListCountsItemsSortedByAmount(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.materialListCountsItemsSortedByAmount(helper);
    }

    @GameTest
    public void cartographyTableScansTheOctantSelection(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.cartographyTableScansTheOctantSelection(helper);
    }

    @GameTest
    public void buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes(helper);
    }

    @GameTest
    public void rotationTurnsTheBuildAndTheScrollPacketStepsIt(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.rotationTurnsTheBuildAndTheScrollPacketStepsIt(helper);
    }

    @GameTest
    public void wandTierRefusesBlueprintsLargerThanItsCube(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.wandTierRefusesBlueprintsLargerThanItsCube(helper);
    }

    @GameTest
    public void editPacketSavesSignsAndLocksTheBlueprint(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.editPacketSavesSignsAndLocksTheBlueprint(helper);
    }

    @GameTest
    public void pictureCodeHoldsTheExpectedBlocks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.pictureCodeHoldsTheExpectedBlocks(helper);
    }

    @GameTest
    public void pictureBlueprintBuildsItsBlocks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.pictureBlueprintBuildsItsBlocks(helper);
    }

    @GameTest
    public void scanFollowsTheOctantShape(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.scanFollowsTheOctantShape(helper);
    }

    @GameTest
    public void largeScanRunsOverSeveralTicks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.largeScanRunsOverSeveralTicks(helper);
    }

    @GameTest
    public void largeBuildRunsOverSeveralTicks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.largeBuildRunsOverSeveralTicks(helper);
    }

    @GameTest
    public void examplesParseFitAndListTheirMaterials(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.examplesParseFitAndListTheirMaterials(helper);
    }

    @GameTest
    public void blockSearchFindsByNameAndId(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.blockSearchFindsByNameAndId(helper);
    }

    @GameTest
    public void cartographyTableCopiesSignedBlueprints(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.cartographyTableCopiesSignedBlueprints(helper);
    }

    @GameTest
    public void buildNeedsTheSignedBlueprint(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildNeedsTheSignedBlueprint(helper);
    }

    @GameTest
    public void editPacketsSaveImmediatelyAndSurviveTheDisconnect(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.editPacketsSaveImmediatelyAndSurviveTheDisconnect(helper);
    }

    @GameTest
    public void buildGrowsLayerByLayerFromTheCentre(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildGrowsLayerByLayerFromTheCentre(helper);
    }

    @GameTest
    public void buildStopsWhenTheWandBreaks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildStopsWhenTheWandBreaks(helper);
    }

    @GameTest
    public void buildStopsWhenTheWandLeavesTheMainHand(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildStopsWhenTheWandLeavesTheMainHand(helper);
    }

    @GameTest
    public void missingBlocksNeedTheSecondClick(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.missingBlocksNeedTheSecondClick(helper);
    }

    @GameTest
    public void materialListCountsMultiItemBlocksByTheirState(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.materialListCountsMultiItemBlocksByTheirState(helper);
    }

    @GameTest
    public void buildConsumesEveryCandleAndPickleItNeeds(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.buildConsumesEveryCandleAndPickleItNeeds(helper);
    }

    @GameTest
    public void missingBlocksCheckHasNoCapAndRunsOverSeveralTicks(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.missingBlocksCheckHasNoCapAndRunsOverSeveralTicks(helper);
    }

    @GameTest
    public void runningBuildSurvivesLogoutAndResumesWithTheSameBlueprint(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.runningBuildSurvivesLogoutAndResumesWithTheSameBlueprint(helper);
    }

    @GameTest
    public void survivalBuildResetsGrownAndFilledStates(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.survivalBuildResetsGrownAndFilledStates(helper);
    }

    @GameTest
    public void failedPlacementHandsTheMaterialBack(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.failedPlacementHandsTheMaterialBack(helper);
    }

    @GameTest(maxTicks = com.simplebuildingtools.test.BlueprintTests.SIGN_BUDGET_MAX_TICKS)
    public void signPacketsShareOneParseBudgetPerPlayer(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.signPacketsShareOneParseBudgetPerPlayer(helper);
    }

    @GameTest
    public void blueprintBuildsGameMasterBlocksOnlyForOperators(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.blueprintBuildsGameMasterBlocksOnlyForOperators(helper);
    }

    @GameTest
    public void survivalBuildKeepsTheNoteBlockInstrument(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.survivalBuildKeepsTheNoteBlockInstrument(helper);
    }

    @GameTest
    public void repeatedClicksNeitherRestartTheCheckNorExceedTheClickBudget(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.repeatedClicksNeitherRestartTheCheckNorExceedTheClickBudget(helper);
    }

    @GameTest
    public void blueprintShowsItsStateInItsTexture(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.blueprintShowsItsStateInItsTexture(helper);
    }

    @GameTest
    public void blueprintTextExportImportRoundTrips(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.blueprintTextExportImportRoundTrips(helper);
    }

    @GameTest
    public void blueprintTextImportRejectsInvalidInput(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.blueprintTextImportRejectsInvalidInput(helper);
    }

    @GameTest(maxTicks = 400)
    public void parseCacheIsBoundedByWeight(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.parseCacheIsBoundedByWeight(helper);
    }

    @GameTest
    public void pixelCodeEditsPayTheParseBudgetFirst(GameTestHelper helper) {
        com.simplebuildingtools.test.BlueprintTests.pixelCodeEditsPayTheParseBudgetFirst(helper);
    }

    @GameTest
    public void airClicksOnlyResetAnUnlockedOctantWhileSneaking(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.airClicksOnlyResetAnUnlockedOctantWhileSneaking(helper);
    }

    @GameTest
    public void shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName(helper);
    }

    @GameTest
    public void octantTooltipListsTheLockAndBothCorners(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.octantTooltipListsTheLockAndBothCorners(helper);
    }

    @GameTest
    public void octantScrollPacketsOnlyEverTouchTheMainHand(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.octantScrollPacketsOnlyEverTouchTheMainHand(helper);
    }

    @GameTest
    public void waterCauldronWashesTheColourOffAnOctant(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.waterCauldronWashesTheColourOffAnOctant(helper);
    }

    @GameTest
    public void theOctantOnlyMeasuresAndPlacesNothing(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.theOctantOnlyMeasuresAndPlacesNothing(helper);
    }

    @GameTest
    public void theOctantRecipeCraftsFromItsDocumentedPattern(GameTestHelper helper) {
        com.simplebuildingtools.test.OctantTests.theOctantRecipeCraftsFromItsDocumentedPattern(helper);
    }

    @GameTest
    public void plannerKeepsTheClickFaceAndRechecksBuildRights(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.plannerKeepsTheClickFaceAndRechecksBuildRights(helper);
    }

    @GameTest
    public void plannerPreservesWaterloggingAndNamedContainers(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.plannerPreservesWaterloggingAndNamedContainers(helper);
    }

    @GameTest
    public void offHandClickIsPassedOnAndTheWandStopsOutsideBothHands(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.offHandClickIsPassedOnAndTheWandStopsOutsideBothHands(helper);
    }

    @GameTest
    public void clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt(helper);
    }

    @GameTest
    public void wandTierCapsTheRadiusSettingAndSizesThePlane(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.wandTierCapsTheRadiusSettingAndSizesThePlane(helper);
    }

    @GameTest
    public void materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack(helper);
    }

    @GameTest
    public void wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone(helper);
    }

    @GameTest
    public void wandStopsOnBrokenFaceOrOtherDimension(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.wandStopsOnBrokenFaceOrOtherDimension(helper);
    }

    @GameTest
    public void wandHungerRatesFollowTheCalibratedTierTable(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.wandHungerRatesFollowTheCalibratedTierTable(helper);
    }

    @GameTest
    public void survivalWandBuildsCostExhaustionOnlyPastTheAllowance(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.survivalWandBuildsCostExhaustionOnlyPastTheAllowance(helper);
    }

    @GameTest
    public void wandHungerCostSkipsCreativeAndTheSwitchedOffOption(GameTestHelper helper) {
        com.simplebuildingtools.test.BuildingWandTests.wandHungerCostSkipsCreativeAndTheSwitchedOffOption(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void linearWhileSneakingBuildsTheLineAwayFromTheClickedFace(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.linearWhileSneakingBuildsTheLineAwayFromTheClickedFace(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void bridgeRunsFromTheBlockUnderfootInTheFacingDirection(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.bridgeRunsFromTheBlockUnderfootInTheFacingDirection(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void coverOnlyGrowsTheSurfaceOfTheClickedKind(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.coverOnlyGrowsTheSurfaceOfTheClickedKind(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandSetsStairsAndLogsLikeThePlayerWould(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.wandSetsStairsAndLogsLikeThePlayerWould(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantInTheOffHandFillsItsShapeWithTheWand(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantInTheOffHandFillsItsShapeWithTheWand(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void bridgeFromTheUsePacketStartsAtTheEdgeOfTheFloorAhead(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.bridgeFromTheUsePacketStartsAtTheEdgeOfTheFloorAhead(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void aHeldUseKeyDoesNotRestartTheRunningLinearLine(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.aHeldUseKeyDoesNotRestartTheRunningLinearLine(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void bridgeAlsoStartsWhenTheClickAimsAcrossTheGap(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.bridgeAlsoStartsWhenTheClickAimsAcrossTheGap(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void linearWithoutSneakingAlsoBuildsOnlyTheLine(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.linearWithoutSneakingAlsoBuildsOnlyTheLine(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void bridgeGrowsFromTheEdgeToTheFarEndInHalfTheTime(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.bridgeGrowsFromTheEdgeToTheFarEndInHalfTheTime(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandSkipsEveryCellThePlayerMayNotBuildOn(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.wandSkipsEveryCellThePlayerMayNotBuildOn(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandNeitherBuildsNorUndoesWithoutBuildRights(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.wandNeitherBuildsNorUndoesWithoutBuildRights(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandSupplyPassesOverStacksWithComponents(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.wandSupplyPassesOverStacksWithComponents(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void wandPlacesGameMasterBlocksOnlyForOperators(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.wandPlacesGameMasterBlocksOnlyForOperators(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void undoLeavesTheSlabThatWasDoubledSinceStanding(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.undoLeavesTheSlabThatWasDoubledSinceStanding(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillPlansLazilyAndRefusesUnloadedChunks(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantFillPlansLazilyAndRefusesUnloadedChunks(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillLeavesGameMasterBlocksOutForNonOperators(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantFillLeavesGameMasterBlocksOutForNonOperators(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillCheckCountsCellsOnLayersItHasNotBuiltYet(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantFillCheckCountsCellsOnLayersItHasNotBuiltYet(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillNeedsTheChunksAroundItsBoxLoaded(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantFillNeedsTheChunksAroundItsBoxLoaded(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void octantFillHonoursOrderLayersHollowAndTheTickBudget(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.octantFillHonoursOrderLayersHollowAndTheTickBudget(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void undoKeepsBlockEntityContentsSpreadFacesAndProtectedCells(GameTestHelper helper) {
        com.simplebuildingtools.test.WandModeTests.undoKeepsBlockEntityContentsSpreadFacesAndProtectedCells(helper);
    }
}
