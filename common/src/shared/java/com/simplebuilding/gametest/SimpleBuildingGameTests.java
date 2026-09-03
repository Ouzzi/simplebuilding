package com.simplebuilding.gametest;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * The single, loader-neutral catalogue of every SimpleBuilding in-game test.
 *
 * <p>The test bodies themselves live in the {@code *Tests} classes next to this one and are
 * plain {@code static void name(GameTestHelper)} methods -- no loader annotation, no loader
 * import. This class binds each body to the runner parameters it needs (tick budget, sky
 * access, structure rotation), so every loader can register the same suite from the same
 * source instead of keeping its own copy.
 *
 * <p>Loader adapters:
 * <ul>
 *   <li>Fabric: the thin {@code *GameTest} classes in the Fabric module carry the
 *       {@code @GameTest} annotations and delegate into the shared bodies.</li>
 *   <li>NeoForge: iterate {@link #all()} and register one test instance per spec.</li>
 * </ul>
 *
 * <p>The names are exactly the ids Fabric derives from its adapter classes, so a report from
 * one loader can be compared line by line with a report from another.
 */
public final class SimpleBuildingGameTests {

    /** Namespace every test id is registered under. */
    public static final String MOD_ID = "simplebuilding";

    private static final List<GameTestSpec> ALL = List.of(
            GameTestSpec.named("smoke_game_test_mod_items_are_registered", SmokeTests::modItemsAreRegistered)
                    .build(),
            GameTestSpec.named("trade_registry_game_test_all_mod_trades_reach_the_registry", TradeRegistryTests::allModTradesReachTheRegistry)
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
            GameTestSpec.named("data_integrity_game_test_mod_enchantments_are_present_in_the_datapack_registry", DataIntegrityTests::modEnchantmentsArePresentInTheDatapackRegistry)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_enchantment_tags_resolve_to_the_expected_entries", DataIntegrityTests::modEnchantmentTagsResolveToTheExpectedEntries)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_void_protected_tag_is_language_independent", DataIntegrityTests::voidProtectedTagIsLanguageIndependent)
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
            GameTestSpec.named("trade_and_migration_game_test_all_mod_trades_are_loaded_into_the_datapack_registry", TradeAndMigrationTests::allModTradesAreLoadedIntoTheDatapackRegistry)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mod_trades_are_merged_into_the_vanilla_trade_pools", TradeAndMigrationTests::modTradesAreMergedIntoTheVanillaTradePools)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_profession_trade_sets_resolve_the_mod_trades", TradeAndMigrationTests::professionTradeSetsResolveTheModTrades)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_trade_definitions_produce_the_expected_offers", TradeAndMigrationTests::tradeDefinitionsProduceTheExpectedOffers)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mason_villager_can_roll_amod_trade", TradeAndMigrationTests::masonVillagerCanRollAModTrade)
                    .maxTicks(TradeAndMigrationTests.MASON_VILLAGER_MAX_TICKS)
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
            GameTestSpec.named("config_option_game_test_trade_switch_conditions_still_name_real_config_fields_on_both_loaders", ConfigOptionTests::tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders)
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
                    .maxTicks(DynamicLightTests.TICK_WIRING_MAX_TICKS)
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
