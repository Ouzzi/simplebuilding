package com.simplebuilding.modules.simplemaps;

import com.simplemaps.test.MapsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link MapsTests}. */
public final class ModuleGameTest {
    @GameTest
    public void registered(GameTestHelper h) {
        MapsTests.ALL.get("registered").accept(h);
    }

    @GameTest
    public void configBounds(GameTestHelper h) {
        MapsTests.ALL.get("config_bounds").accept(h);
    }

    @GameTest
    public void waypoints(GameTestHelper h) {
        MapsTests.ALL.get("waypoints").accept(h);
    }

    @GameTest
    public void tileStorage(GameTestHelper h) {
        MapsTests.ALL.get("tile_storage").accept(h);
    }

    @GameTest
    public void revealExplores(GameTestHelper h) {
        MapsTests.ALL.get("reveal_explores").accept(h);
    }

    @GameTest
    public void dimensionBinding(GameTestHelper h) {
        MapsTests.ALL.get("dimension_binding").accept(h);
    }

    @GameTest
    public void cartographyCopy(GameTestHelper h) {
        MapsTests.ALL.get("cartography_copy").accept(h);
    }

    @GameTest
    public void cartographyExtend(GameTestHelper h) {
        MapsTests.ALL.get("cartography_extend").accept(h);
    }

    @GameTest
    public void cartographyCombine(GameTestHelper h) {
        MapsTests.ALL.get("cartography_combine").accept(h);
    }

    @GameTest
    public void cartographySwitches(GameTestHelper h) {
        MapsTests.ALL.get("cartography_switches").accept(h);
    }

    @GameTest
    public void waypointEdit(GameTestHelper h) {
        MapsTests.ALL.get("waypoint_edit").accept(h);
    }

    @GameTest
    public void frameView(GameTestHelper h) {
        MapsTests.ALL.get("frame_view").accept(h);
    }

    @GameTest
    public void frameSaveRules(GameTestHelper h) {
        MapsTests.ALL.get("frame_save_rules").accept(h);
    }

    @GameTest
    public void frameCode(GameTestHelper h) {
        MapsTests.ALL.get("frame_code").accept(h);
    }

    @GameTest
    public void stackSize(GameTestHelper h) {
        MapsTests.ALL.get("stack_size").accept(h);
    }

    @GameTest
    public void loot(GameTestHelper h) {
        MapsTests.ALL.get("loot").accept(h);
    }

    @GameTest
    public void structureMarks(GameTestHelper h) {
        MapsTests.ALL.get("structure_marks").accept(h);
    }

    @GameTest
    public void hostSwitch(GameTestHelper h) {
        MapsTests.ALL.get("host_switch").accept(h);
    }

    @GameTest
    public void screenLayout(GameTestHelper h) {
        MapsTests.ALL.get("screen_layout").accept(h);
    }
    @GameTest
    public void layers(GameTestHelper h) {
        MapsTests.ALL.get("layers").accept(h);
    }
    @GameTest public void layersOwnChunks(GameTestHelper h) { MapsTests.ALL.get("layers_own_chunks").accept(h); }
    @GameTest public void otherDimensionTiles(GameTestHelper h) { MapsTests.ALL.get("other_dimension_tiles").accept(h); }
    @GameTest public void shopIcon(GameTestHelper h) { MapsTests.ALL.get("shop_icon").accept(h); }
    @GameTest public void claimsLayerAbsent(GameTestHelper h) { MapsTests.ALL.get("claims_layer_absent").accept(h); }
    @GameTest public void webShops(GameTestHelper h) { MapsTests.ALL.get("web_shops").accept(h); }
    @GameTest public void webTileColors(GameTestHelper h) { MapsTests.ALL.get("web_tile_colors").accept(h); }
    @GameTest public void webThrottle(GameTestHelper h) { MapsTests.ALL.get("web_throttle").accept(h); }
    @GameTest public void webExploredOnly(GameTestHelper h) { MapsTests.ALL.get("web_explored_only").accept(h); }
    @GameTest public void webHideClaims(GameTestHelper h) { MapsTests.ALL.get("web_hide_claims").accept(h); }
    @GameTest public void webMarkersOptIn(GameTestHelper h) { MapsTests.ALL.get("web_markers_opt_in").accept(h); }
    @GameTest public void webAutoOff(GameTestHelper h) { MapsTests.ALL.get("web_auto_off").accept(h); }
    @GameTest public void webCommandPermissions(GameTestHelper h) { MapsTests.ALL.get("web_command_permissions").accept(h); }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplemaps.discover.MapsDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplemaps.discover.MapsDiscover.behaviourHints(h); }
    @GameTest public void guideBook(GameTestHelper h) { com.simplebuilding.modules.simplemaps.guide.MapsGuide.gameTest(h); }
}
