package com.simplebuilding.modules.simplestorage;

import com.simplestorage.test.StorageTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link StorageTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplestorage:empty")
    public void registered(GameTestHelper h) {
        StorageTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        StorageTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void recipes(GameTestHelper h) {
        StorageTests.ALL.get("recipes").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void cratesBurnAndFuelLikePlanks(GameTestHelper h) {
        StorageTests.ALL.get("crates_burn_and_fuel_like_planks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void subModSwitch(GameTestHelper h) {
        StorageTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void guideBook(GameTestHelper h) {
        StorageTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        StorageTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        StorageTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void discoverFirstPickupTips(GameTestHelper h) {
        StorageTests.ALL.get("discover_first_pickup_tips").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesStoreFoodForHoppersAndComparators(GameTestHelper h) {
        StorageTests.ALL.get("crates_store_food_for_hoppers_and_comparators").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesTakeAtMostNineKinds(GameTestHelper h) {
        StorageTests.ALL.get("crates_take_at_most_nine_kinds").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesLayerTheNewestKindOnTop(GameTestHelper h) {
        StorageTests.ALL.get("crates_layer_the_newest_kind_on_top").accept(h);
    }

    @GameTest(structure = "simplestorage:empty", maxTicks = 200)
    public void cratesTurnLikeBarrelsAndKeepTheirFood(GameTestHelper h) {
        StorageTests.ALL.get("crates_turn_like_barrels_and_keep_their_food").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableKeepsItsGrid(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_keeps_its_grid").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableSharesItsGrid(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_shares_its_grid").accept(h);
    }

    @GameTest(structure = "simplestorage:empty")
    public void storageCraftingTableDropsItsGridAndIgnoresHoppers(GameTestHelper h) {
        StorageTests.ALL.get("storage_crafting_table_drops_its_grid_and_ignores_hoppers").accept(h);
    }
}
