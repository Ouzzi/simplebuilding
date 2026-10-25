package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.test.EarthTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link EarthTests}, all on simpleearth:empty. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void configDefaults(GameTestHelper h) {
        EarthTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void configCommand(GameTestHelper h) {
        EarthTests.ALL.get("config_command").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void colorTable(GameTestHelper h) {
        EarthTests.ALL.get("color_table").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 400)
    public void tileColorsFromBiomes(GameTestHelper h) {
        EarthTests.ALL.get("tile_colors_from_biomes").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void tileCodecAndStore(GameTestHelper h) {
        EarthTests.ALL.get("tile_codec_and_store").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void exploredOnlyHidesUnexplored(GameTestHelper h) {
        EarthTests.ALL.get("explored_only_hides_unexplored").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 400)
    public void tileRequestsValidated(GameTestHelper h) {
        EarthTests.ALL.get("tile_requests_validated").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void orbitLifecycle(GameTestHelper h) {
        EarthTests.ALL.get("orbit_lifecycle").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        EarthTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void viewsInGame(GameTestHelper h) {
        EarthTests.ALL.get("views_in_game").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void renderConfig(GameTestHelper h) {
        EarthTests.ALL.get("render_config").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void orbitOptions(GameTestHelper h) {
        EarthTests.ALL.get("orbit_options").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void biomeTable(GameTestHelper h) {
        EarthTests.ALL.get("biome_table").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void surfaceRules(GameTestHelper h) {
        EarthTests.ALL.get("surface_rules").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 400)
    public void samplingSpeed(GameTestHelper h) {
        EarthTests.ALL.get("sampling_speed").accept(h);
    }

    @GameTest(structure = "simpleearth:empty", maxTicks = 100)
    public void guidePages(GameTestHelper h) {
        com.simplebuilding.modules.simpleearth.guide.EarthGuide.gameTest(h);
    }
}
