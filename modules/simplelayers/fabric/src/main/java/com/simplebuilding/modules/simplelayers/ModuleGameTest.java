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
}
