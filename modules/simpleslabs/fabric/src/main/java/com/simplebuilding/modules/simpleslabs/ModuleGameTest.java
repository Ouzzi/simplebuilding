package com.simplebuilding.modules.simpleslabs;

import com.simpleslabs.test.SlabTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link SlabTests} (N86). */
public final class ModuleGameTest {
    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void registered(GameTestHelper h) {
        SlabTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void everySlabHasItsHalfSlab(GameTestHelper h) {
        SlabTests.ALL.get("every_slab_has_its_half_slab").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void ownerLayerPatternsByClicks(GameTestHelper h) {
        SlabTests.ALL.get("owner_layer_patterns_by_clicks").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void topAndBottomPlacement(GameTestHelper h) {
        SlabTests.ALL.get("top_and_bottom_placement").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void shapesFollowTheLayers(GameTestHelper h) {
        SlabTests.ALL.get("shapes_follow_the_layers").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void sideHalfSlabAndSideSlab(GameTestHelper h) {
        SlabTests.ALL.get("side_half_slab_and_side_slab").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void halfSlabOntoItsSlab(GameTestHelper h) {
        SlabTests.ALL.get("half_slab_onto_its_slab").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void everyLayerDrops(GameTestHelper h) {
        SlabTests.ALL.get("every_layer_drops").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void waterAndFullCells(GameTestHelper h) {
        SlabTests.ALL.get("water_and_full_cells").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void copperHalfSlabsAgeLikeCopper(GameTestHelper h) {
        SlabTests.ALL.get("copper_half_slabs_age_like_copper").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void sandAndGravelHalfSlabsFall(GameTestHelper h) {
        SlabTests.ALL.get("sand_and_gravel_half_slabs_fall").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void fallingLayersMerge(GameTestHelper h) {
        SlabTests.ALL.get("falling_layers_merge").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void grassHalfSlabIsTintedGrass(GameTestHelper h) {
        SlabTests.ALL.get("grass_half_slab_is_tinted_grass").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void recipesCutAndCraftBack(GameTestHelper h) {
        SlabTests.ALL.get("recipes_cut_and_craft_back").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void woodAndWoolBurn(GameTestHelper h) {
        SlabTests.ALL.get("wood_and_wool_burn").accept(h);
    }

    @GameTest(structure = "simpleslabs:empty", maxTicks = 100)
    public void creativeTabAndSwitch(GameTestHelper h) {
        SlabTests.ALL.get("creative_tab_and_switch").accept(h);
    }
}
