package com.simplebuilding.modules.simplelights;

import com.simplelights.test.LightsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link LightsTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplelights:empty")
    public void registered(GameTestHelper h) {
        LightsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void subModSwitch(GameTestHelper h) {
        LightsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        LightsTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void panesPlaceOnEverySide(GameTestHelper h) {
        LightsTests.ALL.get("panes_place_on_every_side").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void panesNeedFullSupportFace(GameTestHelper h) {
        LightsTests.ALL.get("panes_need_full_support_face").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void panesFallOffWithoutSupport(GameTestHelper h) {
        LightsTests.ALL.get("panes_fall_off_without_support").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void panesWaterlog(GameTestHelper h) {
        LightsTests.ALL.get("panes_waterlog").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void plainPanesShineAndSwitchWithRedstone(GameTestHelper h) {
        LightsTests.ALL.get("plain_panes_shine_and_switch_with_redstone").accept(h);
    }

    @GameTest(structure = "simplelights:empty", maxTicks = 60)
    public void paneLightReachesTheWorldAndGoesOut(GameTestHelper h) {
        LightsTests.ALL.get("pane_light_reaches_the_world_and_goes_out").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void redstonePaneColorFollowsSignalStrength(GameTestHelper h) {
        LightsTests.ALL.get("redstone_pane_color_follows_signal_strength").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void rightClickCyclesTheStartColor(GameTestHelper h) {
        LightsTests.ALL.get("right_click_cycles_the_start_color").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void panesHaveTheirRecipes(GameTestHelper h) {
        LightsTests.ALL.get("panes_have_their_recipes").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void copperPaneFlipsOnEveryRisingEdge(GameTestHelper h) {
        LightsTests.ALL.get("copper_pane_flips_on_every_rising_edge").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void copperRedstonePaneColorsAndFlips(GameTestHelper h) {
        LightsTests.ALL.get("copper_redstone_pane_colors_and_flips").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        LightsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        LightsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplelights:empty")
    public void guideBook(GameTestHelper h) {
        LightsTests.ALL.get("guide_book").accept(h);
    }
}
