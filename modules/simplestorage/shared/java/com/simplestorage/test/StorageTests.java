package com.simplestorage.test;

import com.simplelib.api.IdMoves;
import com.simplestorage.SimpleStorage;
import com.simplestorage.crate.CrateBlock;
import com.simplestorage.crate.CrateBlockEntity;
import com.simplestorage.crate.CrateWood;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import com.simplestorage.table.StorageCraftingMenu;
import com.simplestorage.table.StorageCraftingTableBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (split stage 8a); the Fabric/NeoForge/Forge adapters register them as
 * module_game_test_<name> on {@code simplestorage:empty}. Every test runs standalone and with SimpleBuilding; partner
 * checks (Simple End's woods) read the registry, never a class. The crate and table tests came from SimpleBuilding's
 * {@code WoodworkTests} and {@code BrewingTests} unchanged in what they check.
 */
public final class StorageTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    static final BlockPos SPOT = new BlockPos(2, 2, 2);
    /** The boat deck test waits 40 ticks for the boat to settle. */
    public static final int VEHICLE_DECK_MAX_TICKS = 100;
    /** The placed bundle scroll test waits for the shown item not to cycle on its own ({@code PlacedBundleTests.SCROLL_MAX_TICKS}). */
    public static final int PLACED_BUNDLE_SCROLL_MAX_TICKS = PlacedBundleTests.SCROLL_MAX_TICKS;

    static {
        // Split stage 8b: the chests and shulker boxes (bodies from SimpleBuilding's TieredChestTests / TieredShulkerBoxTests).
        ALL.put("tiered_chest_single_chest_climbs_from_copper_to_enderite_keeping_its_contents", ChestTests::singleChestClimbsFromCopperToEnderiteKeepingItsContents);
        ALL.put("tiered_chest_double_chest_upgrades_both_halves_together", ChestTests::doubleChestUpgradesBothHalvesTogether);
        ALL.put("tiered_chest_slot_counts_and_stack_limits_follow_the_tier", ChestTests::slotCountsAndStackLimitsFollowTheTier);
        ALL.put("tiered_chest_double_chests_form_only_from_equal_tiers", ChestTests::doubleChestsFormOnlyFromEqualTiers);
        MAX_TICKS.put("tiered_chest_vanilla_hoppers_fill_and_empty_oversized_slots", ChestTests.HOPPER_MAX_TICKS);
        ALL.put("tiered_chest_vanilla_hoppers_fill_and_empty_oversized_slots", ChestTests::vanillaHoppersFillAndEmptyOversizedSlots);
        ALL.put("tiered_chest_comparator_reads_oversized_slots_against_the_tier_limit", ChestTests::comparatorReadsOversizedSlotsAgainstTheTierLimit);
        ALL.put("tiered_chest_oversized_stacks_survive_saving_and_loading", ChestTests::oversizedStacksSurviveSavingAndLoading);
        ALL.put("tiered_chest_chest_items_follow_the_family_scheme", ChestTests::chestItemsFollowTheFamilyScheme);
        ALL.put("tiered_chest_trapped_capacity_comparator_and_properties_match_normal_tiers", ChestTests::trappedCapacityComparatorAndPropertiesMatchNormalTiers);
        ALL.put("tiered_chest_trapped_double_chests_require_the_same_kind", ChestTests::trappedDoubleChestsRequireTheSameKind);
        ALL.put("tiered_chest_trapped_recipes_use_the_matching_chest_and_hook", ChestTests::trappedRecipesUseTheMatchingChestAndHook);
        ALL.put("tiered_chest_trapped_signal_counts_viewers_and_updates_neighbors", ChestTests::trappedSignalCountsViewersAndUpdatesNeighbors);
        ALL.put("tiered_chest_trapped_upgrades_keep_both_halves_and_contents", ChestTests::trappedUpgradesKeepBothHalvesAndContents);
        ALL.put("tiered_chest_trapped_titles_match_normal_tiers", ChestTests::trappedTitlesMatchNormalTiers);
        ALL.put("tiered_chest_trapped_copper_chest_signals_its_viewers", ChestTests::trappedCopperChestSignalsItsViewers);
        ALL.put("tiered_chest_trapped_copper_chest_waxes_scrapes_and_ages_with_its_contents", ChestTests::trappedCopperChestWaxesScrapesAndAgesWithItsContents);
        ALL.put("tiered_chest_trapped_copper_chests_pair_only_with_each_other", ChestTests::trappedCopperChestsPairOnlyWithEachOther);
        ALL.put("shulker_box_vanilla_box_climbs_to_enderite_in_ten_blows_keeping_contents_and_color", ShulkerBoxTests::vanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor);
        ALL.put("shulker_box_contents_and_oversized_stacks_survive_breaking_placing_and_burning", ShulkerBoxTests::contentsAndOversizedStacksSurviveBreakingPlacingAndBurning);
        ALL.put("shulker_box_oversized_stacks_and_color_survive_saving_and_loading", ShulkerBoxTests::oversizedStacksAndColorSurviveSavingAndLoading);
        MAX_TICKS.put("shulker_box_hoppers_and_comparators_follow_the_tier_limit", ShulkerBoxTests.RIG_MAX_TICKS);
        ALL.put("shulker_box_hoppers_and_comparators_follow_the_tier_limit", ShulkerBoxTests::hoppersAndComparatorsFollowTheTierLimit);
        MAX_TICKS.put("shulker_box_dispensers_place_the_boxes", ShulkerBoxTests.RIG_MAX_TICKS);
        ALL.put("shulker_box_dispensers_place_the_boxes", ShulkerBoxTests::dispensersPlaceTheBoxes);
        ALL.put("shulker_box_boxes_do_not_nest", ShulkerBoxTests::boxesDoNotNest);
        ALL.put("shulker_box_boxes_are_dyed_crafted_and_washed", ShulkerBoxTests::boxesAreDyedCraftedAndWashed);
        ALL.put("shulker_box_box_items_follow_the_family_scheme", ShulkerBoxTests::boxItemsFollowTheFamilyScheme);
        // Split stage 8b-2: the chest carts and chest boats (bodies from SimpleBuilding's VehicleTests / WorldPolishTests).
        ALL.put("vehicle_chest_vehicles_hold_the_tier_slots_and_keep_oversized_stacks", VehicleTests::chestVehiclesHoldTheTierSlotsAndKeepOversizedStacks);
        ALL.put("vehicle_broken_chest_vehicles_drop_their_tier_item_and_contents", VehicleTests::brokenChestVehiclesDropTheirTierItemAndContents);
        ALL.put("vehicle_chest_vehicle_recipes_craft_and_upgrade", VehicleTests::chestVehicleRecipesCraftAndUpgrade);
        MAX_TICKS.put("vehicle_standers_stay_on_the_chest_boat_deck", VEHICLE_DECK_MAX_TICKS);
        ALL.put("vehicle_standers_stay_on_the_chest_boat_deck", VehicleTests::standersStayOnTheChestBoatDeck);
        ALL.put("vehicle_old_vehicles_load_as_simple_storages", VehicleTests::oldVehiclesLoadAsSimpleStorages);
        // Split stage 8c-1: bundles, quivers, the placed bundle, the storage enchantments (bodies from SimpleBuilding's
        // ReinforcedBundleTests, QuiverTests, StorageEnchantmentTests, PlacedBundleTests and BundleWiringTests).
        MAX_TICKS.put("placed_bundle_sneak_scroll_packets_cycle_the_top_item_and_right_click_takes_it", PLACED_BUNDLE_SCROLL_MAX_TICKS);
        ALL.put("reinforced_bundle_insertion_stops_at_the_brim_and_weighs_by_stack_size", BundleTests::insertionStopsAtTheBrimAndWeighsByStackSize);
        ALL.put("reinforced_bundle_insertion_turns_away_what_cannot_go_into_container_items", BundleTests::insertionTurnsAwayWhatCannotGoIntoContainerItems);
        ALL.put("reinforced_bundle_insertion_merges_equal_stacks_and_pushes_them_to_the_top", BundleTests::insertionMergesEqualStacksAndPushesThemToTheTop);
        ALL.put("reinforced_bundle_drawer_holds_only_the_kind_already_inside", BundleTests::drawerHoldsOnlyTheKindAlreadyInside);
        ALL.put("reinforced_bundle_nested_bundles_weigh_their_contents", BundleTests::nestedBundlesWeighTheirContents);
        ALL.put("reinforced_bundle_the_selected_entry_is_the_one_that_comes_out", BundleTests::theSelectedEntryIsTheOneThatComesOut);
        ALL.put("reinforced_bundle_right_click_throws_the_selected_stack_but_never_blocks", BundleTests::rightClickThrowsTheSelectedStackButNeverBlocks);
        ALL.put("reinforced_bundle_master_builder_places_from_the_bundle_and_color_palette_scatters_it", BundleTests::masterBuilderPlacesFromTheBundleAndColorPaletteScattersIt);
        ALL.put("reinforced_bundle_capacity_follows_tier_and_enchantments_and_matches_the_wiki_export", BundleTests::capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport);
        ALL.put("reinforced_bundle_bar_and_tooltip_read_the_same_capacity_the_filling_uses", BundleTests::barAndTooltipReadTheSameCapacityTheFillingUses);
        ALL.put("reinforced_bundle_overflowing_bundle_weights_are_refused_instead_of_crashing", BundleTests::overflowingBundleWeightsAreRefusedInsteadOfCrashing);
        ALL.put("quiver_right_clicks_do_nothing_even_with_master_builder", QuiverTests::rightClicksDoNothingEvenWithMasterBuilder);
        ALL.put("quiver_arrow_filter_holds_for_clicks_and_the_inverted_binding_slips_past_it", QuiverTests::arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt);
        ALL.put("quiver_capacity_drops_the_bundle_bonus_and_follows_tier_and_enchantments", QuiverTests::capacityDropsTheBundleBonusAndFollowsTierAndEnchantments);
        ALL.put("quiver_bar_width_follows_the_same_capacity_the_filling_uses", QuiverTests::barWidthFollowsTheSameCapacityTheFillingUses);
        ALL.put("quiver_bow_takes_the_topmost_arrow_and_searches_offhand_chest_hotbar_then_backpack", QuiverTests::bowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack);
        ALL.put("quiver_bow_consumes_one_arrow_from_the_quiver_that_supplied_it", QuiverTests::bowConsumesOneArrowFromTheQuiverThatSuppliedIt);
        ALL.put("quiver_bow_shoots_from_the_quiver_and_bills_it_outside_creative_only", QuiverTests::bowShootsFromTheQuiverAndBillsItOutsideCreativeOnly);
        ALL.put("quiver_netherite_quiver_survives_an_explosion_like_the_netherite_bundle", QuiverTests::netheriteQuiverSurvivesAnExplosionLikeTheNetheriteBundle);
        ALL.put("quiver_crossbow_loads_from_the_quiver_and_bills_one_arrow", QuiverTests::crossbowLoadsFromTheQuiverAndBillsOneArrow);
        ALL.put("quiver_inside_worn_containers_feeds_the_bow", QuiverTests::aQuiverInsideTheBackpackFeedsTheBowOnlyWithMasterBuilder);
        ALL.put("quiver_picked_up_arrows_go_into_the_quiver_only_with_funnel", QuiverTests::pickedUpArrowsGoIntoTheQuiverOnlyWithFunnel);
        ALL.put("storage_enchantment_funnel_filter_decides_what_the_touch_sweeps_up", StorageEnchantmentTests::funnelFilterDecidesWhatTheTouchSweepsUp);
        ALL.put("storage_enchantment_drawer_kind_lock_holds_against_the_funnel_too", StorageEnchantmentTests::drawerKindLockHoldsAgainstTheFunnelToo);
        ALL.put("storage_enchantment_funnel_quiver_sweeps_arrows_only_and_stops_at_its_brim", StorageEnchantmentTests::funnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim);
        ALL.put("storage_enchantment_drawer_and_deep_pockets_multiply_on_the_same_container", StorageEnchantmentTests::drawerAndDeepPocketsMultiplyOnTheSameContainer);
        ALL.put("placed_bundle_sneak_use_places_bundles_only_on_top_faces", PlacedBundleTests::sneakUsePlacesBundlesOnlyOnTopFaces);
        ALL.put("placed_bundle_sneak_scroll_packets_cycle_the_top_item_and_right_click_takes_it", PlacedBundleTests::sneakScrollPacketsCycleTheTopItemAndRightClickTakesIt);
        ALL.put("placed_bundle_sneak_right_click_with_an_item_deposits_into_the_placed_bundle", PlacedBundleTests::sneakRightClickWithAnItemDepositsIntoThePlacedBundle);
        ALL.put("placed_bundle_placed_bundles_drop_themselves_with_their_contents", PlacedBundleTests::placedBundlesDropThemselvesWithTheirContents);
        ALL.put("bundle_wiring_funnel_bundle_sweeps_up_drops_on_touch_unless_the_player_sneaks", BundleWiringTests::funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks);
        ALL.put("bundle_wiring_funnel_honours_pickup_delay_and_target_in_the_hand", BundleWiringTests::funnelHonoursPickupDelayAndTargetInTheHand);
        ALL.put("bundle_wiring_netherite_bundle_on_the_ground_survives_fire_and_explosions", BundleWiringTests::netheriteBundleOnTheGroundSurvivesFireAndExplosions);
        ALL.put("bundle_wiring_selection_packet_reaches_entries_past_the_twelfth", BundleWiringTests::selectionPacketReachesEntriesPastTheTwelfth);
        ALL.put("bundle_wiring_funnel_pickup_counts_what_it_took", BundleWiringTests::funnelPickupCountsWhatItTook);
        ALL.put("bundle_wiring_bundles_close_like_vanilla_when_picked_up_or_left", BundleWiringTests::bundlesCloseLikeVanillaWhenPickedUpOrLeft);
        ALL.put("bundle_storage_registered", BundleStorageTests::registered);
        ALL.put("bundle_storage_migration_from_simplebuilding", BundleStorageTests::migrationFromSimplebuilding);
        ALL.put("bundle_storage_recipes_work_on_their_own", BundleStorageTests::recipesWorkOnTheirOwn);
        // Split stage 8c-2: the backpacks (bodies from SimpleBuilding's BackpackTests; the Master Builder / Constructor's Touch
        // interplay stays a SimpleBuilding test, it needs its enchantments and wand).
        ALL.put("backpack_right_click_wears_the_backpack_and_swaps_it_with_the_chestplate", BackpackTests::rightClickWearsTheBackpackAndSwapsItWithTheChestplate);
        ALL.put("backpack_tiers_carry_their_armor_slot_count_and_slot_layout", BackpackTests::tiersCarryTheirArmorSlotCountAndSlotLayout);
        ALL.put("backpack_contents_survive_the_reinforced_recipe_and_both_smithing_upgrades", BackpackTests::contentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades);
        ALL.put("backpack_sneak_right_click_places_the_backpack_and_breaking_it_drops_everything", BackpackTests::sneakRightClickPlacesTheBackpackAndBreakingItDropsEverything);
        ALL.put("backpack_open_key_opens_the_worn_backpack_or_else_the_first_one_carried", BackpackTests::openKeyOpensTheWornBackpackOrElseTheFirstOneCarried);
        ALL.put("backpack_shift_click_from_the_hotbar_fills_inventory_and_backpack_as_one_storage", BackpackTests::shiftClickFromTheHotbarFillsInventoryAndBackpackAsOneStorage);
        ALL.put("backpack_shift_click_sends_storage_to_the_hotbar_and_armor_to_its_slot", BackpackTests::shiftClickSendsStorageToTheHotbarAndArmorToItsSlot);
        ALL.put("backpack_deep_pockets_raises_stack_limits_only_for_stackables", BackpackTests::deepPocketsRaisesStackLimitsOnlyForStackables);
        ALL.put("backpack_funnel_pulls_picked_up_items_into_the_worn_backpack", BackpackTests::funnelPullsPickedUpItemsIntoTheWornBackpack);
        ALL.put("backpack_backpacks_take_their_four_enchantments_but_neither_drawer_nor_color_palette", BackpackTests::backpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette);
        ALL.put("backpack_tooltip_image_carries_the_stored_items_of_every_tier_dyed_too", BackpackTests::tooltipImageCarriesTheStoredItemsOfEveryTierDyedToo);
        ALL.put("backpack_upper_tiers_survive_fire_and_explosions_and_lower_ones_spill_their_contents", BackpackTests::upperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents);
        ALL.put("backpack_backpack_key_locks_the_inventory_slot_of_the_backpack_it_opened", BackpackTests::backpackKeyLocksTheInventorySlotOfTheBackpackItOpened);
        ALL.put("backpack_placed_backpack_outputs_its_fill_level_to_a_comparator", BackpackTests::placedBackpackOutputsItsFillLevelToAComparator);
        ALL.put("backpack_storage_registered", BackpackStorageTests::registered);
        ALL.put("backpack_storage_migration_from_simplebuilding", BackpackStorageTests::migrationFromSimplebuilding);
        ALL.put("backpack_storage_refuses_the_not_allowed_tag", BackpackStorageTests::refusesTheNotAllowedTag);
        ALL.put("backpack_storage_unreadable_entry_keeps_the_rest", BackpackStorageTests::unreadableEntryKeepsTheRest);
        ALL.put("backpack_storage_accessory_backpack_counts_as_worn", BackpackStorageTests::accessoryBackpackCountsAsWorn);
        ALL.put("registered", h -> {
            List<String> problems = new ArrayList<>();
            boolean end = loaded(CrateWood.SIMPLE_END);
            for (CrateWood wood : CrateWood.values()) {
                boolean expected = !wood.external() || end;
                if (BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id(wood.crateId())) != expected) problems.add("block " + wood.crateId());
                if (BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(wood.crateId())) != expected) problems.add("item " + wood.crateId());
            }
            for (String path : List.of(SimpleStorage.STORAGE_CRAFTING_TABLE, "guide_book")) {
                if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
            }
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleStorage.id("crate"))) problems.add("block entity crate");
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleStorage.id(SimpleStorage.STORAGE_CRAFTING_TABLE))) problems.add("block entity table");
            if (!BuiltInRegistries.MENU.containsKey(SimpleStorage.id(SimpleStorage.STORAGE_CRAFTING_TABLE))) problems.add("menu");
            StorageBlocks.CRATES.values().forEach(crate -> {
                if (!StorageBlockEntities.CRATE.isValid(crate.defaultBlockState())) problems.add("crate BE takes " + crate);
            });
            if (StorageBlockEntities.CRATE.isValid(StorageBlocks.STORAGE_CRAFTING_TABLE.defaultBlockState())) problems.add("crate BE takes the table");
            if (StorageMenus.STORAGE_CRAFTING_TABLE == null) problems.add("menu field");
            // Split stage 8b: chests, trapped chests, trapped copper chests, tier shulker boxes, their block entities, menus, component.
            for (String path : SimpleStorage.movedChests()) {
                if (!BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id(path))) problems.add("block " + path);
                if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
            }
            for (var chest : StorageBlocks.tieredChests()) if (!StorageBlockEntities.TIERED_CHEST.isValid(chest.defaultBlockState())) problems.add("chest BE takes " + chest);
            for (var chest : StorageBlocks.trappedCopperChests()) if (!StorageBlockEntities.TRAPPED_COPPER_CHEST.isValid(chest.defaultBlockState())) problems.add("copper BE takes " + chest);
            for (var box : StorageBlocks.shulkerBoxes()) if (!StorageBlockEntities.TIERED_SHULKER_BOX.isValid(box.defaultBlockState())) problems.add("box BE takes " + box);
            for (String tier : SimpleStorage.TIERS) {
                for (String kind : List.of("_chest", "_double_chest", "_shulker_box")) {
                    if (!BuiltInRegistries.MENU.containsKey(SimpleStorage.id(tier + kind))) problems.add("menu " + tier + kind);
                }
            }
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleStorage.id("container_counts"))) problems.add("component container_counts");
            // Split stage 8b-2: chest carts and chest boats (item + entity), the wood component.
            for (String path : SimpleStorage.movedVehicles()) {
                if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleStorage.id(path))) problems.add("entity " + path);
            }
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(SimpleStorage.id("boat_wood"))) problems.add("component boat_wood");
            h.assertTrue(problems.isEmpty(), "registration: " + problems);
            h.succeed();
        });
        ALL.put("migration_from_simplebuilding", h -> {
            List<String> problems = new ArrayList<>();
            List<String> chests = SimpleStorage.movedChests();
            for (String path : SimpleStorage.movedBlocks()) {
                if (!BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id(path))) continue; // End crates without Simple End
                if (BuiltInRegistries.BLOCK.getValue(SimpleStorage.old(path)) != BuiltInRegistries.BLOCK.getValue(SimpleStorage.id(path))) problems.add("block " + path);
                if (BuiltInRegistries.ITEM.getValue(SimpleStorage.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path))) problems.add("item " + path);
                if (chests.contains(path)) continue; // 8b: their recipes are checked below by recipe id
                if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
                Identifier advancement = SimpleStorage.id("recipes/decorations/" + path);
                if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/decorations/" + path)))) problems.add("advancement " + path);
            }
            // Split stage 8b: the chest and box recipes with their recipe-book folders, the component.
            for (String path : SimpleStorage.movedVehicleRecipes()) {
                if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
                Identifier advancement = SimpleStorage.id("recipes/transportation/" + path);
                if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/transportation/" + path)))) problems.add("advancement " + path);
            }
            SimpleStorage.movedChestRecipes().forEach((path, folder) -> {
                if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
                Identifier advancement = SimpleStorage.id("recipes/" + folder + "/" + path);
                if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/" + folder + "/" + path)))) problems.add("advancement " + path);
            });
            if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(SimpleStorage.old("container_counts")) != com.simplestorage.registry.StorageComponents.CONTAINER_COUNTS) {
                problems.add("component container_counts");
            }
            // A filled Netherite chest of an old world: block entity id simplebuilding:tiered_chest, a slot of 100 (over the codec's 99).
            BlockState chestState = StorageBlocks.NETHERITE_CHEST.defaultBlockState();
            CompoundTag chestTag = oldEntity("simplebuilding:tiered_chest", "minecraft:cobblestone", 99);
            net.minecraft.nbt.ListTag counts = new net.minecraft.nbt.ListTag();
            CompoundTag extra = new CompoundTag();
            extra.putInt("Slot", 0);
            extra.putInt("Count", 100);
            counts.add(extra);
            chestTag.put(com.simplestorage.chest.TieredChestBlockEntity.EXTRA_COUNTS, counts);
            BlockEntity oldChest = BlockEntity.loadStatic(h.absolutePos(SPOT), chestState, chestTag, h.getLevel().registryAccess());
            if (!(oldChest instanceof com.simplestorage.chest.TieredChestBlockEntity loadedChest) || loadedChest.getItem(0).getCount() != 100
                    || !loadedChest.getItem(0).is(Items.COBBLESTONE)) problems.add("old netherite chest loads as " + oldChest);
            for (String be : List.of("crate", SimpleStorage.STORAGE_CRAFTING_TABLE, "tiered_chest", "trapped_copper_chest", "tiered_shulker_box")) {
                if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleStorage.old(be)) != BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleStorage.id(be))) problems.add("block entity " + be);
            }
            // A crate of an old world: block entity id simplebuilding:crate with its food.
            BlockState state = StorageBlocks.CRATES.get(CrateWood.OAK).defaultBlockState();
            BlockEntity crate = BlockEntity.loadStatic(h.absolutePos(SPOT), state, oldEntity("simplebuilding:crate", "minecraft:bread", 7), h.getLevel().registryAccess());
            if (!(crate instanceof CrateBlockEntity loaded) || loaded.countItem(Items.BREAD) != 7) problems.add("old crate loads as " + crate);
            else if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(loaded.getType()).equals(SimpleStorage.id("crate"))) problems.add("crate saved as " + loaded.getType());
            // A Storage Crafting Table of an old world keeps its grid.
            BlockEntity table = BlockEntity.loadStatic(h.absolutePos(SPOT), StorageBlocks.STORAGE_CRAFTING_TABLE.defaultBlockState(),
                    oldEntity("simplebuilding:storage_crafting_table", "minecraft:oak_planks", 3), h.getLevel().registryAccess());
            if (!(table instanceof StorageCraftingTableBlockEntity grid) || !grid.items().get(0).is(Items.OAK_PLANKS)) problems.add("old table loads as " + table);
            h.assertTrue(problems.isEmpty(), "migration: " + problems);
            h.succeed();
        });
        ALL.put("recipes", h -> {
            List<String> problems = new ArrayList<>();
            var manager = h.getLevel().getServer().getRecipeManager();
            for (String path : SimpleStorage.movedChestRecipes().keySet()) {
                if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(path))).isEmpty()) problems.add("missing " + path);
                if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.old(path))).isPresent()) problems.add("still under simplebuilding: " + path);
            }
            List<String> chestBlocks = SimpleStorage.movedChests();
            for (String path : SimpleStorage.movedBlocks()) {
                if (chestBlocks.contains(path)) continue; // 8b: own recipe ids above (Netherite/Enderite only in the world)
                boolean registered = BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path));
                var holder = manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(path)));
                if (holder.isPresent() != registered) problems.add((registered ? "missing " : "unexpected ") + path);
                if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.old(path))).isPresent()) problems.add("still under simplebuilding: " + path);
            }
            h.assertTrue(problems.isEmpty(), "recipes: " + problems);
            h.succeed();
        });
        ALL.put("crates_burn_and_fuel_like_planks", h -> {
            List<String> problems = new ArrayList<>();
            var fire = (com.simplestorage.mixin.FireBlockInvoker) Blocks.FIRE;
            StorageBlocks.CRATES.forEach((wood, crate) -> {
                if (fire.simplestorage$canBurn(crate.defaultBlockState()) == wood.nether()) problems.add(wood + " burns: " + !wood.nether() + " expected");
                // 26.3 fuel is the item's cooking_fuel component (300 ticks like planks, none for the Nether woods).
                boolean fuel = new ItemStack(StorageItems.CRATES.get(wood)).has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
                if (fuel == wood.nether()) problems.add(wood + " fuel: " + fuel);
                if (!crate.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)) problems.add(wood + " not mined with the axe");
            });
            if (!StorageBlocks.STORAGE_CRAFTING_TABLE.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)) problems.add("table not mined with the axe");
            h.assertTrue(problems.isEmpty(), "burning: " + problems);
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            com.simplebuilding.framework.api.SubMods.set(SimpleStorage.MOD_ID, false);
            try {
                h.assertFalse(SimpleStorage.on(), "switched off");
                h.assertTrue(StorageRegistry.tabStacks().isEmpty(), "no tab stacks while off");
                h.assertFalse(StorageRegistry.fillsVanillaTabs(false, true), "no Vanilla tab entries while off");
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id("oak_crate")), "registrations stay");
                h.setBlock(SPOT, StorageBlocks.CRATES.get(CrateWood.OAK));
                h.assertTrue(h.getBlockEntity(SPOT, CrateBlockEntity.class).insert(new ItemStack(Items.APPLE, 2)).isEmpty(), "a crate still stores while off");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleStorage.MOD_ID, true);
            }
            h.assertTrue(StorageRegistry.fillsVanillaTabs(false, true) && !StorageRegistry.fillsVanillaTabs(true, true),
                    "Vanilla tabs: only on its own (SimpleBuilding places them by id)");
            h.succeed();
        });
        crates();
        tables();
        ALL.put("guide_book", com.simplestorage.guide.StorageGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplestorage.discover.StorageDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplestorage.discover.StorageDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplestorage.discover.StorageDiscover::firstPickupTips);
    }

    // ------------------------------------------------------------------ crates (from SimpleBuilding WoodworkTests)

    private static final Item[] NINE_FOODS = {Items.APPLE, Items.BREAD, Items.CARROT, Items.POTATO, Items.BAKED_POTATO,
            Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.MELON_SLICE, Items.COOKIE};

    private static Block oakCrate() {
        return StorageBlocks.CRATES.get(CrateWood.OAK);
    }

    private static List<Item> kinds(CrateBlockEntity crate) {
        return crate.layers().stream().map(layer -> layer.kind().getItem()).toList();
    }

    private static void crates() {
        MAX_TICKS.put("crates_store_food_for_hoppers_and_comparators", 200);
        ALL.put("crates_store_food_for_hoppers_and_comparators", helper -> {
            List<String> fails = new ArrayList<>();
            BlockPos rel = SPOT;
            helper.setBlock(rel, oakCrate());
            CrateBlockEntity crate = helper.getBlockEntity(rel, CrateBlockEntity.class);
            ServerPlayer player = player(helper, GameType.SURVIVAL);
            useBlock(helper, player, new ItemStack(Items.DIRT, 10), rel);
            check(crate.isEmpty(), fails, "dirt stays out");
            check(player.getMainHandItem().getCount() == 10, fails, "dirt stays in the hand");
            useBlock(helper, player, new ItemStack(Items.BREAD, 40), rel);
            check(crate.countItem(Items.BREAD) == 40 && player.getMainHandItem().isEmpty(), fails, "the bread stack goes in");
            ItemStack rest = crate.insert(new ItemStack(Items.APPLE, 64 * 9));
            check(crate.countItem(Items.APPLE) == 64 * 8 && rest.getCount() == 64, fails, "nine stacks in all (apples " + crate.countItem(Items.APPLE) + ")");
            int signal = helper.getBlockState(rel).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(rel), Direction.NORTH);
            check(signal >= 14, fails, "comparator reads it nearly full (" + signal + ")");
            useBlock(helper, player, ItemStack.EMPTY, rel);
            check(player.getInventory().countItem(Items.APPLE) == 64, fails, "empty hand takes the top stack");
            player.getInventory().clearContent();
            player.setShiftKeyDown(true);
            useBlock(helper, player, ItemStack.EMPTY, rel);
            check(player.getInventory().countItem(Items.APPLE) == 1, fails, "sneaking takes one");
            crate.clearContent();
            crate.insert(new ItemStack(Items.CARROT, 5));
            helper.setBlock(rel.below(), Blocks.HOPPER);
            if (!fails.isEmpty()) {
                finish(helper, fails);
                return;
            }
            helper.runAfterDelay(60, () -> {
                check(crate.isEmpty(), fails, "a hopper empties the crate");
                var hopper = (net.minecraft.world.Container) helper.getBlockEntity(rel.below(), HopperBlockEntity.class);
                check(hopper.countItem(Items.CARROT) == 5, fails, "the hopper got the carrots");
                hopper.clearContent();
                helper.setBlock(rel.below(), Blocks.STONE);
                crate.insert(new ItemStack(Items.COOKED_BEEF, 3));
                helper.getLevel().destroyBlock(helper.absolutePos(rel), true);
                AABB box = new AABB(helper.absolutePos(rel)).inflate(2.0);
                int beef = 0;
                boolean crateItem = false;
                for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
                    if (entity.getItem().is(Items.COOKED_BEEF)) beef += entity.getItem().getCount();
                    if (entity.getItem().is(oakCrate().asItem())) crateItem = true;
                    entity.discard();
                }
                check(beef == 3, fails, "breaking drops the contents (" + beef + ")");
                check(crateItem, fails, "breaking drops the crate");
                finish(helper, fails);
            });
        });
        // Queue N55: a crate holds at most nine different kinds; a tenth is refused by insert, canPlaceItem, by hand and
        // by a hopper (which still passes on what the crate takes), while a kind that is in already still fills up.
        MAX_TICKS.put("crates_take_at_most_nine_kinds", 200);
        ALL.put("crates_take_at_most_nine_kinds", helper -> {
            List<String> fails = new ArrayList<>();
            BlockPos rel = SPOT;
            helper.setBlock(rel, oakCrate());
            CrateBlockEntity crate = helper.getBlockEntity(rel, CrateBlockEntity.class);
            for (Item food : NINE_FOODS) {
                check(crate.insert(new ItemStack(food, 3)).isEmpty(), fails, food + " goes in");
            }
            check(crate.kindCount() == 9 && crate.layers().size() == 9, fails, "nine kinds, nine layers (" + crate.kindCount() + ")");
            ItemStack rest = crate.insert(new ItemStack(Items.BEETROOT, 3));
            check(rest.getCount() == 3 && crate.countItem(Items.BEETROOT) == 0, fails, "insert refuses a tenth kind");
            check(!crate.canPlaceItem(0, new ItemStack(Items.BEETROOT)) && crate.canPlaceItem(0, new ItemStack(Items.APPLE)), fails,
                    "canPlaceItem: no tenth kind, but more of a present one");
            ServerPlayer player = player(helper, GameType.SURVIVAL);
            useBlock(helper, player, new ItemStack(Items.BEETROOT, 10), rel);
            check(crate.countItem(Items.BEETROOT) == 0 && player.getMainHandItem().getCount() == 10, fails, "by hand: the tenth kind stays in the hand");
            check(crate.insert(new ItemStack(Items.APPLE, 5)).isEmpty() && crate.countItem(Items.APPLE) == 8 && crate.kindCount() == 9, fails,
                    "a present kind still fills up (apples " + crate.countItem(Items.APPLE) + ")");
            BlockPos above = rel.above();
            helper.setBlock(above, Blocks.HOPPER);
            var hopper = (net.minecraft.world.Container) helper.getBlockEntity(above, HopperBlockEntity.class);
            hopper.setItem(0, new ItemStack(Items.BEETROOT, 3));
            hopper.setItem(1, new ItemStack(Items.APPLE, 3));
            helper.runAfterDelay(60, () -> {
                check(hopper.countItem(Items.BEETROOT) == 3 && crate.countItem(Items.BEETROOT) == 0, fails,
                        "the hopper cannot put a tenth kind in (crate beetroot " + crate.countItem(Items.BEETROOT) + ")");
                check(hopper.countItem(Items.APPLE) == 0 && crate.countItem(Items.APPLE) == 11, fails,
                        "the hopper does pass on a present kind (crate apples " + crate.countItem(Items.APPLE) + ")");
                check(crate.kindCount() == 9, fails, "still nine kinds");
                crate.removeItem(crate.items().indexOf(crate.items().stream().filter(s -> s.is(Items.COOKIE)).findFirst().orElseThrow()), 64);
                check(crate.kindCount() == 8 && crate.insert(new ItemStack(Items.BEETROOT, 3)).isEmpty(), fails, "after one kind is gone a new one fits");
                finish(helper, fails);
            });
        });
        // Queue N55: the layers lie in the order the kinds were put in, the newest on top; topping up (also by a hopper)
        // lifts a kind, taking keeps the order, the empty hand takes from the top; order and amounts survive saving, the
        // client gets just kinds and amounts.
        MAX_TICKS.put("crates_layer_the_newest_kind_on_top", 200);
        ALL.put("crates_layer_the_newest_kind_on_top", helper -> {
            List<String> fails = new ArrayList<>();
            BlockPos rel = SPOT;
            helper.setBlock(rel, oakCrate());
            CrateBlockEntity crate = helper.getBlockEntity(rel, CrateBlockEntity.class);
            crate.insert(new ItemStack(Items.APPLE, 10));
            crate.insert(new ItemStack(Items.BREAD, 10));
            crate.insert(new ItemStack(Items.CARROT, 10));
            check(kinds(crate).equals(List.of(Items.APPLE, Items.BREAD, Items.CARROT)), fails, "first in, lowest: " + kinds(crate));
            crate.insert(new ItemStack(Items.APPLE, 5));
            check(kinds(crate).equals(List.of(Items.BREAD, Items.CARROT, Items.APPLE)), fails, "topping up apples lifts them: " + kinds(crate));
            crate.insert(new ItemStack(Items.APPLE, 1));
            check(kinds(crate).equals(List.of(Items.BREAD, Items.CARROT, Items.APPLE)) && crate.layers().get(2).count() == 16, fails,
                    "the top kind stays on top (" + kinds(crate) + ")");
            crate.removeItem(crate.items().indexOf(crate.items().stream().filter(s -> s.is(Items.BREAD)).findFirst().orElseThrow()), 3);
            check(kinds(crate).equals(List.of(Items.BREAD, Items.CARROT, Items.APPLE)) && crate.layers().get(0).count() == 7, fails,
                    "taking keeps the order (" + kinds(crate) + ")");
            var registries = helper.getLevel().registryAccess();
            BlockPos at = helper.absolutePos(rel);
            CrateBlockEntity saved = new CrateBlockEntity(at, crate.getBlockState());
            saved.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, crate.saveCustomOnly(registries)));
            check(kinds(saved).equals(kinds(crate)) && saved.countItem(Items.APPLE) == 16, fails, "saving keeps order and amounts (" + kinds(saved) + ")");
            CompoundTag update = crate.getUpdateTag(registries);
            check(update.get("Items") == null && update.get("Layers") != null, fails, "the update carries layers, not stacks: " + update);
            CrateBlockEntity client = new CrateBlockEntity(at, crate.getBlockState());
            client.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, update));
            check(kinds(client).equals(kinds(crate)) && client.countItem(Items.CARROT) == 10 && !client.isEmpty(), fails,
                    "the client sees the same layers (" + kinds(client) + ")");
            BlockPos above = rel.above();
            helper.setBlock(above, Blocks.HOPPER);
            ((net.minecraft.world.Container) helper.getBlockEntity(above, HopperBlockEntity.class)).setItem(0, new ItemStack(Items.BREAD, 2));
            helper.runAfterDelay(40, () -> {
                check(kinds(crate).equals(List.of(Items.CARROT, Items.APPLE, Items.BREAD)) && crate.layers().get(2).count() == 9, fails,
                        "a hopper merging bread lifts it to the top (" + kinds(crate) + ")");
                ServerPlayer player = player(helper, GameType.SURVIVAL);
                useBlock(helper, player, ItemStack.EMPTY, rel);
                check(player.getInventory().countItem(Items.BREAD) == 9 && kinds(crate).equals(List.of(Items.CARROT, Items.APPLE)), fails,
                        "the empty hand takes the top layer's stack (" + player.getInventory().countItem(Items.BREAD) + ", " + kinds(crate) + ")");
                finish(helper, fails);
            });
        });
        // Queue N31: a crate turns its opening to the player like a barrel (also sideways and upside down), keeps its food
        // when turned, has an open collision shape on the opening side, and hoppers fill it from above and empty it from
        // below in every orientation.
        MAX_TICKS.put("crates_turn_like_barrels_and_keep_their_food", 200);
        ALL.put("crates_turn_like_barrels_and_keep_their_food", helper -> {
            List<String> fails = new ArrayList<>();
            Block crateBlock = oakCrate();
            ServerPlayer player = player(helper, GameType.SURVIVAL);
            BlockPos at = new BlockPos(1, 1, 1);
            float[][] looks = {{0F, 90F}, {0F, -90F}, {0F, 0F}, {90F, 0F}, {180F, 0F}, {-90F, 0F}};
            Direction[] expected = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int i = 0; i < looks.length; i++) {
                player.setYRot(looks[i][0]);
                player.setYHeadRot(looks[i][0]);
                player.setXRot(looks[i][1]);
                var context = new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                        new ItemStack(crateBlock), hit(helper, at, Direction.UP));
                BlockState placed = crateBlock.getStateForPlacement(context);
                check(placed != null && placed.getValue(CrateBlock.FACING) == expected[i], fails,
                        "looking yaw " + looks[i][0] + " pitch " + looks[i][1] + " opens " + (placed == null ? null : placed.getValue(CrateBlock.FACING)) + ", expected " + expected[i]);
            }
            for (Direction facing : Direction.values()) {
                var shape = crateBlock.defaultBlockState().setValue(CrateBlock.FACING, facing).getShape(helper.getLevel(), BlockPos.ZERO);
                Vec3 open = new Vec3(0.5 + facing.getStepX() * 0.45, 0.5 + facing.getStepY() * 0.45, 0.5 + facing.getStepZ() * 0.45);
                Vec3 closed = new Vec3(0.5 - facing.getStepX() * 0.45, 0.5 - facing.getStepY() * 0.45, 0.5 - facing.getStepZ() * 0.45);
                boolean openHit = shape.toAabbs().stream().anyMatch(box -> box.contains(open));
                boolean closedHit = shape.toAabbs().stream().anyMatch(box -> box.contains(closed));
                check(!openHit && closedHit, fails, facing + ": the shape is open towards the opening only");
            }
            BlockPos turn = new BlockPos(1, 5, 6);
            helper.setBlock(turn, crateBlock.defaultBlockState().setValue(CrateBlock.FACING, Direction.NORTH));
            helper.getBlockEntity(turn, CrateBlockEntity.class).insert(new ItemStack(Items.BREAD, 12));
            BlockState turned = helper.getBlockState(turn).rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
            check(turned.getValue(CrateBlock.FACING) == Direction.EAST, fails, "turning north clockwise opens east");
            helper.setBlock(turn, turned.setValue(CrateBlock.FACING, Direction.DOWN));
            helper.setBlock(turn, helper.getBlockState(turn).setValue(CrateBlock.FACING, Direction.EAST));
            CrateBlockEntity kept = helper.getBlockEntity(turn, CrateBlockEntity.class);
            check(kept.countItem(Items.BREAD) == 12, fails, "the bread stays when the crate is turned (" + kept.countItem(Items.BREAD) + ")");
            useBlock(helper, player, new ItemStack(Items.APPLE, 5), turn);
            check(kept.countItem(Items.APPLE) == 5, fails, "a sideways crate takes food by hand");
            Direction[] all = Direction.values();
            for (int i = 0; i < all.length; i++) {
                BlockPos crate = new BlockPos(1 + i, 2, 3);
                helper.setBlock(crate.below(), Blocks.HOPPER);
                helper.setBlock(crate, crateBlock.defaultBlockState().setValue(CrateBlock.FACING, all[i]));
                helper.setBlock(crate.above(), Blocks.HOPPER);
                ((net.minecraft.world.Container) helper.getBlockEntity(crate.above(), HopperBlockEntity.class)).setItem(0, new ItemStack(Items.CARROT, 3));
            }
            if (!fails.isEmpty()) {
                finish(helper, fails);
                return;
            }
            helper.runAfterDelay(80, () -> {
                for (int i = 0; i < all.length; i++) {
                    BlockPos crate = new BlockPos(1 + i, 2, 3);
                    var below = (net.minecraft.world.Container) helper.getBlockEntity(crate.below(), HopperBlockEntity.class);
                    check(below.countItem(Items.CARROT) == 3, fails, all[i] + ": the carrots went through the crate (" + below.countItem(Items.CARROT) + ")");
                }
                finish(helper, fails);
            });
        });
    }

    // ------------------------------------------------------------------ storage crafting table (from SimpleBuilding BrewingTests)

    private static StorageCraftingTableBlockEntity table(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, StorageBlocks.STORAGE_CRAFTING_TABLE);
        return helper.getBlockEntity(pos, StorageCraftingTableBlockEntity.class);
    }

    private static StorageCraftingMenu open(StorageCraftingTableBlockEntity table, ServerPlayer player, int id) {
        return (StorageCraftingMenu) table.createMenu(id, player.getInventory(), player);
    }

    private static void tables() {
        // Four planks in the grid make a crafting table; closing leaves them on the table, reopening shows them again.
        ALL.put("storage_crafting_table_keeps_its_grid", helper -> {
            StorageCraftingTableBlockEntity table = table(helper, new BlockPos(1, 2, 1));
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.getInventory().clearContent();
            StorageCraftingMenu menu = open(table, player, 1);
            for (int slot : new int[] {1, 2, 4, 5}) {
                menu.getSlot(slot).set(new ItemStack(Items.OAK_PLANKS, 3));
            }
            helper.assertTrue(menu.getSlot(StorageCraftingMenu.RESULT_SLOT).getItem().is(Items.CRAFTING_TABLE), "the grid crafts no crafting table");
            menu.removed(player);
            helper.assertTrue(player.getInventory().isEmpty(), "closing gave the grid back to the player");
            helper.assertTrue(table.items().get(0).is(Items.OAK_PLANKS) && table.items().get(0).getCount() == 3
                    && table.items().get(4).is(Items.OAK_PLANKS), "the table lost its grid: " + table.items());
            StorageCraftingMenu again = open(table, player, 2);
            helper.assertTrue(again.getSlot(1).getItem().is(Items.OAK_PLANKS), "the reopened grid is empty");
            helper.assertTrue(again.getSlot(StorageCraftingMenu.RESULT_SLOT).getItem().is(Items.CRAFTING_TABLE), "the reopened grid shows no result");
            ItemStack crafted = again.getSlot(StorageCraftingMenu.RESULT_SLOT).remove(1);
            again.getSlot(StorageCraftingMenu.RESULT_SLOT).onTake(player, crafted);
            helper.assertValueEqual(table.items().get(0).getCount(), 2, "planks left in the first slot after crafting");
            again.removed(player);
            helper.succeed();
        });
        // Two players at one table use one grid: what one puts in, the other sees; what one takes is gone for both.
        ALL.put("storage_crafting_table_shares_its_grid", helper -> {
            StorageCraftingTableBlockEntity table = table(helper, new BlockPos(1, 2, 1));
            ServerPlayer first = helper.makeMockServerPlayerInLevel();
            ServerPlayer second = helper.makeMockServerPlayerInLevel();
            StorageCraftingMenu a = open(table, first, 1);
            StorageCraftingMenu b = open(table, second, 2);
            a.getSlot(5).set(new ItemStack(Items.DIAMOND, 2));
            helper.assertTrue(b.getSlot(5).getItem().is(Items.DIAMOND), "the second menu does not see the diamond");
            ItemStack taken = b.getSlot(5).remove(2);
            helper.assertValueEqual(taken.getCount(), 2, "taken diamonds");
            helper.assertTrue(a.getSlot(5).getItem().isEmpty() && table.items().get(4).isEmpty(), "the diamond is still there after taking it");
            a.removed(first);
            b.removed(second);
            helper.succeed();
        });
        // Breaking drops the table and its grid; hoppers do not see the grid.
        ALL.put("storage_crafting_table_drops_its_grid_and_ignores_hoppers", helper -> {
            BlockPos pos = new BlockPos(1, 2, 1);
            StorageCraftingTableBlockEntity table = table(helper, pos);
            table.grid().setItem(0, new ItemStack(Items.STICK, 5));
            helper.assertTrue(HopperBlockEntity.getContainerAt(helper.getLevel(), helper.absolutePos(pos)) == null, "a hopper can reach the grid");
            helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
            helper.assertItemEntityPresent(Items.STICK, pos, 2.0);
            helper.assertItemEntityPresent(StorageItems.STORAGE_CRAFTING_TABLE, pos, 2.0);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ helpers

    /** A partner mod is there when its namespace has registered items (no loader call in shared code). */
    static boolean loaded(String namespace) {
        return BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals(namespace));
    }

    /** A saved block entity of an old world: {@code id} with one stack of {@code item} in slot 0. */
    private static CompoundTag oldEntity(String id, String item, int count) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        CompoundTag slot = new CompoundTag();
        slot.putByte("Slot", (byte) 0);
        slot.putString("id", item);
        slot.putInt("count", count);
        items.add(slot);
        tag.put("Items", items);
        return tag;
    }

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction face) {
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(pos)).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(centre, face, helper.absolutePos(pos), false);
    }

    /** Right-click on the block at {@code pos} with {@code stack} in the main hand (the block's own use). */
    private static void useBlock(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockState state = helper.getBlockState(pos);
        var result = state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, pos, Direction.UP));
        if (!result.consumesAction() && stack.isEmpty()) {
            state.useWithoutItem(helper.getLevel(), player, hit(helper, pos, Direction.UP));
        }
    }

    private static void check(boolean ok, List<String> fails, String what) {
        if (!ok) fails.add(what);
    }

    private static void finish(GameTestHelper helper, List<String> fails) {
        if (fails.isEmpty()) helper.succeed();
        else helper.fail(String.join("; ", fails));
    }

    private StorageTests() {}
}
