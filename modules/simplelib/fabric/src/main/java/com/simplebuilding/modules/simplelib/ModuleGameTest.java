package com.simplebuilding.modules.simplelib;

import com.simplelib.test.LibTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link LibTests}. Keep in sync with LibTests.ALL. */
public final class ModuleGameTest {
    @GameTest
    public void configBounds(GameTestHelper h) {
        LibTests.ALL.get("config_bounds").accept(h);
    }

    @GameTest
    public void brokenConfigIsQuarantined(GameTestHelper h) {
        LibTests.ALL.get("broken_config_is_quarantined").accept(h);
    }

    @GameTest
    public void warmStacksByMean(GameTestHelper h) {
        LibTests.ALL.get("warm_stacks_by_mean").accept(h);
    }

    @GameTest
    public void warmBundleInsulates(GameTestHelper h) {
        LibTests.ALL.get("warm_bundle_insulates").accept(h);
    }

    @GameTest
    public void hoppersFillRaisedSlots(GameTestHelper h) {
        LibTests.ALL.get("hoppers_fill_raised_slots").accept(h);
    }

    @GameTest
    public void idMovesRegistries(GameTestHelper h) {
        LibTests.ALL.get("id_moves_registries").accept(h);
    }

    @GameTest
    public void idMovesRecipeBookAndAdvancements(GameTestHelper h) {
        LibTests.ALL.get("id_moves_recipe_book_and_advancements").accept(h);
    }

    @GameTest
    public void idMovesRefuseLoops(GameTestHelper h) {
        LibTests.ALL.get("id_moves_refuse_loops").accept(h);
    }

    @GameTest
    public void idMovesAdoptConfig(GameTestHelper h) {
        LibTests.ALL.get("id_moves_adopt_config").accept(h);
    }

    @GameTest
    public void inWorldUpgradesDataType(GameTestHelper h) {
        LibTests.ALL.get("in_world_upgrades_data_type").accept(h);
    }

    @GameTest
    public void inWorldUpgradeHandlers(GameTestHelper h) {
        LibTests.ALL.get("in_world_upgrade_handlers").accept(h);
    }

    @GameTest
    public void octetCells(GameTestHelper h) {
        LibTests.ALL.get("octet_cells").accept(h);
    }

    @GameTest
    public void materialRequestsRegistered(GameTestHelper h) {
        LibTests.ALL.get("material_requests_registered").accept(h);
    }

    @GameTest
    public void materialRequestsRecipes(GameTestHelper h) {
        LibTests.ALL.get("material_requests_recipes").accept(h);
    }

    @GameTest(maxTicks = 100)
    public void materialRequestsAnvilCrushing(GameTestHelper h) {
        LibTests.ALL.get("material_requests_anvil_crushing").accept(h);
    }

    @GameTest
    public void inventoryBrightnessTonesPalettes(GameTestHelper h) {
        LibTests.ALL.get("inventory_brightness_tones_palettes").accept(h);
    }

    @GameTest
    public void uiTextContrast(GameTestHelper h) {
        LibTests.ALL.get("ui_text_contrast").accept(h);
    }

    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplelib.discover.LibDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplelib.discover.LibDiscover.behaviourHints(h); }
    @GameTest public void discoverFirstPickupTips(GameTestHelper h) { com.simplelib.discover.LibDiscover.firstPickupTips(h); }
}
