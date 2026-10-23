package com.simplebuilding.modules.simplestorage;

import com.simplestorage.test.StorageTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link StorageTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplestorage:empty")
    public void registered(GameTestHelper h) {
        StorageTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        StorageTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void recipes(GameTestHelper h) {
        StorageTests.ALL.get("recipes").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void cratesBurnAndFuelLikePlanks(GameTestHelper h) {
        StorageTests.ALL.get("crates_burn_and_fuel_like_planks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void subModSwitch(GameTestHelper h) {
        StorageTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void guideBook(GameTestHelper h) {
        StorageTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        StorageTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        StorageTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverFirstPickupTips(GameTestHelper h) {
        StorageTests.ALL.get("discover_first_pickup_tips").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesStoreFoodForHoppersAndComparators(GameTestHelper h) {
        StorageTests.ALL.get("crates_store_food_for_hoppers_and_comparators").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesTakeAtMostNineKinds(GameTestHelper h) {
        StorageTests.ALL.get("crates_take_at_most_nine_kinds").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesLayerTheNewestKindOnTop(GameTestHelper h) {
        StorageTests.ALL.get("crates_layer_the_newest_kind_on_top").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesTurnLikeBarrelsAndKeepTheirFood(GameTestHelper h) {
        StorageTests.ALL.get("crates_turn_like_barrels_and_keep_their_food").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableKeepsItsGrid(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_keeps_its_grid").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableSharesItsGrid(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_shares_its_grid").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableDropsItsGridAndIgnoresHoppers(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_drops_its_grid_and_ignores_hoppers").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestSingleChestClimbsFromCopperToEnderiteKeepingItsContents(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_single_chest_climbs_from_copper_to_enderite_keeping_its_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestDoubleChestUpgradesBothHalvesTogether(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_double_chest_upgrades_both_halves_together").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestSlotCountsAndStackLimitsFollowTheTier(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_slot_counts_and_stack_limits_follow_the_tier").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestDoubleChestsFormOnlyFromEqualTiers(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_double_chests_form_only_from_equal_tiers").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = com.simplestorage.test.ChestTests.HOPPER_MAX_TICKS)
    public void tieredChestVanillaHoppersFillAndEmptyOversizedSlots(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_vanilla_hoppers_fill_and_empty_oversized_slots").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestComparatorReadsOversizedSlotsAgainstTheTierLimit(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_comparator_reads_oversized_slots_against_the_tier_limit").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestOversizedStacksSurviveSavingAndLoading(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_oversized_stacks_survive_saving_and_loading").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestChestItemsFollowTheFamilyScheme(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_chest_items_follow_the_family_scheme").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedCapacityComparatorAndPropertiesMatchNormalTiers(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_capacity_comparator_and_properties_match_normal_tiers").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedDoubleChestsRequireTheSameKind(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_double_chests_require_the_same_kind").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedRecipesUseTheMatchingChestAndHook(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_recipes_use_the_matching_chest_and_hook").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedSignalCountsViewersAndUpdatesNeighbors(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_signal_counts_viewers_and_updates_neighbors").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedUpgradesKeepBothHalvesAndContents(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_upgrades_keep_both_halves_and_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedTitlesMatchNormalTiers(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_titles_match_normal_tiers").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedCopperChestSignalsItsViewers(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_copper_chest_signals_its_viewers").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedCopperChestWaxesScrapesAndAgesWithItsContents(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_copper_chest_waxes_scrapes_and_ages_with_its_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void tieredChestTrappedCopperChestsPairOnlyWithEachOther(GameTestHelper h) {
        StorageTests.ALL.get("tiered_chest_trapped_copper_chests_pair_only_with_each_other").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxVanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_vanilla_box_climbs_to_enderite_in_ten_blows_keeping_contents_and_color").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxContentsAndOversizedStacksSurviveBreakingPlacingAndBurning(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_contents_and_oversized_stacks_survive_breaking_placing_and_burning").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxOversizedStacksAndColorSurviveSavingAndLoading(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_oversized_stacks_and_color_survive_saving_and_loading").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = com.simplestorage.test.ShulkerBoxTests.RIG_MAX_TICKS)
    public void shulkerBoxHoppersAndComparatorsFollowTheTierLimit(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_hoppers_and_comparators_follow_the_tier_limit").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = com.simplestorage.test.ShulkerBoxTests.RIG_MAX_TICKS)
    public void shulkerBoxDispensersPlaceTheBoxes(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_dispensers_place_the_boxes").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxBoxesDoNotNest(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_boxes_do_not_nest").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxBoxesAreDyedCraftedAndWashed(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_boxes_are_dyed_crafted_and_washed").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void shulkerBoxBoxItemsFollowTheFamilyScheme(GameTestHelper h) {
        StorageTests.ALL.get("shulker_box_box_items_follow_the_family_scheme").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void vehicleChestVehiclesHoldTheTierSlotsAndKeepOversizedStacks(GameTestHelper h) {
        StorageTests.ALL.get("vehicle_chest_vehicles_hold_the_tier_slots_and_keep_oversized_stacks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void vehicleBrokenChestVehiclesDropTheirTierItemAndContents(GameTestHelper h) {
        StorageTests.ALL.get("vehicle_broken_chest_vehicles_drop_their_tier_item_and_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void vehicleChestVehicleRecipesCraftAndUpgrade(GameTestHelper h) {
        StorageTests.ALL.get("vehicle_chest_vehicle_recipes_craft_and_upgrade").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = StorageTests.VEHICLE_DECK_MAX_TICKS)
    public void vehicleStandersStayOnTheChestBoatDeck(GameTestHelper h) {
        StorageTests.ALL.get("vehicle_standers_stay_on_the_chest_boat_deck").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void vehicleOldVehiclesLoadAsSimpleStorages(GameTestHelper h) {
        StorageTests.ALL.get("vehicle_old_vehicles_load_as_simple_storages").accept(h);
    }
    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleInsertionStopsAtTheBrimAndWeighsByStackSize(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_insertion_stops_at_the_brim_and_weighs_by_stack_size").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleInsertionTurnsAwayWhatCannotGoIntoContainerItems(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_insertion_turns_away_what_cannot_go_into_container_items").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleInsertionMergesEqualStacksAndPushesThemToTheTop(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_insertion_merges_equal_stacks_and_pushes_them_to_the_top").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleDrawerHoldsOnlyTheKindAlreadyInside(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_drawer_holds_only_the_kind_already_inside").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleNestedBundlesWeighTheirContents(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_nested_bundles_weigh_their_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleTheSelectedEntryIsTheOneThatComesOut(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_the_selected_entry_is_the_one_that_comes_out").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleRightClickThrowsTheSelectedStackButNeverBlocks(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_right_click_throws_the_selected_stack_but_never_blocks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleMasterBuilderPlacesFromTheBundleAndColorPaletteScattersIt(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_master_builder_places_from_the_bundle_and_color_palette_scatters_it").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleCapacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_capacity_follows_tier_and_enchantments_and_matches_the_wiki_export").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleBarAndTooltipReadTheSameCapacityTheFillingUses(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_bar_and_tooltip_read_the_same_capacity_the_filling_uses").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void reinforcedBundleOverflowingBundleWeightsAreRefusedInsteadOfCrashing(GameTestHelper h) {
        StorageTests.ALL.get("reinforced_bundle_overflowing_bundle_weights_are_refused_instead_of_crashing").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverRightClicksDoNothingEvenWithMasterBuilder(GameTestHelper h) {
        StorageTests.ALL.get("quiver_right_clicks_do_nothing_even_with_master_builder").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverArrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt(GameTestHelper h) {
        StorageTests.ALL.get("quiver_arrow_filter_holds_for_clicks_and_the_inverted_binding_slips_past_it").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverCapacityDropsTheBundleBonusAndFollowsTierAndEnchantments(GameTestHelper h) {
        StorageTests.ALL.get("quiver_capacity_drops_the_bundle_bonus_and_follows_tier_and_enchantments").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverBarWidthFollowsTheSameCapacityTheFillingUses(GameTestHelper h) {
        StorageTests.ALL.get("quiver_bar_width_follows_the_same_capacity_the_filling_uses").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverBowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack(GameTestHelper h) {
        StorageTests.ALL.get("quiver_bow_takes_the_topmost_arrow_and_searches_offhand_chest_hotbar_then_backpack").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverBowConsumesOneArrowFromTheQuiverThatSuppliedIt(GameTestHelper h) {
        StorageTests.ALL.get("quiver_bow_consumes_one_arrow_from_the_quiver_that_supplied_it").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverBowShootsFromTheQuiverAndBillsItOutsideCreativeOnly(GameTestHelper h) {
        StorageTests.ALL.get("quiver_bow_shoots_from_the_quiver_and_bills_it_outside_creative_only").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverNetheriteQuiverSurvivesAnExplosionLikeTheNetheriteBundle(GameTestHelper h) {
        StorageTests.ALL.get("quiver_netherite_quiver_survives_an_explosion_like_the_netherite_bundle").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverCrossbowLoadsFromTheQuiverAndBillsOneArrow(GameTestHelper h) {
        StorageTests.ALL.get("quiver_crossbow_loads_from_the_quiver_and_bills_one_arrow").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverInsideWornContainersFeedsTheBow(GameTestHelper h) {
        StorageTests.ALL.get("quiver_inside_worn_containers_feeds_the_bow").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void quiverPickedUpArrowsGoIntoTheQuiverOnlyWithFunnel(GameTestHelper h) {
        StorageTests.ALL.get("quiver_picked_up_arrows_go_into_the_quiver_only_with_funnel").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageEnchantmentFunnelFilterDecidesWhatTheTouchSweepsUp(GameTestHelper h) {
        StorageTests.ALL.get("storage_enchantment_funnel_filter_decides_what_the_touch_sweeps_up").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageEnchantmentDrawerKindLockHoldsAgainstTheFunnelToo(GameTestHelper h) {
        StorageTests.ALL.get("storage_enchantment_drawer_kind_lock_holds_against_the_funnel_too").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageEnchantmentFunnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim(GameTestHelper h) {
        StorageTests.ALL.get("storage_enchantment_funnel_quiver_sweeps_arrows_only_and_stops_at_its_brim").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageEnchantmentDrawerAndDeepPocketsMultiplyOnTheSameContainer(GameTestHelper h) {
        StorageTests.ALL.get("storage_enchantment_drawer_and_deep_pockets_multiply_on_the_same_container").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedBundleSneakUsePlacesBundlesOnlyOnTopFaces(GameTestHelper h) {
        StorageTests.ALL.get("placed_bundle_sneak_use_places_bundles_only_on_top_faces").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = StorageTests.PLACED_BUNDLE_SCROLL_MAX_TICKS)
    public void placedBundleSneakScrollPacketsCycleTheTopItemAndRightClickTakesIt(GameTestHelper h) {
        StorageTests.ALL.get("placed_bundle_sneak_scroll_packets_cycle_the_top_item_and_right_click_takes_it").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedBundleSneakRightClickWithAnItemDepositsIntoThePlacedBundle(GameTestHelper h) {
        StorageTests.ALL.get("placed_bundle_sneak_right_click_with_an_item_deposits_into_the_placed_bundle").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedBundlePlacedBundlesDropThemselvesWithTheirContents(GameTestHelper h) {
        StorageTests.ALL.get("placed_bundle_placed_bundles_drop_themselves_with_their_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedBundleDepositIntoPlacedModBundlePlaysTheInsertSound(GameTestHelper h) {
        StorageTests.ALL.get("placed_bundle_deposit_into_placed_mod_bundle_plays_the_insert_sound").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringFunnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_funnel_bundle_sweeps_up_drops_on_touch_unless_the_player_sneaks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringFunnelHonoursPickupDelayAndTargetInTheHand(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_funnel_honours_pickup_delay_and_target_in_the_hand").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringNetheriteBundleOnTheGroundSurvivesFireAndExplosions(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_netherite_bundle_on_the_ground_survives_fire_and_explosions").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringSelectionPacketReachesEntriesPastTheTwelfth(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_selection_packet_reaches_entries_past_the_twelfth").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringFunnelPickupCountsWhatItTook(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_funnel_pickup_counts_what_it_took").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleWiringBundlesCloseLikeVanillaWhenPickedUpOrLeft(GameTestHelper h) {
        StorageTests.ALL.get("bundle_wiring_bundles_close_like_vanilla_when_picked_up_or_left").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleStorageRegistered(GameTestHelper h) {
        StorageTests.ALL.get("bundle_storage_registered").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleStorageMigrationFromSimplebuilding(GameTestHelper h) {
        StorageTests.ALL.get("bundle_storage_migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void bundleStorageRecipesWorkOnTheirOwn(GameTestHelper h) {
        StorageTests.ALL.get("bundle_storage_recipes_work_on_their_own").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackRightClickWearsTheBackpackAndSwapsItWithTheChestplate(GameTestHelper h) {
        StorageTests.ALL.get("backpack_right_click_wears_the_backpack_and_swaps_it_with_the_chestplate").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackTiersCarryTheirArmorSlotCountAndSlotLayout(GameTestHelper h) {
        StorageTests.ALL.get("backpack_tiers_carry_their_armor_slot_count_and_slot_layout").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackContentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades(GameTestHelper h) {
        StorageTests.ALL.get("backpack_contents_survive_the_reinforced_recipe_and_both_smithing_upgrades").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackSneakRightClickPlacesTheBackpackAndBreakingItDropsEverything(GameTestHelper h) {
        StorageTests.ALL.get("backpack_sneak_right_click_places_the_backpack_and_breaking_it_drops_everything").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackOpenKeyOpensTheWornBackpackOrElseTheFirstOneCarried(GameTestHelper h) {
        StorageTests.ALL.get("backpack_open_key_opens_the_worn_backpack_or_else_the_first_one_carried").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackShiftClickFromTheHotbarFillsInventoryAndBackpackAsOneStorage(GameTestHelper h) {
        StorageTests.ALL.get("backpack_shift_click_from_the_hotbar_fills_inventory_and_backpack_as_one_storage").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackShiftClickSendsStorageToTheHotbarAndArmorToItsSlot(GameTestHelper h) {
        StorageTests.ALL.get("backpack_shift_click_sends_storage_to_the_hotbar_and_armor_to_its_slot").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackDeepPocketsRaisesStackLimitsOnlyForStackables(GameTestHelper h) {
        StorageTests.ALL.get("backpack_deep_pockets_raises_stack_limits_only_for_stackables").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackFunnelPullsPickedUpItemsIntoTheWornBackpack(GameTestHelper h) {
        StorageTests.ALL.get("backpack_funnel_pulls_picked_up_items_into_the_worn_backpack").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackBackpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette(GameTestHelper h) {
        StorageTests.ALL.get("backpack_backpacks_take_their_four_enchantments_but_neither_drawer_nor_color_palette").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackTooltipImageCarriesTheStoredItemsOfEveryTierDyedToo(GameTestHelper h) {
        StorageTests.ALL.get("backpack_tooltip_image_carries_the_stored_items_of_every_tier_dyed_too").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackUpperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents(GameTestHelper h) {
        StorageTests.ALL.get("backpack_upper_tiers_survive_fire_and_explosions_and_lower_ones_spill_their_contents").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackBackpackKeyLocksTheInventorySlotOfTheBackpackItOpened(GameTestHelper h) {
        StorageTests.ALL.get("backpack_backpack_key_locks_the_inventory_slot_of_the_backpack_it_opened").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackPlacedBackpackFeedsItsFillLevelToComparators(GameTestHelper h) {
        StorageTests.ALL.get("backpack_placed_backpack_feeds_its_fill_level_to_comparators").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackStorageRegistered(GameTestHelper h) {
        StorageTests.ALL.get("backpack_storage_registered").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackStorageMigrationFromSimplebuilding(GameTestHelper h) {
        StorageTests.ALL.get("backpack_storage_migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackStorageRefusesTheNotAllowedTag(GameTestHelper h) {
        StorageTests.ALL.get("backpack_storage_refuses_the_not_allowed_tag").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackStorageUnreadableEntryKeepsTheRest(GameTestHelper h) {
        StorageTests.ALL.get("backpack_storage_unreadable_entry_keeps_the_rest").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void backpackStorageAccessoryBackpackCountsAsWorn(GameTestHelper h) {
        StorageTests.ALL.get("backpack_storage_accessory_backpack_counts_as_worn").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsBetterChestTablesRateAndDoubleChestRule(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_better_chest_tables_rate_and_double_chest_rule").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsBastionTemplateChestBecomesNetheriteChest(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_bastion_template_chest_becomes_netherite_chest").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsPlacedEndCityChestBecomesEnderiteChestWithDoubleLoot(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_placed_end_city_chest_becomes_enderite_chest_with_double_loot").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsRareShulkersHaveTieredHealthAndDropTheirShells(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_rare_shulkers_have_tiered_health_and_drop_their_shells").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsLivingShulkersClimbTheTiersButUpgradedOnesDropNoShells(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_living_shulkers_climb_the_tiers_but_upgraded_ones_drop_no_shells").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsRareShulkerCallsFourEndermitesOnceWhenPlayersComeNear(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_rare_shulker_calls_four_endermites_once_when_players_come_near").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsShellRecipesAndConfigCaps(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_shell_recipes_and_config_caps").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsRegistered(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_registered").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void rareFindsMigrationFromSimplebuilding(GameTestHelper h) {
        StorageTests.ALL.get("rare_finds_migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = com.simplestorage.test.PlacedStorageTests.FALL_MAX_TICKS)
    public void placedStorageBundleFallsLikeSand(GameTestHelper h) {
        StorageTests.ALL.get("placed_storage_bundle_falls_like_sand").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedStorageBundleHangsOnWallNotOnCeiling(GameTestHelper h) {
        StorageTests.ALL.get("placed_storage_bundle_hangs_on_wall_not_on_ceiling").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = com.simplestorage.test.PlacedStorageTests.FALL_MAX_TICKS)
    public void placedStorageBackpackFallsAndKeepsContentsWhenItBreaks(GameTestHelper h) {
        StorageTests.ALL.get("placed_storage_backpack_falls_and_keeps_contents_when_it_breaks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void placedStoragePlacedQuiverTakesArrowsOnly(GameTestHelper h) {
        StorageTests.ALL.get("placed_storage_placed_quiver_takes_arrows_only").accept(h);
    }
}
