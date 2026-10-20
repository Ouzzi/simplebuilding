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
}
