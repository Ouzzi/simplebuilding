package com.simplebuilding.modules.simplearrows;

import com.simplearrows.test.ArrowsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link ArrowsTests}. */
public final class ModuleGameTest {
    /** b-fix09a: info pages of the crafted, Light and Silver Arrow (registered in ArrowsFabric), texts in EN and DE. */
    @GameTest(structure = "simplearrows:empty")
    public void jeiInfoPages(GameTestHelper h) {
        com.simplelib.test.ViewerInfoTests.infoPages(h, "simplearrows", 3);
    }

    @GameTest(structure = "simplearrows:empty")
    public void registered(GameTestHelper h) {
        ArrowsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void oldSilverArrowIdsMove(GameTestHelper h) {
        ArrowsTests.ALL.get("old_silver_arrow_ids_move").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void subModSwitch(GameTestHelper h) {
        ArrowsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void silverArrowRecipeNeedsSimpleOres(GameTestHelper h) {
        ArrowsTests.ALL.get("silver_arrow_recipe_needs_simple_ores").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void silverArrowUndeadBonus(GameTestHelper h) {
        ArrowsTests.ALL.get("silver_arrow_undead_bonus").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowRecipe(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_recipe").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowSetsAndRemovesLight(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_sets_and_removes_light").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowDespawnsLikeAnArrow(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_despawns_like_an_arrow").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowLightIsShared(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_light_is_shared").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowLightFollowsSaveData(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_light_follows_save_data").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void lightArrowSwitchOff(GameTestHelper h) {
        ArrowsTests.ALL.get("light_arrow_switch_off").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingEachTipAddsItsDamageAgainstItsTargets(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_each_tip_adds_its_damage_against_its_targets").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingShaftsAndFletchingsChangeTheFlight(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_shafts_and_fletchings_change_the_flight").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingMaterialShaftsAreOnlyTheDiamondRod(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_material_shafts_are_only_the_diamond_rod").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingOldNetheriteAndEnderiteShaftsLoadAsSticks(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_old_netherite_and_enderite_shafts_load_as_sticks").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingTheTableMakesFourArrowsFromThreeParts(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_the_table_makes_four_arrows_from_three_parts").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingRecipeBookPlacesThePartsFromTheInventory(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_recipe_book_places_the_parts_from_the_inventory").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingEveryPartHasItsRecipeBookEntry(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_every_part_has_its_recipe_book_entry").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingRightClickingTheFletchingTableOpensTheMenu(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_right_clicking_the_fletching_table_opens_the_menu").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingPotionSlotMakesTippedArrows(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_potion_slot_makes_tipped_arrows").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void oldFletchingIdsMove(GameTestHelper h) {
        ArrowsTests.ALL.get("old_fletching_ids_move").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void fletchingLayout(GameTestHelper h) {
        ArrowsTests.ALL.get("fletching_layout").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void amethystArrowIsNotRecovered(GameTestHelper h) {
        ArrowsTests.ALL.get("amethyst_arrow_is_not_recovered").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void dummyHeadCountsForTips(GameTestHelper h) {
        ArrowsTests.ALL.get("dummy_head_counts_for_tips").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void tipTagsAreData(GameTestHelper h) {
        ArrowsTests.ALL.get("tip_tags_are_data").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        ArrowsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        ArrowsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplearrows:empty")
    public void guideBook(GameTestHelper h) {
        ArrowsTests.ALL.get("guide_book").accept(h);
    }
}
