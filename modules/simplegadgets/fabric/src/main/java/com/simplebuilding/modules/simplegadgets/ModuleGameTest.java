package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.test.GadgetsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link GadgetsTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplegadgets:empty")
    public void registered(GameTestHelper h) {
        GadgetsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void subModSwitch(GameTestHelper h) {
        GadgetsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        GadgetsTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeRecipe(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_recipe").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAutowalkKeepsWalkingUnderNonPausingScreens(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_autowalk_keeps_walking_under_non_pausing_screens").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAutowalkFollowsPathsAndRailsAroundCorners(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_autowalk_follows_paths_and_rails_around_corners").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAltimeterReadsTheGroundAndRangeReachesDeeper(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_altimeter_reads_the_ground_and_range_reaches_deeper").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorLogAxisCyclesThroughAllThreeAxesAndIgnoresSneaking(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_log_axis_cycles_through_all_three_axes_and_ignores_sneaking").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorRimIsTheOuterEighthOfEveryFaceAndNowhereInside(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_rim_is_the_outer_eighth_of_every_face_and_nowhere_inside").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorFacingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_facing_blocks_turn_one_quarter_around_the_clicked_axis_or_jump_to_its_start").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorRimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_rim_aims_facing_blocks_at_the_rim_its_opposite_or_the_next_valid_value").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_sixteen_step_blocks_step_once_in_the_middle_and_four_times_at_the_rim").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorChargeRunsDownButTheRotatorNeverBreaks(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_charge_runs_down_but_the_rotator_never_breaks").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorAnvilRechargesWithSixteenEnderPearlsForNoLevels(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_anvil_recharges_with_sixteen_ender_pearls_for_no_levels").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorAnvilTakesUnbreakingButRefusesMending(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_anvil_takes_unbreaking_but_refuses_mending").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorATurnQueuesTheEnderEchoShortlyAfterTheRatchet(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_a_turn_queues_the_ender_echo_shortly_after_the_ratchet").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorCraftingTakesAnIronCoreFourIronAndAnEnderPearlInThatShape(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_crafting_takes_an_iron_core_four_iron_and_an_ender_pearl_in_that_shape").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSwapReplacesTheBlockGivesTheDropsAndUsesTheOffhandBlock(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_swap_replaces_the_block_gives_the_drops_and_uses_the_offhand_block").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSwapWindupGrowsWithHardnessAndShrinksWithEfficiency(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_swap_windup_grows_with_hardness_and_shrinks_with_efficiency").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSwapWindupStartsOnClickAndOnlyFinishesOnTheSameAimedBlock(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_swap_windup_starts_on_click_and_only_finishes_on_the_same_aimed_block").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSwapAllowsStoneToolBlocksAndKeepsValuablesAndListsDecide(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_swap_allows_stone_tool_blocks_and_keeps_valuables_and_lists_decide").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorSwapRefusalsChangeNothingAndCreativeSwapsForFree(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_swap_refusals_change_nothing_and_creative_swaps_for_free").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        GadgetsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        GadgetsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void guideBook(GameTestHelper h) {
        GadgetsTests.ALL.get("guide_book").accept(h);
    }
}
