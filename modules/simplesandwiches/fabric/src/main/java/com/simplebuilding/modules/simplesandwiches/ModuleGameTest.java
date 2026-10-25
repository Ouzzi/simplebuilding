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
    public void endBoardsOnlyWithSimpleEnd(GameTestHelper h) {
        SandwichTests.ALL.get("end_boards_only_with_simple_end").accept(h);
    }

    @GameTest
    public void staleStackRecompute(GameTestHelper h) {
        SandwichTests.ALL.get("stale_stack_recompute").accept(h);
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
    public void cuttingBoardRecipeShape(GameTestHelper h) {
        SandwichTests.ALL.get("cutting_board_recipe_shape").accept(h);
    }

    @GameTest
    public void slipperyFromButter(GameTestHelper h) {
        SandwichTests.ALL.get("slippery_from_butter").accept(h);
    }

    @GameTest
    public void slipperyFriction(GameTestHelper h) {
        SandwichTests.ALL.get("slippery_friction").accept(h);
    }

    @GameTest
    public void butterCarpetPlaceBreak(GameTestHelper h) {
        SandwichTests.ALL.get("butter_carpet_place_break").accept(h);
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
    public void cauldronButterToCheese(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_butter_to_cheese").accept(h);
    }

    @GameTest
    public void cauldronHopperHarvestsButter(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_hopper_harvests_butter").accept(h);
    }

    @GameTest
    public void cauldronLockedHopperWaits(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_locked_hopper_waits").accept(h);
    }

    @GameTest
    public void cauldronLegacyMigrates(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_legacy_migrates").accept(h);
    }

    @GameTest(maxTicks = 600)
    public void cauldronRipensInWorld(GameTestHelper h) {
        SandwichTests.ALL.get("cauldron_ripens_in_world").accept(h);
    }

    @GameTest
    public void bundleEating(GameTestHelper h) {
        SandwichTests.ALL.get("bundle_eating").accept(h);
    }

    @GameTest
    public void subModSwitch(GameTestHelper h) {
        SandwichTests.ALL.get("sub_mod_switch").accept(h);
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
    @GameTest
    public void noEnderiteWithoutRequester(GameTestHelper h) {
        SandwichTests.ALL.get("no_enderite_without_requester").accept(h);
    }
    @GameTest public void vanillaTabStacks(GameTestHelper h) { SandwichTests.ALL.get("vanilla_tab_stacks").accept(h); }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplesandwiches.discover.SandwichDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplesandwiches.discover.SandwichDiscover.behaviourHints(h); }
    @GameTest public void discoverFirstPickupTips(GameTestHelper h) { com.simplesandwiches.discover.SandwichDiscover.firstPickupTips(h); }

    @GameTest
    public void jeiSpecialWays(GameTestHelper h) {
        com.simplesandwiches.test.SandwichJeiTests.specialWays(h);
    }
}
