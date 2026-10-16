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
    public void guideBook(GameTestHelper h) {
        BlocksTests.ALL.get("guide_book").accept(h);
    }
}
