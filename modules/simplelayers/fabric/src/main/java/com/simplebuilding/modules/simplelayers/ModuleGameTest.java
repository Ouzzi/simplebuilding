package com.simplebuilding.modules.simplelayers;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link LayersTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void registered(GameTestHelper h) {
        LayersTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void subModSwitch(GameTestHelper h) {
        LayersTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudBlocksInstabreak(GameTestHelper h) {
        LayersTests.ALL.get("cloud_blocks_instabreak").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 100)
    public void mistHasNoCollision(GameTestHelper h) {
        LayersTests.ALL.get("mist_has_no_collision").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudLightProperties(GameTestHelper h) {
        LayersTests.ALL.get("cloud_light_properties").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 100)
    public void cloudRoofLetsLightThrough(GameTestHelper h) {
        LayersTests.ALL.get("cloud_roof_lets_light_through").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudFaceCulling(GameTestHelper h) {
        LayersTests.ALL.get("cloud_face_culling").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudTextureLayers(GameTestHelper h) {
        LayersTests.ALL.get("cloud_texture_layers").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void fallDamageFactors(GameTestHelper h) {
        LayersTests.ALL.get("fall_damage_factors").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void dimensionsLoad(GameTestHelper h) {
        LayersTests.ALL.get("dimensions_load").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudEntryRule(GameTestHelper h) {
        LayersTests.ALL.get("cloud_entry_rule").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void cloudTravelBothWays(GameTestHelper h) {
        LayersTests.ALL.get("cloud_travel_both_ways").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void backroomsGrid(GameTestHelper h) {
        LayersTests.ALL.get("backrooms_grid").accept(h);
    }



    @GameTest(structure = "simplelayers:empty")
    public void glitchedBedrockNeedsNetherite(GameTestHelper h) {
        LayersTests.ALL.get("glitched_bedrock_needs_netherite").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void riftLeadsIntoBackrooms(GameTestHelper h) {
        LayersTests.ALL.get("rift_leads_into_backrooms").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void exitLeadsToTheSurface(GameTestHelper h) {
        LayersTests.ALL.get("exit_leads_to_the_surface").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void almondWater(GameTestHelper h) {
        LayersTests.ALL.get("almond_water").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void guideBook(GameTestHelper h) {
        LayersTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void backroomEffectBlocksMining(GameTestHelper h) {
        LayersTests.ALL.get("backroom_effect_blocks_mining").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 100)
    public void netheritePistonMovesWall(GameTestHelper h) {
        LayersTests.ALL.get("netherite_piston_moves_wall").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bedrockHull(GameTestHelper h) {
        LayersTests.ALL.get("bedrock_hull").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void suppliesGenerated(GameTestHelper h) {
        LayersTests.ALL.get("supplies_generated").accept(h);
    }



    @GameTest(structure = "simplelayers:empty")
    public void easiestExitReachable(GameTestHelper h) {
        LayersTests.ALL.get("easiest_exit_reachable").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void levelsHaveFreeArrival(GameTestHelper h) {
        LayersTests.ALL.get("levels_have_free_arrival").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void entranceShaftGenerated(GameTestHelper h) {
        LayersTests.ALL.get("entrance_shaft_generated").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void sealedHatchPuzzle(GameTestHelper h) {
        LayersTests.ALL.get("sealed_hatch_puzzle").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void ambienceSounds(GameTestHelper h) {
        LayersTests.ALL.get("ambience_sounds").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void levelBlocksProtected(GameTestHelper h) {
        LayersTests.ALL.get("level_blocks_protected").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void labyrinthDepthGradient(GameTestHelper h) {
        LayersTests.ALL.get("labyrinth_depth_gradient").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void levelHeightsVary(GameTestHelper h) {
        LayersTests.ALL.get("level_heights_vary").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void chalkMarks(GameTestHelper h) {
        LayersTests.ALL.get("chalk_marks").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void chalkBox(GameTestHelper h) {
        LayersTests.ALL.get("chalk_box").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void chalkRecipes(GameTestHelper h) {
        LayersTests.ALL.get("chalk_recipes").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void stairwellsWalkable(GameTestHelper h) {
        LayersTests.ALL.get("stairwells_walkable").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsLayout(GameTestHelper h) {
        LayersTests.ALL.get("bands_layout").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsHull(GameTestHelper h) {
        LayersTests.ALL.get("bands_hull").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsSeparatorsBedrock(GameTestHelper h) {
        LayersTests.ALL.get("bands_separators_bedrock").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsGenBudget(GameTestHelper h) {
        LayersTests.ALL.get("bands_gen_budget").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsTrackingFilter(GameTestHelper h) {
        LayersTests.ALL.get("bands_tracking_filter").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsStairsRule(GameTestHelper h) {
        LayersTests.ALL.get("bands_stairs_rule").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsPuzzlePointer(GameTestHelper h) {
        LayersTests.ALL.get("bands_puzzle_pointer").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsDoorsPaired(GameTestHelper h) {
        LayersTests.ALL.get("bands_doors_paired").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void bandsDoorsFramed(GameTestHelper h) {
        LayersTests.ALL.get("bands_doors_framed").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void portalRulesPerLevel(GameTestHelper h) {
        LayersTests.ALL.get("portal_rules_per_level").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 200)
    public void portalTransitLoaded(GameTestHelper h) {
        LayersTests.ALL.get("portal_transit_loaded").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 200)
    public void cellarDoorRoundtrip(GameTestHelper h) {
        LayersTests.ALL.get("cellar_door_roundtrip").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void migrationRescue(GameTestHelper h) {
        LayersTests.ALL.get("migration_rescue").accept(h);
    }
}
