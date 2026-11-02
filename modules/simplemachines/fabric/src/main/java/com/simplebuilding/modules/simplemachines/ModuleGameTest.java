package com.simplebuilding.modules.simplemachines;

import com.simplemachines.test.MachinesTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link MachinesTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplemachines:empty")
    public void registered(GameTestHelper h) {
        MachinesTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void switchOffRemovesRecipe(GameTestHelper h) {
        MachinesTests.ALL.get("switch_off_removes_recipe").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void oldIdsMove(GameTestHelper h) {
        MachinesTests.ALL.get("old_ids_move").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void subModSwitch(GameTestHelper h) {
        MachinesTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void recipesCraft(GameTestHelper h) {
        MachinesTests.ALL.get("recipes_craft").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void dampenerSubtractsTheStage(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_subtracts_the_stage").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void dampenerWaitsOneRedstoneTick(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_waits_one_redstone_tick").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void dampenerStageCyclesOnRightClick(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_stage_cycles_on_right_click").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 40)
    public void dampenerIgnoresSideInputs(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_ignores_side_inputs").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void dampenerSwitchOffOutputsNothing(GameTestHelper h) {
        MachinesTests.ALL.get("dampener_switch_off_outputs_nothing").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void doublePistonPlacesOneCell(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_places_one_cell").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void doublePistonStageOneForWeakSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stage_one_for_weak_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 100)
    public void doublePistonStageTwoForStrongSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stage_two_for_strong_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 140)
    public void doublePistonPushesAndRetracts(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_pushes_and_retracts").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 160)
    public void doublePistonStepsBackOnWeakerSignal(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_steps_back_on_weaker_signal").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 120)
    public void doublePistonPushLimit(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_push_limit").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 60)
    public void doublePistonStoppedByObsidian(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_stopped_by_obsidian").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 40)
    public void doublePistonCannotBePushed(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_cannot_be_pushed").accept(h);
    }

    @GameTest(structure = "simplemachines:empty", maxTicks = 120)
    public void doublePistonFallsAsUnit(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_falls_as_unit").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void doublePistonSwitchOffKeepsItStill(GameTestHelper h) {
        MachinesTests.ALL.get("double_piston_switch_off_keeps_it_still").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        MachinesTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        MachinesTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplemachines:empty")
    public void guideBook(GameTestHelper h) {
        MachinesTests.ALL.get("guide_book").accept(h);
    }
}
