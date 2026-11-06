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
    public void rotatorArmorStandPosesStepForwardAndBack(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_armor_stand_poses_step_forward_and_back").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rotatorBrokenArmorStandKeepsItsPose(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_broken_armor_stand_keeps_its_pose").accept(h);
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
    public void rotatorTurnQueuesTheEnderEchoShortlyAfterTheRatchet(GameTestHelper h) {
        GadgetsTests.ALL.get("rotator_turn_queues_the_ender_echo_shortly_after_the_ratchet").accept(h);
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
    public void rodTheLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_laser_relay_checks_the_sender_and_only_reaches_nearby_players").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheAmethystLensIsCraftedAroundAnIronCore(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_amethyst_lens_is_crafted_around_an_iron_core").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamMeltsIceAndSnow(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_melts_ice_and_snow").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamIgnitesFlammableBlocksOnlyAfterDwelling(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_ignites_flammable_blocks_only_after_dwelling").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamLightsSoulFireCampfiresAndCandles(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_lights_soul_fire_campfires_and_candles").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamNeverLightsNetherPortals(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_never_lights_nether_portals").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamPrimesTntAfterDwellingButRespectsTheRules(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_primes_tnt_after_dwelling_but_respects_the_rules").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensDrainsChargeEvenWhenItPointsIntoTheAir(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_drains_charge_even_when_it_points_into_the_air").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensRecordsTheLastMeasurementOnlyWithConstructorsTouch(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_records_the_last_measurement_only_with_constructors_touch").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensDwellTimeGrowsModeratelyWithDistance(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_dwell_time_grows_moderately_with_distance").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensSetsLivingEntitiesOnFireTakingTwiceAsLong(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_sets_living_entities_on_fire_taking_twice_as_long").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensOnlyIgnitesPlayersWhenPvpAllowsIt(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_only_ignites_players_when_pvp_allows_it").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_hums_on_any_surface_and_sizzles_or_crackles_while_heating").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamDriesWetSponges(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_dries_wet_sponges").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensBeamRespectsAdventureModeAndTheFireSpreadRule(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_beam_respects_adventure_mode_and_the_fire_spread_rule").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLensChargeRunsDownButTheLensNeverBreaks(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_lens_charge_runs_down_but_the_lens_never_breaks").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodAnvilRechargeWithAmethystShardsCostsNoLevels(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_anvil_recharge_with_amethyst_shards_costs_no_levels").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheRodDrainsFourChargePerSecondOfBeaming(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_rod_drains_four_charge_per_second_of_beaming").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheResonanceRodScansCreaturesButNotPlayers(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_resonance_rod_scans_creatures_but_not_players").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodTheLaserSwitchesStopWhatTheBeamIgnites(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_the_laser_switches_stop_what_the_beam_ignites").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void rodLensBeamLeavesBlocksTheLoaderEventsProtect(GameTestHelper h) {
        GadgetsTests.ALL.get("rod_lens_beam_leaves_blocks_the_loader_events_protect").accept(h);
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

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderIsCraftedFromTheRecoveryCompassACoreAndSevenNuggets(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_is_crafted_from_the_recovery_compass_a_core_and_seven_nuggets").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderIsRegisteredAndNamed(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_is_registered_and_named").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_links_to_the_lodestone_and_teleports_without_any_pearl").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoAJumpEmptiesTheWholeChargeEvenWithUnbreaking(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_a_jump_empties_the_whole_charge_even_with_unbreaking").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderHonorsStackCapacityAndCreativeMode(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_honors_stack_capacity_and_creative_mode").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderChargesForThreeSecondsAndReleasingEarlyCostsNothing(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_charges_for_three_seconds_and_releasing_early_costs_nothing").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoAFullChargeJumpsAndLeavesTheEchoSounderEmpty(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_a_full_charge_jumps_and_leaves_the_echo_sounder_empty").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderIsOnlyChargedAgainAfterAllItsRepairPoints(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_is_only_charged_again_after_all_its_repair_points").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoACrackedEchoSounderChargesTwiceAsLongAndShattersAfterTheJump(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_a_cracked_echo_sounder_charges_twice_as_long_and_shatters_after_the_jump").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoABlockedJumpCostsNothing(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_a_blocked_jump_costs_nothing").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheCooldownAfterAJumpIsTwentyFourSeconds(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_cooldown_after_a_jump_is_twenty_four_seconds").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_does_not_relink_the_lodestone_it_is_linked_to").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void echoTheEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance(GameTestHelper h) {
        GadgetsTests.ALL.get("echo_the_echo_sounder_locks_for_up_to_five_seconds_after_an_attempt_depending_on_the_distance").accept(h);
    }
}
