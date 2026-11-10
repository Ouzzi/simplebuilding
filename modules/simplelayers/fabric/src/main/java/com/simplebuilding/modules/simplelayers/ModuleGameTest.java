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
    public void backroomsChunkMatchesGrid(GameTestHelper h) {
        LayersTests.ALL.get("backrooms_chunk_matches_grid").accept(h);
    }

    @GameTest(structure = "simplelayers:empty")
    public void specialRoomsRegistered(GameTestHelper h) {
        LayersTests.ALL.get("special_rooms_registered").accept(h);
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
    public void levelsRegistered(GameTestHelper h) {
        LayersTests.ALL.get("levels_registered").accept(h);
    }

    @GameTest(structure = "simplelayers:empty", maxTicks = 100)
    public void levelTransitions(GameTestHelper h) {
        LayersTests.ALL.get("level_transitions").accept(h);
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
    public void stairwellsWalkable(GameTestHelper h) {
        LayersTests.ALL.get("stairwells_walkable").accept(h);
    }
}
