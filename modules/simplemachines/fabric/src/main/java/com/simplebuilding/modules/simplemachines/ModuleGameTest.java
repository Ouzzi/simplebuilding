package com.simplebuilding.modules.simplemachines;

import com.simplemachines.test.MachinesTests;
import com.simplemachines.test.PistonUpgradeTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link MachinesTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplemachines:empty")
    public void registered(GameTestHelper h) {
        MachinesTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void switchOffRemovesRecipe(GameTestHelper h) {
        MachinesTests.ALL.get("switch_off_removes_recipe").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void oldIdsMove(GameTestHelper h) {
        MachinesTests.ALL.get("old_ids_move").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void subModSwitch(GameTestHelper h) {
        MachinesTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void recipesCraft(GameTestHelper h) {
        MachinesTests.ALL.get("recipes_craft").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void dampenerSubtractsTheStage(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_subtracts_the_stage").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void dampenerWaitsOneRedstoneTick(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_waits_one_redstone_tick").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void dampenerStageCyclesOnRightClick(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_stage_cycles_on_right_click").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 40)
    public void dampenerIgnoresSideInputs(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_ignores_side_inputs").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void dampenerIsPushable(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_is_pushable").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void dampenerSwitchOffOutputsNothing(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_switch_off_outputs_nothing").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void doublePistonPlacesOneCell(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_places_one_cell").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void doublePistonStageOneForWeakSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stage_one_for_weak_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void doublePistonStageTwoForStrongSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stage_two_for_strong_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 140)
    public void doublePistonPushesAndRetracts(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_pushes_and_retracts").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 160)
    public void doublePistonStepsBackOnWeakerSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_steps_back_on_weaker_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 120)
    public void doublePistonPushLimit(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_push_limit").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void doublePistonStoppedByObsidian(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stopped_by_obsidian").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 40)
    public void doublePistonCannotBePushed(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_cannot_be_pushed").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void doublePistonPushedByVanillaPiston(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_pushed_by_vanilla_piston").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 120)
    public void doublePistonFallsAsUnit(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_falls_as_unit").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_switch_off_keeps_it_still").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 200)
    public void doublePistonAllFacings(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_all_facings").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 200)
    public void doublePistonMovesWithVanillaBlocks(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_moves_with_vanilla_blocks").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        MachinesTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        MachinesTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void guideBook(GameTestHelper h) {
        MachinesTests.ALL.get("guide_book").accept(h);
    }

    // ---- piston upgrades (MACH32-MOD stage 2): bodies in PistonUpgradeTests ----

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.PAID_PUSH_MAX_TICKS)
    public void reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays(GameTestHelper h) {
        PistonUpgradeTests.reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays(h);
    }

    @GameTest(structure = "simplemachines:empty", skyAccess = true, maxTicks = PistonUpgradeTests.LIMIT_MAX_TICKS)
    public void reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock(GameTestHelper h) {
        PistonUpgradeTests.reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.STICKY_MAX_TICKS)
    public void stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack(GameTestHelper h) {
        PistonUpgradeTests.stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.SACRIFICE_MAX_TICKS)
    public void netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock(GameTestHelper h) {
        PistonUpgradeTests.netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.ENDERITE_MAX_TICKS)
    public void enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks(GameTestHelper h) {
        PistonUpgradeTests.enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.HEAD_MAX_TICKS)
    public void breakingTheHeadOfModPistonsBreaksThePistonToo(GameTestHelper h) {
        PistonUpgradeTests.breakingTheHeadOfModPistonsBreaksThePistonToo(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.OWN_HEAD_MAX_TICKS)
    public void everyModPistonCarriesTheHeadOfItsTierAndTakesItBack(GameTestHelper h) {
        PistonUpgradeTests.everyModPistonCarriesTheHeadOfItsTierAndTakesItBack(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.BORE_EFFECTS_MAX_TICKS)
    public void modPistonBreaksReportEveryDestroyedBlockForParticlesAndSound(GameTestHelper h) {
        PistonUpgradeTests.modPistonBreaksReportEveryDestroyedBlockForParticlesAndSound(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.IMMUNE_MAX_TICKS)
    public void immuneBlocksNeverMoveOrBreak(GameTestHelper h) {
        PistonUpgradeTests.immuneBlocksNeverMoveOrBreak(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.EXPLOSION_MAX_TICKS)
    public void explosionsCannotDeleteUnbreakableBlocksWhileTheyMove(GameTestHelper h) {
        PistonUpgradeTests.explosionsCannotDeleteUnbreakableBlocksWhileTheyMove(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.GUARD_MAX_TICKS)
    public void modPistonsAskThePlatformGuardBeforeEveryBreak(GameTestHelper h) {
        PistonUpgradeTests.modPistonsAskThePlatformGuardBeforeEveryBreak(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.DURABILITY_MAX_TICKS)
    public void netheriteBreakerLosesDurabilityAndCrumblesToReinforcedPiston(GameTestHelper h) {
        PistonUpgradeTests.netheriteBreakerLosesDurabilityAndCrumblesToReinforcedPiston(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.ENDERITE_DURABILITY_MAX_TICKS)
    public void enderitePistonLosesDurabilityAndCrumblesToNetheriteBreaker(GameTestHelper h) {
        PistonUpgradeTests.enderitePistonLosesDurabilityAndCrumblesToNetheriteBreaker(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = PistonUpgradeTests.LOADER_EVENTS_MAX_TICKS)
    public void modPistonsFireTheRealLoaderEventsAndHonourTheirConfigSwitch(GameTestHelper h) {
        PistonUpgradeTests.modPistonsFireTheRealLoaderEventsAndHonourTheirConfigSwitch(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void pistonUpgradesRegistered(GameTestHelper h) {
        PistonUpgradeTests.pistonUpgradesRegistered(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void oldPistonIdsMove(GameTestHelper h) {
        PistonUpgradeTests.oldPistonIdsMove(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void pistonOptionsComeOverFromSimpleBuilding(GameTestHelper h) {
        PistonUpgradeTests.pistonOptionsComeOverFromSimpleBuilding(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn(GameTestHelper h) {
        PistonUpgradeTests.endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void reinforcedStickyPistonCraftsFromSlimeAndDropsItself(GameTestHelper h) {
        PistonUpgradeTests.reinforcedStickyPistonCraftsFromSlimeAndDropsItself(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void breakerPistonDurabilityTravelsWithTheItemAsItsDurabilityBar(GameTestHelper h) {
        PistonUpgradeTests.breakerPistonDurabilityTravelsWithTheItemAsItsDurabilityBar(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void onlyVanillaUnbreakablesAreBreachedUnlessTheConfigSaysOtherwise(GameTestHelper h) {
        PistonUpgradeTests.onlyVanillaUnbreakablesAreBreachedUnlessTheConfigSaysOtherwise(h);
    }

    // ---- hoppers and hopper carts (MACH32-MOD stage 3): bodies in TierHopperTests and HopperModuleTests ----

    @GameTest(structure = "simplemachines:empty", maxTicks = com.simplemachines.test.TierHopperTests.REDSTONE_LOCK_MAX_TICKS)
    public void redstonePowerStopsEveryHopperTransfer(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.redstonePowerStopsEveryHopperTransfer(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = com.simplemachines.test.TierHopperTests.KEEP_ONE_MAX_TICKS)
    public void aFilteredHopperKeepsOneFilterItemInEverySlot(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.aFilteredHopperKeepsOneFilterItemInEverySlot(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void theModeDelegateReadsAndWritesTheFilterMode(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.theModeDelegateReadsAndWritesTheFilterMode(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void automationOnlyTopsUpMatchingSlots(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.automationOnlyTopsUpMatchingSlots(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.hopperConfigurationSurvivesTheSaveAndLoadRoundTrip(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void theUpdateTagCarriesTheFilterModeToTheClient(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.theUpdateTagCarriesTheFilterModeToTheClient(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.hopperMenuOpensOnUseAndFilterSlotsTakeOnlyTheirItem(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperBlocksCarryTheirRegisteredStrengthSoundAndTags(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.hopperBlocksCarryTheirRegisteredStrengthSoundAndTags(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperRecipesCraftFromTheirDocumentedPatterns(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.hopperRecipesCraftFromTheirDocumentedPatterns(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = com.simplemachines.test.TierHopperTests.HOPPER_DROP_MAX_TICKS)
    public void bothHoppersDropThemselvesWhenBroken(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.bothHoppersDropThemselvesWhenBroken(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = com.simplemachines.test.TierHopperTests.ITEM_AUTOMATION_MAX_TICKS)
    public void modHoppersFallBackToTheLoaderTransferApiWithoutContainer(GameTestHelper h) {
        com.simplemachines.test.TierHopperTests.modHoppersFallBackToTheLoaderTransferApiWithoutContainer(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hoppersRegistered(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hoppersRegistered(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void oldHopperIdsMove(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.oldHopperIdsMove(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperSpeedsComeOverFromSimpleBuilding(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hopperSpeedsComeOverFromSimpleBuilding(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void filterKeyIsMenuButton(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.filterKeyIsMenuButton(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void tierChangeKeepsTheBlockEntity(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.tierChangeKeepsTheBlockEntity(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void hopperCartsTakeInAtTheHopperSpeed(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hopperCartsTakeInAtTheHopperSpeed(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void hopperCartFiltersLikeTheHopperBlock(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hopperCartFiltersLikeTheHopperBlock(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperCartRecipesAndDrop(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hopperCartRecipesAndDrop(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void hopperTooltipsNameSpeedAndFilter(GameTestHelper h) {
        com.simplemachines.test.HopperModuleTests.hopperTooltipsNameSpeedAndFilter(h);
    }

    // MACH32-MOD stage 4: the Auto Smither (AutoSmitherTests).
    @GameTest(structure = "simplemachines:empty")
    public void autoSmitherRegistered(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherRegistered(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void oldAutoSmitherIdsMove(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.oldAutoSmitherIdsMove(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void smitherDelayComesOverFromSimpleBuilding(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.smitherDelayComesOverFromSimpleBuilding(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void autoSmitherUpgradesDiamondToNetherite(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherUpgradesDiamondToNetherite(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void autoSmitherAppliesArmourTrim(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherAppliesArmourTrim(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 120)
    public void autoSmitherStopsWithoutRedstone(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherStopsWithoutRedstone(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void autoSmitherSmithsOncePerPulse(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherSmithsOncePerPulse(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void autoSmitherEjectsWithoutContainer(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherEjectsWithoutContainer(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 80)
    public void autoSmitherSortsHopperInput(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherSortsHopperInput(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void autoSmitherOutputRejectsInsertion(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherOutputRejectsInsertion(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void autoSmitherOutputCapacityAndRecipeError(GameTestHelper h) {
        com.simplemachines.test.AutoSmitherTests.autoSmitherOutputCapacityAndRecipeError(h);
    }
}
