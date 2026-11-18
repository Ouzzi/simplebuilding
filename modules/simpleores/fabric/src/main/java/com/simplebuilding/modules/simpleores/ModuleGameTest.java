package com.simplebuilding.modules.simpleores;

import com.simpleores.test.OresTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link OresTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { OresTests.ALL.get("config_command").accept(h); }

    /** JEI24 (a-fix09b): Sage Ore, Sage Orb and Dimensional Scrap pages, N4 (b-ores13): six deposit pages; texts in EN and DE. */
    @GameTest public void jeiInfoPages(GameTestHelper h) { OresTests.ALL.get("jei_info_pages").accept(h); }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        OresTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverIsNoToolMaterial(GameTestHelper h) {
        OresTests.ALL.get("silver_is_no_tool_material").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void grateModels(GameTestHelper h) {
        OresTests.ALL.get("grate_models").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        OresTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void toolValues(GameTestHelper h) {
        OresTests.ALL.get("tool_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void armorValues(GameTestHelper h) {
        OresTests.ALL.get("armor_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void pressingRecipes(GameTestHelper h) {
        OresTests.ALL.get("pressing_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void pressingMeltsOnlyInTheBlastFurnace(GameTestHelper h) {
        OresTests.ALL.get("pressing_melts_only_in_the_blast_furnace").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSmelting(GameTestHelper h) {
        OresTests.ALL.get("silver_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void oreDrops(GameTestHelper h) {
        OresTests.ALL.get("ore_drops").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void worldgenFrequency(GameTestHelper h) {
        OresTests.ALL.get("worldgen_frequency").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        OresTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        OresTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        OresTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadBlastResistance(GameTestHelper h) {
        OresTests.ALL.get("lead_blast_resistance").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadOccludesVibrations(GameTestHelper h) {
        OresTests.ALL.get("lead_occludes_vibrations").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void soundproofAttenuation(GameTestHelper h) {
        OresTests.ALL.get("soundproof_attenuation").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverMirrorRecipe(GameTestHelper h) {
        OresTests.ALL.get("silver_mirror_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSheetRecipe(GameTestHelper h) {
        OresTests.ALL.get("silver_sheet_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverSheetPlacement(GameTestHelper h) {
        OresTests.ALL.get("silver_sheet_placement").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void silverMirrorPlacement(GameTestHelper h) {
        OresTests.ALL.get("silver_mirror_placement").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadPlateSoundproofing(GameTestHelper h) {
        OresTests.ALL.get("lead_plate_soundproofing").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadPlateRecipe(GameTestHelper h) {
        OresTests.ALL.get("lead_plate_recipe").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void leadWorldgen(GameTestHelper h) {
        OresTests.ALL.get("lead_worldgen").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void applesEatEffects(GameTestHelper h) {
        OresTests.ALL.get("apples_eat_effects").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void appleRecipes(GameTestHelper h) {
        OresTests.ALL.get("apple_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseCoreRecipes(GameTestHelper h) {
        OresTests.ALL.get("rose_core_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseGoldVeinRule(GameTestHelper h) {
        OresTests.ALL.get("rose_gold_vein_rule").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseGoldOreDropsAndSmelting(GameTestHelper h) {
        OresTests.ALL.get("rose_gold_ore_drops_and_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void sageCrystalChain(GameTestHelper h) {
        OresTests.ALL.get("sage_crystal_chain").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void storageBlockRecipes(GameTestHelper h) {
        OresTests.ALL.get("storage_block_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void roseCarrots(GameTestHelper h) {
        OresTests.ALL.get("rose_carrots").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalRecipes(GameTestHelper h) {
        OresTests.ALL.get("metal_recipes").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalsAreCosmetic(GameTestHelper h) {
        OresTests.ALL.get("metals_are_cosmetic").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalBlocks(GameTestHelper h) {
        OresTests.ALL.get("metal_blocks").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalWorldgenDrops(GameTestHelper h) {
        OresTests.ALL.get("metal_worldgen_drops").accept(h);
    }

    /** N4 (b-ores13): closed zinc mini geodes in the Overworld and the End, 1 to 8 ores, never at air or water. */
    @GameTest(structure = "simpleores:arena", maxTicks = 200)
    public void zincGeodes(GameTestHelper h) {
        OresTests.ALL.get("zinc_geodes").accept(h);
    }

    /** N4 (b-ores13): tin-bearing gravel/sand (shovel, 2 to 5 nuggets, rivers and beaches), End ore and crust drops. */
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void tinPlacers(GameTestHelper h) {
        OresTests.ALL.get("tin_placers").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftShardSmelting(GameTestHelper h) {
        OresTests.ALL.get("rift_shard_smelting").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftChain(GameTestHelper h) {
        OresTests.ALL.get("rift_chain").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftStorageReversible(GameTestHelper h) {
        OresTests.ALL.get("rift_storage_reversible").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void riftBeaconTags(GameTestHelper h) {
        OresTests.ALL.get("rift_beacon_tags").accept(h);
    }

    /** N25 (b-metals25): nuggets of the plain alloys, stacked plates, mirror faces, bars and grates, metal sounds. */
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void mixNuggets(GameTestHelper h) {
        OresTests.ALL.get("mix_nuggets").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void plateLayers(GameTestHelper h) {
        OresTests.ALL.get("plate_layers").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void mirrorFaces(GameTestHelper h) {
        OresTests.ALL.get("mirror_faces").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalDoors(GameTestHelper h) {
        OresTests.ALL.get("metal_doors").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void pal35bChiseledAndFood(GameTestHelper h) {
        OresTests.ALL.get("pal35b_chiseled_and_food").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalLights(GameTestHelper h) {
        OresTests.ALL.get("metal_lights").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void barsAndGrates(GameTestHelper h) {
        OresTests.ALL.get("bars_and_grates").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void metalSounds(GameTestHelper h) {
        OresTests.ALL.get("metal_sounds").accept(h);
    }

    // Enderite gear, food and poles (N47-M3, moved from the core): bodies in com.simpleores.gear.test.GearTests, registered via OresTests.
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void gearValuesEqualTheOldCoreValues(GameTestHelper h) {
        OresTests.ALL.get("gear_values_equal_the_old_core_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void gearFoodEffectsEqualTheOldCoreValues(GameTestHelper h) {
        OresTests.ALL.get("gear_food_effects_equal_the_old_core_values").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void gearPolesPullLightningWithinTheirRange(GameTestHelper h) {
        OresTests.ALL.get("gear_poles_pull_lightning_within_their_range").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void gearOldSimplebuildingIdsLoadAsSimpleOresIds(GameTestHelper h) {
        OresTests.ALL.get("gear_old_simplebuilding_ids_load_as_simple_ores_ids").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void gearEnderiteArmorDampensSneakingFall(GameTestHelper h) {
        OresTests.ALL.get("gear_enderite_armor_dampens_sneaking_fall").accept(h);
    }

    // Redstone line (moved from the core with the line): bodies in com.simpleores.line.test.LineTests, registered via OresTests.
    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineLinesStaySeparateSideBySide(GameTestHelper h) {
        OresTests.ALL.get("line_lines_stay_separate_side_by_side").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineLinesStaySeparateThroughTheBlock(GameTestHelper h) {
        OresTests.ALL.get("line_lines_stay_separate_through_the_block").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineVanillaSourcesFeedAstralAndNihil(GameTestHelper h) {
        OresTests.ALL.get("line_vanilla_sources_feed_astral_and_nihil").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineAstralPowersLampAndPiston(GameTestHelper h) {
        OresTests.ALL.get("line_astral_powers_lamp_and_piston").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineBridgeLinksAllLines(GameTestHelper h) {
        OresTests.ALL.get("line_bridge_links_all_lines").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineLampsToggleOnEveryLine(GameTestHelper h) {
        OresTests.ALL.get("line_lamps_toggle_on_every_line").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineEveryDeviceTakesRedstone(GameTestHelper h) {
        OresTests.ALL.get("line_every_device_takes_redstone").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineEveryDeviceTakesAstral(GameTestHelper h) {
        OresTests.ALL.get("line_every_device_takes_astral").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineEveryDeviceTakesNihil(GameTestHelper h) {
        OresTests.ALL.get("line_every_device_takes_nihil").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineEveryDeviceTakesBridge(GameTestHelper h) {
        OresTests.ALL.get("line_every_device_takes_bridge").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 140)
    public void lineLineBlocksPowerEveryDevice(GameTestHelper h) {
        OresTests.ALL.get("line_line_blocks_power_every_device").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineLineBlocksFeedOnlyTheirLine(GameTestHelper h) {
        OresTests.ALL.get("line_line_blocks_feed_only_their_line").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineLineTorchesFeedOnlyTheirLine(GameTestHelper h) {
        OresTests.ALL.get("line_line_torches_feed_only_their_line").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void lineLineTorchPowersTheBlockAboveAndInverts(GameTestHelper h) {
        OresTests.ALL.get("line_line_torch_powers_the_block_above_and_inverts").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineLineTorchRecipeAndDrop(GameTestHelper h) {
        OresTests.ALL.get("line_line_torch_recipe_and_drop").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineLineBlockRecipesRoundTrip(GameTestHelper h) {
        OresTests.ALL.get("line_line_block_recipes_round_trip").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineLineBlocksHaveOwnSounds(GameTestHelper h) {
        OresTests.ALL.get("line_line_blocks_have_own_sounds").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineRedstoneRecipesYieldTwo(GameTestHelper h) {
        OresTests.ALL.get("line_redstone_recipes_yield_two").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 220)
    public void lineChannelsStayIsolatedAndStopAtFifteen(GameTestHelper h) {
        OresTests.ALL.get("line_channels_stay_isolated_and_stop_at_fifteen").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 220)
    public void linePowderConnectsLikeRedstoneWire(GameTestHelper h) {
        OresTests.ALL.get("line_powder_connects_like_redstone_wire").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void linePistonPushesAllSixAtOnce(GameTestHelper h) {
        OresTests.ALL.get("line_piston_pushes_all_six_at_once").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void linePistonNeverChains(GameTestHelper h) {
        OresTests.ALL.get("line_piston_never_chains").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineNihilPullsAcrossTheGap(GameTestHelper h) {
        OresTests.ALL.get("line_nihil_pulls_across_the_gap").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void linePistonLeavesImmovablesAlone(GameTestHelper h) {
        OresTests.ALL.get("line_piston_leaves_immovables_alone").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineTwoPistonsMoveOneBlockOnce(GameTestHelper h) {
        OresTests.ALL.get("line_two_pistons_move_one_block_once").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void linePistonFiresOnRisingEdgeOnly(GameTestHelper h) {
        OresTests.ALL.get("line_piston_fires_on_rising_edge_only").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void linePistonTakesEveryLine(GameTestHelper h) {
        OresTests.ALL.get("line_piston_takes_every_line").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineAstralBoostApproachesTopSpeedWithoutPassingIt(GameTestHelper h) {
        OresTests.ALL.get("line_astral_boost_approaches_top_speed_without_passing_it").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineAstralBoostsMoreThanThePoweredRail(GameTestHelper h) {
        OresTests.ALL.get("line_astral_boosts_more_than_the_powered_rail").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineNihilBrakeStopsSmoothly(GameTestHelper h) {
        OresTests.ALL.get("line_nihil_brake_stops_smoothly").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineEndRailConfigIsClamped(GameTestHelper h) {
        OresTests.ALL.get("line_end_rail_config_is_clamped").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void lineEndRailIsFedByEveryLine(GameTestHelper h) {
        OresTests.ALL.get("line_end_rail_is_fed_by_every_line").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 140)
    public void lineAstralRailLaunchesTheCartPastVanillaSpeed(GameTestHelper h) {
        OresTests.ALL.get("line_astral_rail_launches_the_cart_past_vanilla_speed").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 120)
    public void lineNihilRailStopsAndFastCartTakesTheCurve(GameTestHelper h) {
        OresTests.ALL.get("line_nihil_rail_stops_and_fast_cart_takes_the_curve").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void circuitRelayTogglesAndSends(GameTestHelper h) {
        OresTests.ALL.get("circuit_relay_toggles_and_sends").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void circuitCoreGroupSwitchesTogether(GameTestHelper h) {
        OresTests.ALL.get("circuit_core_group_switches_together").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void circuitWallDustSticksAndPowers(GameTestHelper h) {
        OresTests.ALL.get("circuit_wall_dust_sticks_and_powers").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 100)
    public void circuitWallDiodesPlaceAndSwitch(GameTestHelper h) {
        OresTests.ALL.get("circuit_wall_diodes_place_and_switch").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 60)
    public void circuitWallDustRoundsTheEdge(GameTestHelper h) {
        OresTests.ALL.get("circuit_wall_dust_rounds_the_edge").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 20)
    public void circuitConfigTakesOverSimplebuilding(GameTestHelper h) {
        OresTests.ALL.get("circuit_config_takes_over_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleores:arena", maxTicks = 40)
    public void circuitLineIdsMigrate(GameTestHelper h) {
        OresTests.ALL.get("circuit_line_ids_migrate").accept(h);
    }
}
