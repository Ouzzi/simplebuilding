package com.simplebuilding.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * The single, loader-neutral catalogue of every SimpleBuilding in-game test of the MC 1.21.11 line.
 *
 * <p>Structurally identical to the catalogue of the 26.2 tree; the difference is the content of a
 * few trade tests, because 1.21.11 has no data-driven villager trades. Everything else - block
 * behaviour, data integrity, tool behaviour, the legacy spatula migration - carries the very same
 * ids, so a report from one Minecraft line can be read next to a report from the other.
 *
 * <p>Loader adapters:
 * <ul>
 *   <li>Fabric: the thin {@code *GameTest} classes in {@code mc1_21_11/fabric} carry the
 *       {@code @GameTest} annotations and delegate into the shared bodies.</li>
 *   <li>NeoForge: iterate {@link #all()} and register one test instance per spec.</li>
 * </ul>
 */
public final class SimpleBuildingGameTests {

    /** Namespace every test id is registered under. */
    public static final String MOD_ID = "simplebuilding";

    private static final List<GameTestSpec> ALL = List.of(
            GameTestSpec.named("smoke_game_test_mod_items_are_registered", SmokeTests::modItemsAreRegistered)
                    .build(),
            GameTestSpec.named("trade_registry_game_test_all_mod_trades_resolve_against_the_server_registries",
                            TradeRegistryTests::allModTradesResolveAgainstTheServerRegistries)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_reinforced_and_netherite_furnaces_smelt_faster_than_vanilla", BlockBehaviourTests::reinforcedAndNetheriteFurnacesSmeltFasterThanVanilla)
                    .maxTicks(BlockBehaviourTests.FURNACE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_reinforced_and_netherite_blast_furnaces_and_smokers_outpace_vanilla", BlockBehaviourTests::reinforcedAndNetheriteBlastFurnacesAndSmokersOutpaceVanilla)
                    .maxTicks(BlockBehaviourTests.BLAST_AND_SMOKER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_reinforced_and_netherite_hoppers_move_items_faster_than_vanilla", BlockBehaviourTests::reinforcedAndNetheriteHoppersMoveItemsFasterThanVanilla)
                    .maxTicks(BlockBehaviourTests.HOPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_reinforced_piston_pushes_thirteen_blocks_where_vanilla_piston_refuses", BlockBehaviourTests::reinforcedPistonPushesThirteenBlocksWhereVanillaPistonRefuses)
                    .maxTicks(BlockBehaviourTests.REINFORCED_PISTON_MAX_TICKS)
                    .skyAccess(true)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_netherite_piston_breaks_the_block_in_front_while_vanilla_piston_pushes_it", BlockBehaviourTests::netheritePistonBreaksTheBlockInFrontWhileVanillaPistonPushesIt)
                    .maxTicks(BlockBehaviourTests.NETHERITE_PISTON_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_suspended_sand_and_gravel_stay_in_place_while_vanilla_ones_fall", BlockBehaviourTests::suspendedSandAndGravelStayInPlaceWhileVanillaOnesFall)
                    .maxTicks(BlockBehaviourTests.SUSPENDED_FALLING_BLOCK_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_levitating_sand_and_gravel_rise_as_an_accelerating_entity", BlockBehaviourTests::levitatingSandAndGravelRiseAsAnAcceleratingEntity)
                    .maxTicks(BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_levitating_sand_turns_back_into_ablock_under_aceiling", BlockBehaviourTests::levitatingSandTurnsBackIntoABlockUnderACeiling)
                    .maxTicks(BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_levitating_sand_drops_as_an_item_at_the_build_limit", BlockBehaviourTests::levitatingSandDropsAsAnItemAtTheBuildLimit)
                    .maxTicks(BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_construction_light_shines_but_lets_monsters_spawn", BlockBehaviourTests::constructionLightShinesButLetsMonstersSpawn)
                    .maxTicks(BlockBehaviourTests.CONSTRUCTION_LIGHT_MAX_TICKS)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_mod_item_is_in_the_item_registry", DataIntegrityTests::everyModItemIsInTheItemRegistry)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_mod_block_is_registered_and_has_its_block_item", DataIntegrityTests::everyModBlockIsRegisteredAndHasItsBlockItem)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_recipes_only_reference_registered_items", DataIntegrityTests::modRecipesOnlyReferenceRegisteredItems)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_mod_block_loot_table_loads", DataIntegrityTests::everyModBlockLootTableLoads)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_broken_mod_blocks_drop_their_expected_item", DataIntegrityTests::brokenModBlocksDropTheirExpectedItem)
                    .maxTicks(DataIntegrityTests.BLOCK_DROP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_generated_enchantment_files_still_match_their_source", DataIntegrityTests::generatedEnchantmentFilesStillMatchTheirSource)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_enchantments_are_present_in_the_datapack_registry", DataIntegrityTests::modEnchantmentsArePresentInTheDatapackRegistry)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_enchantment_tags_resolve_to_the_expected_entries", DataIntegrityTests::modEnchantmentTagsResolveToTheExpectedEntries)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_void_protected_tag_is_language_independent", DataIntegrityTests::voidProtectedTagIsLanguageIndependent)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_quartz_checkers_are_mined_by_pickaxe_and_crafted_from_their_material", DataIntegrityTests::quartzCheckersAreMinedByPickaxeAndCraftedFromTheirMaterial)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_end_brick_sets_are_crafted_cut_mined_and_tagged_like_vanilla", DataIntegrityTests::endBrickSetsAreCraftedCutMinedAndTaggedLikeVanilla)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_end_palettes_are_recoloured_from_end_stone_and_purpur_like_dye", DataIntegrityTests::endPalettesAreRecolouredFromEndStoneAndPurpurLikeDye)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_ender_quartz_palette_is_recoloured_from_quartz_like_dye", DataIntegrityTests::enderQuartzPaletteIsRecolouredFromQuartzLikeDye)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_basic_upgrade_template_costs_twice_the_crafting_material", DataIntegrityTests::basicUpgradeTemplateCostsTwiceTheCraftingMaterial)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_ender_quartz_is_crafted_from_astralit_dust_nihilith_shard_and_quartz", DataIntegrityTests::enderQuartzIsCraftedFromAstralitDustNihilithShardAndQuartz)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_mod_item_is_in_exactly_one_creative_tab", DataIntegrityTests::everyModItemIsInExactlyOneCreativeTab)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_machines_and_storage_tab_is_laid_out_in_rows_of_nine", DataIntegrityTests::machinesAndStorageTabIsLaidOutInRowsOfNine)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_tools_tab_is_laid_out_in_rows_of_nine", DataIntegrityTests::toolsTabIsLaidOutInRowsOfNine)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_creative_spacer_cannot_be_taken_or_kept", DataIntegrityTests::creativeSpacerCannotBeTakenOrKept)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_dev_enchanted_tab_offers_every_exclusive_choice_at_max_level_on_top_tiers", DataIntegrityTests::devEnchantedTabOffersEveryExclusiveChoiceAtMaxLevelOnTopTiers)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_dev_enchanted_tab_is_only_filled_in_development_or_when_configured", DataIntegrityTests::devEnchantedTabIsOnlyFilledInDevelopmentOrWhenConfigured)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_trimmable_armour_shows_every_trim_pattern_on_its_icon", DataIntegrityTests::everyTrimmableArmourShowsEveryTrimPatternOnItsIcon)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_vanilla_enchantment_has_its_own_book_model", DataIntegrityTests::everyVanillaEnchantmentHasItsOwnBookModel)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_vanilla_book_texture_follows_the_client_option", DataIntegrityTests::vanillaBookTextureFollowsTheClientOption)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_sledgehammer_breaks_three_by_three_around_origin", ToolBehaviourTests::sledgehammerBreaksThreeByThreeAroundOrigin)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_sledgehammer_override_levels_widen_block_selection", ToolBehaviourTests::sledgehammerOverrideLevelsWidenBlockSelection)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_chisel_and_spatula_transform_block_in_both_directions", ToolBehaviourTests::chiselAndSpatulaTransformBlockInBothDirections)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_chisel_tier_gates_transformations", ToolBehaviourTests::chiselTierGatesTransformations)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_vein_miner_collects_connected_ore_cluster", ToolBehaviourTests::veinMinerCollectsConnectedOreCluster)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_strip_miner_follows_player_facing_and_stops_at_gaps", ToolBehaviourTests::stripMinerFollowsPlayerFacingAndStopsAtGaps)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_magnet_pulls_nearby_items_and_ignores_distant_ones", ToolBehaviourTests::magnetPullsNearbyItemsAndIgnoresDistantOnes)
                    .maxTicks(ToolBehaviourTests.MAGNET_MAX_TICKS)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("tool_behaviour_game_test_shears_turn_placed_wool_into_four_string_and_wear_by_one", ToolBehaviourTests::shearsTurnPlacedWoolIntoFourStringAndWearByOne)
                    .build(),
            GameTestSpec.named("chisel_game_test_conversion_tables_are_pinned_entry_by_entry", ChiselTests::conversionTablesArePinnedEntryByEntry)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mod_trades_are_merged_into_the_villager_trade_pools", TradeAndMigrationTests::modTradesAreMergedIntoTheVillagerTradePools)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mod_trades_are_merged_into_the_wandering_trader_pools", TradeAndMigrationTests::modTradesAreMergedIntoTheWanderingTraderPools)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_trade_definitions_produce_the_expected_offers", TradeAndMigrationTests::tradeDefinitionsProduceTheExpectedOffers)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mod_trades_stay_worth_it_without_being_exploitable", TradeAndMigrationTests::modTradesStayWorthItWithoutBeingExploitable)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mason_villager_can_roll_amod_trade", TradeAndMigrationTests::masonVillagerCanRollAModTrade)
                    .maxTicks(TradeAndMigrationTests.MASON_VILLAGER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_wandering_trader_can_roll_amod_trade", TradeAndMigrationTests::wanderingTraderCanRollAModTrade)
                    .maxTicks(TradeAndMigrationTests.WANDERING_TRADER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_legacy_spatulas_in_player_inventory_become_chisels", TradeAndMigrationTests::legacySpatulasInPlayerInventoryBecomeChisels)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_legacy_spatula_item_entity_is_rewritten_in_place", TradeAndMigrationTests::legacySpatulaItemEntityIsRewrittenInPlace)
                    .maxTicks(TradeAndMigrationTests.LEGACY_ITEM_ENTITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("network_handler_game_test_double_jump_needs_enchanted_boots_and_wears_them", NetworkHandlerTests::doubleJumpNeedsEnchantedBootsAndWearsThem)
                    .build(),
            GameTestSpec.named("network_handler_game_test_space_key_and_trim_benefit_flags_reach_the_player", NetworkHandlerTests::spaceKeyAndTrimBenefitFlagsReachThePlayer)
                    .build(),
            GameTestSpec.named("network_handler_game_test_building_wand_configure_stores_radius_and_axis", NetworkHandlerTests::buildingWandConfigureStoresRadiusAndAxis)
                    .build(),
            GameTestSpec.named("network_handler_game_test_octant_configure_stores_the_whole_selection_state", NetworkHandlerTests::octantConfigureStoresTheWholeSelectionState)
                    .build(),
            GameTestSpec.named("network_handler_game_test_octant_scroll_cycles_shapes_and_nudges_corners_by_facing", NetworkHandlerTests::octantScrollCyclesShapesAndNudgesCornersByFacing)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("network_handler_game_test_master_builder_pick_takes_blocks_out_of_the_enchanted_bundle", NetworkHandlerTests::masterBuilderPickTakesBlocksOutOfTheEnchantedBundle)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_rotator_turns_logs_by_clicked_face_and_rim", ItemBehaviourTests::rotatorTurnsLogsByClickedFaceAndRim)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_rotator_cycles_facing_blocks_and_leaves_plain_blocks_alone", ItemBehaviourTests::rotatorCyclesFacingBlocksAndLeavesPlainBlocksAlone)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_quiver_takes_arrows_and_refuses_everything_else", ItemBehaviourTests::quiverTakesArrowsAndRefusesEverythingElse)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_bundle_capacity_grows_with_tier_and_enchantments", ItemBehaviourTests::bundleCapacityGrowsWithTierAndEnchantments)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_ore_detector_cycles_modes_and_learns_acustom_block", ItemBehaviourTests::oreDetectorCyclesModesAndLearnsACustomBlock)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_octant_stores_both_corners_and_respects_the_lock", ItemBehaviourTests::octantStoresBothCornersAndRespectsTheLock)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("item_behaviour_game_test_building_wand_fills_the_plane_it_is_pointed_at", ItemBehaviourTests::buildingWandFillsThePlaneItIsPointedAt)
                    .maxTicks(ItemBehaviourTests.WAND_MAX_TICKS)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_radius_widens_the_sledgehammer_face_and_sneaking_suppresses_it", EnchantmentEffectTests::radiusWidensTheSledgehammerFaceAndSneakingSuppressesIt)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_break_through_adds_layers_behind_the_mined_face", EnchantmentEffectTests::breakThroughAddsLayersBehindTheMinedFace)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_versatility_swaps_in_the_better_tool_while_sneaking", EnchantmentEffectTests::versatilitySwapsInTheBetterToolWhileSneaking)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_funnel_decides_what_the_bundle_picks_up", EnchantmentEffectTests::funnelDecidesWhatTheBundlePicksUp)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_data_driven_enchantment_effects_survive_datagen", EnchantmentEffectTests::dataDrivenEnchantmentEffectsSurviveDatagen)
                    .build(),
            GameTestSpec.named("enchantment_effect_game_test_cover_and_bridge_are_inert_and_this_is_deliberately_pinned_down", EnchantmentEffectTests::coverAndBridgeAreInertAndThisIsDeliberatelyPinnedDown)
                    .build(),
            GameTestSpec.named("hopper_and_trim_game_test_hopper_filter_modes_gate_what_may_enter", HopperAndTrimTests::hopperFilterModesGateWhatMayEnter)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("hopper_and_trim_game_test_hopper_payloads_only_act_on_an_open_hopper_menu", HopperAndTrimTests::hopperPayloadsOnlyActOnAnOpenHopperMenu)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("hopper_and_trim_game_test_trim_multiplier_follows_the_experience_curve_and_the_configured_base", HopperAndTrimTests::trimMultiplierFollowsTheExperienceCurveAndTheConfiguredBase)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_constructors_touch_unlocks_the_extra_chisel_tables_in_both_directions", BuildingEnchantmentTests::constructorsTouchUnlocksTheExtraChiselTablesInBothDirections)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_constructors_touch_stick_cycles_the_first_block_state_property", BuildingEnchantmentTests::constructorsTouchStickCyclesTheFirstBlockStateProperty)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_fast_chiseling_shortens_the_cooldown_and_speeds_up_mining", BuildingEnchantmentTests::fastChiselingShortensTheCooldownAndSpeedsUpMining)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_color_palette_spreads_the_carried_blocks_over_the_wand_preview", BuildingEnchantmentTests::colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_color_palette_keeps_the_wand_building_when_one_block_runs_out", BuildingEnchantmentTests::colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_linear_only_shortens_the_wand_step_delay", BuildingEnchantmentTests::linearOnlyShortensTheWandStepDelay)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("consumption_and_durability_game_test_chisel_charges_durability_and_cooldown_only_outside_creative", ConsumptionAndDurabilityTests::chiselChargesDurabilityAndCooldownOnlyOutsideCreative)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("consumption_and_durability_game_test_octant_and_rotator_spend_one_point_of_wear_per_accepted_click", ConsumptionAndDurabilityTests::octantAndRotatorSpendOnePointOfWearPerAcceptedClick)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("consumption_and_durability_game_test_building_wand_bills_one_block_and_one_point_of_wear_per_placement", ConsumptionAndDurabilityTests::buildingWandBillsOneBlockAndOnePointOfWearPerPlacement)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("consumption_and_durability_game_test_sledgehammer_secondary_use_wears_down_only_the_survival_player", ConsumptionAndDurabilityTests::sledgehammerSecondaryUseWearsDownOnlyTheSurvivalPlayer)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("consumption_and_durability_game_test_double_jump_boots_wear_down_for_the_survival_player", ConsumptionAndDurabilityTests::doubleJumpBootsWearDownForTheSurvivalPlayer)
                    .build(),
            GameTestSpec.named("vein_and_strip_miner_game_test_vein_miner_breaks_the_whole_vein_through_the_block_break_event", VeinAndStripMinerTests::veinMinerBreaksTheWholeVeinThroughTheBlockBreakEvent)
                    .maxTicks(VeinAndStripMinerTests.DROP_MAX_TICKS)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("vein_and_strip_miner_game_test_vein_miner_follows_logs_with_an_axe_through_the_block_break_event", VeinAndStripMinerTests::veinMinerFollowsLogsWithAnAxeThroughTheBlockBreakEvent)
                    .maxTicks(VeinAndStripMinerTests.DROP_MAX_TICKS)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("vein_and_strip_miner_game_test_vein_miner_refuses_non_ores_and_too_weak_pickaxes_and_diverges_from_the_highlight_on_quartz", VeinAndStripMinerTests::veinMinerRefusesNonOresAndTooWeakPickaxesAndDivergesFromTheHighlightOnQuartz)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("vein_and_strip_miner_game_test_strip_miner_tunnels_along_the_facing_and_refunds_durability_through_the_block_break_event", VeinAndStripMinerTests::stripMinerTunnelsAlongTheFacingAndRefundsDurabilityThroughTheBlockBreakEvent)
                    .maxTicks(VeinAndStripMinerTests.DROP_MAX_TICKS)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("protection_and_range_game_test_kinetic_protection_scales_with_level_and_only_covers_its_own_damage_types", ProtectionAndRangeTests::kineticProtectionScalesWithLevelAndOnlyCoversItsOwnDamageTypes)
                    .build(),
            GameTestSpec.named("protection_and_range_game_test_kinetic_protection_actually_reduces_the_damage_the_player_takes", ProtectionAndRangeTests::kineticProtectionActuallyReducesTheDamageThePlayerTakes)
                    .build(),
            GameTestSpec.named("protection_and_range_game_test_range_adds_block_interaction_reach_in_the_main_hand_only", ProtectionAndRangeTests::rangeAddsBlockInteractionReachInTheMainHandOnly)
                    .build(),
            GameTestSpec.named("protection_and_range_game_test_void_protection_lifts_enderite_back_into_the_world_while_other_items_are_lost", ProtectionAndRangeTests::voidProtectionLiftsEnderiteBackIntoTheWorldWhileOtherItemsAreLost)
                    .maxTicks(ProtectionAndRangeTests.VOID_PROTECTION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_trim_counts_follow_the_pattern_and_material_matching", TrimEffectTests::trimCountsFollowThePatternAndMaterialMatching)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_damage_reduction_follows_the_pattern_and_keeps_its_floor", TrimEffectTests::damageReductionFollowsThePatternAndKeepsItsFloor)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_utility_bonuses_are_neutral_until_the_matching_trim_is_worn", TrimEffectTests::utilityBonusesAreNeutralUntilTheMatchingTrimIsWorn)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_benefit_gate_switches_every_trim_effect_off", TrimEffectTests::benefitGateSwitchesEveryTrimEffectOff)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_astralit_jump_boost_crosses_its_thresholds_on_tick", TrimEffectTests::astralitJumpBoostCrossesItsThresholdsOnTick)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_nihilith_pulls_down_the_sneaking_airborne_player", TrimEffectTests::nihilithPullsDownTheSneakingAirbornePlayer)
                    .build(),
            GameTestSpec.named("trim_effect_game_test_trim_bonuses_reach_the_player_through_the_mixins", TrimEffectTests::trimBonusesReachThePlayerThroughTheMixins)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_end_ore_features_carry_the_right_ore_block_and_vein_size", OreGenAndItemFrameTests::endOreFeaturesCarryTheRightOreBlockAndVeinSize)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_end_ore_placement_differs_between_astralit_and_nihilith", OreGenAndItemFrameTests::endOrePlacementDiffersBetweenAstralitAndNihilith)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_both_end_ores_reach_the_end_biomes_and_stay_out_of_the_overworld", OreGenAndItemFrameTests::bothEndOresReachTheEndBiomesAndStayOutOfTheOverworld)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_nihilith_placement_only_accepts_end_stone_undersides_and_lifts_the_origin", OreGenAndItemFrameTests::nihilithPlacementOnlyAcceptsEndStoneUndersidesAndLiftsTheOrigin)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_glass_pane_locks_the_frame_and_the_lock_survives_the_save_round_trip", OreGenAndItemFrameTests::glassPaneLocksTheFrameAndTheLockSurvivesTheSaveRoundTrip)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_shears_hide_the_frame_and_the_lock_takes_priority_over_them", OreGenAndItemFrameTests::shearsHideTheFrameAndTheLockTakesPriorityOverThem)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_survival_players_pay_for_the_lock_and_cannot_break_the_locked_frame", OreGenAndItemFrameTests::survivalPlayersPayForTheLockAndCannotBreakTheLockedFrame)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_constructors_touch_magnet_takes_its_filter_from_the_framed_item", OreGenAndItemFrameTests::constructorsTouchMagnetTakesItsFilterFromTheFramedItem)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_brush_reveal_is_wired_to_an_interface_nothing_implements", OreGenAndItemFrameTests::brushRevealIsWiredToAnInterfaceNothingImplements)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("config_option_game_test_bundle_click_inversion_follows_the_configured_option", ConfigOptionTests::bundleClickInversionFollowsTheConfiguredOption)
                    .build(),
            GameTestSpec.named("config_option_game_test_loot_table_changes_stop_when_the_option_is_switched_off", ConfigOptionTests::lootTableChangesStopWhenTheOptionIsSwitchedOff)
                    .build(),
            GameTestSpec.named("config_option_game_test_loot_balance_keeps_every_chest_within_its_budget", ConfigOptionTests::lootBalanceKeepsEveryChestWithinItsBudget)
                    .build(),
            GameTestSpec.named("config_option_game_test_every_config_option_keeps_its_persisted_name_and_default", ConfigOptionTests::everyConfigOptionKeepsItsPersistedNameAndDefault)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_two_level_counters_keep_their_own_storage_and_caps", DynamicLightTests::theTwoLevelCountersKeepTheirOwnStorageAndCaps)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_both_smithing_upgrades_add_one_level_per_step_and_stop_at_their_cap", DynamicLightTests::bothSmithingUpgradesAddOneLevelPerStepAndStopAtTheirCap)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_smithing_upgrade_only_fires_for_armour_and_the_matching_material", DynamicLightTests::theSmithingUpgradeOnlyFiresForArmourAndTheMatchingMaterial)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_smithing_table_takes_the_mod_templates_and_keeps_the_vanilla_ones", DynamicLightTests::theSmithingTableTakesTheModTemplatesAndKeepsTheVanillaOnes)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_worn_emission_levels_add_up_into_the_light_block_over_the_players_head", DynamicLightTests::wornEmissionLevelsAddUpIntoTheLightBlockOverThePlayersHead)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_light_block_only_replaces_air_or_water_sources_and_puts_the_water_back", DynamicLightTests::theLightBlockOnlyReplacesAirOrWaterSourcesAndPutsTheWaterBack)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_light_follows_the_player_and_goes_out_with_the_armour", DynamicLightTests::theLightFollowsThePlayerAndGoesOutWithTheArmour)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_server_tick_wiring_lights_the_wearer_on_its_own", DynamicLightTests::theServerTickWiringLightsTheWearerOnItsOwn)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_armour_stands_and_item_frames_light_their_block_and_clean_up", DynamicLightTests::armourStandsAndItemFramesLightTheirBlockAndCleanUp)
                    .maxTicks(DynamicLightTests.HOLDER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_mobs_wearing_radiant_armour_light_the_block_above_them", DynamicLightTests::mobsWearingRadiantArmourLightTheBlockAboveThem)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_the_radiance_tooltip_shows_only_the_level", DynamicLightTests::theRadianceTooltipShowsOnlyTheLevel)
                    .build(),
            GameTestSpec.named("dynamic_light_game_test_radiance_glints_stay_rare_and_full_sets_do_not_add_up", DynamicLightTests::radianceGlintsStayRareAndFullSetsDoNotAddUp)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_detector_reports_the_nearest_target_inside_its_budget", OreDetectorTests::detectorReportsTheNearestTargetInsideItsBudget)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_detector_modes_match_their_ore_tags", OreDetectorTests::detectorModesMatchTheirOreTags)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_dense_blocks_shorten_the_beam_more_than_soft_ones", OreDetectorTests::denseBlocksShortenTheBeamMoreThanSoftOnes)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_constructors_touch_doubles_the_reach_through_solid_rock", OreDetectorTests::constructorsTouchDoublesTheReachThroughSolidRock)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_sneak_clicking_calibrates_the_detector_and_plain_clicks_do_not", OreDetectorTests::sneakClickingCalibratesTheDetectorAndPlainClicksDoNot)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_mode_switch_is_free_in_creative_and_the_tool_stays_unstackable", OreDetectorTests::modeSwitchIsFreeInCreativeAndTheToolStaysUnstackable)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_tooltip_names_every_mode_with_its_power_and_target", OreDetectorTests::tooltipNamesEveryModeWithItsPowerAndTarget)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_all_ores_reach_follows_the_ore_rarity_and_radius_stretches_the_rare_ones", OreDetectorTests::allOresReachFollowsTheOreRarityAndRadiusStretchesTheRareOnes)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_open_air_reach_ends_at_the_range_of_each_ore_class", OreDetectorTests::openAirReachEndsAtTheRangeOfEachOreClass)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_calibrated_detector_glimmers_in_the_colour_of_its_target", OreDetectorTests::calibratedDetectorGlimmersInTheColourOfItsTarget)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_the_ore_detector_recipe_crafts_from_its_documented_pattern", OreDetectorTests::theOreDetectorRecipeCraftsFromItsDocumentedPattern)
                    .build(),
            GameTestSpec.named("quiver_game_test_right_clicks_do_nothing_even_with_master_builder", QuiverTests::rightClicksDoNothingEvenWithMasterBuilder)
                    .build(),
            GameTestSpec.named("quiver_game_test_arrow_filter_holds_for_clicks_and_the_inverted_binding_slips_past_it", QuiverTests::arrowFilterHoldsForClicksAndTheInvertedBindingSlipsPastIt)
                    .build(),
            GameTestSpec.named("quiver_game_test_capacity_drops_the_bundle_bonus_and_follows_tier_and_enchantments", QuiverTests::capacityDropsTheBundleBonusAndFollowsTierAndEnchantments)
                    .build(),
            GameTestSpec.named("quiver_game_test_bar_width_follows_the_same_capacity_the_filling_uses", QuiverTests::barWidthFollowsTheSameCapacityTheFillingUses)
                    .build(),
            GameTestSpec.named("quiver_game_test_bow_takes_the_topmost_arrow_and_searches_offhand_chest_hotbar_then_backpack", QuiverTests::bowTakesTheTopmostArrowAndSearchesOffhandChestHotbarThenBackpack)
                    .build(),
            GameTestSpec.named("quiver_game_test_bow_consumes_one_arrow_from_the_quiver_that_supplied_it", QuiverTests::bowConsumesOneArrowFromTheQuiverThatSuppliedIt)
                    .build(),
            GameTestSpec.named("quiver_game_test_bow_shoots_from_the_quiver_and_bills_it_outside_creative_only", QuiverTests::bowShootsFromTheQuiverAndBillsItOutsideCreativeOnly)
                    .build(),
            GameTestSpec.named("quiver_game_test_netherite_quiver_burns_in_an_explosion_while_the_netherite_bundle_survives", QuiverTests::netheriteQuiverBurnsInAnExplosionWhileTheNetheriteBundleSurvives)
                    .build(),
            GameTestSpec.named("rotator_game_test_log_axis_cycles_through_all_three_axes_and_ignores_sneaking", RotatorTests::logAxisCyclesThroughAllThreeAxesAndIgnoresSneaking)
                    .build(),
            GameTestSpec.named("rotator_game_test_rim_is_the_outer_eighth_of_every_face_and_nowhere_inside", RotatorTests::rimIsTheOuterEighthOfEveryFaceAndNowhereInside)
                    .build(),
            GameTestSpec.named("rotator_game_test_facing_blocks_turn_one_quarter_around_the_clicked_axis_or_jump_to_its_start", RotatorTests::facingBlocksTurnOneQuarterAroundTheClickedAxisOrJumpToItsStart)
                    .build(),
            GameTestSpec.named("rotator_game_test_rim_aims_facing_blocks_at_the_rim_its_opposite_or_the_next_valid_value", RotatorTests::rimAimsFacingBlocksAtTheRimItsOppositeOrTheNextValidValue)
                    .build(),
            GameTestSpec.named("rotator_game_test_sixteen_step_blocks_step_once_in_the_middle_and_four_times_at_the_rim", RotatorTests::sixteenStepBlocksStepOnceInTheMiddleAndFourTimesAtTheRim)
                    .build(),
            GameTestSpec.named("rotator_game_test_wears_out_at_its_rated_durability_and_takes_durability_enchantments", RotatorTests::wearsOutAtItsRatedDurabilityAndTakesDurabilityEnchantments)
                    .build(),
            GameTestSpec.named("rotator_game_test_crafting_takes_five_iron_and_one_ender_pearl_in_that_shape", RotatorTests::craftingTakesFiveIronAndOneEnderPearlInThatShape)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_only_runs_for_players_holding_it_and_stops_while_sneaking", MagnetTests::magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_in_the_off_hand_drags_loose_items_into_the_inventory", MagnetTests::magnetInTheOffHandDragsLooseItemsIntoTheInventory)
                    .maxTicks(MagnetTests.OFF_HAND_MAX_TICKS)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_pull_follows_the_acceleration_and_braking_curve", MagnetTests::magnetPullFollowsTheAccelerationAndBrakingCurve)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_reach_is_four_blocks_and_constructors_touch_widens_it", MagnetTests::magnetReachIsFourBlocksAndConstructorsTouchWidensIt)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_filter_matches_the_full_registry_id_and_nothing_else", MagnetTests::magnetFilterMatchesTheFullRegistryIdAndNothingElse)
                    .build(),
            GameTestSpec.named("magnet_game_test_sneak_right_click_clears_the_filter_and_the_tooltip_follows", MagnetTests::sneakRightClickClearsTheFilterAndTheTooltipFollows)
                    .build(),
            GameTestSpec.named("magnet_game_test_the_magnet_recipe_still_crafts_from_its_documented_pattern", MagnetTests::theMagnetRecipeStillCraftsFromItsDocumentedPattern)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_tag_keyed_patterns_cover_the_whole_damage_family", TrimBonusTests::tagKeyedPatternsCoverTheWholeDamageFamily)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_exactly_keyed_patterns_ignore_their_neighbours", TrimBonusTests::exactlyKeyedPatternsIgnoreTheirNeighbours)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_magic_is_softened_by_the_vex_pattern_and_the_gold_and_lapis_materials", TrimBonusTests::magicIsSoftenedByTheVexPatternAndTheGoldAndLapisMaterials)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_wild_and_silence_ride_on_the_damage_message_id", TrimBonusTests::wildAndSilenceRideOnTheDamageMessageId)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_flow_reads_the_type_name_of_the_projectile_that_landed", TrimBonusTests::flowReadsTheTypeNameOfTheProjectileThatLanded)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_armour_bypassing_hits_skip_the_three_physical_materials", TrimBonusTests::armourBypassingHitsSkipTheThreePhysicalMaterials)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_iron_and_quartz_materials_add_to_their_own_patterns", TrimBonusTests::ironAndQuartzMaterialsAddToTheirOwnPatterns)
                    .build(),
            GameTestSpec.named("trim_bonus_game_test_attacker_keyed_materials_read_the_entity_behind_the_hit", TrimBonusTests::attackerKeyedMaterialsReadTheEntityBehindTheHit)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_survival_factor_tracks_distance_and_time_since_the_last_death", TrimWiringTests::theSurvivalFactorTracksDistanceAndTimeSinceTheLastDeath)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_survival_time_counts_only_while_the_player_moves", TrimWiringTests::survivalTimeCountsOnlyWhileThePlayerMoves)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_combat_factor_weighs_kills_and_damage_by_mob_category", TrimWiringTests::theCombatFactorWeighsKillsAndDamageByMobCategory)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_tracker_survives_the_save_and_rebases_only_on_death", TrimWiringTests::theTrackerSurvivesTheSaveAndRebasesOnlyOnDeath)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_player_mixin_delivers_speed_hunger_and_experience_behind_its_guards", TrimWiringTests::thePlayerMixinDeliversSpeedHungerAndExperienceBehindItsGuards)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_every_server_side_hit_runs_through_the_trim_damage_modifier", TrimWiringTests::everyServerSideHitRunsThroughTheTrimDamageModifier)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_coast_holds_the_air_supply_and_silence_lowers_the_visibility", TrimWiringTests::coastHoldsTheAirSupplyAndSilenceLowersTheVisibility)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_tick_driven_trim_effects_fire_on_their_own_cadence", TrimWiringTests::theTickDrivenTrimEffectsFireOnTheirOwnCadence)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_three_trim_materials_keep_their_colours_and_their_tags", TrimWiringTests::theThreeTrimMaterialsKeepTheirColoursAndTheirTags)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_trim_multiplier_command_guards_its_range_and_its_permission", TrimWiringTests::theTrimMultiplierCommandGuardsItsRangeAndItsPermission)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_stops_at_the_brim_and_weighs_by_stack_size", ReinforcedBundleTests::insertionStopsAtTheBrimAndWeighsByStackSize)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_turns_away_what_cannot_go_into_container_items", ReinforcedBundleTests::insertionTurnsAwayWhatCannotGoIntoContainerItems)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_merges_equal_stacks_and_pushes_them_to_the_top", ReinforcedBundleTests::insertionMergesEqualStacksAndPushesThemToTheTop)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_drawer_caps_the_bundle_at_five_kinds", ReinforcedBundleTests::drawerCapsTheBundleAtFiveKinds)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_the_selected_entry_is_the_one_that_comes_out", ReinforcedBundleTests::theSelectedEntryIsTheOneThatComesOut)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_right_click_throws_the_selected_stack_but_never_blocks", ReinforcedBundleTests::rightClickThrowsTheSelectedStackButNeverBlocks)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_master_builder_places_from_the_bundle_and_color_palette_scatters_it", ReinforcedBundleTests::masterBuilderPlacesFromTheBundleAndColorPaletteScattersIt)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_capacity_follows_tier_and_enchantments_and_matches_the_wiki_export", ReinforcedBundleTests::capacityFollowsTierAndEnchantmentsAndMatchesTheWikiExport)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_bar_and_tooltip_read_the_same_capacity_the_filling_uses", ReinforcedBundleTests::barAndTooltipReadTheSameCapacityTheFillingUses)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_funnel_bundle_sweeps_up_drops_on_touch_unless_the_player_sneaks", BundleWiringTests::funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_netherite_bundle_on_the_ground_survives_fire_and_explosions", BundleWiringTests::netheriteBundleOnTheGroundSurvivesFireAndExplosions)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_bundle_packets_only_touch_the_slots_they_own", BundleWiringTests::bundlePacketsOnlyTouchTheSlotsTheyOwn)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_anvil_blanks_the_result_for_colour_palette_without_master_builder", BundleWiringTests::anvilBlanksTheResultForColourPaletteWithoutMasterBuilder)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_building_wand_builds_from_the_bundle_and_pays_one_piece_per_block", BundleWiringTests::buildingWandBuildsFromTheBundleAndPaysOnePiecePerBlock)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_bundle_recipes_craft_the_base_and_upgrade_it_tier_by_tier", BundleWiringTests::bundleRecipesCraftTheBaseAndUpgradeItTierByTier)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_wandering_trader_sells_and_buys_the_reinforced_bundle", BundleWiringTests::wanderingTraderSellsAndBuysTheReinforcedBundle)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_reinforced_bundle_sits_in_dungeon_shipwreck_and_mineshaft_loot", BundleWiringTests::reinforcedBundleSitsInDungeonShipwreckAndMineshaftLoot)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_container_enchantments_accept_the_bundles_they_are_meant_for", BundleWiringTests::containerEnchantmentsAcceptTheBundlesTheyAreMeantFor)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_field_skips_air_gaps_and_unbreakable_blocks", SledgehammerTests::sledgehammerFieldSkipsAirGapsAndUnbreakableBlocks)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_refuses_the_whole_field_when_the_origin_is_out_of_reach", SledgehammerTests::sledgehammerRefusesTheWholeFieldWhenTheOriginIsOutOfReach)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_bills_one_durability_per_block_and_two_for_the_wrong_tool", SledgehammerTests::sledgehammerBillsOneDurabilityPerBlockAndTwoForTheWrongTool)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_stops_the_swing_when_the_hammer_breaks", SledgehammerTests::sledgehammerStopsTheSwingWhenTheHammerBreaks)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_field_plane_and_depth_follow_the_look_direction", SledgehammerTests::sledgehammerFieldPlaneAndDepthFollowTheLookDirection)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_right_click_charges_only_on_blocks_it_can_reshape", SledgehammerTests::sledgehammerRightClickChargesOnlyOnBlocksItCanReshape)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_reshapes_full_blocks_stairs_and_slabs", SledgehammerTests::sledgehammerReshapesFullBlocksStairsAndSlabs)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_speed_and_block_count_scale_with_its_enchantments", SledgehammerTests::sledgehammerSpeedAndBlockCountScaleWithItsEnchantments)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_field_mines_three_times_slower_than_one_block", SledgehammerTests::sledgehammerFieldMinesThreeTimesSlowerThanOneBlock)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_sneaking_breaks_only_the_targeted_block", SledgehammerTests::sledgehammerSneakingBreaksOnlyTheTargetedBlock)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_charge_time_shortens_with_material_and_efficiency", SledgehammerTests::sledgehammerChargeTimeShortensWithMaterialAndEfficiency)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_turns_framed_trim_templates_glowing", SledgehammerTests::sledgehammerTurnsFramedTrimTemplatesGlowing)
                    .build(),
            GameTestSpec.named("chisel_game_test_spatula_runs_forward_while_sneaking_and_chisel_runs_backward", ChiselTests::spatulaRunsForwardWhileSneakingAndChiselRunsBackward)
                    .build(),
            GameTestSpec.named("chisel_game_test_tier_tables_are_inherited_upwards_and_shared_in_pairs", ChiselTests::tierTablesAreInheritedUpwardsAndSharedInPairs)
                    .build(),
            GameTestSpec.named("chisel_game_test_netherite_tier_cycles_the_nether_brick_family", ChiselTests::netheriteTierCyclesTheNetherBrickFamily)
                    .build(),
            GameTestSpec.named("chisel_game_test_cooldown_ticks_follow_the_tier_table", ChiselTests::cooldownTicksFollowTheTierTable)
                    .build(),
            GameTestSpec.named("chisel_game_test_shared_properties_survive_and_self_mappings_reorient", ChiselTests::sharedPropertiesSurviveAndSelfMappingsReorient)
                    .build(),
            GameTestSpec.named("chisel_game_test_intuitive_orientation_derives_the_edge_direction", ChiselTests::intuitiveOrientationDerivesTheEdgeDirection)
                    .build(),
            GameTestSpec.named("chisel_game_test_chiselled_pillars_and_stairs_take_the_click_orientation", ChiselTests::chiselledPillarsAndStairsTakeTheClickOrientation)
                    .build(),
            GameTestSpec.named("chisel_game_test_chisel_mines_at_half_material_speed", ChiselTests::chiselMinesAtHalfMaterialSpeed)
                    .build(),
            GameTestSpec.named("chisel_game_test_last_target_is_stored_and_shown_in_the_tooltip", ChiselTests::lastTargetIsStoredAndShownInTheTooltip)
                    .build(),
            GameTestSpec.named("chisel_game_test_smithing_upgrades_carry_wear_name_and_enchantments", ChiselTests::smithingUpgradesCarryWearNameAndEnchantments)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_vein_miner_spends_its_per_level_budget_and_stops_when_the_tool_breaks", MiningEnchantmentTests::veinMinerSpendsItsPerLevelBudgetAndStopsWhenTheToolBreaks)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_vein_miner_and_strip_miner_ignore_tools_and_blocks_outside_their_gates", MiningEnchantmentTests::veinMinerAndStripMinerIgnoreToolsAndBlocksOutsideTheirGates)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_strip_miner_digs_upwards_only_past_the_steep_pitch_threshold", MiningEnchantmentTests::stripMinerDigsUpwardsOnlyPastTheSteepPitchThreshold)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_strip_miner_divides_the_destroy_progress_per_level", MiningEnchantmentTests::stripMinerDividesTheDestroyProgressPerLevel)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_versatility_refuses_candidates_that_cannot_harvest_the_block", MiningEnchantmentTests::versatilityRefusesCandidatesThatCannotHarvestTheBlock)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_versatility_prefers_the_hammer_and_ranks_the_chisel_last", MiningEnchantmentTests::versatilityPrefersTheHammerAndRanksTheChiselLast)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_mining_enchantment_tags_hold_exactly_what_they_declare", MiningEnchantmentTests::miningEnchantmentTagsHoldExactlyWhatTheyDeclare)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_mining_enchantment_books_and_the_diamond_hammer_sit_in_their_loot_pools", MiningEnchantmentTests::miningEnchantmentBooksAndTheDiamondHammerSitInTheirLootPools)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_mining_pickaxe_trade_always_carries_an_enchantment_from_its_pool", MiningEnchantmentTests::miningPickaxeTradeAlwaysCarriesAnEnchantmentFromItsPool)
                    .build(),
            GameTestSpec.named("mining_enchantment_game_test_creative_tab_offers_every_mining_enchantment_book_at_max_level", MiningEnchantmentTests::creativeTabOffersEveryMiningEnchantmentBookAtMaxLevel)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_reinforced_piston_moves_eighteen_blocks_while_the_netherite_one_keeps_vanillas_twelve", GravityBlockTests::reinforcedPistonMovesEighteenBlocksWhileTheNetheriteOneKeepsVanillasTwelve)
                    .maxTicks(GravityBlockTests.PUSH_LIMIT_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_netherite_piston_breaks_only_what_the_signal_strength_can_afford", GravityBlockTests::netheritePistonBreaksOnlyWhatTheSignalStrengthCanAfford)
                    .maxTicks(GravityBlockTests.BREAK_THRESHOLD_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_mod_pistons_are_not_sticky_and_use_the_vanilla_head", GravityBlockTests::modPistonsAreNotStickyAndUseTheVanillaHead)
                    .maxTicks(GravityBlockTests.RETRACTION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_extended_mod_pistons_cannot_be_shoved_by_other_pistons", GravityBlockTests::extendedModPistonsCannotBeShovedByOtherPistons)
                    .maxTicks(GravityBlockTests.PISTON_VERSUS_PISTON_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_levitating_sand_leaves_on_vanillas_schedule_and_rises_on_its_curve", GravityBlockTests::levitatingSandLeavesOnVanillasScheduleAndRisesOnItsCurve)
                    .maxTicks(GravityBlockTests.RISE_CURVE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_levitating_sand_waits_under_the_ceiling_until_the_way_up_is_free", GravityBlockTests::levitatingSandWaitsUnderTheCeilingUntilTheWayUpIsFree)
                    .maxTicks(GravityBlockTests.CEILING_WAIT_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_blocked_landing_spots_drop_the_block_or_keep_it_flying", GravityBlockTests::blockedLandingSpotsDropTheBlockOrKeepItFlying)
                    .maxTicks(GravityBlockTests.BLOCKED_LANDING_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_suspended_sand_and_gravel_hold_items_and_stop_levitating_blocks", GravityBlockTests::suspendedSandAndGravelHoldItemsAndStopLevitatingBlocks)
                    .maxTicks(GravityBlockTests.COLLISION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_gravity_blocks_and_pistons_carry_their_registered_strength_and_tags", GravityBlockTests::gravityBlocksAndPistonsCarryTheirRegisteredStrengthAndTags)
                    .build(),
            GameTestSpec.named("gravity_block_game_test_gravity_block_recipes_craft_from_their_documented_patterns", GravityBlockTests::gravityBlockRecipesCraftFromTheirDocumentedPatterns)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_reinforced_pistons_push_one_unbreakable_only_when_the_redstone_block_pays", PistonBreachTests::reinforcedPistonsPushOneUnbreakableOnlyWhenTheRedstoneBlockPays)
                    .maxTicks(PistonBreachTests.PAID_PUSH_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_reinforced_breach_counts_towards_the_limit_and_only_reaches_the_front_block", PistonBreachTests::reinforcedBreachCountsTowardsTheLimitAndOnlyReachesTheFrontBlock)
                    .maxTicks(PistonBreachTests.LIMIT_MAX_TICKS)
                    .skyAccess(true)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_sticky_reinforced_piston_pushes_the_unbreakable_but_never_pulls_it_back", PistonBreachTests::stickyReinforcedPistonPushesTheUnbreakableButNeverPullsItBack)
                    .maxTicks(PistonBreachTests.STICKY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_netherite_piston_sacrifice_leaves_nothing_behind_and_needs_the_redstone_block", PistonBreachTests::netheritePistonSacrificeLeavesNothingBehindAndNeedsTheRedstoneBlock)
                    .maxTicks(PistonBreachTests.SACRIFICE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_enderite_piston_breaches_three_cells_skipping_air_and_stopping_at_immune_blocks", PistonBreachTests::enderitePistonBreachesThreeCellsSkippingAirAndStoppingAtImmuneBlocks)
                    .maxTicks(PistonBreachTests.ENDERITE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_breaking_the_head_of_mod_pistons_breaks_the_piston_too", PistonBreachTests::breakingTheHeadOfModPistonsBreaksThePistonToo)
                    .maxTicks(PistonBreachTests.HEAD_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_immune_blocks_never_move_or_break", PistonBreachTests::immuneBlocksNeverMoveOrBreak)
                    .maxTicks(PistonBreachTests.IMMUNE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_explosions_cannot_delete_unbreakable_blocks_while_they_move", PistonBreachTests::explosionsCannotDeleteUnbreakableBlocksWhileTheyMove)
                    .maxTicks(PistonBreachTests.EXPLOSION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_end_portal_frames_breach_only_while_their_config_option_is_on", PistonBreachTests::endPortalFramesBreachOnlyWhileTheirConfigOptionIsOn)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_reinforced_sticky_piston_crafts_from_slime_and_drops_itself", PistonBreachTests::reinforcedStickyPistonCraftsFromSlimeAndDropsItself)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_leather_sheet_takes_exactly_nine_leather", LeatherAndQuiverTests::leatherSheetTakesExactlyNineLeather)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_reinforced_quiver_crafts_from_the_plain_quiver_with_sheet_pebble_and_nugget", LeatherAndQuiverTests::reinforcedQuiverCraftsFromThePlainQuiverWithSheetPebbleAndNugget)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_netherite_quiver_smiths_only_from_the_reinforced_quiver", LeatherAndQuiverTests::netheriteQuiverSmithsOnlyFromTheReinforcedQuiver)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_upgrades_keep_contents_enchantments_and_name", LeatherAndQuiverTests::upgradesKeepContentsEnchantmentsAndName)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_reinforced_quiver_is_an_ordinary_tier_in_the_container_tags", LeatherAndQuiverTests::reinforcedQuiverIsAnOrdinaryTierInTheContainerTags)
                    .build(),
            GameTestSpec.named("backpack_game_test_right_click_wears_the_backpack_and_swaps_it_with_the_chestplate", BackpackTests::rightClickWearsTheBackpackAndSwapsItWithTheChestplate)
                    .build(),
            GameTestSpec.named("backpack_game_test_tiers_carry_their_armor_slot_count_and_slot_layout", BackpackTests::tiersCarryTheirArmorSlotCountAndSlotLayout)
                    .build(),
            GameTestSpec.named("backpack_game_test_contents_survive_the_reinforced_recipe_and_both_smithing_upgrades", BackpackTests::contentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades)
                    .build(),
            GameTestSpec.named("backpack_game_test_sneak_right_click_places_the_backpack_and_breaking_it_drops_everything", BackpackTests::sneakRightClickPlacesTheBackpackAndBreakingItDropsEverything)
                    .build(),
            GameTestSpec.named("backpack_game_test_open_key_opens_the_menu_only_for_the_worn_backpack", BackpackTests::openKeyOpensTheMenuOnlyForTheWornBackpack)
                    .build(),
            GameTestSpec.named("backpack_game_test_shift_click_from_the_hotbar_fills_inventory_and_backpack_as_one_storage", BackpackTests::shiftClickFromTheHotbarFillsInventoryAndBackpackAsOneStorage)
                    .build(),
            GameTestSpec.named("backpack_game_test_shift_click_sends_storage_to_the_hotbar_and_armor_to_its_slot", BackpackTests::shiftClickSendsStorageToTheHotbarAndArmorToItsSlot)
                    .build(),
            GameTestSpec.named("backpack_game_test_deep_pockets_raises_stack_limits_only_for_stackables", BackpackTests::deepPocketsRaisesStackLimitsOnlyForStackables)
                    .build(),
            GameTestSpec.named("backpack_game_test_funnel_pulls_picked_up_items_into_the_worn_backpack", BackpackTests::funnelPullsPickedUpItemsIntoTheWornBackpack)
                    .build(),
            GameTestSpec.named("backpack_game_test_master_builder_opens_the_backpack_only_when_the_backpack_carries_it", BackpackTests::masterBuilderOpensTheBackpackOnlyWhenTheBackpackCarriesIt)
                    .build(),
            GameTestSpec.named("backpack_game_test_constructors_touch_refills_the_empty_hand_from_the_worn_backpack", BackpackTests::constructorsTouchRefillsTheEmptyHandFromTheWornBackpack)
                    .build(),
            GameTestSpec.named("backpack_game_test_backpacks_take_their_four_enchantments_but_neither_drawer_nor_color_palette", BackpackTests::backpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette)
                    .build(),
            GameTestSpec.named("backpack_game_test_upper_tiers_survive_fire_and_explosions_and_lower_ones_spill_their_contents", BackpackTests::upperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents)
                    .build(),
            GameTestSpec.named("dyed_storage_game_test_dyeing_colours_every_backpack_and_bundle_and_keeps_its_components", DyedStorageTests::dyeingColoursEveryBackpackAndBundleAndKeepsItsComponents)
                    .build(),
            GameTestSpec.named("dyed_storage_game_test_water_cauldron_washes_only_the_dye_off_backpacks_and_bundles", DyedStorageTests::waterCauldronWashesOnlyTheDyeOffBackpacksAndBundles)
                    .build(),
            GameTestSpec.named("dyed_storage_game_test_the_dye_colour_reaches_the_backpack_menu_and_the_bundle_tooltip", DyedStorageTests::theDyeColourReachesTheBackpackMenuAndTheBundleTooltip)
                    .build(),
            GameTestSpec.named("hopper_game_test_redstone_power_stops_every_hopper_transfer", HopperTests::redstonePowerStopsEveryHopperTransfer)
                    .maxTicks(HopperTests.REDSTONE_LOCK_MAX_TICKS)
                    .build(),
            GameTestSpec.named("hopper_game_test_filter_items_are_stored_as_single_count_copies_and_can_be_cleared", HopperTests::filterItemsAreStoredAsSingleCountCopiesAndCanBeCleared)
                    .build(),
            GameTestSpec.named("hopper_game_test_the_mode_delegate_reads_and_writes_the_filter_mode", HopperTests::theModeDelegateReadsAndWritesTheFilterMode)
                    .build(),
            GameTestSpec.named("hopper_game_test_the_filter_learns_its_ghost_from_the_first_item_that_is_placed", HopperTests::theFilterLearnsItsGhostFromTheFirstItemThatIsPlaced)
                    .build(),
            GameTestSpec.named("hopper_game_test_hopper_configuration_survives_the_save_and_load_round_trip", HopperTests::hopperConfigurationSurvivesTheSaveAndLoadRoundTrip)
                    .build(),
            GameTestSpec.named("hopper_game_test_the_update_tag_carries_mode_and_filter_items_to_the_client", HopperTests::theUpdateTagCarriesModeAndFilterItemsToTheClient)
                    .build(),
            GameTestSpec.named("hopper_game_test_hopper_menu_opens_on_use_and_filter_clicks_never_store_the_item", HopperTests::hopperMenuOpensOnUseAndFilterClicksNeverStoreTheItem)
                    .build(),
            GameTestSpec.named("hopper_game_test_hopper_blocks_carry_their_registered_strength_sound_and_tags", HopperTests::hopperBlocksCarryTheirRegisteredStrengthSoundAndTags)
                    .build(),
            GameTestSpec.named("hopper_game_test_hopper_recipes_craft_from_their_documented_patterns", HopperTests::hopperRecipesCraftFromTheirDocumentedPatterns)
                    .build(),
            GameTestSpec.named("hopper_game_test_both_hoppers_drop_themselves_when_broken", HopperTests::bothHoppersDropThemselvesWhenBroken)
                    .maxTicks(HopperTests.HOPPER_DROP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_end_ores_drop_their_dust_and_follow_fortune_while_silk_touch_keeps_the_ore", WorldAndPlayerTests::endOresDropTheirDustAndFollowFortuneWhileSilkTouchKeepsTheOre)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_end_ore_blocks_keep_their_strength_light_and_diamond_tool_requirement", WorldAndPlayerTests::endOreBlocksKeepTheirStrengthLightAndDiamondToolRequirement)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_breaking_an_end_ore_awards_three_to_seven_experience", WorldAndPlayerTests::breakingAnEndOreAwardsThreeToSevenExperience)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_locked_frames_still_answer_the_magnet_while_other_sneak_clicks_fall_through", WorldAndPlayerTests::lockedFramesStillAnswerTheMagnetWhileOtherSneakClicksFallThrough)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_the_double_jump_enchantment_keeps_its_levels_weight_costs_and_boot_slot", WorldAndPlayerTests::theDoubleJumpEnchantmentKeepsItsLevelsWeightCostsAndBootSlot)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_the_server_only_credits_the_boots_for_an_air_jump", WorldAndPlayerTests::theServerOnlyCreditsTheBootsForAnAirJump)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_enderite_armour_swallows_void_damage_except_on_its_interval_tick", WorldAndPlayerTests::enderiteArmourSwallowsVoidDamageExceptOnItsIntervalTick)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_enderite_slow_fall_needs_two_pieces_falling_speed_and_the_jump_key", WorldAndPlayerTests::enderiteSlowFallNeedsTwoPiecesFallingSpeedAndTheJumpKey)
                    .build(),
            GameTestSpec.named("world_and_player_game_test_mod_loot_pools_keep_their_exact_count_and_the_air_jump_book_weights", WorldAndPlayerTests::modLootPoolsKeepTheirExactCountAndTheAirJumpBookWeights)
                    .build(),
            GameTestSpec.named("building_wand_game_test_off_hand_click_is_passed_on_and_the_wand_stops_outside_both_hands", BuildingWandTests::offHandClickIsPassedOnAndTheWandStopsOutsideBothHands)
                    .build(),
            GameTestSpec.named("building_wand_game_test_clicked_face_sets_the_plane_until_an_axis_mode_overrides_it", BuildingWandTests::clickedFaceSetsThePlaneUntilAnAxisModeOverridesIt)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_tier_caps_the_radius_setting_and_sizes_the_plane", BuildingWandTests::wandTierCapsTheRadiusSettingAndSizesThePlane)
                    .build(),
            GameTestSpec.named("building_wand_game_test_material_search_prefers_the_off_hand_and_only_master_builder_reaches_the_backpack", BuildingWandTests::materialSearchPrefersTheOffHandAndOnlyMasterBuilderReachesTheBackpack)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_armed_without_material_searches_again_and_palette_falls_back_to_stone", BuildingWandTests::wandArmedWithoutMaterialSearchesAgainAndPaletteFallsBackToStone)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_enchantments_only_stick_to_the_wands_in_their_item_tag", BuildingWandTests::wandEnchantmentsOnlyStickToTheWandsInTheirItemTag)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_recipes_craft_the_lower_tiers_and_forge_the_upper_ones", BuildingWandTests::wandRecipesCraftTheLowerTiersAndForgeTheUpperOnes)
                    .build(),
            GameTestSpec.named("building_wand_game_test_iron_wand_drops_in_the_mansion_and_the_diamond_wand_in_the_end_city", BuildingWandTests::ironWandDropsInTheMansionAndTheDiamondWandInTheEndCity)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_hunger_rates_follow_the_calibrated_tier_table", BuildingWandTests::wandHungerRatesFollowTheCalibratedTierTable)
                    .build(),
            GameTestSpec.named("building_wand_game_test_survival_wand_builds_cost_exhaustion_only_past_the_allowance", BuildingWandTests::survivalWandBuildsCostExhaustionOnlyPastTheAllowance)
                    .build(),
            GameTestSpec.named("building_wand_game_test_wand_hunger_cost_skips_creative_and_the_switched_off_option", BuildingWandTests::wandHungerCostSkipsCreativeAndTheSwitchedOffOption)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_master_builder_opens_the_backpack_and_the_bundles_inside_it_to_the_wand", WandEnchantmentTests::masterBuilderOpensTheBackpackAndTheBundlesInsideItToTheWand)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_master_builder_moves_the_preview_sources_the_same_way_it_moves_the_placement", WandEnchantmentTests::masterBuilderMovesThePreviewSourcesTheSameWayItMovesThePlacement)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_fast_chiseling_never_pushes_the_chisel_cooldown_below_one_tick", WandEnchantmentTests::fastChiselingNeverPushesTheChiselCooldownBelowOneTick)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_the_building_enchantments_reach_every_tool_whose_code_reads_them", WandEnchantmentTests::theBuildingEnchantmentsReachEveryToolWhoseCodeReadsThem)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_building_enchantment_books_sit_in_the_structure_chests_they_belong_to", WandEnchantmentTests::buildingEnchantmentBooksSitInTheStructureChestsTheyBelongTo)
                    .build(),
            GameTestSpec.named("wand_enchantment_game_test_librarian_book_trades_hand_out_only_the_enchantments_they_declare", WandEnchantmentTests::librarianBookTradesHandOutOnlyTheEnchantmentsTheyDeclare)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_funnel_filter_decides_what_the_touch_sweeps_up", StorageEnchantmentTests::funnelFilterDecidesWhatTheTouchSweepsUp)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_drawer_kind_cap_holds_against_the_funnel_too", StorageEnchantmentTests::drawerKindCapHoldsAgainstTheFunnelToo)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_funnel_quiver_sweeps_arrows_only_and_stops_at_its_brim", StorageEnchantmentTests::funnelQuiverSweepsArrowsOnlyAndStopsAtItsBrim)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_drawer_and_deep_pockets_multiply_on_the_same_container", StorageEnchantmentTests::drawerAndDeepPocketsMultiplyOnTheSameContainer)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_storage_books_sit_in_the_chests_they_are_meant_for", StorageEnchantmentTests::storageBooksSitInTheChestsTheyAreMeantFor)
                    .build(),
            GameTestSpec.named("storage_enchantment_game_test_loot_quivers_carry_one_of_the_container_enchantments", StorageEnchantmentTests::lootQuiversCarryOneOfTheContainerEnchantments)
                    .build(),
            GameTestSpec.named("octant_game_test_all_octant_colours_share_durability_and_their_paint", OctantTests::allOctantColoursShareDurabilityAndTheirPaint)
                    .build(),
            GameTestSpec.named("octant_game_test_air_clicks_only_reset_an_unlocked_octant_while_sneaking", OctantTests::airClicksOnlyResetAnUnlockedOctantWhileSneaking)
                    .build(),
            GameTestSpec.named("octant_game_test_shape_catalogue_is_fixed_and_the_chosen_shape_shows_in_the_name", OctantTests::shapeCatalogueIsFixedAndTheChosenShapeShowsInTheName)
                    .build(),
            GameTestSpec.named("octant_game_test_octant_tooltip_lists_the_lock_and_both_corners", OctantTests::octantTooltipListsTheLockAndBothCorners)
                    .build(),
            GameTestSpec.named("octant_game_test_octant_scroll_packets_only_ever_touch_the_main_hand", OctantTests::octantScrollPacketsOnlyEverTouchTheMainHand)
                    .build(),
            GameTestSpec.named("octant_game_test_dyeing_recipes_produce_every_coloured_octant", OctantTests::dyeingRecipesProduceEveryColouredOctant)
                    .build(),
            GameTestSpec.named("octant_game_test_water_cauldron_washes_the_colour_off_an_octant", OctantTests::waterCauldronWashesTheColourOffAnOctant)
                    .build(),
            GameTestSpec.named("octant_game_test_chest_octants_are_enchanted_in_the_two_dangerous_chests_only", OctantTests::chestOctantsAreEnchantedInTheTwoDangerousChestsOnly)
                    .build(),
            GameTestSpec.named("octant_game_test_the_octant_only_measures_and_places_nothing", OctantTests::theOctantOnlyMeasuresAndPlacesNothing)
                    .build(),
            GameTestSpec.named("octant_game_test_the_octant_recipe_crafts_from_its_documented_pattern", OctantTests::theOctantRecipeCraftsFromItsDocumentedPattern)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_round_trips_and_parses_the_spec_examples", BlueprintTests::codeRoundTripsAndParsesTheSpecExamples)
                    .build(),
            GameTestSpec.named("blueprint_game_test_size_limits_cap_the_code_and_map_wand_tiers", BlueprintTests::sizeLimitsCapTheCodeAndMapWandTiers)
                    .build(),
            GameTestSpec.named("blueprint_game_test_material_list_counts_items_sorted_by_amount", BlueprintTests::materialListCountsItemsSortedByAmount)
                    .build(),
            GameTestSpec.named("blueprint_game_test_material_list_counts_multi_item_blocks_by_their_state", BlueprintTests::materialListCountsMultiItemBlocksByTheirState)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_consumes_every_candle_and_pickle_it_needs", BlueprintTests::buildConsumesEveryCandleAndPickleItNeeds)
                    .build(),
            GameTestSpec.named("blueprint_game_test_cartography_table_scans_the_octant_selection", BlueprintTests::cartographyTableScansTheOctantSelection)
                    .build(),
            GameTestSpec.named("blueprint_game_test_scan_follows_the_octant_shape", BlueprintTests::scanFollowsTheOctantShape)
                    .build(),
            GameTestSpec.named("blueprint_game_test_large_scan_runs_over_several_ticks", BlueprintTests::largeScanRunsOverSeveralTicks)
                    .build(),
            GameTestSpec.named("blueprint_game_test_large_build_runs_over_several_ticks", BlueprintTests::largeBuildRunsOverSeveralTicks)
                    .build(),
            GameTestSpec.named("blueprint_game_test_examples_parse_fit_and_list_their_materials", BlueprintTests::examplesParseFitAndListTheirMaterials)
                    .build(),
            GameTestSpec.named("blueprint_game_test_block_search_finds_by_name_and_id", BlueprintTests::blockSearchFindsByNameAndId)
                    .build(),
            GameTestSpec.named("blueprint_game_test_cartography_table_copies_signed_blueprints", BlueprintTests::cartographyTableCopiesSignedBlueprints)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_needs_the_signed_blueprint", BlueprintTests::buildNeedsTheSignedBlueprint)
                    .build(),
            GameTestSpec.named("blueprint_game_test_edit_packets_save_immediately_and_survive_the_disconnect", BlueprintTests::editPacketsSaveImmediatelyAndSurviveTheDisconnect)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_grows_layer_by_layer_from_the_centre", BlueprintTests::buildGrowsLayerByLayerFromTheCentre)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_stops_when_the_wand_breaks", BlueprintTests::buildStopsWhenTheWandBreaks)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_stops_when_the_wand_leaves_the_main_hand", BlueprintTests::buildStopsWhenTheWandLeavesTheMainHand)
                    .build(),
            GameTestSpec.named("blueprint_game_test_missing_blocks_need_the_second_click", BlueprintTests::missingBlocksNeedTheSecondClick)
                    .build(),
            GameTestSpec.named("blueprint_game_test_missing_blocks_check_has_no_cap_and_runs_over_several_ticks", BlueprintTests::missingBlocksCheckHasNoCapAndRunsOverSeveralTicks)
                    .build(),
            GameTestSpec.named("blueprint_game_test_running_build_survives_logout_and_resumes_with_the_same_blueprint", BlueprintTests::runningBuildSurvivesLogoutAndResumesWithTheSameBlueprint)
                    .build(),
            GameTestSpec.named("blueprint_game_test_build_places_only_available_blocks_and_skips_existing_ones", BlueprintTests::buildPlacesOnlyAvailableBlocksAndSkipsExistingOnes)
                    .build(),
            GameTestSpec.named("blueprint_game_test_rotation_turns_the_build_and_the_scroll_packet_steps_it", BlueprintTests::rotationTurnsTheBuildAndTheScrollPacketStepsIt)
                    .build(),
            GameTestSpec.named("blueprint_game_test_wand_tier_refuses_blueprints_larger_than_its_cube", BlueprintTests::wandTierRefusesBlueprintsLargerThanItsCube)
                    .build(),
            GameTestSpec.named("blueprint_game_test_edit_packet_saves_signs_and_locks_the_blueprint", BlueprintTests::editPacketSavesSignsAndLocksTheBlueprint)
                    .build(),
            GameTestSpec.named("blueprint_game_test_recipe_crafts_one_blank_blueprint", BlueprintTests::recipeCraftsOneBlankBlueprint)
                    .build(),
            GameTestSpec.named("furnace_game_test_boost_only_runs_while_the_furnace_burns_and_cooks", FurnaceTests::boostOnlyRunsWhileTheFurnaceBurnsAndCooks)
                    .maxTicks(FurnaceTests.BOOST_GUARD_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_progress_cools_down_at_the_vanilla_rate_once_the_fuel_is_spent", FurnaceTests::progressCoolsDownAtTheVanillaRateOnceTheFuelIsSpent)
                    .maxTicks(FurnaceTests.COOL_DOWN_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_boost_never_pushes_cooking_progress_to_the_full_cook_time", FurnaceTests::boostNeverPushesCookingProgressToTheFullCookTime)
                    .maxTicks(FurnaceTests.COOK_CAP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_every_tier_opens_the_menu_of_its_vanilla_counterpart", FurnaceTests::everyTierOpensTheMenuOfItsVanillaCounterpart)
                    .build(),
            GameTestSpec.named("furnace_game_test_furnace_blocks_carry_their_registered_hardness_resistance_and_tags", FurnaceTests::furnaceBlocksCarryTheirRegisteredHardnessResistanceAndTags)
                    .build(),
            GameTestSpec.named("furnace_game_test_only_netherite_and_enderite_furnace_items_survive_lava", FurnaceTests::onlyNetheriteAndEnderiteFurnaceItemsSurviveLava)
                    .build(),
            GameTestSpec.named("furnace_game_test_all_nine_furnaces_drop_themselves_when_broken", FurnaceTests::allNineFurnacesDropThemselvesWhenBroken)
                    .maxTicks(FurnaceTests.DROP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_furnace_recipes_keep_their_book_category_and_reject_near_miss_grids", FurnaceTests::furnaceRecipesKeepTheirBookCategoryAndRejectNearMissGrids)
                    .build(),
            GameTestSpec.named("furnace_game_test_one_coal_feeds_several_netherite_smelts_where_vanilla_manages_one", FurnaceTests::oneCoalFeedsSeveralNetheriteSmeltsWhereVanillaManagesOne)
                    .maxTicks(FurnaceTests.FUEL_PARITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_every_tier_is_fed_and_emptied_by_vanilla_hoppers", FurnaceTests::everyTierIsFedAndEmptiedByVanillaHoppers)
                    .maxTicks(FurnaceTests.HOPPER_RIG_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_every_tier_is_fed_and_emptied_by_its_own_tier_of_hopper", FurnaceTests::everyTierIsFedAndEmptiedByItsOwnTierOfHopper)
                    .maxTicks(FurnaceTests.HOPPER_RIG_MAX_TICKS)
                    .build(),
            GameTestSpec.named("furnace_game_test_every_machine_offers_its_slots_to_pipes_through_the_loader_api", FurnaceTests::everyMachineOffersItsSlotsToPipesThroughTheLoaderApi)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_every_machine_climbs_from_reinforced_to_netherite_to_enderite", SledgehammerUpgradeTests::everyMachineClimbsFromReinforcedToNetheriteToEnderite)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_upgraded_furnace_keeps_cooking_the_same_item_at_the_new_pace", SledgehammerUpgradeTests::upgradedFurnaceKeepsCookingTheSameItemAtTheNewPace)
                    .maxTicks(SledgehammerUpgradeTests.FURNACE_PACE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_upgraded_hopper_keeps_its_items_filter_and_mode", SledgehammerUpgradeTests::upgradedHopperKeepsItsItemsFilterAndMode)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_creative_upgrade_keeps_the_nugget_and_the_hammer", SledgehammerUpgradeTests::creativeUpgradeKeepsTheNuggetAndTheHammer)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_refused_upgrades_never_start_the_hammer", SledgehammerUpgradeTests::refusedUpgradesNeverStartTheHammer)
                    .maxTicks(SledgehammerUpgradeTests.REFUSAL_MAX_TICKS)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_interrupted_upgrades_consume_no_nugget", SledgehammerUpgradeTests::interruptedUpgradesConsumeNoNugget)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_aborted_upgrade_keeps_its_blows_and_resumes_there", SledgehammerUpgradeTests::abortedUpgradeKeepsItsBlowsAndResumesThere)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_upgrade_progress_lasts_until_the_block_changes", SledgehammerUpgradeTests::upgradeProgressLastsUntilTheBlockChanges)
                    .maxTicks(SledgehammerUpgradeTests.PROGRESS_MAX_TICKS)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_hammer_draws_back_between_blows_and_hints_beforehand", SledgehammerUpgradeTests::hammerDrawsBackBetweenBlowsAndHintsBeforehand)
                    .build(),
            GameTestSpec.named("sledgehammer_upgrade_game_test_netherite_machine_recipes_and_their_unlocks_are_gone", SledgehammerUpgradeTests::netheriteMachineRecipesAndTheirUnlocksAreGone)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_upgrade_steps_name_the_weakest_hammer_that_works", InWorldExportTests::upgradeStepsNameTheWeakestHammerThatWorks)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_reshape_ticks_match_the_use_duration_of_every_hammer", InWorldExportTests::reshapeTicksMatchTheUseDurationOfEveryHammer)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_chisel_tables_follow_the_tool_tiers", InWorldExportTests::chiselTablesFollowTheToolTiers)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_jei_catalog_covers_every_in_world_entry", InWorldExportTests::jeiCatalogCoversEveryInWorldEntry)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_hopper_moves_an_item_every_tick", EnderiteMachineTests::enderiteHopperMovesAnItemEveryTick)
                    .maxTicks(EnderiteMachineTests.HOPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_furnaces_cook_eight_times_as_fast_as_vanilla", EnderiteMachineTests::enderiteFurnacesCookEightTimesAsFastAsVanilla)
                    .maxTicks(EnderiteMachineTests.FURNACE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_machine_items_are_fire_resistant_epic_and_void_protected", EnderiteMachineTests::enderiteMachineItemsAreFireResistantEpicAndVoidProtected)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_hopper_and_piston_drop_themselves_when_broken", EnderiteMachineTests::enderiteHopperAndPistonDropThemselvesWhenBroken)
                    .maxTicks(EnderiteMachineTests.DROP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_machines_fit_their_block_entity_types_and_titles", EnderiteMachineTests::enderiteMachinesFitTheirBlockEntityTypesAndTitles)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_gear_inherits_every_netherite_trait", EnderiteMachineTests::enderiteGearInheritsEveryNetheriteTrait)
                    .build(),
            GameTestSpec.named("enderite_machine_game_test_enderite_ingot_tier_drops_last_twice_as_long_as_vanilla", EnderiteMachineTests::enderiteIngotTierDropsLastTwiceAsLongAsVanilla)
                    .maxTicks(EnderiteMachineTests.LIFETIME_MAX_TICKS)
                    .build(),
            GameTestSpec.named("smelting_game_test_raw_enderite_blasts_for_an_hour_and_pays_ten_experience", SmeltingTests::rawEnderiteBlastsForAnHourAndPaysTenExperience)
                    .build(),
            GameTestSpec.named("smelting_game_test_long_cook_timers_survive_the_save_and_load_as_ints", SmeltingTests::longCookTimersSurviveTheSaveAndLoadAsInts)
                    .maxTicks(SmeltingTests.ROUND_TRIP_MAX_TICKS)
                    .build(),
            GameTestSpec.named("smelting_game_test_furnace_menu_scales_long_cooks_into_the_short_range", SmeltingTests::furnaceMenuScalesLongCooksIntoTheShortRange)
                    .build(),
            GameTestSpec.named("smelting_game_test_upper_tier_furnaces_pay_double_experience", SmeltingTests::upperTierFurnacesPayDoubleExperience)
                    .maxTicks(SmeltingTests.EXPERIENCE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("smelting_game_test_blast_furnace_bonus_pays_raw_metals_every_fourth_or_second_smelt", SmeltingTests::blastFurnaceBonusPaysRawMetalsEveryFourthOrSecondSmelt)
                    .maxTicks(SmeltingTests.BONUS_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_master_book_trade_draws_every_enchantment_in_its_pool", TradeOfferTests::masterBookTradeDrawsEveryEnchantmentInItsPool)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_weighted_enchant_honours_its_second_chance_setting", TradeOfferTests::weightedEnchantHonoursItsSecondChanceSetting)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_weighted_enchant_turns_plain_books_into_enchanted_books", TradeOfferTests::weightedEnchantTurnsPlainBooksIntoEnchantedBooks)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_weighted_enchant_ignores_pools_without_any_weight", TradeOfferTests::weightedEnchantIgnoresPoolsWithoutAnyWeight)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_spatulas_in_containers_survive_the_world_scan", TradeOfferTests::spatulasInContainersSurviveTheWorldScan)
                    .build(),
            GameTestSpec.named("trade_offer_game_test_no_recipe_references_the_legacy_spatulas", TradeOfferTests::noRecipeReferencesTheLegacySpatulas)
                    .build());

    private SimpleBuildingGameTests() {
    }

    /** Every test of the suite, in a stable order. */
    public static List<GameTestSpec> all() {
        return ALL;
    }

    /** Hands every test to {@code sink} as (id, spec) -- convenient for loader registries. */
    public static void forEach(BiConsumer<String, GameTestSpec> sink) {
        for (GameTestSpec spec : ALL) {
            sink.accept(spec.name(), spec);
        }
    }

    /**
     * Runs the body registered under {@code name}. For loader adapters that prefer going
     * through the catalogue instead of calling a body class directly.
     *
     * @throws IllegalArgumentException if no test is registered under that name
     */
    public static void run(String name, GameTestHelper helper) {
        for (GameTestSpec spec : ALL) {
            if (spec.name().equals(name)) {
                Consumer<GameTestHelper> body = spec.body();
                body.accept(helper);
                return;
            }
        }
        throw new IllegalArgumentException("no SimpleBuilding game test named " + name);
    }
}
