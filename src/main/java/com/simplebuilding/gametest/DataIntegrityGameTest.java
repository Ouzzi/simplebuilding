package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the data integrity tests.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link DataIntegrityTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class DataIntegrityGameTest {

    @GameTest
    public void everyModItemIsInTheItemRegistry(GameTestHelper helper) {
        DataIntegrityTests.everyModItemIsInTheItemRegistry(helper);
    }

    @GameTest
    public void everyRecipelessModItemHasJeiInfo(GameTestHelper helper) {
        DataIntegrityTests.everyRecipelessModItemHasJeiInfo(helper);
    }

    @GameTest
    public void basicUpgradeTemplateTextNamesOnlyRealToolsAndMaterials(GameTestHelper helper) {
        DataIntegrityTests.basicUpgradeTemplateTextNamesOnlyRealToolsAndMaterials(helper);
    }

    @GameTest
    public void everyModBlockIsRegisteredAndHasItsBlockItem(GameTestHelper helper) {
        DataIntegrityTests.everyModBlockIsRegisteredAndHasItsBlockItem(helper);
    }

    @GameTest
    public void modRecipesOnlyReferenceRegisteredItems(GameTestHelper helper) {
        DataIntegrityTests.modRecipesOnlyReferenceRegisteredItems(helper);
    }

    @GameTest
    public void everyModBlockLootTableLoads(GameTestHelper helper) {
        DataIntegrityTests.everyModBlockLootTableLoads(helper);
    }

    @GameTest(maxTicks = DataIntegrityTests.BLOCK_DROP_MAX_TICKS)
    public void brokenModBlocksDropTheirExpectedItem(GameTestHelper helper) {
        DataIntegrityTests.brokenModBlocksDropTheirExpectedItem(helper);
    }

    @GameTest
    public void modEnchantmentsArePresentInTheDatapackRegistry(GameTestHelper helper) {
        DataIntegrityTests.modEnchantmentsArePresentInTheDatapackRegistry(helper);
    }

    @GameTest
    public void modEnchantmentTagsResolveToTheExpectedEntries(GameTestHelper helper) {
        DataIntegrityTests.modEnchantmentTagsResolveToTheExpectedEntries(helper);
    }

    @GameTest
    public void voidProtectedTagIsLanguageIndependent(GameTestHelper helper) {
        DataIntegrityTests.voidProtectedTagIsLanguageIndependent(helper);
    }

    @GameTest
    public void generatedEnchantmentFilesStillMatchTheirSource(GameTestHelper helper) {
        DataIntegrityTests.generatedEnchantmentFilesStillMatchTheirSource(helper);
    }

    @GameTest
    public void quartzCheckersAreMinedByPickaxeAndCraftedFromTheirMaterial(GameTestHelper helper) {
        DataIntegrityTests.quartzCheckersAreMinedByPickaxeAndCraftedFromTheirMaterial(helper);
    }

    @GameTest
    public void endBrickSetsAreCraftedCutMinedAndTaggedLikeVanilla(GameTestHelper helper) {
        DataIntegrityTests.endBrickSetsAreCraftedCutMinedAndTaggedLikeVanilla(helper);
    }

    @GameTest
    public void endPalettesAreRecolouredFromEndStoneAndPurpurLikeDye(GameTestHelper helper) {
        DataIntegrityTests.endPalettesAreRecolouredFromEndStoneAndPurpurLikeDye(helper);
    }

    @GameTest
    public void enderQuartzPaletteIsRecolouredFromQuartzLikeDye(GameTestHelper helper) {
        DataIntegrityTests.enderQuartzPaletteIsRecolouredFromQuartzLikeDye(helper);
    }

    @GameTest
    public void basicUpgradeTemplateCostsTwiceTheCraftingMaterial(GameTestHelper helper) {
        DataIntegrityTests.basicUpgradeTemplateCostsTwiceTheCraftingMaterial(helper);
    }

    @GameTest
    public void enderQuartzIsCraftedFromAstralitDustNihilithShardAndQuartz(GameTestHelper helper) {
        DataIntegrityTests.enderQuartzIsCraftedFromAstralitDustNihilithShardAndQuartz(helper);
    }

    @GameTest
    public void everyModItemIsInExactlyOneCreativeTab(GameTestHelper helper) {
        DataIntegrityTests.everyModItemIsInExactlyOneCreativeTab(helper);
    }

    @GameTest
    public void machinesAndStorageTabIsLaidOutInRowsOfNine(GameTestHelper helper) {
        DataIntegrityTests.machinesAndStorageTabIsLaidOutInRowsOfNine(helper);
    }

    @GameTest
    public void materialsTabIsLaidOutInRows(GameTestHelper helper) {
        DataIntegrityTests.materialsTabIsLaidOutInRows(helper);
    }

    @GameTest
    public void padsTabIsLaidOutInRowsOfNine(GameTestHelper helper) {
        DataIntegrityTests.padsTabIsLaidOutInRowsOfNine(helper);
    }

    @GameTest
    public void buildingBlocksTabIsLaidOutInRows(GameTestHelper helper) {
        DataIntegrityTests.buildingBlocksTabIsLaidOutInRows(helper);
    }

    @GameTest
    public void toolsTabIsLaidOutInRowsOfNine(GameTestHelper helper) {
        DataIntegrityTests.toolsTabIsLaidOutInRowsOfNine(helper);
    }

    @GameTest
    public void creativeSpacerCannotBeTakenOrKept(GameTestHelper helper) {
        DataIntegrityTests.creativeSpacerCannotBeTakenOrKept(helper);
    }

    @GameTest
    public void devEnchantedTabOffersEveryExclusiveChoiceAtMaxLevelOnTopTiers(GameTestHelper helper) {
        DataIntegrityTests.devEnchantedTabOffersEveryExclusiveChoiceAtMaxLevelOnTopTiers(helper);
    }

    @GameTest
    public void devEnchantedTabIsOnlyFilledInDevelopmentOrWhenConfigured(GameTestHelper helper) {
        DataIntegrityTests.devEnchantedTabIsOnlyFilledInDevelopmentOrWhenConfigured(helper);
    }

    @GameTest
    public void everyTrimmableArmourShowsEveryTrimPatternOnItsIcon(GameTestHelper helper) {
        DataIntegrityTests.everyTrimmableArmourShowsEveryTrimPatternOnItsIcon(helper);
    }

    @GameTest
    public void everyItemHasAnItemDefinitionWhoseModelsAndTexturesExist(GameTestHelper helper) {
        DataIntegrityTests.everyItemHasAnItemDefinitionWhoseModelsAndTexturesExist(helper);
    }

    @GameTest
    public void everyVanillaEnchantmentHasItsOwnBookModel(GameTestHelper helper) {
        DataIntegrityTests.everyVanillaEnchantmentHasItsOwnBookModel(helper);
    }

    @GameTest
    public void vanillaBookTextureFollowsTheClientOption(GameTestHelper helper) {
        DataIntegrityTests.vanillaBookTextureFollowsTheClientOption(helper);
    }

    @GameTest
    public void modBookTextureFollowsTheClientOption(GameTestHelper helper) {
        DataIntegrityTests.modBookTextureFollowsTheClientOption(helper);
    }

    @GameTest
    public void visibleTrimIconsFollowTheClientOptions(GameTestHelper helper) {
        DataIntegrityTests.visibleTrimIconsFollowTheClientOptions(helper);
    }

    @GameTest
    public void everyEnderiteGearPieceUpgradesFromItsNetheriteTwin(GameTestHelper helper) {
        DataIntegrityTests.everyEnderiteGearPieceUpgradesFromItsNetheriteTwin(helper);
    }

    @GameTest
    public void everyEnderiteItemIsInTheEnderiteItemsTag(GameTestHelper helper) {
        DataIntegrityTests.everyEnderiteItemIsInTheEnderiteItemsTag(helper);
    }

    @GameTest
    public void legacySpatulasAreHiddenFromRecipeViewers(GameTestHelper helper) {
        DataIntegrityTests.legacySpatulasAreHiddenFromRecipeViewers(helper);
    }

    @GameTest
    public void enderiteHorseAndNautilusArmorRankOneStepAboveNetherite(GameTestHelper helper) {
        DataIntegrityTests.enderiteHorseAndNautilusArmorRankOneStepAboveNetherite(helper);
    }

    @GameTest
    public void everyPlayerFacingTextHasEnglishAndGermanTranslations(GameTestHelper helper) {
        DataIntegrityTests.everyPlayerFacingTextHasEnglishAndGermanTranslations(helper);
    }

    @GameTest
    public void toolNamesCarryNoLeftoverOldNames(GameTestHelper helper) {
        DataIntegrityTests.toolNamesCarryNoLeftoverOldNames(helper);
    }

    @GameTest
    public void modItemRaritiesFollowTheFamilyScheme(GameTestHelper helper) {
        DataIntegrityTests.modItemRaritiesFollowTheFamilyScheme(helper);
    }

    @GameTest
    public void modItemNamesFollowTheFamilyPatterns(GameTestHelper helper) {
        DataIntegrityTests.modItemNamesFollowTheFamilyPatterns(helper);
    }
}
