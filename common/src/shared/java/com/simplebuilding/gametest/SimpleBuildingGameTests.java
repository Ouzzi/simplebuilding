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
            GameTestSpec.named("block_behaviour_game_test_levitating_sand_and_gravel_rise_upwards_instead_of_staying_put", BlockBehaviourTests::levitatingSandAndGravelRiseUpwardsInsteadOfStayingPut)
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
