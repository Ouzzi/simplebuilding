package com.simplepads.test;

import com.simplepads.PadsConfig;
import com.simplepads.SimplePads;
import com.simplepads.block.PadsBlocks;
import com.simplepads.item.PadsItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Module tests of Simple Pads (name -> body; the Fabric adapter {@code ModuleGameTest} calls them, ids
 * {@code simplepads:module_game_test_<name>}): the module's own checks (registration, moved ids, config takeover, sub-mod switch,
 * Enderite needs Simple Ores, discover, guide) and the pad tests that moved with the code from SimpleBuilding (b-pads01).
 */
public final class PadsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("registered", PadsTests::registered);
        ALL.put("old_pad_ids_move", PadsTests::oldPadIdsMove);
        ALL.put("config_takeover", PadsTests::configTakeover);
        ALL.put("sub_mod_switch", PadsTests::subModSwitch);
        ALL.put("enderite_tiers_need_simple_ores", PadsTests::enderiteTiersNeedSimpleOres);
        ALL.put("discover_recipe_unlocks", com.simplepads.discover.PadsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplepads.discover.PadsDiscover::behaviourHints);
        ALL.put("guide_book", com.simplepads.guide.PadsGuide::gameTest);
        ALL.put("spawn_teleporter_tiers_wait_fifty_twenty_and_five_seconds", PadOverhaulTests::spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds);
        ALL.put("old_spawn_teleporters_become_their_new_tier_in_the_world_and_the_inventory", PadOverhaulTests::oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory);
        ALL.put("old_elytra_pads_become_their_new_tier_in_the_world_and_the_inventory", PadOverhaulTests::oldElytraPadsBecomeTheirNewTierInTheWorldAndTheInventory);
        ALL.put("tier_one_of_every_pad_family_is_smithed_from_its_plate_and_unlock_item", PadOverhaulTests::tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem);
        ALL.put("redstone_switches_launchpads_and_flypads_off_and_comparators_read_them", PadOverhaulTests::redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem);
        ALL.put("hoppers_fill_only_wind_charges_into_the_launchpad", PadOverhaulTests::hoppersFillOnlyWindChargesIntoTheLaunchpad);
        ALL.put("spawn_teleporter_signal_changes_reset_the_entire_warmup", PadOverhaulTests::spawnTeleporterSignalChangesResetTheEntireWarmup);
        ALL.put("the_spawn_teleporter_takes_players_to_their_bed_and_with_redstone_to_the_world_spawn", PadOverhaulTests::theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn);
        ALL.put("every_pad_family_shows_whether_it_is_working", PadOverhaulTests::everyPadFamilyShowsWhetherItIsWorking);
        ALL.put("pad_effects_fade_in_and_stop_scheduling_when_settled", PadOverhaulTests::padEffectsFadeInAndStopSchedulingWhenSettled);
        ALL.put("splash_potions_landing_on_the_pad_are_stored_and_replaced", PotionPadTests::splashPotionsLandingOnThePadAreStoredAndReplaced);
        ALL.put("standing_on_the_pad_ramps_the_effect_to_twenty_five_fifty_and_one_hundred_percent_in_three_seconds", PotionPadTests::standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds);
        ALL.put("instant_effects_apply_once_at_the_three_second_mark", PotionPadTests::instantEffectsApplyOnceAtTheThreeSecondMark);
        ALL.put("a_full_application_puts_the_pad_on_cooldown_for_twice_the_effect_duration", PotionPadTests::aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration);
        ALL.put("a_pad_broken_during_cooldown_keeps_the_remaining_time_and_resumes_when_placed_again", PotionPadTests::aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain);
        ALL.put("every_pad_item_stacks_to_one", PotionPadTests::everyPadItemStacksToOne);
        ALL.put("potion_pad_recipes_cover_all_three_tiers", PotionPadTests::potionPadRecipesCoverAllThreeTiers);
        ALL.put("charged_creeper_explosions_drop_one_blaze_head_each", PotionPadTests::chargedCreeperExplosionsDropOneBlazeHeadEach);
        ALL.put("blazes_killed_otherwise_drop_no_head", PotionPadTests::blazesKilledOtherwiseDropNoHead);
        ALL.put("potion_pads_are_switched_off_by_redstone_and_report_their_state_to_a_comparator", PotionPadTests::potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToAComparator);
        ALL.put("a_dispenser_fills_the_potion_pad_but_a_hopper_cannot", PotionPadTests::aDispenserFillsThePotionPadButAHopperCannot);
        ALL.put("harmful_effects_reach_only_the_owner_and_strangers_cannot_charge_the_pad", PotionPadRuleTests::harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad);
        ALL.put("blocked_effects_are_never_given_and_start_no_cooldown", PotionPadRuleTests::blockedEffectsAreNeverGivenAndStartNoCooldown);
        ALL.put("levels_and_durations_never_exceed_vanilla_or_the_potion", PotionPadRuleTests::levelsAndDurationsNeverExceedVanillaOrThePotion);
        ALL.put("the_cooldown_follows_the_granted_duration_and_the_effect_multiplier", PotionPadRuleTests::theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier);
        ALL.put("healing_locks_the_player_out_of_every_pad_for_one_minute", PotionPadRuleTests::healingLocksThePlayerOutOfEveryPadForOneMinute);
        ALL.put("mobs_never_receive_any_pad_effect", PotionPadRuleTests::mobsNeverReceiveAnyPadEffect);
        ALL.put("every_vanilla_effect_has_deliberate_rules", PotionPadRuleTests::everyVanillaEffectHasDeliberateRules);
        ALL.put("honeycomb_waxes_every_stage_and_waxed_plates_stop_oxidizing", PressurePlateTests::honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing);
        ALL.put("the_axe_scrapes_the_wax_off_before_the_oxidation", PressurePlateTests::theAxeScrapesTheWaxOffBeforeTheOxidation);
        ALL.put("waxed_copper_plates_are_crafted_from_the_plate_and_one_honeycomb", PressurePlateTests::waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb);
        ALL.put("copper_plates_release_as_late_as_they_press", PressurePlateTests::copperPlatesReleaseAsLateAsTheyPress);
        ALL.put("every_mod_pressure_plate_visibly_sinks_when_pressed", PressurePlateTests::everyModPressurePlateVisiblySinksWhenPressed);
        ALL.put("every_mod_pressure_plate_is_named_like_the_vanilla_ones", PressurePlateTests::everyModPressurePlateIsNamedLikeTheVanillaOnes);
        ALL.put("launchpad_tiers_hold_four_eight_and_sixteen_wind_charges", PadTierTests::launchpadTiersHoldFourEightAndSixteenWindCharges);
        ALL.put("sneaking_with_wind_charges_loads_the_whole_hand_up_to_capacity", PadTierTests::sneakingWithWindChargesLoadsTheWholeHandUpToCapacity);
        ALL.put("launch_strength_per_charge_is_doubled_so_full_tiers_launch_like_twice_the_old_charges", PadTierTests::launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges);
        ALL.put("old_launchpads_drop_the_charges_their_tier_no_longer_holds", PadTierTests::oldLaunchpadsDropTheChargesTheirTierNoLongerHolds);
        ALL.put("chunk_loader_tiers_force_one_five_and_nine_chunks", PadTierTests::chunkLoaderTiersForceOneFiveAndNineChunks);
        ALL.put("a_netherite_chunk_loader_takes_over_only_the_chunks_of_its_cross", PadTierTests::aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross);
        ALL.put("elytra_pad_and_flypad_areas_match_their_tiers", PadTierTests::elytraPadAndFlypadAreasMatchTheirTiers);
        ALL.put("old_flypads_turn_into_their_new_tier_in_the_world_and_in_the_inventory", PadTierTests::oldFlypadsTurnIntoTheirNewTierInTheWorldAndInTheInventory);
        ALL.put("every_family_names_its_last_tier", PadTierTests::everyFamilyNamesItsLastTier);
        ALL.put("the_last_tier_smiths_back_into_dont_do_it_that_works_like_tier_one", PadEasterTests::theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne);
        ALL.put("easter_chain_names_every_stage_and_costs_what_the_normal_tiers_cost", PadEasterTests::easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost);
        ALL.put("the_final_easter_pad_is_twice_as_strong_as_the_last_tier", PadEasterTests::theFinalEasterPadIsTwiceAsStrongAsTheLastTier);
        ALL.put("the_easter_advancements_are_hidden_and_fire_along_the_chain", PadEasterTests::theEasterAdvancementsAreHiddenAndFireAlongTheChain);
        ALL.put("the_funny_stick_is_smithed_from_the_final_pad_and_sparkles_in_the_hand", PadEasterTests::theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand);
        ALL.put("pad_tiers_grow_and_enderite_sits_between_netherite_and_the_nether_star_tier", PadTweaksTests::padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier);
        ALL.put("the_stellar_flypad_is_smithed_from_two_reinforced_flypads", PadTweaksTests::theStellarFlypadIsSmithedFromTwoReinforcedFlypads);
        ALL.put("elytra_pads_equip_an_unsafe_spawn_elytra_in_their_area", PadTweaksTests::elytraPadsEquipAnUnsafeSpawnElytraInTheirArea);
        ALL.put("elytra_pads_recharge_boosts_in_the_column_and_from_enderite_on_in_the_whole_area", PadTweaksTests::elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea);
        ALL.put("the_spawn_elytra_boost_spends_one_charge_and_only_while_gliding", PadTweaksTests::theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding);
        ALL.put("safe_spawn_elytras_and_enderite_launches_prevent_fall_damage", PadTweaksTests::safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage);
        ALL.put("pad_elytras_expire_even_with_the_spawn_elytra_switched_off", PadTweaksTests::padElytrasExpireEvenWithTheSpawnElytraSwitchedOff);
        ALL.put("flypads_grant_flight_inside_and_take_it_back_outside", PadTweaksTests::flypadsGrantFlightInsideAndTakeItBackOutside);
        ALL.put("enderite_flypads_catch_flyers_leaving_their_area_with_slow_falling", PadTweaksTests::enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling);
        ALL.put("switched_off_flypads_grant_no_flight", PadTweaksTests::switchedOffFlypadsGrantNoFlight);
        ALL.put("spawn_teleporters_send_still_players_to_their_spawn_point", PadTweaksTests::spawnTeleportersSendStillPlayersToTheirSpawnPoint);
        ALL.put("spawn_teleporter_targets_fall_back_to_the_world_spawn", PadTweaksTests::spawnTeleporterTargetsFallBackToTheWorldSpawn);
        ALL.put("copper_plates_wait_longer_the_more_they_oxidized", PadTweaksTests::copperPlatesWaitLongerTheMoreTheyOxidized);
        ALL.put("copper_plates_oxidize_in_order_and_the_axe_scrapes_them_back", PadTweaksTests::copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack);
        ALL.put("diamond_pressure_plates_react_to_players_only", PadTweaksTests::diamondPressurePlatesReactToPlayersOnly);
        ALL.put("netherite_plates_admit_only_holders_of_barrel_items", PadTweaksTests::netheritePlatesAdmitOnlyHoldersOfBarrelItems);
        ALL.put("enderite_plates_lock_to_their_owner_and_named_tags", PadTweaksTests::enderitePlatesLockToTheirOwnerAndNamedTags);
        ALL.put("owners_break_their_pads_fast_and_strangers_slowly", PadTweaksTests::ownersBreakTheirPadsFastAndStrangersSlowly);
        ALL.put("placing_pads_makes_the_placer_the_owner", PadTweaksTests::placingPadsMakesThePlacerTheOwner);
        ALL.put("chunk_loaders_force_their_chunks_and_release_only_their_own", PadTweaksTests::chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn);
        ALL.put("the_first_join_gift_comes_once_and_honours_simple_tweaks_players", PadTweaksTests::theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers);
        ALL.put("spawn_elytras_vanish_as_soon_as_they_leave_the_chest_slot", PadTweaksTests::spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot);
        ALL.put("the_spawn_area_lies_only_in_the_spawn_dimension_and_fall_protection_covers_all_of_it", PadTweaksTests::theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt);
        ALL.put("chunk_loaders_release_on_setblock_and_hand_over_shared_chunks", PadTweaksTests::chunkLoadersReleaseOnSetblockAndHandOverSharedChunks);
        ALL.put("overlapping_flypads_keep_the_player_flying_and_take_only_their_own_flight", PadTweaksTests::overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight);
        ALL.put("flight_time_and_boosts_are_capped_and_broken_launchpads_drop_their_charges", PadTweaksTests::flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges);
        ALL.put("a_chunk_loader_replaced_by_another_loader_type_releases_its_chunks", PadTweaksTests::aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks);
        ALL.put("creative_players_lose_the_stale_flypad_flight_tag", PadTweaksTests::creativePlayersLoseTheStaleFlypadFlightTag);
        ALL.put("stale_flypad_flight_is_swept_away", PadTweaksTests::staleFlypadFlightIsSweptAway);
        ALL.put("pads_placed_in_water_are_waterlogged_and_leave_the_water_behind", PadTweaksTests::padsPlacedInWaterAreWaterloggedAndLeaveTheWaterBehind);
    }

    private PadsTests() {
    }

    /** Every pad and plate, the Spawn Elytra and the Funny Stick are registered under simplepads:, the tab exists. */
    public static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Block block : PadsBlocks.all()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!SimplePads.MOD_ID.equals(id.getNamespace())) problems.add("block " + id);
            if (block.asItem() == Items.AIR) problems.add("no item for " + id);
        }
        if (PadsBlocks.all().size() != 35) problems.add("35 blocks expected, found " + PadsBlocks.all().size());
        for (String path : List.of("spawn_elytra", "funny_stick", "guide_book")) {
            if (!BuiltInRegistries.ITEM.containsKey(SimplePads.id(path))) problems.add("item " + path);
        }
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimplePads.id("pads"))) problems.add("tab simplepads:pads");
        for (String path : List.of("launchpad_be", "elytra_pad_be", "flypad_be", "potion_pad_be", "spawn_teleporter_be", "chunk_loader_be",
                "copper_pressure_plate_be", "netherite_pressure_plate_be")) {
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimplePads.id(path))) problems.add("block entity type " + path);
        }
        if (PadsItems.tabStacks().size() < 30) problems.add("tab has only " + PadsItems.tabStacks().size() + " stacks");
        h.assertTrue(problems.isEmpty(), "registration: " + problems);
        h.succeed();
    }

    /** Old worlds: every simplebuilding: id of the pads resolves to the module's object (blocks, items, block entities, components). */
    public static void oldPadIdsMove(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Block block : PadsBlocks.all()) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            Identifier old = Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, path);
            if (BuiltInRegistries.BLOCK.getValue(old) != block) problems.add("block " + path);
            if (BuiltInRegistries.ITEM.getValue(old) != block.asItem()) problems.add("item " + path);
        }
        if (BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, "spawn_elytra")) != PadsItems.SPAWN_ELYTRA) {
            problems.add("item spawn_elytra");
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, "launchpad_be"))
                != com.simplepads.block.entity.PadsBlockEntities.LAUNCHPAD) problems.add("block entity launchpad_be");
        if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, "flight_time"))
                != com.simplepads.component.PadsComponents.FLIGHT_TIME) problems.add("component flight_time");
        Identifier recipe = com.simplelib.api.IdMoves.target(Registries.RECIPE, Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, "launchpad_smithing"));
        if (!SimplePads.id("launchpad_smithing").equals(recipe)) problems.add("recipe launchpad_smithing -> " + recipe);
        Identifier adv = com.simplelib.api.IdMoves.target(Registries.ADVANCEMENT, Identifier.fromNamespaceAndPath(SimplePads.SB_NAMESPACE, "pads/liftoff"));
        if (!SimplePads.id("pads/liftoff").equals(adv)) problems.add("advancement pads/liftoff -> " + adv);
        h.assertTrue(problems.isEmpty(), "old ids: " + problems);
        h.succeed();
    }

    /** A first start takes the pad options of an existing simplebuilding.json over (names and the moved server values). */
    public static void configTakeover(GameTestHelper h) {
        String old = "{\"tweaks\":{\"pads\":{\"enableFlypads\":false},\"padTuning\":{\"potionPadChargeStepTicks\":40},"
                + "\"spawn\":{\"maxBoosts\":7,\"forceExactSpawn\":true}},\"server\":{\"pads\":{\"strangerPadBreakSeconds\":30},"
                + "\"dimensionLocks\":{\"flypadBlockedDimensions\":\"the_end\"},\"chunkLoaders\":{\"requireOwnerOnline\":false}}}";
        PadsConfig taken = new com.google.gson.Gson().fromJson(PadsConfig.fromLegacyForTests(old), PadsConfig.class).validate();
        h.assertFalse(taken.pads.enableFlypads, "pads.enableFlypads taken over");
        h.assertValueEqual(taken.padTuning.potionPadChargeStepTicks, 40, "padTuning taken over");
        h.assertValueEqual(taken.spawn.maxBoosts, 7, "spawn.maxBoosts taken over");
        h.assertValueEqual(taken.tuning.strangerPadBreakSeconds, 30, "server.pads -> tuning");
        h.assertTrue(taken.tuning.flypadBlockedIn(Identifier.withDefaultNamespace("the_end")), "flypad lock taken over");
        h.assertFalse(taken.tuning.chunkLoaderRequireOwnerOnline, "chunk loader owner rule taken over");
        h.succeed();
    }

    /** "Enable Simple Pads" off: the tab is empty. */
    public static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimplePads.MOD_ID, false);
        try {
            h.assertFalse(SimplePads.on(), "switched off");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimplePads.MOD_ID, true);
        }
        h.assertTrue(SimplePads.on(), "switched on again");
        h.succeed();
    }

    /** The Enderite tiers are smithed with Simple Ores' Enderite Upgrade (the owner: without Simple Ores no Enderite). */
    public static void enderiteTiersNeedSimpleOres(GameTestHelper h) {
        var recipes = h.getLevel().getServer().getRecipeManager();
        List<String> problems = new ArrayList<>();
        for (String path : List.of("enderite_launchpad_smithing", "enderite_elytra_pad_smithing", "enderite_chunk_loader_smithing",
                "enderite_spawn_teleporter_smithing", "enderite_pressure_plate_smithing")) {
            var holder = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimplePads.id(path)));
            if (holder.isEmpty()) {
                problems.add(path + " missing");
                continue;
            }
            if (!(holder.get().value() instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe smithing)
                    || smithing.templateIngredient().isEmpty()
                    || !smithing.templateIngredient().get().test(new net.minecraft.world.item.ItemStack(com.simplepads.util.PadRefs.enderiteTemplate()))) {
                problems.add(path + " does not take Simple Ores' Enderite Upgrade");
            }
        }
        h.assertTrue(com.simplepads.util.PadRefs.enderiteTemplate() != Items.AIR, "Simple Ores' Enderite Upgrade is there (required mod)");
        h.assertTrue(problems.isEmpty(), "Enderite tiers: " + problems);
        h.succeed();
    }
}
