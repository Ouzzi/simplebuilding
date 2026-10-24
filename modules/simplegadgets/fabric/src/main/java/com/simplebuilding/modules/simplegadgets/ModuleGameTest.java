package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.test.GadgetsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link GadgetsTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplegadgets:empty")
    public void registered(GameTestHelper h) {
        GadgetsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void subModSwitch(GameTestHelper h) {
        GadgetsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        GadgetsTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeRecipe(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_recipe").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAutowalkKeepsWalkingUnderNonPausingScreens(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_autowalk_keeps_walking_under_non_pausing_screens").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAutowalkFollowsPathsAndRailsAroundCorners(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_autowalk_follows_paths_and_rails_around_corners").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void gaugeAltimeterReadsTheGroundAndRangeReachesDeeper(GameTestHelper h) {
        GadgetsTests.ALL.get("gauge_altimeter_reads_the_ground_and_range_reaches_deeper").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        GadgetsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        GadgetsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplegadgets:empty")
    public void guideBook(GameTestHelper h) {
        GadgetsTests.ALL.get("guide_book").accept(h);
    }
}
