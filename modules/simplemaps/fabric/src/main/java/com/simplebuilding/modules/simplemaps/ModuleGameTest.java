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
}
