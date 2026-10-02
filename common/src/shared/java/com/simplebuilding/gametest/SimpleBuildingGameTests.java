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
            GameTestSpec.named("tweaks_game_test_boost_command_refuses_invalid_strength", HardenTests::boostCommandRefusesInvalidStrength).build(),
            GameTestSpec.named("tweaks_game_test_xp_and_launch_runtime_caps", HardenTests::xpAndLaunchRuntimeCaps).build(),
            GameTestSpec.named("tweaks_game_test_boost_nonfinite_and_wrong_equipment", HardenTests::boostNonfiniteAndWrongEquipment).build(),
            GameTestSpec.named("tweaks_game_test_all_defaults_unchanged_by_validation", HardenTests::allDefaultsUnchangedByValidation).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_teleporter_tier1warmup_ticks", HardenTests::hardenPadTuningTeleporterTier1WarmupTicks).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_teleporter_tier2warmup_ticks", HardenTests::hardenPadTuningTeleporterTier2WarmupTicks).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_teleporter_tier3warmup_ticks", HardenTests::hardenPadTuningTeleporterTier3WarmupTicks).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_launchpad_strength_multiplier", HardenTests::hardenPadTuningLaunchpadStrengthMultiplier).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_potion_pad_charge_step_ticks", HardenTests::hardenPadTuningPotionPadChargeStepTicks).build(),
            GameTestSpec.named("tweaks_game_test_harden_pad_tuning_potion_pad_cooldown_factor", HardenTests::hardenPadTuningPotionPadCooldownFactor).build(),
            GameTestSpec.named("tweaks_game_test_harden_commands_kill_command_radius", HardenTests::hardenCommandsKillCommandRadius).build(),
            GameTestSpec.named("tweaks_game_test_harden_optimization_xp_clump_radius", HardenTests::hardenOptimizationXpClumpRadius).build(),
            GameTestSpec.named("tweaks_game_test_harden_spawn_spawn_elytra_radius", HardenTests::hardenSpawnSpawnElytraRadius).build(),
            GameTestSpec.named("tweaks_game_test_harden_spawn_boost_strength", HardenTests::hardenSpawnBoostStrength).build(),
            GameTestSpec.named("tweaks_game_test_harden_laser_pointer_range", HardenTests::hardenLaserPointerRange).build(),
            GameTestSpec.named("tweaks_game_test_harden_balancing_echo_sounder_jump_cooldown_ticks", HardenTests::hardenBalancingEchoSounderJumpCooldownTicks).build(),
            GameTestSpec.named("tweaks_game_test_harden_balancing_echo_sounder_attempt_lock_ticks", HardenTests::hardenBalancingEchoSounderAttemptLockTicks).build(),
            GameTestSpec.named("tweaks_game_test_boost_packet_budget", HardenTests::boostPacketBudget).maxTicks(220).build(),
            GameTestSpec.named("tweaks_game_test_recipe_rename", HardenTests::recipeRename).build(),

            GameTestSpec.named("end_systems_game_test_redstone_recipes_yield_two", EndSystemsTests::redstoneRecipesYieldTwo).build(),
            GameTestSpec.named("end_systems_game_test_redstone_aliases_resolve_items_and_blocks", EndSystemsTests::redstoneAliasesResolveItemsAndBlocks).build(),
            GameTestSpec.named("end_systems_game_test_vault_shares_only_its_first_half_and_persists", EndSystemsTests::vaultSharesOnlyItsFirstHalfAndPersists).maxTicks(220).build(),
            GameTestSpec.named("end_systems_game_test_vault_opens_and_config_preserves_contents", EndSystemsTests::vaultOpensAndConfigPreservesContents).maxTicks(220).build(),
            GameTestSpec.named("end_systems_game_test_channels_stay_isolated_and_stop_at_fifteen", EndSystemsTests::channelsStayIsolatedAndStopAtFifteen).maxTicks(220).build(),
            GameTestSpec.named("end_systems_game_test_matching_lamps_and_config_limits", EndSystemsTests::matchingLampsAndConfigLimits).maxTicks(220).build(),
            GameTestSpec.named("smoke_game_test_mod_items_are_registered", SmokeTests::modItemsAreRegistered)
                    .build(),
            GameTestSpec.named("test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre", TestCentreTests::everyModItemAndBlockHasItsPlaceInTheTestCentre)
                    .build(),
            GameTestSpec.named("test_centre_game_test_the_whole_centre_builds_and_matches_its_plan", TestCentreTests::theWholeCentreBuildsAndMatchesItsPlan)
                    .build(),
            GameTestSpec.named("test_centre_game_test_broken_and_repaired_states_stand_side_by_side", TestCentreTests::brokenAndRepairedStatesStandSideBySide)
                    .build(),
            GameTestSpec.named("test_centre_game_test_every_creative_tab_has_its_item_browser_wall", TestCentreTests::everyCreativeTabHasItsItemBrowserWall)
                    .build(),
            GameTestSpec.named("test_centre_game_test_every_feature_station_sets_up_its_scenario", TestCentreTests::everyFeatureStationSetsUpItsScenario)
                    .build(),
            GameTestSpec.named("test_centre_game_test_command_blocks_are_isolated_and_every_station_has_its_give_button", TestCentreTests::commandBlocksAreIsolatedAndEveryStationHasItsGiveButton)
                    .build(),
            GameTestSpec.named("test_centre_game_test_each_button_runs_exactly_its_own_command_block", TestCentreTests::eachButtonRunsExactlyItsOwnCommandBlock)
                    .maxTicks(TestCentreTests.BUTTON_RUN_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_registry_game_test_all_mod_trades_reach_the_registry", TradeRegistryTests::allModTradesReachTheRegistry)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_metal_rods_attract_lightning_within_their_own_range", BlockBehaviourTests::metalRodsAttractLightningWithinTheirOwnRange)
                    .build(),
            GameTestSpec.named("block_behaviour_game_test_material_rods_craft_back_and_smith_upward", BlockBehaviourTests::materialRodsCraftBackAndSmithUpward)
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
            GameTestSpec.named("data_integrity_game_test_every_recipeless_mod_item_has_jei_info", DataIntegrityTests::everyRecipelessModItemHasJeiInfo)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_basic_upgrade_template_text_names_only_real_tools_and_materials", DataIntegrityTests::basicUpgradeTemplateTextNamesOnlyRealToolsAndMaterials)
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
            GameTestSpec.named("data_integrity_game_test_materials_tab_is_laid_out_in_rows", DataIntegrityTests::materialsTabIsLaidOutInRows)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_pads_tab_is_laid_out_in_rows_of_nine", DataIntegrityTests::padsTabIsLaidOutInRowsOfNine)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_building_blocks_tab_is_laid_out_in_rows", DataIntegrityTests::buildingBlocksTabIsLaidOutInRows)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_mod_item_has_its_place_in_the_search_tab", DataIntegrityTests::everyModItemHasItsPlaceInTheSearchTab)
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
            GameTestSpec.named("data_integrity_game_test_every_item_has_an_item_definition_whose_models_and_textures_exist", DataIntegrityTests::everyItemHasAnItemDefinitionWhoseModelsAndTexturesExist)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_vanilla_enchantment_has_its_own_book_model", DataIntegrityTests::everyVanillaEnchantmentHasItsOwnBookModel)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_vanilla_book_texture_follows_the_client_option", DataIntegrityTests::vanillaBookTextureFollowsTheClientOption)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_book_texture_follows_the_client_option", DataIntegrityTests::modBookTextureFollowsTheClientOption)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_visible_trim_icons_follow_the_client_options", DataIntegrityTests::visibleTrimIconsFollowTheClientOptions)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_enderite_gear_piece_upgrades_from_its_netherite_twin", DataIntegrityTests::everyEnderiteGearPieceUpgradesFromItsNetheriteTwin)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_enderite_item_is_in_the_enderite_items_tag", DataIntegrityTests::everyEnderiteItemIsInTheEnderiteItemsTag)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_legacy_spatulas_are_hidden_from_recipe_viewers", DataIntegrityTests::legacySpatulasAreHiddenFromRecipeViewers)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_enderite_horse_and_nautilus_armor_rank_one_step_above_netherite", DataIntegrityTests::enderiteHorseAndNautilusArmorRankOneStepAboveNetherite)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_every_player_facing_text_has_english_and_german_translations", DataIntegrityTests::everyPlayerFacingTextHasEnglishAndGermanTranslations)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_tool_names_carry_no_leftover_old_names", DataIntegrityTests::toolNamesCarryNoLeftoverOldNames)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_item_rarities_follow_the_family_scheme", DataIntegrityTests::modItemRaritiesFollowTheFamilyScheme)
                    .build(),
            GameTestSpec.named("data_integrity_game_test_mod_item_names_follow_the_family_patterns", DataIntegrityTests::modItemNamesFollowTheFamilyPatterns)
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
            GameTestSpec.named("chisel_game_test_enderite_tier_walks_the_end_stone_palettes", ChiselTests::enderiteTierWalksTheEndStonePalettes)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_all_mod_trades_are_loaded_into_the_datapack_registry", TradeAndMigrationTests::allModTradesAreLoadedIntoTheDatapackRegistry)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_mod_trades_are_merged_into_the_vanilla_trade_pools", TradeAndMigrationTests::modTradesAreMergedIntoTheVanillaTradePools)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_profession_trade_sets_resolve_the_mod_trades", TradeAndMigrationTests::professionTradeSetsResolveTheModTrades)
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
            GameTestSpec.named("trade_and_migration_game_test_cores_are_sold_only_by_the_wandering_trader_and_get_rarer_by_tier", TradeAndMigrationTests::coresAreSoldOnlyByTheWanderingTraderAndGetRarerByTier)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_legacy_spatulas_in_player_inventory_become_chisels", TradeAndMigrationTests::legacySpatulasInPlayerInventoryBecomeChisels)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_legacy_spatula_item_entity_is_rewritten_in_place", TradeAndMigrationTests::legacySpatulaItemEntityIsRewrittenInPlace)
                    .maxTicks(TradeAndMigrationTests.LEGACY_ITEM_ENTITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_legacy_spatula_above_the_build_limit_is_rewritten_too", TradeAndMigrationTests::legacySpatulaAboveTheBuildLimitIsRewrittenToo)
                    .maxTicks(TradeAndMigrationTests.LEGACY_ITEM_ENTITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("trade_and_migration_game_test_renamed_item_ids_still_load_as_the_renamed_items", TradeAndMigrationTests::renamedItemIdsStillLoadAsTheRenamedItems)
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
            GameTestSpec.named("network_handler_game_test_octant_packets_reject_far_corners_unknown_names_and_huge_scrolls", NetworkHandlerTests::octantPacketsRejectFarCornersUnknownNamesAndHugeScrolls)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("network_handler_game_test_master_builder_pick_takes_blocks_out_of_the_enchanted_bundle", NetworkHandlerTests::masterBuilderPickTakesBlocksOutOfTheEnchantedBundle)
                    .build(),
            GameTestSpec.named("network_handler_game_test_air_jump_is_refused_on_the_ground_and_granted_once_per_fall", NetworkHandlerTests::airJumpIsRefusedOnTheGroundAndGrantedOncePerFall)
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
            GameTestSpec.named("wand_mode_game_test_linear_while_sneaking_builds_the_line_away_from_the_clicked_face", WandModeTests::linearWhileSneakingBuildsTheLineAwayFromTheClickedFace)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_bridge_runs_from_the_block_underfoot_in_the_facing_direction", WandModeTests::bridgeRunsFromTheBlockUnderfootInTheFacingDirection)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_cover_only_grows_the_surface_of_the_clicked_kind", WandModeTests::coverOnlyGrowsTheSurfaceOfTheClickedKind)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_wand_sets_stairs_and_logs_like_the_player_would", WandModeTests::wandSetsStairsAndLogsLikeThePlayerWould)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_undo_takes_back_only_the_last_action_and_only_unchanged_blocks", WandModeTests::undoTakesBackOnlyTheLastActionAndOnlyUnchangedBlocks)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_in_the_off_hand_fills_its_shape_with_the_wand", WandModeTests::octantInTheOffHandFillsItsShapeWithTheWand)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_roof_mode_lays_stairs_towards_the_ridge_with_slabs_on_top", WandModeTests::roofModeLaysStairsTowardsTheRidgeWithSlabsOnTop)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_bridge_from_the_use_packet_starts_at_the_edge_of_the_floor_ahead", WandModeTests::bridgeFromTheUsePacketStartsAtTheEdgeOfTheFloorAhead)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_a_held_use_key_does_not_restart_the_running_linear_line", WandModeTests::aHeldUseKeyDoesNotRestartTheRunningLinearLine)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_bridge_also_starts_when_the_click_aims_across_the_gap", WandModeTests::bridgeAlsoStartsWhenTheClickAimsAcrossTheGap)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_linear_without_sneaking_also_builds_only_the_line", WandModeTests::linearWithoutSneakingAlsoBuildsOnlyTheLine)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_bridge_grows_from_the_edge_to_the_far_end_in_half_the_time", WandModeTests::bridgeGrowsFromTheEdgeToTheFarEndInHalfTheTime)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_core_game_test_building_cores_are_not_stackable", BuildingCoreTests::buildingCoresAreNotStackable)
                    .build(),
            GameTestSpec.named("building_core_game_test_every_core_recipe_crafts_with_one_core_per_slot", BuildingCoreTests::everyCoreRecipeCraftsWithOneCorePerSlot)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_animation_roll_follows_the_seventy_twenty_ten_weights", BuildingCoreTests::coreAnimationRollFollowsTheSeventyTwentyTenWeights)
                    .build(),
            GameTestSpec.named("building_core_game_test_right_clicking_the_core_plays_an_animation_and_starts_the_cooldown", BuildingCoreTests::rightClickingTheCorePlaysAnAnimationAndStartsTheCooldown)
                    .build(),
            GameTestSpec.named("building_core_game_test_right_clicking_stone_with_the_core_starts_the_one_second_cooldown", BuildingCoreTests::rightClickingStoneWithTheCoreStartsTheOneSecondCooldown)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_animations_are_server_timed_and_end_within_the_cooldown", BuildingCoreTests::coreAnimationsAreServerTimedAndEndWithinTheCooldown)
                    .maxTicks(BuildingCoreTests.ANIMATION_TEST_MAX_TICKS)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_ore_hosts_are_the_blocks_ores_generate_in", BuildingCoreTests::coreOreHostsAreTheBlocksOresGenerateIn)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_ore_tables_only_hold_the_hosts_ores_and_follow_their_weights", BuildingCoreTests::coreOreTablesOnlyHoldTheHostsOresAndFollowTheirWeights)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_ore_chance_climbs_from_copper_to_enderite", BuildingCoreTests::coreOreChanceClimbsFromCopperToEnderite)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_transmutation_turns_only_host_blocks_into_their_ores", BuildingCoreTests::coreTransmutationTurnsOnlyHostBlocksIntoTheirOres)
                    .build(),
            GameTestSpec.named("building_core_game_test_core_transmutation_shows_no_text", BuildingCoreTests::coreTransmutationShowsNoText)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_roof_mode_works_with_the_test_centre_kit_enderite_wand", WandModeTests::roofModeWorksWithTheTestCentreKitEnderiteWand)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_wand_skips_every_cell_the_player_may_not_build_on", WandModeTests::wandSkipsEveryCellThePlayerMayNotBuildOn)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_wand_neither_builds_nor_undoes_without_build_rights", WandModeTests::wandNeitherBuildsNorUndoesWithoutBuildRights)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_multipart_places_beds_doors_and_plants", WandModeTests::wandMultipartPlacesBedsDoorsAndPlants)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_multipart_refuses_blocked_or_protected_second_cells", WandModeTests::wandMultipartRefusesBlockedOrProtectedSecondCells)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_multipart_octant_refuses_without_spending", WandModeTests::wandMultipartOctantRefusesWithoutSpending)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_wand_supply_passes_over_stacks_with_components", WandModeTests::wandSupplyPassesOverStacksWithComponents)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_wand_places_game_master_blocks_only_for_operators", WandModeTests::wandPlacesGameMasterBlocksOnlyForOperators)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_undo_leaves_the_slab_that_was_doubled_since_standing", WandModeTests::undoLeavesTheSlabThatWasDoubledSinceStanding)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_fill_plans_lazily_and_refuses_unloaded_chunks", WandModeTests::octantFillPlansLazilyAndRefusesUnloadedChunks)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_fill_leaves_game_master_blocks_out_for_non_operators", WandModeTests::octantFillLeavesGameMasterBlocksOutForNonOperators)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_fill_check_counts_cells_on_layers_it_has_not_built_yet", WandModeTests::octantFillCheckCountsCellsOnLayersItHasNotBuiltYet)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_fill_needs_the_chunks_around_its_box_loaded", WandModeTests::octantFillNeedsTheChunksAroundItsBoxLoaded)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_octant_fill_honours_order_layers_hollow_and_the_tick_budget", WandModeTests::octantFillHonoursOrderLayersHollowAndTheTickBudget)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("wand_mode_game_test_undo_keeps_block_entity_contents_spread_faces_and_protected_cells", WandModeTests::undoKeepsBlockEntityContentsSpreadFacesAndProtectedCells)
                    .rotation(Rotation.NONE)
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
            GameTestSpec.named("building_enchantment_game_test_linear_builds_only_the_line_and_paces_it_with_the_line_delay", BuildingEnchantmentTests::linearBuildsOnlyTheLineAndPacesItWithTheLineDelay)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_constructors_touch_only_turns_orientation_and_needs_build_rights", BuildingEnchantmentTests::constructorsTouchOnlyTurnsOrientationAndNeedsBuildRights)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("building_enchantment_game_test_constructors_touch_updates_neighbours_and_passes_what_it_cannot_turn", BuildingEnchantmentTests::constructorsTouchUpdatesNeighboursAndPassesWhatItCannotTurn)
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
            GameTestSpec.named("vein_and_strip_miner_game_test_strip_miner_refund_never_repairs_an_unbreaking_pickaxe", VeinAndStripMinerTests::stripMinerRefundNeverRepairsAnUnbreakingPickaxe)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("vein_and_strip_miner_game_test_extra_blocks_skip_positions_the_player_may_not_interact_with", VeinAndStripMinerTests::extraBlocksSkipPositionsThePlayerMayNotInteractWith)
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
            GameTestSpec.named("fletching_game_test_each_tip_adds_its_damage_against_its_targets", FletchingTests::eachTipAddsItsDamageAgainstItsTargets)
                    .build(),
            GameTestSpec.named("fletching_game_test_shafts_and_fletchings_change_the_flight", FletchingTests::shaftsAndFletchingsChangeTheFlight)
                    .build(),
            GameTestSpec.named("fletching_game_test_rod_shafts_pierce_hit_harder_and_resist_fire", FletchingTests::rodShaftsPierceHitHarderAndResistFire)
                    .build(),
            GameTestSpec.named("fletching_game_test_the_table_makes_four_arrows_from_three_parts", FletchingTests::theTableMakesFourArrowsFromThreeParts)
                    .build(),
            GameTestSpec.named("fletching_game_test_material_buttons_only_move_items_they_find", FletchingTests::materialButtonsOnlyMoveItemsTheyFind)
                    .build(),
            GameTestSpec.named("fletching_game_test_right_clicking_the_fletching_table_opens_the_menu", FletchingTests::rightClickingTheFletchingTableOpensTheMenu)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_dimensional_scrap_is_enderite_gated_and_indestructible", OreGenAndItemFrameTests::dimensionalScrapIsEnderiteGatedAndIndestructible)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_sage_ore_generates_in_the_overworld_and_drops_only_with_silk_touch", OreGenAndItemFrameTests::sageOreGeneratesInTheOverworldAndDropsOnlyWithSilkTouch)
                    .build(),
            GameTestSpec.named("ore_gen_and_item_frame_game_test_the_sage_orb_gives_fifty_to_one_hundred_experience_after_six_ticks", OreGenAndItemFrameTests::theSageOrbGivesFiftyToOneHundredExperienceAfterSixTicks)
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
            GameTestSpec.named("config_option_game_test_building_cores_are_very_rare_in_loot_chests", ConfigOptionTests::buildingCoresAreVeryRareInLootChests)
                    .build(),
            GameTestSpec.named("config_option_game_test_trade_switch_conditions_still_name_real_config_fields_on_both_loaders", ConfigOptionTests::tradeSwitchConditionsStillNameRealConfigFieldsOnBothLoaders)
                    .build(),
            GameTestSpec.named("config_option_game_test_every_config_option_keeps_its_persisted_name_and_default", ConfigOptionTests::everyConfigOptionKeepsItsPersistedNameAndDefault)
                    .build(),
            GameTestSpec.named("config_option_game_test_every_option_has_name_tooltip_and_tab", ConfigOptionTests::everyOptionHasNameTooltipAndTab)
                    .build(),
            GameTestSpec.named("config_option_game_test_the_config_command_reaches_every_option", ConfigOptionTests::theConfigCommandReachesEveryOption)
                    .build(),
            GameTestSpec.named("config_option_game_test_new_tool_options_change_what_the_tools_do", ConfigOptionTests::newToolOptionsChangeWhatTheToolsDo)
                    .build(),
            GameTestSpec.named("config_option_game_test_new_pad_options_change_what_the_pads_do", ConfigOptionTests::newPadOptionsChangeWhatThePadsDo)
                    .build(),
            GameTestSpec.named("config_option_game_test_new_tweak_options_change_what_the_tweaks_do", ConfigOptionTests::newTweakOptionsChangeWhatTheTweaksDo)
                    .build(),
            GameTestSpec.named("config_option_game_test_core_loot_chance_follows_its_multiplier", ConfigOptionTests::coreLootChanceFollowsItsMultiplier)
                    .build(),
            GameTestSpec.named("config_option_game_test_the_air_jump_cooldown_travels_from_server_to_client", ConfigOptionTests::theAirJumpCooldownTravelsFromServerToClient)
                    .build(),
            GameTestSpec.named("air_jump_game_test_the_cooldown_is_twenty_seconds_at_level_one_and_ten_at_level_two", AirJumpTests::theCooldownIsTwentySecondsAtLevelOneAndTenAtLevelTwo)
                    .build(),
            GameTestSpec.named("air_jump_game_test_the_bar_follows_vanillas_experience_over_locator_rule", AirJumpTests::theBarFollowsVanillasExperienceOverLocatorRule)
                    .build(),
            GameTestSpec.named("air_jump_game_test_the_bar_fills_up_while_the_air_jump_recharges", AirJumpTests::theBarFillsUpWhileTheAirJumpRecharges)
                    .build(),
            GameTestSpec.named("air_jump_game_test_the_server_enforces_the_full_cooldown_across_landings", AirJumpTests::theServerEnforcesTheFullCooldownAcrossLandings)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_the_server_value_wins_over_the_client_file", ServerTuningTests::theServerValueWinsOverTheClientFile)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_every_speed_and_range_option_is_clamped", ServerTuningTests::everySpeedAndRangeOptionIsClamped)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_the_air_jump_switch_refuses_air_jumps", ServerTuningTests::theAirJumpSwitchRefusesAirJumps)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_chunk_loaders_idle_while_their_owner_is_offline", ServerTuningTests::chunkLoadersIdleWhileTheirOwnerIsOffline)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_disabled_features_lose_their_recipes", ServerTuningTests::disabledFeaturesLoseTheirRecipes)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_the_loot_multiplier_and_structure_switches_shape_the_mod_loot", ServerTuningTests::theLootMultiplierAndStructureSwitchesShapeTheModLoot)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_the_laser_switches_stop_what_the_beam_ignites", ServerTuningTests::theLaserSwitchesStopWhatTheBeamIgnites)
                    .build(),
            GameTestSpec.named("server_tuning_game_test_tuning_values_reach_the_tools_and_machines", ServerTuningTests::tuningValuesReachTheToolsAndMachines)
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
            GameTestSpec.named("ore_detector_game_test_detector_calibrated_in_either_minecraft_line_keeps_its_target", OreDetectorTests::detectorCalibratedInEitherMinecraftLineKeepsItsTarget)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_the_ore_detector_recipe_crafts_from_its_documented_pattern", OreDetectorTests::theOreDetectorRecipeCraftsFromItsDocumentedPattern)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_pings_cost_durability_only_when_they_find_something", OreDetectorTests::pingsCostDurabilityOnlyWhenTheyFindSomething)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_block_entities_cannot_be_calibrated_or_found", OreDetectorTests::blockEntitiesCannotBeCalibratedOrFound)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_scans_are_capped_per_server_tick", OreDetectorTests::scansAreCappedPerServerTick)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_the_needle_points_at_the_found_ore_and_glows_brighter_when_closer", OreDetectorTests::theNeedlePointsAtTheFoundOreAndGlowsBrighterWhenCloser)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_off_hand_detector_is_slower_quieter_and_fainter", OreDetectorTests::offHandDetectorIsSlowerQuieterAndFainter)
                    .build(),
            GameTestSpec.named("ore_detector_game_test_calibrated_detectors_lie_down_and_keep_searching", OreDetectorTests::calibratedDetectorsLieDownAndKeepSearching)
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
            GameTestSpec.named("quiver_game_test_netherite_quiver_survives_an_explosion_like_the_netherite_bundle", QuiverTests::netheriteQuiverSurvivesAnExplosionLikeTheNetheriteBundle)
                    .build(),
            GameTestSpec.named("quiver_game_test_crossbow_loads_from_the_quiver_and_bills_one_arrow", QuiverTests::crossbowLoadsFromTheQuiverAndBillsOneArrow)
                    .build(),
            GameTestSpec.named("quiver_game_test_a_quiver_inside_the_backpack_feeds_the_bow_only_with_master_builder", QuiverTests::aQuiverInsideTheBackpackFeedsTheBowOnlyWithMasterBuilder)
                    .build(),
            GameTestSpec.named("quiver_game_test_picked_up_arrows_go_into_the_quiver_only_with_funnel", QuiverTests::pickedUpArrowsGoIntoTheQuiverOnlyWithFunnel)
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
            GameTestSpec.named("rotator_game_test_charge_runs_down_but_the_rotator_never_breaks", RotatorTests::chargeRunsDownButTheRotatorNeverBreaks)
                    .build(),
            GameTestSpec.named("rotator_game_test_anvil_recharges_with_sixteen_ender_pearls_for_no_levels", RotatorTests::anvilRechargesWithSixteenEnderPearlsForNoLevels)
                    .build(),
            GameTestSpec.named("rotator_game_test_anvil_takes_unbreaking_but_refuses_mending", RotatorTests::anvilTakesUnbreakingButRefusesMending)
                    .build(),
            GameTestSpec.named("rotator_game_test_a_turn_queues_the_ender_echo_shortly_after_the_ratchet", RotatorTests::aTurnQueuesTheEnderEchoShortlyAfterTheRatchet)
                    .build(),
            GameTestSpec.named("rotator_game_test_crafting_takes_an_iron_core_four_iron_and_an_ender_pearl_in_that_shape", RotatorTests::craftingTakesAnIronCoreFourIronAndAnEnderPearlInThatShape)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_only_runs_for_players_holding_it_and_stops_while_sneaking", MagnetTests::magnetOnlyRunsForPlayersHoldingItAndStopsWhileSneaking)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_in_the_off_hand_drags_loose_items_into_the_inventory", MagnetTests::magnetInTheOffHandDragsLooseItemsIntoTheInventory)
                    .maxTicks(MagnetTests.OFF_HAND_MAX_TICKS)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_pull_follows_the_acceleration_and_braking_curve", MagnetTests::magnetPullFollowsTheAccelerationAndBrakingCurve)
                    .build(),
            GameTestSpec.named("magnet_game_test_magnet_reach_is_three_blocks_and_range_widens_it_up_to_its_cap", MagnetTests::magnetReachIsThreeBlocksAndRangeWidensItUpToItsCap)
                    .build(),
            GameTestSpec.named("magnet_game_test_the_filter_needs_constructors_touch_and_is_set_like_the_detector", MagnetTests::theFilterNeedsConstructorsTouchAndIsSetLikeTheDetector)
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
            GameTestSpec.named("trim_wiring_game_test_enderite_trim_turns_darker_on_enderite_armour", TrimWiringTests::enderiteTrimTurnsDarkerOnEnderiteArmour)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_trim_multiplier_command_guards_its_range_and_its_permission", TrimWiringTests::theTrimMultiplierCommandGuardsItsRangeAndItsPermission)
                    .build(),
            GameTestSpec.named("trim_wiring_game_test_the_trim_multiplier_command_saves_and_syncs_its_value", TrimWiringTests::theTrimMultiplierCommandSavesAndSyncsItsValue)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_stops_at_the_brim_and_weighs_by_stack_size", ReinforcedBundleTests::insertionStopsAtTheBrimAndWeighsByStackSize)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_turns_away_what_cannot_go_into_container_items", ReinforcedBundleTests::insertionTurnsAwayWhatCannotGoIntoContainerItems)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_insertion_merges_equal_stacks_and_pushes_them_to_the_top", ReinforcedBundleTests::insertionMergesEqualStacksAndPushesThemToTheTop)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_drawer_holds_only_the_kind_already_inside", ReinforcedBundleTests::drawerHoldsOnlyTheKindAlreadyInside)
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
            GameTestSpec.named("reinforced_bundle_game_test_nested_bundles_weigh_their_contents", ReinforcedBundleTests::nestedBundlesWeighTheirContents)
                    .build(),
            GameTestSpec.named("reinforced_bundle_game_test_overflowing_bundle_weights_are_refused_instead_of_crashing", ReinforcedBundleTests::overflowingBundleWeightsAreRefusedInsteadOfCrashing)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_funnel_bundle_sweeps_up_drops_on_touch_unless_the_player_sneaks", BundleWiringTests::funnelBundleSweepsUpDropsOnTouchUnlessThePlayerSneaks)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_netherite_bundle_on_the_ground_survives_fire_and_explosions", BundleWiringTests::netheriteBundleOnTheGroundSurvivesFireAndExplosions)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_bundle_packets_only_touch_the_slots_they_own", BundleWiringTests::bundlePacketsOnlyTouchTheSlotsTheyOwn)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_bundles_close_like_vanilla_when_picked_up_or_left", BundleWiringTests::bundlesCloseLikeVanillaWhenPickedUpOrLeft)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_anvil_repair_keeps_the_cost_unless_enchanting", BundleWiringTests::anvilRepairKeepsTheCostUnlessEnchanting)
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
            GameTestSpec.named("bundle_wiring_game_test_funnel_honours_pickup_delay_and_target_in_the_hand", BundleWiringTests::funnelHonoursPickupDelayAndTargetInTheHand)
                    .build(),
            GameTestSpec.named("bundle_wiring_game_test_funnel_pickup_counts_what_it_took", BundleWiringTests::funnelPickupCountsWhatItTook)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_corners_subtract_only_the_aimed_quarter", SledgehammerTests::sledgehammerCornersSubtractOnlyTheAimedQuarter).build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_transform_hints_cover_both_hands_without_side_effects", SledgehammerTests::sledgehammerTransformHintsCoverBothHandsWithoutSideEffects).build(),
            GameTestSpec.named("sledgehammer_game_test_transform_hint_partial_follows_the_upgrade_rules", SledgehammerTests::transformHintPartialFollowsTheUpgradeRules).build(),
            GameTestSpec.named("sledgehammer_game_test_transform_hint_shows_the_breaker_piston_repair", SledgehammerTests::transformHintShowsTheBreakerPistonRepair).build(),
            GameTestSpec.named("sledgehammer_game_test_transform_hint_covers_cauldron_wash_and_copper_plates", SledgehammerTests::transformHintCoversCauldronWashAndCopperPlates).build(),
            GameTestSpec.named("sledgehammer_game_test_transform_hint_skips_clicks_the_block_or_the_main_hand_takes", SledgehammerTests::transformHintSkipsClicksTheBlockOrTheMainHandTakes).build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_field_skips_air_gaps_and_unbreakable_blocks", SledgehammerTests::sledgehammerFieldSkipsAirGapsAndUnbreakableBlocks)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_refuses_the_whole_field_when_the_origin_is_out_of_reach", SledgehammerTests::sledgehammerRefusesTheWholeFieldWhenTheOriginIsOutOfReach)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_bills_two_durability_per_block_and_three_for_the_wrong_tool", SledgehammerTests::sledgehammerBillsTwoDurabilityPerBlockAndThreeForTheWrongTool)
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
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_area_mines_each_block_like_the_pickaxe_one_tier_below", SledgehammerTests::sledgehammerAreaMinesEachBlockLikeThePickaxeOneTierBelow)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_sneaking_breaks_only_the_targeted_block", SledgehammerTests::sledgehammerSneakingBreaksOnlyTheTargetedBlock)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_charge_time_shortens_with_material_and_efficiency", SledgehammerTests::sledgehammerChargeTimeShortensWithMaterialAndEfficiency)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_charged_hammer_only_finishes_on_the_block_it_started_on", SledgehammerTests::chargedHammerOnlyFinishesOnTheBlockItStartedOn)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_only_iron_or_better_sledgehammers_crush_diamond_blocks", SledgehammerTests::onlyIronOrBetterSledgehammersCrushDiamondBlocks)
                    .maxTicks(100)
                    .build(),
            GameTestSpec.named("sledgehammer_game_test_sledgehammer_breaks_the_octant_selection_at_twice_the_area_time_per_block", SledgehammerTests::sledgehammerBreaksTheOctantSelectionAtTwiceTheAreaTimePerBlock)
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
            GameTestSpec.named("chisel_game_test_smithing_table_takes_the_whole_addition_count", ChiselTests::smithingTableTakesTheWholeAdditionCount)
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
            GameTestSpec.named("gravity_block_game_test_mod_pistons_are_not_sticky_and_carry_their_own_head", GravityBlockTests::modPistonsAreNotStickyAndCarryTheirOwnHead)
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
            GameTestSpec.named("piston_breach_game_test_every_mod_piston_carries_the_head_of_its_tier_and_takes_it_back", PistonBreachTests::everyModPistonCarriesTheHeadOfItsTierAndTakesItBack)
                    .maxTicks(PistonBreachTests.OWN_HEAD_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_mod_piston_breaks_report_every_destroyed_block_for_particles_and_sound", PistonBreachTests::modPistonBreaksReportEveryDestroyedBlockForParticlesAndSound)
                    .maxTicks(PistonBreachTests.BORE_EFFECTS_MAX_TICKS)
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
            GameTestSpec.named("piston_breach_game_test_mod_pistons_ask_the_platform_guard_before_every_break", PistonBreachTests::modPistonsAskThePlatformGuardBeforeEveryBreak)
                    .maxTicks(PistonBreachTests.GUARD_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_netherite_breaker_loses_durability_and_crumbles_to_reinforced_piston", PistonBreachTests::netheriteBreakerLosesDurabilityAndCrumblesToReinforcedPiston)
                    .maxTicks(PistonBreachTests.DURABILITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_enderite_piston_loses_durability_and_crumbles_to_netherite_breaker", PistonBreachTests::enderitePistonLosesDurabilityAndCrumblesToNetheriteBreaker)
                    .maxTicks(PistonBreachTests.ENDERITE_DURABILITY_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_breaker_piston_durability_travels_with_the_item_as_its_durability_bar", PistonBreachTests::breakerPistonDurabilityTravelsWithTheItemAsItsDurabilityBar)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_mod_pistons_fire_the_real_loader_events_and_honour_their_config_switch", PistonBreachTests::modPistonsFireTheRealLoaderEventsAndHonourTheirConfigSwitch)
                    .maxTicks(PistonBreachTests.LOADER_EVENTS_MAX_TICKS)
                    .build(),
            GameTestSpec.named("piston_breach_game_test_only_vanilla_unbreakables_are_breached_unless_the_config_says_otherwise", PistonBreachTests::onlyVanillaUnbreakablesAreBreachedUnlessTheConfigSaysOtherwise)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_leather_sheet_takes_exactly_nine_leather", LeatherAndQuiverTests::leatherSheetTakesExactlyNineLeather)
                    .build(),
            GameTestSpec.named("leather_and_quiver_game_test_reinforced_quiver_crafts_from_the_plain_quiver_with_the_bundle_pattern_and_six_pebbles", LeatherAndQuiverTests::reinforcedQuiverCraftsFromThePlainQuiverWithTheBundlePatternAndSixPebbles)
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
            GameTestSpec.named("backpack_game_test_open_key_opens_the_worn_backpack_or_else_the_first_one_carried", BackpackTests::openKeyOpensTheWornBackpackOrElseTheFirstOneCarried)
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
            GameTestSpec.named("backpack_game_test_tooltip_image_carries_the_stored_items_of_every_tier_dyed_too", BackpackTests::tooltipImageCarriesTheStoredItemsOfEveryTierDyedToo)
                    .build(),
            GameTestSpec.named("backpack_game_test_backpack_key_locks_the_inventory_slot_of_the_backpack_it_opened", BackpackTests::backpackKeyLocksTheInventorySlotOfTheBackpackItOpened)
                    .build(),
            GameTestSpec.named("backpack_game_test_placed_backpack_outputs_its_fill_level_to_acomparator", BackpackTests::placedBackpackOutputsItsFillLevelToAComparator)
                    .build(),
            GameTestSpec.named("accessory_slot_game_test_backpack_lookup_goes_chest_then_accessory_then_inventory", AccessorySlotTests::backpackLookupGoesChestThenAccessoryThenInventory)
                    .build(),
            GameTestSpec.named("accessory_slot_game_test_backpack_key_opens_and_writes_back_the_accessory_backpack", AccessorySlotTests::backpackKeyOpensAndWritesBackTheAccessoryBackpack)
                    .build(),
            GameTestSpec.named("accessory_slot_game_test_quiver_lookup_goes_chest_then_accessory_then_hotbar", AccessorySlotTests::quiverLookupGoesChestThenAccessoryThenHotbar)
                    .build(),
            GameTestSpec.named("accessory_slot_game_test_master_builder_and_funnel_read_the_accessory_backpack", AccessorySlotTests::masterBuilderAndFunnelReadTheAccessoryBackpack)
                    .build(),
            GameTestSpec.named("accessory_slot_game_test_an_incompatible_accessory_mod_is_dropped_instead_of_crashing", AccessorySlotTests::anIncompatibleAccessoryModIsDroppedInsteadOfCrashing)
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
            GameTestSpec.named("hopper_game_test_filtered_slots_hand_out_what_they_hold_and_refuse_the_spawn_elytra", HopperTests::filteredSlotsHandOutWhatTheyHoldAndRefuseTheSpawnElytra)
                    .build(),
            GameTestSpec.named("hopper_game_test_mod_hoppers_fall_back_to_the_loader_transfer_api_without_container", HopperTests::modHoppersFallBackToTheLoaderTransferApiWithoutContainer)
                    .maxTicks(HopperTests.ITEM_AUTOMATION_MAX_TICKS)
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
            GameTestSpec.named("storage_enchantment_game_test_drawer_kind_lock_holds_against_the_funnel_too", StorageEnchantmentTests::drawerKindLockHoldsAgainstTheFunnelToo)
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
            GameTestSpec.named("octant_game_test_the_octant_is_rarer_in_chest_loot", OctantTests::theOctantIsRarerInChestLoot)
                    .build(),
            GameTestSpec.named("octant_game_test_the_octant_only_measures_and_places_nothing", OctantTests::theOctantOnlyMeasuresAndPlacesNothing)
                    .build(),
            GameTestSpec.named("octant_game_test_the_octant_recipe_crafts_from_its_documented_pattern", OctantTests::theOctantRecipeCraftsFromItsDocumentedPattern)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_round_trips_and_parses_the_spec_examples", BlueprintTests::codeRoundTripsAndParsesTheSpecExamples)
                    .build(),
            GameTestSpec.named("blueprint_game_test_size_limits_cap_the_code_and_map_wand_tiers", BlueprintTests::sizeLimitsCapTheCodeAndMapWandTiers)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_extensions_keep_existing_codes_identical", BlueprintTests::codeExtensionsKeepExistingCodesIdentical)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_shapes_fill_the_octant_figures", BlueprintTests::codeShapesFillTheOctantFigures)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_hollow_shapes_keep_only_the_shell", BlueprintTests::codeHollowShapesKeepOnlyTheShell)
                    .build(),
            GameTestSpec.named("blueprint_game_test_code_variables_compute_coordinates_sizes_and_counts", BlueprintTests::codeVariablesComputeCoordinatesSizesAndCounts)
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
            GameTestSpec.named("blueprint_game_test_survival_build_resets_grown_and_filled_states", BlueprintTests::survivalBuildResetsGrownAndFilledStates)
                    .build(),
            GameTestSpec.named("blueprint_game_test_failed_placement_hands_the_material_back", BlueprintTests::failedPlacementHandsTheMaterialBack)
                    .build(),
            GameTestSpec.named("blueprint_game_test_sign_packets_share_one_parse_budget_per_player", BlueprintTests::signPacketsShareOneParseBudgetPerPlayer)
                    .maxTicks(BlueprintTests.SIGN_BUDGET_MAX_TICKS)
                    .build(),
            GameTestSpec.named("blueprint_game_test_blueprint_builds_game_master_blocks_only_for_operators", BlueprintTests::blueprintBuildsGameMasterBlocksOnlyForOperators)
                    .build(),
            GameTestSpec.named("blueprint_game_test_survival_build_keeps_the_note_block_instrument", BlueprintTests::survivalBuildKeepsTheNoteBlockInstrument)
                    .build(),
            GameTestSpec.named("blueprint_game_test_repeated_clicks_neither_restart_the_check_nor_exceed_the_click_budget", BlueprintTests::repeatedClicksNeitherRestartTheCheckNorExceedTheClickBudget)
                    .build(),
            GameTestSpec.named("blueprint_game_test_blueprint_shows_its_state_in_its_texture", BlueprintTests::blueprintShowsItsStateInItsTexture)
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
            GameTestSpec.named("tiered_chest_game_test_single_chest_climbs_from_copper_to_enderite_keeping_its_contents", TieredChestTests::singleChestClimbsFromCopperToEnderiteKeepingItsContents)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_double_chest_upgrades_both_halves_together", TieredChestTests::doubleChestUpgradesBothHalvesTogether)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_slot_counts_and_stack_limits_follow_the_tier", TieredChestTests::slotCountsAndStackLimitsFollowTheTier)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_double_chests_form_only_from_equal_tiers", TieredChestTests::doubleChestsFormOnlyFromEqualTiers)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_vanilla_hoppers_fill_and_empty_oversized_slots", TieredChestTests::vanillaHoppersFillAndEmptyOversizedSlots)
                    .maxTicks(TieredChestTests.HOPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_comparator_reads_oversized_slots_against_the_tier_limit", TieredChestTests::comparatorReadsOversizedSlotsAgainstTheTierLimit)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_oversized_stacks_survive_saving_and_loading", TieredChestTests::oversizedStacksSurviveSavingAndLoading)
                    .build(),
            GameTestSpec.named("tiered_chest_game_test_chest_items_follow_the_family_scheme", TieredChestTests::chestItemsFollowTheFamilyScheme)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_vanilla_box_climbs_to_enderite_in_ten_blows_keeping_contents_and_color", TieredShulkerBoxTests::vanillaBoxClimbsToEnderiteInTenBlowsKeepingContentsAndColor)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_contents_and_oversized_stacks_survive_breaking_placing_and_burning", TieredShulkerBoxTests::contentsAndOversizedStacksSurviveBreakingPlacingAndBurning)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_oversized_stacks_and_color_survive_saving_and_loading", TieredShulkerBoxTests::oversizedStacksAndColorSurviveSavingAndLoading)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_hoppers_and_comparators_follow_the_tier_limit", TieredShulkerBoxTests::hoppersAndComparatorsFollowTheTierLimit)
                    .maxTicks(TieredShulkerBoxTests.RIG_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_dispensers_place_the_boxes", TieredShulkerBoxTests::dispensersPlaceTheBoxes)
                    .maxTicks(TieredShulkerBoxTests.RIG_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_boxes_do_not_nest", TieredShulkerBoxTests::boxesDoNotNest)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_boxes_are_dyed_crafted_and_washed", TieredShulkerBoxTests::boxesAreDyedCraftedAndWashed)
                    .build(),
            GameTestSpec.named("tiered_shulker_box_game_test_box_items_follow_the_family_scheme", TieredShulkerBoxTests::boxItemsFollowTheFamilyScheme)
                    .build(),
            GameTestSpec.named("block_info_game_test_launchpad_shows_its_charges_against_its_capacity", BlockInfoTests::launchpadShowsItsChargesAgainstItsCapacity)
                    .build(),
            GameTestSpec.named("block_info_game_test_pad_owner_is_named_by_the_server", BlockInfoTests::padOwnerIsNamedByTheServer)
                    .build(),
            GameTestSpec.named("block_info_game_test_potion_pad_shows_its_potion_and_cooldown", BlockInfoTests::potionPadShowsItsPotionAndCooldown)
                    .build(),
            GameTestSpec.named("block_info_game_test_chunk_loader_shows_how_many_chunks_it_holds", BlockInfoTests::chunkLoaderShowsHowManyChunksItHolds)
                    .build(),
            GameTestSpec.named("block_info_game_test_piston_durability_follows_the_block_state", BlockInfoTests::pistonDurabilityFollowsTheBlockState)
                    .build(),
            GameTestSpec.named("block_info_game_test_chest_slots_follow_the_tier_and_double_chests", BlockInfoTests::chestSlotsFollowTheTierAndDoubleChests)
                    .build(),
            GameTestSpec.named("block_info_game_test_hopper_filter_names_the_mode_and_items", BlockInfoTests::hopperFilterNamesTheModeAndItems)
                    .build(),
            GameTestSpec.named("block_info_game_test_furnace_speed_follows_the_tier", BlockInfoTests::furnaceSpeedFollowsTheTier)
                    .build(),
            GameTestSpec.named("block_info_game_test_lines_survive_the_server_data_tag", BlockInfoTests::linesSurviveTheServerDataTag)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_upgrade_steps_name_the_weakest_hammer_that_works", InWorldExportTests::upgradeStepsNameTheWeakestHammerThatWorks)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_reshape_ticks_match_the_use_duration_of_every_hammer", InWorldExportTests::reshapeTicksMatchTheUseDurationOfEveryHammer)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_chisel_tables_follow_the_tool_tiers", InWorldExportTests::chiselTablesFollowTheToolTiers)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_jei_catalog_covers_every_in_world_entry", InWorldExportTests::jeiCatalogCoversEveryInWorldEntry)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_mob_drop_catalog_has_every_head_and_the_discs", InWorldExportTests::mobDropCatalogHasEveryHeadAndTheDiscs)
                    .build(),
            GameTestSpec.named("in_world_export_game_test_mob_drop_catalog_matches_the_game", InWorldExportTests::mobDropCatalogMatchesTheGame)
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
            GameTestSpec.named("enderite_machine_game_test_every_enderite_item_drop_lasts_twice_as_long_as_vanilla", EnderiteMachineTests::everyEnderiteItemDropLastsTwiceAsLongAsVanilla)
                    .maxTicks(EnderiteMachineTests.LIFETIME_MAX_TICKS)
                    .build(),
            GameTestSpec.named("smelting_game_test_layered_raw_enderite_blasts_for_two_hours_into_one_scrap", SmeltingTests::layeredRawEnderiteBlastsForTwoHoursIntoOneScrap)
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
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_fixtures_are_what_twenty_six_two_writes", WorldUpgradeTests::fixturesAreWhatTwentySixTwoWrites)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_mod_block_entities_survive_the_upgrade", WorldUpgradeTests::modBlockEntitiesSurviveTheUpgrade)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_mod_items_survive_the_upgrade", WorldUpgradeTests::modItemsSurviveTheUpgrade)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_mod_entities_survive_the_upgrade", WorldUpgradeTests::modEntitiesSurviveTheUpgrade)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_player_data_survives_the_upgrade", WorldUpgradeTests::playerDataSurvivesTheUpgrade)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_saved_data_survives_the_upgrade", WorldUpgradeTests::savedDataSurvivesTheUpgrade)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_wand_mid_build_from_an_older_version_stops_instead_of_building_on_with_shifted_ids", WorldUpgradeTests::wandMidBuildFromAnOlderVersionStopsInsteadOfBuildingOnWithShiftedIds)
                    .build(),
            GameTestSpec.named("world_upgrade_game_test_backpack_with_an_unreadable_entry_keeps_the_rest", WorldUpgradeTests::backpackWithAnUnreadableEntryKeepsTheRest)
                    .build(),
            // --- tweaks (generated) ---
            GameTestSpec.named("tweaks_game_test_pad_tiers_grow_and_enderite_sits_between_netherite_and_the_nether_star_tier", TweaksTests::padTiersGrowAndEnderiteSitsBetweenNetheriteAndTheNetherStarTier)
                    .build(),
            GameTestSpec.named("tweaks_game_test_enderite_tiers_are_smithed_from_the_netherite_tier_and_the_nether_star_tiers_from_enderite", TweaksTests::enderiteTiersAreSmithedFromTheNetheriteTierAndTheNetherStarTiersFromEnderite)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_stellar_flypad_is_smithed_from_two_reinforced_flypads", TweaksTests::theStellarFlypadIsSmithedFromTwoReinforcedFlypads)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_echo_sounder_is_crafted_from_the_recovery_compass_the_enderite_core_and_seven_enderite_nuggets", TweaksTests::theEchoSounderIsCraftedFromTheRecoveryCompassTheEnderiteCoreAndSevenEnderiteNuggets)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_velocity_gauge_is_crafted_from_amethyst_copper_nuggets_and_the_copper_core", TweaksTests::theVelocityGaugeIsCraftedFromAmethystCopperNuggetsAndTheCopperCore)
                    .build(),
            GameTestSpec.named("tweaks_game_test_elytra_pads_equip_an_unsafe_spawn_elytra_in_their_area", TweaksTests::elytraPadsEquipAnUnsafeSpawnElytraInTheirArea)
                    .build(),
            GameTestSpec.named("tweaks_game_test_elytra_pads_recharge_boosts_in_the_column_and_from_enderite_on_in_the_whole_area", TweaksTests::elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_spawn_elytra_boost_spends_one_charge_and_only_while_gliding", TweaksTests::theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding)
                    .build(),
            GameTestSpec.named("tweaks_game_test_safe_spawn_elytras_and_enderite_launches_prevent_fall_damage", TweaksTests::safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage)
                    .build(),
            GameTestSpec.named("tweaks_game_test_pad_elytras_expire_even_with_the_spawn_elytra_switched_off", TweaksTests::padElytrasExpireEvenWithTheSpawnElytraSwitchedOff)
                    .build(),
            GameTestSpec.named("tweaks_game_test_flypads_grant_flight_inside_and_take_it_back_outside", TweaksTests::flypadsGrantFlightInsideAndTakeItBackOutside)
                    .build(),
            GameTestSpec.named("tweaks_game_test_enderite_flypads_catch_flyers_leaving_their_area_with_slow_falling", TweaksTests::enderiteFlypadsCatchFlyersLeavingTheirAreaWithSlowFalling)
                    .build(),
            GameTestSpec.named("tweaks_game_test_switched_off_flypads_grant_no_flight", TweaksTests::switchedOffFlypadsGrantNoFlight)
                    .build(),
            GameTestSpec.named("tweaks_game_test_spawn_teleporters_send_still_players_to_their_spawn_point", TweaksTests::spawnTeleportersSendStillPlayersToTheirSpawnPoint)
                    .maxTicks(TweaksTests.TELEPORTER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tweaks_game_test_spawn_teleporter_targets_fall_back_to_the_world_spawn", TweaksTests::spawnTeleporterTargetsFallBackToTheWorldSpawn)
                    .build(),
            GameTestSpec.named("tweaks_game_test_copper_plates_wait_longer_the_more_they_oxidized", TweaksTests::copperPlatesWaitLongerTheMoreTheyOxidized)
                    .maxTicks(TweaksTests.COPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tweaks_game_test_copper_plates_oxidize_in_order_and_the_axe_scrapes_them_back", TweaksTests::copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack)
                    .build(),
            GameTestSpec.named("tweaks_game_test_diamond_pressure_plates_react_to_players_only", TweaksTests::diamondPressurePlatesReactToPlayersOnly)
                    .maxTicks(TweaksTests.COPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tweaks_game_test_netherite_plates_admit_only_holders_of_barrel_items", TweaksTests::netheritePlatesAdmitOnlyHoldersOfBarrelItems)
                    .build(),
            GameTestSpec.named("tweaks_game_test_enderite_plates_lock_to_their_owner_and_named_tags", TweaksTests::enderitePlatesLockToTheirOwnerAndNamedTags)
                    .build(),
            GameTestSpec.named("tweaks_game_test_owners_break_their_pads_fast_and_strangers_slowly", TweaksTests::ownersBreakTheirPadsFastAndStrangersSlowly)
                    .build(),
            GameTestSpec.named("tweaks_game_test_placing_pads_makes_the_placer_the_owner", TweaksTests::placingPadsMakesThePlacerTheOwner)
                    .build(),
            GameTestSpec.named("tweaks_game_test_chunk_loaders_force_their_chunks_and_release_only_their_own", TweaksTests::chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_spawn_teleporter_tiers_wait_fifty_twenty_and_five_seconds", PadOverhaulTests::spawnTeleporterTiersWaitFiftyTwentyAndFiveSeconds)
                    .maxTicks(PadOverhaulTests.WAIT_MAX_TICKS)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_old_spawn_teleporters_become_their_new_tier_in_the_world_and_the_inventory", PadOverhaulTests::oldSpawnTeleportersBecomeTheirNewTierInTheWorldAndTheInventory)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_tier_one_of_every_pad_family_is_smithed_from_its_plate_and_unlock_item", PadOverhaulTests::tierOneOfEveryPadFamilyIsSmithedFromItsPlateAndUnlockItem)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_pads_and_gadgets_write_no_text_on_the_screen", PadOverhaulTests::padsAndGadgetsWriteNoTextOnTheScreen)
                    .maxTicks(PadOverhaulTests.NO_TEXT_MAX_TICKS)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_the_echo_sounder_cooldown_is_four_times_longer", PadOverhaulTests::theEchoSounderCooldownIsFourTimesLonger)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_the_echo_sounder_does_not_relink_the_lodestone_it_is_linked_to", PadOverhaulTests::theEchoSounderDoesNotRelinkTheLodestoneItIsLinkedTo)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_redstone_switches_launchpads_and_flypads_off_and_comparators_read_them", PadOverhaulTests::redstoneSwitchesLaunchpadsAndFlypadsOffAndComparatorsReadThem)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_hoppers_fill_only_wind_charges_into_the_launchpad", PadOverhaulTests::hoppersFillOnlyWindChargesIntoTheLaunchpad)
                    .maxTicks(PadOverhaulTests.HOPPER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_the_spawn_teleporter_takes_players_to_their_bed_and_with_redstone_to_the_world_spawn", PadOverhaulTests::theSpawnTeleporterTakesPlayersToTheirBedAndWithRedstoneToTheWorldSpawn)
                    .maxTicks(PadOverhaulTests.DESTINATION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_spawn_teleporter_signal_changes_reset_the_entire_warmup", PadOverhaulTests::spawnTeleporterSignalChangesResetTheEntireWarmup)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_the_echo_sounder_locks_for_up_to_five_seconds_after_an_attempt_depending_on_the_distance", PadOverhaulTests::theEchoSounderLocksForUpToFiveSecondsAfterAnAttemptDependingOnTheDistance)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_a_wrong_nugget_on_the_sledgehammer_writes_nothing_and_does_not_tilt", PadOverhaulTests::aWrongNuggetOnTheSledgehammerWritesNothingAndDoesNotTilt)
                    .build(),
            GameTestSpec.named("pad_overhaul_game_test_every_pad_family_shows_whether_it_is_working", PadOverhaulTests::everyPadFamilyShowsWhetherItIsWorking)
                    .maxTicks(PadOverhaulTests.STATE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_launchpad_tiers_hold_four_eight_and_sixteen_wind_charges", TweaksTierTests::launchpadTiersHoldFourEightAndSixteenWindCharges)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_sneaking_with_wind_charges_loads_the_whole_hand_up_to_capacity", TweaksTierTests::sneakingWithWindChargesLoadsTheWholeHandUpToCapacity)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_launch_strength_per_charge_is_doubled_so_full_tiers_launch_like_twice_the_old_charges", TweaksTierTests::launchStrengthPerChargeIsDoubledSoFullTiersLaunchLikeTwiceTheOldCharges)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_old_launchpads_drop_the_charges_their_tier_no_longer_holds", TweaksTierTests::oldLaunchpadsDropTheChargesTheirTierNoLongerHolds)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_chunk_loader_tiers_force_one_five_and_nine_chunks", TweaksTierTests::chunkLoaderTiersForceOneFiveAndNineChunks)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_a_netherite_chunk_loader_takes_over_only_the_chunks_of_its_cross", TweaksTierTests::aNetheriteChunkLoaderTakesOverOnlyTheChunksOfItsCross)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_pad_upgrades_pay_with_the_pressure_plate_of_their_target_material", TweaksTierTests::padUpgradesPayWithThePressurePlateOfTheirTargetMaterial)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_elytra_pad_and_flypad_areas_match_their_tiers", TweaksTierTests::elytraPadAndFlypadAreasMatchTheirTiers)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_old_flypads_turn_into_their_new_tier_in_the_world_and_in_the_inventory", TweaksTierTests::oldFlypadsTurnIntoTheirNewTierInTheWorldAndInTheInventory)
                    .build(),
            GameTestSpec.named("tweaks_tier_game_test_every_family_names_its_last_tier", TweaksTierTests::everyFamilyNamesItsLastTier)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_the_last_tier_smiths_back_into_dont_do_it_that_works_like_tier_one", TweaksEasterTests::theLastTierSmithsBackIntoDontDoItThatWorksLikeTierOne)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_easter_chain_names_every_stage_and_costs_what_the_normal_tiers_cost", TweaksEasterTests::easterChainNamesEveryStageAndCostsWhatTheNormalTiersCost)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_the_final_easter_pad_is_twice_as_strong_as_the_last_tier", TweaksEasterTests::theFinalEasterPadIsTwiceAsStrongAsTheLastTier)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_the_easter_advancements_are_hidden_and_fire_along_the_chain", TweaksEasterTests::theEasterAdvancementsAreHiddenAndFireAlongTheChain)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_the_funny_stick_is_smithed_from_the_final_pad_and_sparkles_in_the_hand", TweaksEasterTests::theFunnyStickIsSmithedFromTheFinalPadAndSparklesInTheHand)
                    .build(),
            GameTestSpec.named("tweaks_easter_game_test_the_easter_eggs_are_hidden_from_recipe_viewers_and_creative_tabs", TweaksEasterTests::theEasterEggsAreHiddenFromRecipeViewersAndCreativeTabs)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_the_tree_loads_completely_and_every_entry_is_translated", AdvancementTreeTests::theTreeLoadsCompletelyAndEveryEntryIsTranslated)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_every_feature_grants_the_advancements_that_wait_for_it", AdvancementTreeTests::everyFeatureGrantsTheAdvancementsThatWaitForIt)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_item_advancements_follow_the_inventory", AdvancementTreeTests::itemAdvancementsFollowTheInventory)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_marking_one_corner_with_the_octant_earns_measure_twice", AdvancementTreeTests::markingOneCornerWithTheOctantEarnsMeasureTwice)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_every_recipe_unlock_hands_out_an_existing_recipe", AdvancementTreeTests::everyRecipeUnlockHandsOutAnExistingRecipe)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_the_ftb_quests_book_is_complete_and_forms_stages", AdvancementTreeTests::theFtbQuestsBookIsCompleteAndFormsStages)
                    .build(),
            GameTestSpec.named("advancement_tree_game_test_installing_the_quest_book_adds_but_never_overwrites", AdvancementTreeTests::installingTheQuestBookAddsButNeverOverwrites)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_the_counter_grants_at_its_threshold_and_survives_save_and_respawn", AdvancementTriggerTests::theCounterGrantsAtItsThresholdAndSurvivesSaveAndRespawn)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_a_sledgehammer_swing_counts_the_blocks_it_took_along", AdvancementTriggerTests::aSledgehammerSwingCountsTheBlocksItTookAlong)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_the_mining_enchantments_earn_their_advancements_on_first_use", AdvancementTriggerTests::theMiningEnchantmentsEarnTheirAdvancementsOnFirstUse)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_air_jump_and_kinetic_protection_earn_their_advancements_on_first_use", AdvancementTriggerTests::airJumpAndKineticProtectionEarnTheirAdvancementsOnFirstUse)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_radiance_dyed_storage_and_all_octant_colors_follow_the_inventory", AdvancementTriggerTests::radianceDyedStorageAndAllOctantColorsFollowTheInventory)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_a_full_bonus_trim_set_is_noticed_by_the_player_tick", AdvancementTriggerTests::aFullBonusTrimSetIsNoticedByThePlayerTick)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_setting_down_the_backpack_earns_pitching_camp", AdvancementTriggerTests::settingDownTheBackpackEarnsPitchingCamp)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_repairing_the_breaker_piston_earns_good_as_new", AdvancementTriggerTests::repairingTheBreakerPistonEarnsGoodAsNew)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_the_void_catches_athrown_enderite_item_for_its_thrower", AdvancementTriggerTests::theVoidCatchesAThrownEnderiteItemForItsThrower)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_the_cracked_echo_sounder_shatters_and_earns_broken_record", AdvancementTriggerTests::theCrackedEchoSounderShattersAndEarnsBrokenRecord)
                    .build(),
            GameTestSpec.named("advancement_trigger_game_test_the_lens_beam_priming_tnt_earns_remote_detonation", AdvancementTriggerTests::theLensBeamPrimingTntEarnsRemoteDetonation)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_splash_potions_landing_on_the_pad_are_stored_and_replaced", PotionPadTests::splashPotionsLandingOnThePadAreStoredAndReplaced)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_standing_on_the_pad_ramps_the_effect_to_twenty_five_fifty_and_one_hundred_percent_in_three_seconds", PotionPadTests::standingOnThePadRampsTheEffectToTwentyFiveFiftyAndOneHundredPercentInThreeSeconds)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_instant_effects_apply_once_at_the_three_second_mark", PotionPadTests::instantEffectsApplyOnceAtTheThreeSecondMark)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_a_full_application_puts_the_pad_on_cooldown_for_twice_the_effect_duration", PotionPadTests::aFullApplicationPutsThePadOnCooldownForTwiceTheEffectDuration)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_a_pad_broken_during_cooldown_keeps_the_remaining_time_and_resumes_when_placed_again", PotionPadTests::aPadBrokenDuringCooldownKeepsTheRemainingTimeAndResumesWhenPlacedAgain)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_every_pad_item_stacks_to_one", PotionPadTests::everyPadItemStacksToOne)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_potion_pad_recipes_cover_all_three_tiers", PotionPadTests::potionPadRecipesCoverAllThreeTiers)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_charged_creeper_explosions_drop_one_blaze_head_each", PotionPadTests::chargedCreeperExplosionsDropOneBlazeHeadEach)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_blazes_killed_otherwise_drop_no_head", PotionPadTests::blazesKilledOtherwiseDropNoHead)
                    .build(),
            GameTestSpec.named("mob_head_game_test_charged_creepers_drop_the_new_heads_and_no_other_death_does", MobHeadTests::chargedCreepersDropTheNewHeadsAndNoOtherDeathDoes)
                    .build(),
            GameTestSpec.named("mob_head_game_test_every_mod_head_is_wearable_like_vanilla_skulls_and_keeps_its_secret", MobHeadTests::everyModHeadIsWearableLikeVanillaSkullsAndKeepsItsSecret)
                    .build(),
            GameTestSpec.named("mob_head_game_test_flypad_one_needs_the_shulker_head_and_an_elytra_with_mending", MobHeadTests::flypadOneNeedsTheShulkerHeadAndAnElytraWithMending)
                    .build(),
            GameTestSpec.named("mob_head_game_test_blaze_head_wearers_ignore_magma_blocks_and_campfires", MobHeadTests::blazeHeadWearersIgnoreMagmaBlocksAndCampfires)
                    .build(),
            GameTestSpec.named("mob_head_game_test_enderman_head_wearers_take_no_ender_pearl_damage", MobHeadTests::endermanHeadWearersTakeNoEnderPearlDamage)
                    .build(),
            GameTestSpec.named("mob_head_game_test_husk_head_skips_the_hunger_of_food", MobHeadTests::huskHeadSkipsTheHungerOfFood)
                    .build(),
            GameTestSpec.named("mob_head_game_test_bogged_skull_skips_the_poison_of_food", MobHeadTests::boggedSkullSkipsThePoisonOfFood)
                    .build(),
            GameTestSpec.named("mob_head_game_test_spider_head_wearers_are_not_slowed_by_cobwebs", MobHeadTests::spiderHeadWearersAreNotSlowedByCobwebs)
                    .build(),
            GameTestSpec.named("mob_head_game_test_cave_spider_head_cuts_cobwebs_as_fast_as_swords", MobHeadTests::caveSpiderHeadCutsCobwebsAsFastAsSwords)
                    .build(),
            GameTestSpec.named("mob_head_game_test_stray_skull_keeps_powder_snow_from_freezing", MobHeadTests::straySkullKeepsPowderSnowFromFreezing)
                    .build(),
            GameTestSpec.named("mob_head_game_test_slime_head_adds_one_block_of_safe_fall", MobHeadTests::slimeHeadAddsOneBlockOfSafeFall)
                    .build(),
            GameTestSpec.named("mob_head_game_test_silverfish_head_shrinks_the_wearer_to_half_size", MobHeadTests::silverfishHeadShrinksTheWearerToHalfSize)
                    .build(),
            GameTestSpec.named("mob_head_game_test_breeze_head_wearers_do_not_trample_farmland", MobHeadTests::breezeHeadWearersDoNotTrampleFarmland)
                    .build(),
            GameTestSpec.named("mob_head_game_test_shulker_head_opens_blocked_shulker_boxes", MobHeadTests::shulkerHeadOpensBlockedShulkerBoxes)
                    .build(),
            GameTestSpec.named("mob_head_game_test_drowned_head_resists_downward_bubble_columns", MobHeadTests::drownedHeadResistsDownwardBubbleColumns)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_potion_pads_are_switched_off_by_redstone_and_report_their_state_to_acomparator", PotionPadTests::potionPadsAreSwitchedOffByRedstoneAndReportTheirStateToAComparator)
                    .build(),
            GameTestSpec.named("potion_pad_game_test_a_dispenser_fills_the_potion_pad_but_ahopper_cannot", PotionPadTests::aDispenserFillsThePotionPadButAHopperCannot)
                    .maxTicks(PotionPadTests.AUTOMATION_MAX_TICKS)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_harmful_effects_reach_only_the_owner_and_strangers_cannot_charge_the_pad", PotionPadRuleTests::harmfulEffectsReachOnlyTheOwnerAndStrangersCannotChargeThePad)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_blocked_effects_are_never_given_and_start_no_cooldown", PotionPadRuleTests::blockedEffectsAreNeverGivenAndStartNoCooldown)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_levels_and_durations_never_exceed_vanilla_or_the_potion", PotionPadRuleTests::levelsAndDurationsNeverExceedVanillaOrThePotion)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_the_cooldown_follows_the_granted_duration_and_the_effect_multiplier", PotionPadRuleTests::theCooldownFollowsTheGrantedDurationAndTheEffectMultiplier)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_healing_locks_the_player_out_of_every_pad_for_one_minute", PotionPadRuleTests::healingLocksThePlayerOutOfEveryPadForOneMinute)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_mobs_never_receive_any_pad_effect", PotionPadRuleTests::mobsNeverReceiveAnyPadEffect)
                    .build(),
            GameTestSpec.named("potion_pad_rule_game_test_every_vanilla_effect_has_deliberate_rules", PotionPadRuleTests::everyVanillaEffectHasDeliberateRules)
                    .build(),
            GameTestSpec.named("performance_game_test_player_scan_finds_exactly_the_players_the_section_search_finds", PerformanceTests::playerScanFindsExactlyThePlayersTheSectionSearchFinds)
                    .build(),
            GameTestSpec.named("performance_game_test_an_idle_spawn_teleporter_stops_tracking_once_players_leave", PerformanceTests::anIdleSpawnTeleporterStopsTrackingOncePlayersLeave)
                    .build(),
            GameTestSpec.named("performance_game_test_the_cached_octant_surface_matches_the_per_frame_scan_it_replaced", PerformanceTests::theCachedOctantSurfaceMatchesThePerFrameScanItReplaced)
                    .build(),
            GameTestSpec.named("placed_template_game_test_placed_eggs_go_back_with_silk_touch_and_hatch_like_thrown_eggs", PlacedTemplateTests::placedEggsGoBackWithSilkTouchAndHatchLikeThrownEggs)
                    .build(),
            GameTestSpec.named("placed_template_game_test_small_parts_lie_down_and_the_server_options_gate_them", PlacedTemplateTests::smallPartsLieDownAndTheServerOptionsGateThem)
                    .build(),
            GameTestSpec.named("placed_template_game_test_sneak_use_places_templates_on_the_floor_against_the_wall_and_under_the_ceiling", PlacedTemplateTests::sneakUsePlacesTemplatesOnTheFloorAgainstTheWallAndUnderTheCeiling)
                    .build(),
            GameTestSpec.named("placed_template_game_test_without_sneaking_the_template_keeps_its_normal_behaviour", PlacedTemplateTests::withoutSneakingTheTemplateKeepsItsNormalBehaviour)
                    .build(),
            GameTestSpec.named("placed_template_game_test_placed_templates_survive_water_and_drop_themselves_with_their_data", PlacedTemplateTests::placedTemplatesSurviveWaterAndDropThemselvesWithTheirData)
                    .maxTicks(PlacedTemplateTests.WATER_MAX_TICKS)
                    .build(),
            GameTestSpec.named("placed_template_game_test_placed_trim_templates_need_three_hammer_hits", PlacedTemplateTests::placedTrimTemplatesNeedThreeHammerHits)
                    .build(),
            GameTestSpec.named("placed_template_game_test_hint_sparks_only_show_near_players_holding_glowstone_or_glow_ink", PlacedTemplateTests::hintSparksOnlyShowNearPlayersHoldingGlowstoneOrGlowInk)
                    .build(),
            GameTestSpec.named("placed_template_game_test_placed_templates_carry_the_name_of_their_template", PlacedTemplateTests::placedTemplatesCarryTheNameOfTheirTemplate)
                    .build(),
            GameTestSpec.named("placed_template_game_test_the_hitbox_covers_only_the_pixels_of_the_plate", PlacedTemplateTests::theHitboxCoversOnlyThePixelsOfThePlate)
                    .build(),
            GameTestSpec.named("placed_template_game_test_blueprints_are_placed_like_templates_and_drop_themselves", PlacedTemplateTests::blueprintsArePlacedLikeTemplatesAndDropThemselves)
                    .build(),
            GameTestSpec.named("placed_template_game_test_placed_attractors_pull_loose_items_toward_themselves", PlacedTemplateTests::placedAttractorsPullLooseItemsTowardThemselves)
                    .maxTicks(PlacedTemplateTests.ATTRACTOR_MAX_TICKS)
                    .build(),
            GameTestSpec.named("placed_template_game_test_locked_octants_are_placed_and_right_click_toggles_the_outline_per_player", PlacedTemplateTests::lockedOctantsArePlacedAndRightClickTogglesTheOutlinePerPlayer)
                    .build(),
            GameTestSpec.named("pulsating_trim_game_test_the_pulsating_template_is_crafted_from_an_echo_shard_and_any_sledgehammer_that_stays", PulsatingTrimTests::thePulsatingTemplateIsCraftedFromAnEchoShardAndAnySledgehammerThatStays)
                    .build(),
            GameTestSpec.named("pulsating_trim_game_test_the_pulsating_upgrade_makes_the_trim_pulse_once_and_combines_with_glowing", PulsatingTrimTests::thePulsatingUpgradeMakesTheTrimPulseOnceAndCombinesWithGlowing)
                    .build(),
            GameTestSpec.named("pulsating_trim_game_test_templates_and_the_attractor_carry_their_new_names", PulsatingTrimTests::templatesAndTheAttractorCarryTheirNewNames)
                    .build(),
            GameTestSpec.named("placed_bundle_game_test_sneak_use_places_bundles_only_on_top_faces", PlacedBundleTests::sneakUsePlacesBundlesOnlyOnTopFaces)
                    .build(),
            GameTestSpec.named("placed_bundle_game_test_sneak_scroll_packets_cycle_the_top_item_and_right_click_takes_it", PlacedBundleTests::sneakScrollPacketsCycleTheTopItemAndRightClickTakesIt)
                    .maxTicks(PlacedBundleTests.SCROLL_MAX_TICKS)
                    .build(),
            GameTestSpec.named("placed_bundle_game_test_sneak_right_click_with_an_item_deposits_into_the_placed_bundle", PlacedBundleTests::sneakRightClickWithAnItemDepositsIntoThePlacedBundle)
                    .build(),
            GameTestSpec.named("placed_bundle_game_test_placed_bundles_drop_themselves_with_their_contents", PlacedBundleTests::placedBundlesDropThemselvesWithTheirContents)
                    .build(),
            GameTestSpec.named("guide_book_game_test_the_first_join_gives_the_guide_once_and_honours_the_config", GuideBookTests::theFirstJoinGivesTheGuideOnceAndHonoursTheConfig)
                    .build(),
            GameTestSpec.named("guide_book_game_test_every_topic_book_recipe_takes_book_or_guide_and_the_guide_stays", GuideBookTests::everyTopicBookRecipeTakesBookOrGuideAndTheGuideStays)
                    .build(),
            GameTestSpec.named("guide_book_game_test_every_guide_page_uses_translation_keys_that_exist_in_english_and_german", GuideBookTests::everyGuidePageUsesTranslationKeysThatExistInEnglishAndGerman)
                    .build(),
            GameTestSpec.named("guide_book_game_test_guide_books_read_like_written_books", GuideBookTests::guideBooksReadLikeWrittenBooks)
                    .build(),
            GameTestSpec.named("guide_book_game_test_every_guide_chapter_icon_and_recipe_resolves", GuideBookTests::everyGuideChapterIconAndRecipeResolves)
                    .build(),
            GameTestSpec.named("guide_book_game_test_the_guides_explain_every_enchantment_and_the_wave_items", GuideBookTests::theGuidesExplainEveryEnchantmentAndTheWaveItems)
                    .build(),
            GameTestSpec.named("guide_book_game_test_the_admin_guide_names_only_commands_and_options_that_exist", GuideBookTests::theAdminGuideNamesOnlyCommandsAndOptionsThatExist)
                    .build(),
            GameTestSpec.named("guide_book_game_test_every_guide_page_fits_the_book_in_english_and_german", GuideBookTests::everyGuidePageFitsTheBookInEnglishAndGerman)
                    .build(),
            GameTestSpec.named("guide_book_game_test_reading_the_guide_does_not_pause_the_game", GuideBookTests::readingTheGuideDoesNotPauseTheGame)
                    .build(),
            GameTestSpec.named("guide_book_game_test_only_operators_craft_the_admin_guide", GuideBookTests::onlyOperatorsCraftTheAdminGuide)
                    .build(),
            GameTestSpec.named("guide_book_game_test_the_enchantments_guide_covers_every_mod_enchantment", GuideBookTests::theEnchantmentsGuideCoversEveryModEnchantment)
                    .build(),
            GameTestSpec.named("guide_book_game_test_every_guide_tab_gate_is_an_unlockable_recipe", GuideBookTests::everyGuideTabGateIsAnUnlockableRecipe)
                    .build(),
            GameTestSpec.named("guide_book_game_test_learning_gate_recipes_opens_tabs_without_consuming_items", GuideBookTests::learningGateRecipesOpensTabsWithoutConsumingItems)
                    .build(),
            GameTestSpec.named("guide_book_game_test_old_chapter_masks_move_to_the_reading_player", GuideBookTests::oldChapterMasksMoveToTheReadingPlayer)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_honeycomb_waxes_every_stage_and_waxed_plates_stop_oxidizing", PressurePlateTests::honeycombWaxesEveryStageAndWaxedPlatesStopOxidizing)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_the_axe_scrapes_the_wax_off_before_the_oxidation", PressurePlateTests::theAxeScrapesTheWaxOffBeforeTheOxidation)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_waxed_copper_plates_are_crafted_from_the_plate_and_one_honeycomb", PressurePlateTests::waxedCopperPlatesAreCraftedFromThePlateAndOneHoneycomb)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_copper_plates_release_as_late_as_they_press", PressurePlateTests::copperPlatesReleaseAsLateAsTheyPress)
                    .maxTicks(PressurePlateTests.RELEASE_MAX_TICKS)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_every_mod_pressure_plate_visibly_sinks_when_pressed", PressurePlateTests::everyModPressurePlateVisiblySinksWhenPressed)
                    .build(),
            GameTestSpec.named("pressure_plate_game_test_every_mod_pressure_plate_is_named_like_the_vanilla_ones", PressurePlateTests::everyModPressurePlateIsNamedLikeTheVanillaOnes)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_echo_sounder_links_to_the_lodestone_and_teleports_without_any_pearl", TweaksTests::theEchoSounderLinksToTheLodestoneAndTeleportsWithoutAnyPearl)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_echo_sounder_is_registered_and_named_echo_sounder", TweaksTests::theEchoSounderIsRegisteredAndNamedEchoSounder)
                    .build(),
            GameTestSpec.named("tweaks_game_test_unbreaking_lowers_how_much_the_jump_empties_the_echo_compass", TweaksTests::unbreakingLowersHowMuchTheJumpEmptiesTheEchoCompass)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_echo_compass_charges_for_three_seconds_and_releasing_early_costs_nothing", TweaksTests::theEchoCompassChargesForThreeSecondsAndReleasingEarlyCostsNothing)
                    .build(),
            GameTestSpec.named("tweaks_game_test_a_full_charge_jumps_and_leaves_the_echo_compass_empty", TweaksTests::aFullChargeJumpsAndLeavesTheEchoCompassEmpty)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_echo_compass_is_only_charged_again_after_fifteen_hundred_repair_points", TweaksTests::theEchoCompassIsOnlyChargedAgainAfterFifteenHundredRepairPoints)
                    .build(),
            GameTestSpec.named("tweaks_game_test_a_cracked_echo_compass_charges_twice_as_long_and_shatters_after_the_jump", TweaksTests::aCrackedEchoCompassChargesTwiceAsLongAndShattersAfterTheJump)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_first_join_gift_comes_once_and_honours_simple_tweaks_players", TweaksTests::theFirstJoinGiftComesOnceAndHonoursSimpleTweaksPlayers)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_nether_and_the_end_can_be_locked_by_config", TweaksTests::theNetherAndTheEndCanBeLockedByConfig)
                    .build(),
            GameTestSpec.named("tweaks_game_test_xp_orbs_clump_without_losing_experience", TweaksTests::xpOrbsClumpWithoutLosingExperience)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_rocket_stack_size_follows_the_config", TweaksTests::theRocketStackSizeFollowsTheConfig)
                    .build(),
            GameTestSpec.named("tweaks_game_test_kill_boats_removes_boats_by_mode", TweaksTests::killBoatsRemovesBoatsByMode)
                    .build(),
            GameTestSpec.named("tweaks_game_test_tweaks_config_keeps_its_names_and_defaults", TweaksTests::tweaksConfigKeepsItsNamesAndDefaults)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_tweaks_commands_write_the_config", TweaksTests::theTweaksCommandsWriteTheConfig)
                    .build(),
            GameTestSpec.named("tweaks_game_test_spawn_elytras_vanish_as_soon_as_they_leave_the_chest_slot", TweaksTests::spawnElytrasVanishAsSoonAsTheyLeaveTheChestSlot)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_spawn_area_lies_only_in_the_spawn_dimension_and_fall_protection_covers_all_of_it", TweaksTests::theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt)
                    .build(),
            GameTestSpec.named("tweaks_game_test_chunk_loaders_release_on_setblock_and_hand_over_shared_chunks", TweaksTests::chunkLoadersReleaseOnSetblockAndHandOverSharedChunks)
                    .build(),
            GameTestSpec.named("tweaks_game_test_overlapping_flypads_keep_the_player_flying_and_take_only_their_own_flight", TweaksTests::overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_laser_relay_checks_the_sender_and_only_reaches_nearby_players", TweaksTests::theLaserRelayChecksTheSenderAndOnlyReachesNearbyPlayers)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_client_relevant_tweaks_values_are_sent_at_login_and_on_every_change", TweaksTests::theClientRelevantTweaksValuesAreSentAtLoginAndOnEveryChange)
                    .build(),
            GameTestSpec.named("tweaks_game_test_a_blocked_echo_compass_jump_costs_nothing", TweaksTests::aBlockedEchoCompassJumpCostsNothing)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_amethyst_lens_is_crafted_around_an_iron_core", TweaksTests::theAmethystLensIsCraftedAroundAnIronCore)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_melts_ice_and_snow", TweaksTests::theLensBeamMeltsIceAndSnow)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_ignites_flammable_blocks_only_after_dwelling", TweaksTests::theLensBeamIgnitesFlammableBlocksOnlyAfterDwelling)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_lights_soul_fire_campfires_and_candles", TweaksTests::theLensBeamLightsSoulFireCampfiresAndCandles)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_never_lights_nether_portals", TweaksTests::theLensBeamNeverLightsNetherPortals)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_primes_tnt_after_dwelling_but_respects_the_rules", TweaksTests::theLensBeamPrimesTntAfterDwellingButRespectsTheRules)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_dries_wet_sponges", TweaksTests::theLensBeamDriesWetSponges)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_beam_respects_adventure_mode_and_the_fire_spread_rule", TweaksTests::theLensBeamRespectsAdventureModeAndTheFireSpreadRule)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_charge_runs_down_but_the_lens_never_breaks", TweaksTests::theLensChargeRunsDownButTheLensNeverBreaks)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_drains_charge_even_when_it_points_into_the_air", TweaksTests::theLensDrainsChargeEvenWhenItPointsIntoTheAir)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_records_the_last_measurement_only_with_constructors_touch", TweaksTests::theLensRecordsTheLastMeasurementOnlyWithConstructorsTouch)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_dwell_time_grows_moderately_with_distance", TweaksTests::theLensDwellTimeGrowsModeratelyWithDistance)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_sets_living_entities_on_fire_taking_twice_as_long", TweaksTests::theLensSetsLivingEntitiesOnFireTakingTwiceAsLong)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_only_ignites_players_when_pvp_allows_it", TweaksTests::theLensOnlyIgnitesPlayersWhenPvpAllowsIt)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_lens_hums_on_any_surface_and_sizzles_or_crackles_while_heating", TweaksTests::theLensHumsOnAnySurfaceAndSizzlesOrCracklesWhileHeating)
                    .build(),
            GameTestSpec.named("tweaks_game_test_anvil_recharge_with_amethyst_shards_costs_no_levels", TweaksTests::anvilRechargeWithAmethystShardsCostsNoLevels)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_rod_drains_four_charge_per_second_of_beaming", TweaksTests::theRodDrainsFourChargePerSecondOfBeaming)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_gauge_altimeter_reads_the_ground_and_range_reaches_deeper", TweaksTests::theGaugeAltimeterReadsTheGroundAndRangeReachesDeeper)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_gauge_autowalk_follows_paths_and_rails_around_corners", TweaksTests::theGaugeAutowalkFollowsPathsAndRailsAroundCorners)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_world_spawn_command_takes_effect_immediately", TweaksTests::theWorldSpawnCommandTakesEffectImmediately)
                    .build(),
            GameTestSpec.named("tweaks_game_test_kill_boats_all_drops_the_contents_of_chest_boats", TweaksTests::killBoatsAllDropsTheContentsOfChestBoats)
                    .build(),
            GameTestSpec.named("tweaks_game_test_flight_time_and_boosts_are_capped_and_broken_launchpads_drop_their_charges", TweaksTests::flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges)
                    .build(),
            GameTestSpec.named("tweaks_game_test_forced_exact_respawn_puts_the_player_on_the_bed_centre", TweaksTests::forcedExactRespawnPutsThePlayerOnTheBedCentre)
                    .build(),
            GameTestSpec.named("tweaks_game_test_command_teleports_pass_the_dimension_lock_and_the_lock_message_waits", TweaksTests::commandTeleportsPassTheDimensionLockAndTheLockMessageWaits)
                    .build(),
            GameTestSpec.named("tweaks_game_test_a_chunk_loader_replaced_by_another_loader_type_releases_its_chunks", TweaksTests::aChunkLoaderReplacedByAnotherLoaderTypeReleasesItsChunks)
                    .build(),
            GameTestSpec.named("tweaks_game_test_only_world_spawn_commands_reapply_the_custom_world_spawn", TweaksTests::onlyWorldSpawnCommandsReapplyTheCustomWorldSpawn)
                    .build(),
            GameTestSpec.named("tweaks_game_test_the_first_join_key_migration_saves_the_config_once", TweaksTests::theFirstJoinKeyMigrationSavesTheConfigOnce)
                    .build(),
            GameTestSpec.named("tweaks_game_test_creative_players_lose_the_stale_flypad_flight_tag", TweaksTests::creativePlayersLoseTheStaleFlypadFlightTag)
                    .build(),
            GameTestSpec.named("tweaks_game_test_kill_carts_obeys_its_switch_and_operators_and_drops_cart_contents", TweaksTests::killCartsObeysItsSwitchAndOperatorsAndDropsCartContents)
                    .build(),
            GameTestSpec.named("tweaks_game_test_pads_placed_in_water_are_waterlogged_and_leave_the_water_behind", TweaksTests::padsPlacedInWaterAreWaterloggedAndLeaveTheWaterBehind)
                    .build(),
            GameTestSpec.named("immersion_game_test_launchpad_shows_its_fill_level_in_its_block_state", ImmersionTests::launchpadShowsItsFillLevelInItsBlockState)
                    .build(),
            GameTestSpec.named("immersion_game_test_chunk_loader_shows_whether_it_keeps_chunks_loaded", ImmersionTests::chunkLoaderShowsWhetherItKeepsChunksLoaded)
                    .build(),
            GameTestSpec.named("immersion_game_test_flypad_shows_active_while_someone_is_in_its_field", ImmersionTests::flypadShowsActiveWhileSomeoneIsInItsField)
                    .build(),
            GameTestSpec.named("immersion_game_test_flypad_warning_rises_towards_the_edge_of_its_field", ImmersionTests::flypadWarningRisesTowardsTheEdgeOfItsField)
                    .build(),
            GameTestSpec.named("immersion_game_test_pad_and_machine_tooltips_name_their_numbers", ImmersionTests::padAndMachineTooltipsNameTheirNumbers)
                    .build(),
            GameTestSpec.named("immersion_game_test_enderite_armor_damps_the_fall_while_sneaking", ImmersionTests::enderiteArmorDampsTheFallWhileSneaking)
                    .build(),
            GameTestSpec.named("immersion_game_test_armor_and_food_tooltips_explain_what_they_do", ImmersionTests::armorAndFoodTooltipsExplainWhatTheyDo)
                    .build(),
            GameTestSpec.named("immersion_game_test_core_tooltips_list_exactly_the_recipes_that_take_the_core", ImmersionTests::coreTooltipsListExactlyTheRecipesThatTakeTheCore)
                    .build(),
            GameTestSpec.named("immersion_game_test_hud_boxes_follow_the_configured_position_and_scale", ImmersionTests::hudBoxesFollowTheConfiguredPositionAndScale)
                    .build(),
            // --- /tweaks ---
            // --- modpack ---
            GameTestSpec.named("modpack_game_test_building_wand_skips_cells_the_loader_events_refuse_and_counts_its_blocks", ModpackTests::buildingWandSkipsCellsTheLoaderEventsRefuseAndCountsItsBlocks)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_octant_fill_skips_cells_the_loader_events_refuse", ModpackTests::octantFillSkipsCellsTheLoaderEventsRefuse)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_lens_beam_leaves_blocks_the_loader_events_protect", ModpackTests::lensBeamLeavesBlocksTheLoaderEventsProtect)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_sledgehammer_area_swing_leaves_blocks_the_loader_events_protect", ModpackTests::sledgehammerAreaSwingLeavesBlocksTheLoaderEventsProtect)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_attractor_leaves_display_items_owned_items_and_other_players_death_drops_alone", ModpackTests::attractorLeavesDisplayItemsOwnedItemsAndOtherPlayersDeathDropsAlone)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_ore_and_common_tags_cover_every_mod_material", ModpackTests::oreAndCommonTagsCoverEveryModMaterial)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_backpack_refuses_items_from_the_not_allowed_tag", ModpackTests::backpackRefusesItemsFromTheNotAllowedTag)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_chisel_and_upgrade_tables_come_from_the_datapack_and_match_the_built_in_tables", ModpackTests::chiselAndUpgradeTablesComeFromTheDatapackAndMatchTheBuiltInTables)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_datapack_files_extend_and_remove_chisel_and_upgrade_entries", ModpackTests::datapackFilesExtendAndRemoveChiselAndUpgradeEntries)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_loot_injection_tables_match_the_code_and_vanilla_tables_roll_them", ModpackTests::lootInjectionTablesMatchTheCodeAndVanillaTablesRollThem)
                    .rotation(Rotation.NONE)
                    .build(),
            GameTestSpec.named("modpack_game_test_mod_statistics_are_registered_and_count_chisel_use_and_teleports", ModpackTests::modStatisticsAreRegisteredAndCountChiselUseAndTeleports)
                    .rotation(Rotation.NONE)
                    .build()
            // --- /modpack ---
            );

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
