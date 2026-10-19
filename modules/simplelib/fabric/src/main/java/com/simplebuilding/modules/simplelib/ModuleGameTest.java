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

    @GameTest
    public void warpedWartGrowsLikeNetherWart(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_grows_like_nether_wart").accept(h);
    }

    @GameTest
    public void warpedWartHarvestDrops(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_harvest_drops").accept(h);
    }

    @GameTest
    public void warpedWartIsCompostableAndPlantable(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_is_compostable_and_plantable").accept(h);
    }

    @GameTest
    public void warpedWartInBastionChests(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_in_bastion_chests").accept(h);
    }

    @GameTest
    public void warpedWartBrewsTheWarpedPotion(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_brews_the_warped_potion").accept(h);
    }

    @GameTest
    public void warpedWartBlockRecipes(GameTestHelper h) {
        LibTests.ALL.get("warped_wart_block_recipes").accept(h);
    }

    @GameTest
    public void pixelPaletteLabNearest(GameTestHelper h) {
        LibTests.ALL.get("pixel_palette_lab_nearest").accept(h);
    }

    @GameTest
    public void pixelDitherDeterministic(GameTestHelper h) {
        LibTests.ALL.get("pixel_dither_deterministic").accept(h);
    }

    @GameTest
    public void pixelRleRoundTrip(GameTestHelper h) {
        LibTests.ALL.get("pixel_rle_round_trip").accept(h);
    }

    @GameTest
    public void pixelSizeLimit(GameTestHelper h) {
        LibTests.ALL.get("pixel_size_limit").accept(h);
    }
}
