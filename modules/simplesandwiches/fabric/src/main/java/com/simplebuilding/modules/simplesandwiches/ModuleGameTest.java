package com.simplebuilding.modules.simplesandwiches;

import com.simplesandwiches.test.SandwichTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link SandwichTests}. */
public final class ModuleGameTest {
    @GameTest
    public void guideBook(GameTestHelper h) {
        SandwichTests.ALL.get("guide_book").accept(h);
    }

    @GameTest
    public void configBounds(GameTestHelper h) {
        SandwichTests.ALL.get("config_bounds").accept(h);
    }

    @GameTest
    public void ingredientTagComplete(GameTestHelper h) {
        SandwichTests.ALL.get("ingredient_tag_complete").accept(h);
    }

    @GameTest
    public void hungerSumWithoutCap(GameTestHelper h) {
        SandwichTests.ALL.get("hunger_sum_without_cap").accept(h);
    }

    @GameTest
    public void butterBonus(GameTestHelper h) {
        SandwichTests.ALL.get("butter_bonus").accept(h);
    }

    @GameTest
    public void effectMerge(GameTestHelper h) {
        SandwichTests.ALL.get("effect_merge").accept(h);
    }

    @GameTest
    public void deterministicStacking(GameTestHelper h) {
        SandwichTests.ALL.get("deterministic_stacking").accept(h);
    }

    @GameTest
    public void boardFullCycle(GameTestHelper h) {
        SandwichTests.ALL.get("board_full_cycle").accept(h);
    }

    @GameTest
    public void boardButterOnlyFirst(GameTestHelper h) {
        SandwichTests.ALL.get("board_butter_only_first").accept(h);
    }

    @GameTest
    public void boardBreakDrops(GameTestHelper h) {
        SandwichTests.ALL.get("board_break_drops").accept(h);
    }

    @GameTest
    public void knifeCakeSlices(GameTestHelper h) {
        SandwichTests.ALL.get("knife_cake_slices").accept(h);
    }

    @GameTest
    public void knifeRecipeShape(GameTestHelper h) {
        SandwichTests.ALL.get("knife_recipe_shape").accept(h);
    }

    @GameTest
    public void knifeMelonAndTools(GameTestHelper h) {
        SandwichTests.ALL.get("knife_melon_and_tools").accept(h);
    }

    @GameTest
    public void sliceBlockCutting(GameTestHelper h) {
        SandwichTests.ALL.get("slice_block_cutting").accept(h);
    }

    @GameTest
    public void cauldronButter(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_butter").accept(h);
    }

    @GameTest
    public void cauldronCheeseSpoils(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_cheese_spoils").accept(h);
    }

    @GameTest(maxTicks = 400)
    public void cauldronRipensInWorld(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_ripens_in_world").accept(h);
    }

    @GameTest
    public void bundleEating(GameTestHelper h) {
        SandwichTests.ALL.get("bundle_eating").accept(h);
    }

    @GameTest
    public void cakeSlice(GameTestHelper h) {
        SandwichTests.ALL.get("cake_slice").accept(h);
    }

    @GameTest
    public void boardPriorityOverEating(GameTestHelper h) {
        SandwichTests.ALL.get("board_priority_over_eating").accept(h);
    }

    @GameTest
    public void sliceBlockKeepsState(GameTestHelper h) {
        SandwichTests.ALL.get("slice_block_keeps_state").accept(h);
    }

    @GameTest
    public void cheeseBouncesLikeBed(GameTestHelper h) {
        SandwichTests.ALL.get("cheese_bounces_like_bed").accept(h);
    }

    @GameTest
    public void cutSoundsRegistered(GameTestHelper h) {
        SandwichTests.ALL.get("cut_sounds_registered").accept(h);
    }

    @GameTest
    public void sandwichWarmsInCrucible(GameTestHelper h) {
        SandwichTests.ALL.get("sandwich_warms_in_crucible").accept(h);
    }

    @GameTest
    public void reinforcedMilkCauldron(GameTestHelper h) {
        SandwichTests.ALL.get("reinforced_milk_cauldron").accept(h);
    }
}
