package com.simplebuilding.modules.simpleend;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric runs only @GameTest methods: one wrapper per shared test body in {@link EndTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void guideBook(GameTestHelper helper) { EndTests.ALL.get("guide_book").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void registered(GameTestHelper helper) { EndTests.ALL.get("registered").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void woodTagsAndRecipes(GameTestHelper helper) { EndTests.ALL.get("wood_tags_and_recipes").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void axeStripsAndWoodBurns(GameTestHelper helper) { EndTests.ALL.get("axe_strips_and_wood_burns").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void partnerWoodFeatures(GameTestHelper helper) { EndTests.ALL.get("partner_wood_features").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void budLightGrowsTree(GameTestHelper helper) { EndTests.ALL.get("bud_light_grows_tree").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void budDarkGrowsScrub(GameTestHelper helper) { EndTests.ALL.get("bud_dark_grows_scrub").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void budSquareGrowsHugeTree(GameTestHelper helper) { EndTests.ALL.get("bud_square_grows_huge_tree").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void sticksTaperLikeDripstone(GameTestHelper helper) { EndTests.ALL.get("sticks_taper_like_dripstone").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void naturalTreeFeature(GameTestHelper helper) { EndTests.ALL.get("natural_tree_feature").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void leavesDropBudSticksFruit(GameTestHelper helper) { EndTests.ALL.get("leaves_drop_bud_sticks_fruit").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void driftFruitGlide(GameTestHelper helper) { EndTests.ALL.get("drift_fruit_glide").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void voidSafeItems(GameTestHelper helper) { EndTests.ALL.get("void_safe_items").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void biomePatches(GameTestHelper helper) { EndTests.ALL.get("biome_patches").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void submodSwitch(GameTestHelper helper) { EndTests.ALL.get("submod_switch").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void nibblemiteNibblesEndStone(GameTestHelper helper) { EndTests.ALL.get("nibblemite_nibbles_end_stone").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void burrowedEndStoneRecovers(GameTestHelper helper) { EndTests.ALL.get("burrowed_end_stone_recovers").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void nibblemiteIsPassive(GameTestHelper helper) { EndTests.ALL.get("nibblemite_is_passive").accept(helper); }
    @GameTest(structure = "simpleend:grove", maxTicks = 200) public void endGrassOnEndStone(GameTestHelper helper) { EndTests.ALL.get("end_grass_on_end_stone").accept(helper); }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplebuilding.modules.simpleend.discover.EndDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplebuilding.modules.simpleend.discover.EndDiscover.behaviourHints(h); }
}
