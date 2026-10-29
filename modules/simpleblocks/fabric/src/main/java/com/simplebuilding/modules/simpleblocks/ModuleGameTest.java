package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.test.BlocksTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link BlocksTests}, all on the empty 8 x 8 x 8 structure. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void registered(GameTestHelper h) {
        BlocksTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void tagsAndShapes(GameTestHelper h) {
        BlocksTests.ALL.get("tags_and_shapes").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 160)
    public void hushedBlocksVibrations(GameTestHelper h) {
        BlocksTests.ALL.get("hushed_blocks_vibrations").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sculkTiles(GameTestHelper h) {
        BlocksTests.ALL.get("sculk_tiles").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 60)
    public void sculkDeepslateDropsAndXp(GameTestHelper h) {
        BlocksTests.ALL.get("sculk_deepslate_drops_and_xp").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void recipesAndLootExist(GameTestHelper h) {
        BlocksTests.ALL.get("recipes_and_loot_exist").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void ancientCityLoot(GameTestHelper h) {
        BlocksTests.ALL.get("ancient_city_loot").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        BlocksTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        BlocksTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sharedTabWithSimplebuilding(GameTestHelper h) {
        BlocksTests.ALL.get("shared_tab_with_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        BlocksTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void blueNetherBricksRecipes(GameTestHelper h) {
        BlocksTests.ALL.get("blue_nether_bricks_recipes").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void discoverFirstPickupTips(GameTestHelper h) {
        BlocksTests.ALL.get("discover_first_pickup_tips").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawRegistered(GameTestHelper h) {
        BlocksTests.ALL.get("saw_registered").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawWoodCuts(GameTestHelper h) {
        BlocksTests.ALL.get("saw_wood_cuts").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawSupersetOfStonecutter(GameTestHelper h) {
        BlocksTests.ALL.get("saw_superset_of_stonecutter").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawCutsEveryShape(GameTestHelper h) {
        BlocksTests.ALL.get("saw_cuts_every_shape").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawMenuCuts(GameTestHelper h) {
        BlocksTests.ALL.get("saw_menu_cuts").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawOptionalMods(GameTestHelper h) {
        BlocksTests.ALL.get("saw_optional_mods").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawUiContrast(GameTestHelper h) {
        BlocksTests.ALL.get("saw_ui_contrast").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawMenuLayout(GameTestHelper h) {
        BlocksTests.ALL.get("saw_menu_layout").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawIdMoved(GameTestHelper h) {
        BlocksTests.ALL.get("saw_id_moved").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawUpgradeAxe(GameTestHelper h) {
        BlocksTests.ALL.get("saw_upgrade_axe").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawUpgradeHammer(GameTestHelper h) {
        BlocksTests.ALL.get("saw_upgrade_hammer").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sawUpgradeMode(GameTestHelper h) {
        BlocksTests.ALL.get("saw_upgrade_mode").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void natureRecipes(GameTestHelper h) {
        BlocksTests.ALL.get("nature_recipes").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void grassSlabDropsDirtSlabWithoutSilkTouch(GameTestHelper h) {
        BlocksTests.ALL.get("grass_slab_drops_dirt_slab_without_silk_touch").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void sandAndGravelSlabsFallAndMerge(GameTestHelper h) {
        BlocksTests.ALL.get("sand_and_gravel_slabs_fall_and_merge").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void gravitySlabsSettleOnSlabsAndHalfSteps(GameTestHelper h) {
        BlocksTests.ALL.get("gravity_slabs_settle_on_slabs_and_half_steps").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void upperGravitySlabStartsFallingFromItsOwnHeight(GameTestHelper h) {
        BlocksTests.ALL.get("upper_gravity_slab_starts_falling_from_its_own_height").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void naturalSlabsPlaceIntoTheFullBlock(GameTestHelper h) {
        BlocksTests.ALL.get("natural_slabs_place_into_the_full_block").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void crackedIceLeavesNoWater(GameTestHelper h) {
        BlocksTests.ALL.get("cracked_ice_leaves_no_water").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 200)
    public void crackedIceMeltsUnderAnEntityAfterSomeSeconds(GameTestHelper h) {
        BlocksTests.ALL.get("cracked_ice_melts_under_an_entity_after_some_seconds").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void crackedIceBlocksPlacementOnAllSidesAndBlendsWithIce(GameTestHelper h) {
        BlocksTests.ALL.get("cracked_ice_allows_placement_on_all_sides_and_blends_with_ice").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void crackedIceDropsItselfWithSilkTouch(GameTestHelper h) {
        BlocksTests.ALL.get("cracked_ice_drops_itself_with_silk_touch").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void movedBlocksRegistered(GameTestHelper h) {
        BlocksTests.ALL.get("moved_blocks_registered").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void movedIdsFromSimplebuilding(GameTestHelper h) {
        BlocksTests.ALL.get("moved_ids_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void gravityRecipesWithAndWithoutSimplebuilding(GameTestHelper h) {
        BlocksTests.ALL.get("gravity_recipes_with_and_without_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void suspendedBlocksStayLevitatingBlocksRise(GameTestHelper h) {
        BlocksTests.ALL.get("suspended_blocks_stay_levitating_blocks_rise").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void movedBlocksInTheTab(GameTestHelper h) {
        BlocksTests.ALL.get("moved_blocks_in_the_tab").accept(h);
    }

    @GameTest(structure = "simpleblocks:empty", maxTicks = 100)
    public void grassSpreadsOntoAndFromSlabs(GameTestHelper h) {
        BlocksTests.ALL.get("grass_spreads_onto_and_from_slabs").accept(h);
    }
}
