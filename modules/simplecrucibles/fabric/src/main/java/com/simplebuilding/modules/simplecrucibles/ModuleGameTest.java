package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.test.CrucibleTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link CrucibleTests}. Keep in sync with CrucibleTests.ALL. */
public final class ModuleGameTest {
    @GameTest(structure = "simplecrucibles:empty")
    public void configBounds(GameTestHelper h) {
        CrucibleTests.ALL.get("config_bounds").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void configCommand(GameTestHelper h) {
        CrucibleTests.ALL.get("config_command").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void configAdoptsSimplelibOptions(GameTestHelper h) {
        CrucibleTests.ALL.get("config_adopts_simplelib_options").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void brokenConfigIsQuarantined(GameTestHelper h) {
        CrucibleTests.ALL.get("broken_config_is_quarantined").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void registered(GameTestHelper h) {
        CrucibleTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void migrationFromSimplelibAndSimplebuilding(GameTestHelper h) {
        CrucibleTests.ALL.get("migration_from_simplelib_and_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleUpgradeHandler(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_upgrade_handler").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void creativeTabRouting(GameTestHelper h) {
        CrucibleTests.ALL.get("creative_tab_routing").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void tierLayout(GameTestHelper h) {
        CrucibleTests.ALL.get("tier_layout").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void heatSources(GameTestHelper h) {
        CrucibleTests.ALL.get("heat_sources").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void heatTwoBelow(GameTestHelper h) {
        CrucibleTests.ALL.get("heat_two_below").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void cooksIntoSlotBelow(GameTestHelper h) {
        CrucibleTests.ALL.get("cooks_into_slot_below").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void mediumHeatBlocksSmelting(GameTestHelper h) {
        CrucibleTests.ALL.get("medium_heat_blocks_smelting").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reservedSlotBlocksAndKeepsProgress(GameTestHelper h) {
        CrucibleTests.ALL.get("reserved_slot_blocks_and_keeps_progress").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void resultNotRecooked(GameTestHelper h) {
        CrucibleTests.ALL.get("result_not_recooked").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void warmsFoodAndEatsFaster(GameTestHelper h) {
        CrucibleTests.ALL.get("warms_food_and_eats_faster").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void afterglowKeepsHeat(GameTestHelper h) {
        CrucibleTests.ALL.get("afterglow_keeps_heat").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void hopperRules(GameTestHelper h) {
        CrucibleTests.ALL.get("hopper_rules").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleHitboxesMatchModel(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_hitboxes_match_model").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeBuildsIronCrucible(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_builds_iron_crucible").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void breakDropsContents(GameTestHelper h) {
        CrucibleTests.ALL.get("break_drops_contents").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradeKeepsContents(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrade_keeps_contents").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void villageKitchenInPools(GameTestHelper h) {
        CrucibleTests.ALL.get("village_kitchen_in_pools").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void barrelAttachAndResultsFirst(GameTestHelper h) {
        CrucibleTests.ALL.get("barrel_attach_and_results_first").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void barrelAttachedLaterTakesOverReservations(GameTestHelper h) {
        CrucibleTests.ALL.get("barrel_attached_later_takes_over_reservations").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void clientMenuShowsTheRaisedStackLimit(GameTestHelper h) {
        CrucibleTests.ALL.get("client_menu_shows_the_raised_stack_limit").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeClickReachesAxeNotMenu(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_click_reaches_axe_not_menu").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void barrelStrikeCountersAreSeparate(GameTestHelper h) {
        CrucibleTests.ALL.get("barrel_strike_counters_are_separate").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradesCauldron(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrades_cauldron").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCauldronHoldsBuckets(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_cauldron_holds_buckets").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCauldronInheritsVanilla(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_cauldron_inherits_vanilla").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradesBarrelToNetherite(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrades_barrel_to_netherite").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleBurnDamageFollowsConfig(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_burn_damage_follows_config").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void looseBarrelMenuKeepsTheRaisedLimit(GameTestHelper h) {
        CrucibleTests.ALL.get("loose_barrel_menu_keeps_the_raised_limit").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCrucibleLoadsOldNineSlots(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_crucible_loads_old_nine_slots").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleBarrelBesideInventoryCentred(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_barrel_beside_inventory_centred").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void hammerUpgradeStepsShipWithTheModule(GameTestHelper h) {
        CrucibleTests.ALL.get("hammer_upgrade_steps_ship_with_the_module").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty") public void discoverRecipeUnlocks(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.recipeUnlocks(h); }
    @GameTest(structure = "simplecrucibles:empty") public void discoverBehaviourHints(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.behaviourHints(h); }
    @GameTest(structure = "simplecrucibles:empty") public void discoverFirstPickupTips(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.firstPickupTips(h); }

    // Split stage 5b: soul lava, buckets, JEI/Jade data (BucketTests).

    @GameTest(structure = "simplecrucibles:empty", maxTicks = 300)
    public void soulLavaFlowsTwoBlocksInTheOverworld(GameTestHelper h) {
        CrucibleTests.ALL.get("soul_lava_flows_two_blocks_in_the_overworld").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void soulLavaIsNotReplaceable(GameTestHelper h) {
        CrucibleTests.ALL.get("soul_lava_is_not_replaceable").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void waterTouchingSoulLavaTurnsToQuartzOrBlackstone(GameTestHelper h) {
        CrucibleTests.ALL.get("water_touching_soul_lava_turns_to_quartz_or_blackstone").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void touchingSoulLavaBurnsLongerAndGivesSoulBurn(GameTestHelper h) {
        CrucibleTests.ALL.get("touching_soul_lava_burns_longer_and_gives_soul_burn").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void fireResistanceOnlyBlocksSoulBurnDamage(GameTestHelper h) {
        CrucibleTests.ALL.get("fire_resistance_only_blocks_soul_burn_damage").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void soulBurnBitesTwiceAsOftenOnColdGround(GameTestHelper h) {
        CrucibleTests.ALL.get("soul_burn_bites_twice_as_often_on_cold_ground").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void copperBucketRules(GameTestHelper h) {
        CrucibleTests.ALL.get("copper_bucket_rules").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void copperBucketFullyOxidizedScoopsNothing(GameTestHelper h) {
        CrucibleTests.ALL.get("copper_bucket_fully_oxidized_scoops_nothing").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void ceramicBucketHoldsWaterAndBreaksAfterFourPours(GameTestHelper h) {
        CrucibleTests.ALL.get("ceramic_bucket_holds_water_and_breaks_after_four_pours").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void modBucketsStayInTheHandWhenPouringAndScooping(GameTestHelper h) {
        CrucibleTests.ALL.get("mod_buckets_stay_in_the_hand_when_pouring_and_scooping").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void enderiteBucketTakesMilkAndDrinksOneFilling(GameTestHelper h) {
        CrucibleTests.ALL.get("enderite_bucket_takes_milk_and_drinks_one_filling").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void enderiteBucketHoldsTwoBucketsOfOneFluid(GameTestHelper h) {
        CrucibleTests.ALL.get("enderite_bucket_holds_two_buckets_of_one_fluid").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty", maxTicks = 100)
    public void enderiteBucketTwoBucketsInCauldronsAndDispensers(GameTestHelper h) {
        CrucibleTests.ALL.get("enderite_bucket_two_buckets_in_cauldrons_and_dispensers").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void ceramicLavaBucketScoopsPoursAndWears(GameTestHelper h) {
        CrucibleTests.ALL.get("ceramic_lava_bucket_scoops_pours_and_wears").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void copperBucketOxidizesOnlyInHotbarOrOffhand(GameTestHelper h) {
        CrucibleTests.ALL.get("copper_bucket_oxidizes_only_in_hotbar_or_offhand").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void copperBucketWaxedWaterWeathersAtHalfPace(GameTestHelper h) {
        CrucibleTests.ALL.get("copper_bucket_waxed_water_weathers_at_half_pace").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void copperBucketPlacedWeathersAndFrameCare(GameTestHelper h) {
        CrucibleTests.ALL.get("copper_bucket_placed_weathers_and_frame_care").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void ceramicBucketWithLavaIsBrittleAndBreaksOnPour(GameTestHelper h) {
        CrucibleTests.ALL.get("ceramic_bucket_with_lava_is_brittle_and_breaks_on_pour").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void ceramicBucketWearsThroughStagesAndKeepsItsFilling(GameTestHelper h) {
        CrucibleTests.ALL.get("ceramic_bucket_wears_through_stages_and_keeps_its_filling").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void ironBucketBreaksOnSoulLavaEnderiteNever(GameTestHelper h) {
        CrucibleTests.ALL.get("iron_bucket_breaks_on_soul_lava_enderite_never").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCauldronHoldsSoulLava(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_cauldron_holds_soul_lava").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void vanillaCauldronTakesCopperAndEnderiteBuckets(GameTestHelper h) {
        CrucibleTests.ALL.get("vanilla_cauldron_takes_copper_and_enderite_buckets").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void jadeReportsHeatSlotsAndShortestRemainingTime(GameTestHelper h) {
        CrucibleTests.ALL.get("jade_reports_heat_slots_and_shortest_remaining_time").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void recipeHeatAndCatalogMatchCookingRules(GameTestHelper h) {
        CrucibleTests.ALL.get("recipe_heat_and_catalog_match_cooking_rules").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void soulLavaHeatsExtremeFlowingHigh(GameTestHelper h) {
        CrucibleTests.ALL.get("soul_lava_heats_extreme_flowing_high").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(GameTestHelper h) {
        CrucibleTests.ALL.get("buckets_draw_no_bar_and_the_enderite_bucket_shows_only_its_name").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void registeredSoulLavaAndBuckets(GameTestHelper h) {
        CrucibleTests.ALL.get("registered_soul_lava_and_buckets").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void migrationOfBucketsAndSoulLava(GameTestHelper h) {
        CrucibleTests.ALL.get("migration_of_buckets_and_soul_lava").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void configSoulLavaBoundsAndTakeover(GameTestHelper h) {
        CrucibleTests.ALL.get("config_soul_lava_bounds_and_takeover").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void jadeReinforcedCauldronLines(GameTestHelper h) {
        CrucibleTests.ALL.get("jade_reinforced_cauldron_lines").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void jeiInfoPagesCoverTheFilledBuckets(GameTestHelper h) {
        CrucibleTests.ALL.get("jei_info_pages_cover_the_filled_buckets").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void loadKeepsReadableSlotsAroundAnUnreadableOne(GameTestHelper h) {
        CrucibleTests.ALL.get("load_keeps_readable_slots_around_an_unreadable_one").accept(h);
    }
}
