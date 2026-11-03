package com.simplebuilding.modules.simplearrows;

import com.simplearrows.test.ArrowsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link ArrowsTests}. */
public final class ModuleGameTest {
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
}
