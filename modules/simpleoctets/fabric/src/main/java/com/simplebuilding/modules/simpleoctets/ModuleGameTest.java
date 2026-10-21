package com.simplebuilding.modules.simpleoctets;

import com.simpleoctets.test.OctetTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link OctetTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleoctets:empty")
    public void registered(GameTestHelper h) {
        OctetTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void stairsAndSlabsHaveOctets(GameTestHelper h) {
        OctetTests.ALL.get("stairs_and_slabs_have_octets").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void subModSwitch(GameTestHelper h) {
        OctetTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        OctetTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void foreignMaterialsOnlyWithTheirMod(GameTestHelper h) {
        OctetTests.ALL.get("foreign_materials_only_with_their_mod").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void recipesCutAndCraftBack(GameTestHelper h) {
        OctetTests.ALL.get("recipes_cut_and_craft_back").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void cellsForCarvingTools(GameTestHelper h) {
        OctetTests.ALL.get("cells_for_carving_tools").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void octetsPlaceIntoTheSubGrid(GameTestHelper h) {
        OctetTests.ALL.get("octets_place_into_the_sub_grid").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void octetsMixMaterialsInOneCell(GameTestHelper h) {
        OctetTests.ALL.get("octets_mix_materials_in_one_cell").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void sneakingBreaksOnlyTheLookedAtOctet(GameTestHelper h) {
        OctetTests.ALL.get("sneaking_breaks_only_the_looked_at_octet").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void mixedCellPersistsAndDropsEveryMaterial(GameTestHelper h) {
        OctetTests.ALL.get("mixed_cell_persists_and_drops_every_material").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void melonSlicePlacesMelonOctets(GameTestHelper h) {
        OctetTests.ALL.get("melon_slice_places_melon_octets").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void logOctetsMineWithTheAxe(GameTestHelper h) {
        OctetTests.ALL.get("log_octets_mine_with_the_axe").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void woodAndWoolOctetsBurn(GameTestHelper h) {
        OctetTests.ALL.get("wood_and_wool_octets_burn").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        OctetTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        OctetTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleoctets:empty")
    public void discoverFirstPickupTips(GameTestHelper h) {
        OctetTests.ALL.get("discover_first_pickup_tips").accept(h);
    }
}
