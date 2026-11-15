package com.simplebuilding.modules.simpleenchanting;

import com.simpleenchanting.test.EnchantingTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link EnchantingTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleenchanting:empty")
    public void registered(GameTestHelper h) {
        EnchantingTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void oldIdsMove(GameTestHelper h) {
        EnchantingTests.ALL.get("old_ids_move").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void buildingBlocksByInstallation(GameTestHelper h) {
        EnchantingTests.ALL.get("building_blocks_by_installation").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazewoodIdsMigrate(GameTestHelper h) {
        EnchantingTests.ALL.get("blazewood_ids_migrate").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void libShelvesAreStrongShelves(GameTestHelper h) {
        EnchantingTests.ALL.get("lib_shelves_are_strong_shelves").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazeBookAndShelfRecipes(GameTestHelper h) {
        EnchantingTests.ALL.get("blaze_book_and_shelf_recipes").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void chiseledBlazewoodShelvesStoreBooks(GameTestHelper h) {
        EnchantingTests.ALL.get("chiseled_blazewood_shelves_store_books").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazingObsidianKeepsFireBurning(GameTestHelper h) {
        EnchantingTests.ALL.get("blazing_obsidian_keeps_fire_burning").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void budgetAndCostRules(GameTestHelper h) {
        EnchantingTests.ALL.get("budget_and_cost_rules").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void shelvesAndFloorSetTheTier(GameTestHelper h) {
        EnchantingTests.ALL.get("shelves_and_floor_set_the_tier").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void upgradesAreLoaded(GameTestHelper h) {
        EnchantingTests.ALL.get("upgrades_are_loaded").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void breakingDropsTableNuggetsAndStock(GameTestHelper h) {
        EnchantingTests.ALL.get("breaking_drops_table_nuggets_and_stock").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void stockStaysInTheTable(GameTestHelper h) {
        EnchantingTests.ALL.get("stock_stays_in_the_table").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void enchantingUsesUpWhatTheRulesSay(GameTestHelper h) {
        EnchantingTests.ALL.get("enchanting_uses_up_what_the_rules_say").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void goldenAppleBecomesEnchanted(GameTestHelper h) {
        EnchantingTests.ALL.get("golden_apple_becomes_enchanted").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void endgameLevelRules(GameTestHelper h) {
        EnchantingTests.ALL.get("endgame_level_rules").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void endgameLevelsAtTheTable(GameTestHelper h) {
        EnchantingTests.ALL.get("endgame_levels_at_the_table").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void configTakesOverTheEndgameSwitch(GameTestHelper h) {
        EnchantingTests.ALL.get("config_takes_over_the_endgame_switch").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void endShelvesCountDouble(GameTestHelper h) {
        EnchantingTests.ALL.get("end_shelves_count_double").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void chiseledShelvesTakeModBooks(GameTestHelper h) {
        EnchantingTests.ALL.get("chiseled_shelves_take_mod_books").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void endShelfDropsItsBooks(GameTestHelper h) {
        EnchantingTests.ALL.get("end_shelf_drops_its_books").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazewoodFamilyRegistered(GameTestHelper h) {
        EnchantingTests.ALL.get("blazewood_family_registered").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazewoodRecipes(GameTestHelper h) {
        EnchantingTests.ALL.get("blazewood_recipes").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void blazewoodDoorHasTwoHalves(GameTestHelper h) {
        EnchantingTests.ALL.get("blazewood_door_has_two_halves").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void configLabels(GameTestHelper h) {
        EnchantingTests.ALL.get("config_labels").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        EnchantingTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        EnchantingTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void guideBook(GameTestHelper h) {
        EnchantingTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simpleenchanting:empty")
    public void enchanterScreenMargins(GameTestHelper h) {
        EnchantingTests.ALL.get("enchanter_screen_margins").accept(h);
    }
}
