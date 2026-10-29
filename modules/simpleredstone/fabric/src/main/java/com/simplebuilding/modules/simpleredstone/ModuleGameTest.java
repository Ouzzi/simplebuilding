package com.simplebuilding.modules.simpleredstone;

import com.simpleredstone.test.RedstoneTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link RedstoneTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleredstone:empty")
    public void registered(GameTestHelper h) {
        RedstoneTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void subModSwitch(GameTestHelper h) {
        RedstoneTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void recipesCraft(GameTestHelper h) {
        RedstoneTests.ALL.get("recipes_craft").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 200)
    public void dampenerSubtractsTheStage(GameTestHelper h) {
        RedstoneTests.ALL.get("dampener_subtracts_the_stage").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 100)
    public void dampenerWaitsOneRedstoneTick(GameTestHelper h) {
        RedstoneTests.ALL.get("dampener_waits_one_redstone_tick").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void dampenerStageCyclesOnRightClick(GameTestHelper h) {
        RedstoneTests.ALL.get("dampener_stage_cycles_on_right_click").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 60)
    public void dampenerIgnoresSideInputs(GameTestHelper h) {
        RedstoneTests.ALL.get("dampener_ignores_side_inputs").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void doublePistonPlacesTwoCells(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_places_two_cells").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 200)
    public void doublePistonOneBlockForWeakSignal(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_one_block_for_weak_signal").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 200)
    public void doublePistonTwoBlocksForStrongSignal(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_two_blocks_for_strong_signal").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 300)
    public void doublePistonPushesAndRetracts(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_pushes_and_retracts").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 60)
    public void doublePistonCannotBePushed(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_cannot_be_pushed").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty", maxTicks = 200)
    public void doublePistonFallsAsUnit(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_falls_as_unit").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        RedstoneTests.ALL.get("double_piston_switch_off_keeps_it_still").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        RedstoneTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        RedstoneTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleredstone:empty")
    public void guideBook(GameTestHelper h) {
        RedstoneTests.ALL.get("guide_book").accept(h);
    }
}
