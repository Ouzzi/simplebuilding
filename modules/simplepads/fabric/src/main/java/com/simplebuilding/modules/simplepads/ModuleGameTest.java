package com.simplebuilding.modules.simplepads;

import com.simplepads.test.PadsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter: ids module_game_test_<name>, bodies in {@link PadsTests} (the moved pad tests keep their SimpleBuilding budgets).
 * GENERATED in b-pads01 from the SimpleBuilding adapters; names are load bearing.
 */
public final class ModuleGameTest {
    /** JEI24 (a-fix09b): the eleven pad, plate and Spawn Elytra pages with the server line, texts in EN and DE. */
    @GameTest
    public void jeiInfoPages(GameTestHelper h) { com.simplelib.test.ViewerInfoTests.infoPages(h, "simplepads", 11); }

    @GameTest
    public void registered(GameTestHelper h) {
        PadsTests.ALL.get("registered").accept(h);
    }

    @GameTest
    public void oldPadIdsMove(GameTestHelper h) {
        PadsTests.ALL.get("old_pad_ids_move").accept(h);
    }

    @GameTest
    public void configTakeover(GameTestHelper h) {
        PadsTests.ALL.get("config_takeover").accept(h);
    }

    @GameTest
    public void subModSwitch(GameTestHelper h) {
        PadsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest
    public void enderiteTiersNeedSimpleOres(GameTestHelper h) {
        PadsTests.ALL.get("enderite_tiers_need_simple_ores").accept(h);
    }

    @GameTest
    public void discoverRecipeUnlocks(GameTestHelper h) {
        PadsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest
    public void discoverBehaviourHints(GameTestHelper h) {
        PadsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest
    public void guideBook(GameTestHelper h) {
        PadsTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadOverhaulTests.WAIT_MAX_TICKS)
    public void spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds(GameTestHelper h) {
        PadsTests.ALL.get("spawn_teleporter_tiers_wait_fifty_twenty_and_five_seconds").accept(h);
    }

    @GameTest
    public void oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper h) {
        PadsTests.ALL.get("old_spawn_teleporters_become_their_new_tier_in_the_world_and_the_inventory").accept(h);
    }

    @GameTest
    public void oldElytraPadsBecomeTheirNewTierInTheWorldAndTheInventory(GameTestHelper h) {
        PadsTests.ALL.get("old_elytra_pads_become_their_new_tier_in_the_world_and_the_inventory").accept(h);
    }

    @GameTest
    public void tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem(GameTestHelper h) {
        PadsTests.ALL.get("tier_one_of_every_pad_family_is_smithed_from_its_plate_and_unlock_item").accept(h);
    }

    @GameTest
    public void redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem(GameTestHelper h) {
        PadsTests.ALL.get("redstone_switches_launchpads_and_flypads_off_and_comparators_read_them").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadOverhaulTests.HOPPER_MAX_TICKS)
    public void hoppersFillOnlyWindChargesIntoTheLaunchpad(GameTestHelper h) {
        PadsTests.ALL.get("hoppers_fill_only_wind_charges_into_the_launchpad").accept(h);
    }

    @GameTest
    public void spawnTeleporterSignalChangesResetTheEntireWarmup(GameTestHelper h) {
        PadsTests.ALL.get("spawn_teleporter_signal_changes_reset_the_entire_warmup").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadOverhaulTests.DESTINATION_MAX_TICKS)
    public void theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn(GameTestHelper h) {
        PadsTests.ALL.get("the_spawn_teleporter_takes_players_to_their_bed_and_with_redstone_to_the_world_spawn").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadOverhaulTests.STATE_MAX_TICKS)
    public void everyPadFamilyShowsWhetherItIsWorking(GameTestHelper h) {
        PadsTests.ALL.get("every_pad_family_shows_whether_it_is_working").accept(h);
    }

    @GameTest(maxTicks = 60)
    public void padEffectsFadeInAndStopSchedulingWhenSettled(GameTestHelper h) {
        PadsTests.ALL.get("pad_effects_fade_in_and_stop_scheduling_when_settled").accept(h);
    }

    @GameTest
    public void splashPotionsLandingOnThePadAreStoredAndReplaced(GameTestHelper h) {
        PadsTests.ALL.get("splash_potions_landing_on_the_pad_are_stored_and_replaced").accept(h);
    }

    @GameTest
    public void standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds(GameTestHelper h) {
        PadsTests.ALL.get("standing_on_the_pad_ramps_the_effect_to_twenty_five_fifty_and_one_hundred_percent_in_three_seconds").accept(h);
    }

    @GameTest
    public void instantEffectsApplyOnceAtTheThreeSecondMark(GameTestHelper h) {
        PadsTests.ALL.get("instant_effects_apply_once_at_the_three_second_mark").accept(h);
    }

    @GameTest
    public void aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration(GameTestHelper h) {
        PadsTests.ALL.get("a_full_application_puts_the_pad_on_cooldown_for_twice_the_effect_duration").accept(h);
    }

    @GameTest
    public void aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain(GameTestHelper h) {
        PadsTests.ALL.get("a_pad_broken_during_cooldown_keeps_the_remaining_time_and_resumes_when_placed_again").accept(h);
    }

    @GameTest
    public void everyPadItemStacksToOne(GameTestHelper h) {
        PadsTests.ALL.get("every_pad_item_stacks_to_one").accept(h);
    }

    @GameTest
    public void potionPadRecipesCoverAllThreeTiers(GameTestHelper h) {
        PadsTests.ALL.get("potion_pad_recipes_cover_all_three_tiers").accept(h);
    }

    @GameTest
    public void chargedCreeperExplosionsDropOneBlazeHeadEach(GameTestHelper h) {
        PadsTests.ALL.get("charged_creeper_explosions_drop_one_blaze_head_each").accept(h);
    }

    @GameTest
    public void blazesKilledOtherwiseDropNoHead(GameTestHelper h) {
        PadsTests.ALL.get("blazes_killed_otherwise_drop_no_head").accept(h);
    }

    @GameTest
    public void potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToComparator(GameTestHelper h) {
        PadsTests.ALL.get("potion_pads_are_switched_off_by_redstone_and_report_their_state_to_a_comparator").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PotionPadTests.AUTOMATION_MAX_TICKS)
    public void aDispenserFillsThePotionPadButHopperCannot(GameTestHelper h) {
        PadsTests.ALL.get("a_dispenser_fills_the_potion_pad_but_a_hopper_cannot").accept(h);
    }

    @GameTest
    public void harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad(GameTestHelper h) {
        PadsTests.ALL.get("harmful_effects_reach_only_the_owner_and_strangers_cannot_charge_the_pad").accept(h);
    }

    @GameTest
    public void blockedEffectsAreNeverGivenAndStartNoCooldown(GameTestHelper h) {
        PadsTests.ALL.get("blocked_effects_are_never_given_and_start_no_cooldown").accept(h);
    }

    @GameTest
    public void levelsAndDurationsNeverExceedVanillaOrThePotion(GameTestHelper h) {
        PadsTests.ALL.get("levels_and_durations_never_exceed_vanilla_or_the_potion").accept(h);
    }

    @GameTest
    public void theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier(GameTestHelper h) {
        PadsTests.ALL.get("the_cooldown_follows_the_granted_duration_and_the_effect_multiplier").accept(h);
    }

    @GameTest
    public void healingLocksThePlayerOutOfEveryPadForOneMinute(GameTestHelper h) {
        PadsTests.ALL.get("healing_locks_the_player_out_of_every_pad_for_one_minute").accept(h);
    }

    @GameTest
    public void mobsNeverReceiveAnyPadEffect(GameTestHelper h) {
        PadsTests.ALL.get("mobs_never_receive_any_pad_effect").accept(h);
    }

    @GameTest
    public void everyVanillaEffectHasDeliberateRules(GameTestHelper h) {
        PadsTests.ALL.get("every_vanilla_effect_has_deliberate_rules").accept(h);
    }

    @GameTest
    public void honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing(GameTestHelper h) {
        PadsTests.ALL.get("honeycomb_waxes_every_stage_and_waxed_plates_stop_oxidizing").accept(h);
    }

    @GameTest
    public void theAxeScrapesTheWaxOffBeforeTheOxidation(GameTestHelper h) {
        PadsTests.ALL.get("the_axe_scrapes_the_wax_off_before_the_oxidation").accept(h);
    }

    @GameTest
    public void waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb(GameTestHelper h) {
        PadsTests.ALL.get("waxed_copper_plates_are_crafted_from_the_plate_and_one_honeycomb").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PressurePlateTests.RELEASE_MAX_TICKS)
    public void copperPlatesReleaseAsLateAsTheyPress(GameTestHelper h) {
        PadsTests.ALL.get("copper_plates_release_as_late_as_they_press").accept(h);
    }

    @GameTest
    public void everyModPressurePlateVisiblySinksWhenPressed(GameTestHelper h) {
        PadsTests.ALL.get("every_mod_pressure_plate_visibly_sinks_when_pressed").accept(h);
    }

    @GameTest
    public void everyModPressurePlateIsNamedLikeTheVanillaOnes(GameTestHelper h) {
        PadsTests.ALL.get("every_mod_pressure_plate_is_named_like_the_vanilla_ones").accept(h);
    }

    @GameTest
    public void launchpadTiersHoldFourEightAndSixteenWindCharges(GameTestHelper h) {
        PadsTests.ALL.get("launchpad_tiers_hold_four_eight_and_sixteen_wind_charges").accept(h);
    }

    @GameTest
    public void sneakingWithWindChargesLoadsTheWholeHandUpToCapacity(GameTestHelper h) {
        PadsTests.ALL.get("sneaking_with_wind_charges_loads_the_whole_hand_up_to_capacity").accept(h);
    }

    @GameTest
    public void launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges(GameTestHelper h) {
        PadsTests.ALL.get("launch_strength_per_charge_is_doubled_so_full_tiers_launch_like_twice_the_old_charges").accept(h);
    }

    @GameTest
    public void oldLaunchpadsDropTheChargesTheirTierNoLongerHolds(GameTestHelper h) {
        PadsTests.ALL.get("old_launchpads_drop_the_charges_their_tier_no_longer_holds").accept(h);
    }

    @GameTest
    public void chunkLoaderTiersForceOneFiveAndNineChunks(GameTestHelper h) {
        PadsTests.ALL.get("chunk_loader_tiers_force_one_five_and_nine_chunks").accept(h);
    }

    @GameTest
    public void aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross(GameTestHelper h) {
        PadsTests.ALL.get("a_netherite_chunk_loader_takes_over_only_the_chunks_of_its_cross").accept(h);
    }

    @GameTest
    public void elytraPadAndFlypadAreasMatchTheirTiers(GameTestHelper h) {
        PadsTests.ALL.get("elytra_pad_and_flypad_areas_match_their_tiers").accept(h);
    }

    @GameTest
    public void oldFlypadsTurnIntoTheirNewTierInTheWorldAndInTheInventory(GameTestHelper h) {
        PadsTests.ALL.get("old_flypads_turn_into_their_new_tier_in_the_world_and_in_the_inventory").accept(h);
    }

    @GameTest
    public void everyFamilyNamesItsLastTier(GameTestHelper h) {
        PadsTests.ALL.get("every_family_names_its_last_tier").accept(h);
    }

    @GameTest
    public void theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne(GameTestHelper h) {
        PadsTests.ALL.get("the_last_tier_smiths_back_into_dont_do_it_that_works_like_tier_one").accept(h);
    }

    @GameTest
    public void easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost(GameTestHelper h) {
        PadsTests.ALL.get("easter_chain_names_every_stage_and_costs_what_the_normal_tiers_cost").accept(h);
    }

    @GameTest
    public void theFinalEasterPadIsTwiceAsStrongAsTheLastTier(GameTestHelper h) {
        PadsTests.ALL.get("the_final_easter_pad_is_twice_as_strong_as_the_last_tier").accept(h);
    }

    @GameTest
    public void theEasterAdvancementsAreHiddenAndFireAlongTheChain(GameTestHelper h) {
        PadsTests.ALL.get("the_easter_advancements_are_hidden_and_fire_along_the_chain").accept(h);
    }

    @GameTest
    public void theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand(GameTestHelper h) {
        PadsTests.ALL.get("the_funny_stick_is_smithed_from_the_final_pad_and_sparkles_in_the_hand").accept(h);
    }

    @GameTest
    public void padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier(GameTestHelper h) {
        PadsTests.ALL.get("pad_tiers_grow_and_enderite_sits_between_netherite_and_the_nether_star_tier").accept(h);
    }

    @GameTest
    public void theStellarFlypadIsSmithedFromTwoReinforcedFlypads(GameTestHelper h) {
        PadsTests.ALL.get("the_stellar_flypad_is_smithed_from_two_reinforced_flypads").accept(h);
    }

    @GameTest
    public void elytraPadsEquipAnUnsafeSpawnElytraInTheirArea(GameTestHelper h) {
        PadsTests.ALL.get("elytra_pads_equip_an_unsafe_spawn_elytra_in_their_area").accept(h);
    }

    @GameTest
    public void elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea(GameTestHelper h) {
        PadsTests.ALL.get("elytra_pads_recharge_boosts_in_the_column_and_from_enderite_on_in_the_whole_area").accept(h);
    }

    @GameTest
    public void theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding(GameTestHelper h) {
        PadsTests.ALL.get("the_spawn_elytra_boost_spends_one_charge_and_only_while_gliding").accept(h);
    }

    @GameTest
    public void safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage(GameTestHelper h) {
        PadsTests.ALL.get("safe_spawn_elytras_and_enderite_launches_prevent_fall_damage").accept(h);
    }

    @GameTest
    public void padElytrasExpireEvenWithTheSpawnElytraSwitchedOff(GameTestHelper h) {
        PadsTests.ALL.get("pad_elytras_expire_even_with_the_spawn_elytra_switched_off").accept(h);
    }

    @GameTest
    public void flypadsGrantFlightInsideAndTakeItBackOutside(GameTestHelper h) {
        PadsTests.ALL.get("flypads_grant_flight_inside_and_take_it_back_outside").accept(h);
    }

    @GameTest
    public void enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling(GameTestHelper h) {
        PadsTests.ALL.get("enderite_flypads_catch_flyers_leaving_their_area_with_slow_falling").accept(h);
    }

    @GameTest
    public void switchedOffFlypadsGrantNoFlight(GameTestHelper h) {
        PadsTests.ALL.get("switched_off_flypads_grant_no_flight").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadTweaksTests.TELEPORTER_MAX_TICKS)
    public void spawnTeleportersSendStillPlayersToTheirSpawnPoint(GameTestHelper h) {
        PadsTests.ALL.get("spawn_teleporters_send_still_players_to_their_spawn_point").accept(h);
    }

    @GameTest
    public void spawnTeleporterTargetsFallBackToTheWorldSpawn(GameTestHelper h) {
        PadsTests.ALL.get("spawn_teleporter_targets_fall_back_to_the_world_spawn").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadTweaksTests.COPPER_MAX_TICKS)
    public void copperPlatesWaitLongerTheMoreTheyOxidized(GameTestHelper h) {
        PadsTests.ALL.get("copper_plates_wait_longer_the_more_they_oxidized").accept(h);
    }

    @GameTest
    public void copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack(GameTestHelper h) {
        PadsTests.ALL.get("copper_plates_oxidize_in_order_and_the_axe_scrapes_them_back").accept(h);
    }

    @GameTest(maxTicks = com.simplepads.test.PadTweaksTests.COPPER_MAX_TICKS)
    public void diamondPressurePlatesReactToPlayersOnly(GameTestHelper h) {
        PadsTests.ALL.get("diamond_pressure_plates_react_to_players_only").accept(h);
    }

    @GameTest
    public void netheritePlatesAdmitOnlyHoldersOfBarrelItems(GameTestHelper h) {
        PadsTests.ALL.get("netherite_plates_admit_only_holders_of_barrel_items").accept(h);
    }

    @GameTest
    public void enderitePlatesLockToTheirOwnerAndNamedTags(GameTestHelper h) {
        PadsTests.ALL.get("enderite_plates_lock_to_their_owner_and_named_tags").accept(h);
    }

    @GameTest
    public void ownersBreakTheirPadsFastAndStrangersSlowly(GameTestHelper h) {
        PadsTests.ALL.get("owners_break_their_pads_fast_and_strangers_slowly").accept(h);
    }

    @GameTest
    public void placingPadsMakesThePlacerTheOwner(GameTestHelper h) {
        PadsTests.ALL.get("placing_pads_makes_the_placer_the_owner").accept(h);
    }

    @GameTest
    public void chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn(GameTestHelper h) {
        PadsTests.ALL.get("chunk_loaders_force_their_chunks_and_release_only_their_own").accept(h);
    }

    @GameTest
    public void theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers(GameTestHelper h) {
        PadsTests.ALL.get("the_first_join_gift_comes_once_and_honours_simple_tweaks_players").accept(h);
    }

    @GameTest
    public void spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot(GameTestHelper h) {
        PadsTests.ALL.get("spawn_elytras_vanish_as_soon_as_they_leave_the_chest_slot").accept(h);
    }

    @GameTest
    public void theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt(GameTestHelper h) {
        PadsTests.ALL.get("the_spawn_area_lies_only_in_the_spawn_dimension_and_fall_protection_covers_all_of_it").accept(h);
    }

    @GameTest
    public void chunkLoadersReleaseOnSetblockAndHandOverSharedChunks(GameTestHelper h) {
        PadsTests.ALL.get("chunk_loaders_release_on_setblock_and_hand_over_shared_chunks").accept(h);
    }

    @GameTest
    public void overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight(GameTestHelper h) {
        PadsTests.ALL.get("overlapping_flypads_keep_the_player_flying_and_take_only_their_own_flight").accept(h);
    }

    @GameTest
    public void flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges(GameTestHelper h) {
        PadsTests.ALL.get("flight_time_and_boosts_are_capped_and_broken_launchpads_drop_their_charges").accept(h);
    }

    @GameTest
    public void aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks(GameTestHelper h) {
        PadsTests.ALL.get("a_chunk_loader_replaced_by_another_loader_type_releases_its_chunks").accept(h);
    }

    @GameTest
    public void creativePlayersLoseTheStaleFlypadFlightTag(GameTestHelper h) {
        PadsTests.ALL.get("creative_players_lose_the_stale_flypad_flight_tag").accept(h);
    }

    @GameTest
    public void staleFlypadFlightIsSweptAway(GameTestHelper h) {
        PadsTests.ALL.get("stale_flypad_flight_is_swept_away").accept(h);
    }

    @GameTest
    public void padsPlacedInWaterAreWaterloggedAndLeaveTheWaterBehind(GameTestHelper h) {
        PadsTests.ALL.get("pads_placed_in_water_are_waterlogged_and_leave_the_water_behind").accept(h);
    }
}
