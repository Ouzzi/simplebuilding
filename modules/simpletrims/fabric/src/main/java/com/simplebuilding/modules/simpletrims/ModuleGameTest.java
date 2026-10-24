package com.simplebuilding.modules.simpletrims;

import com.simpletrims.test.TrimsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link TrimsTests}. */
public final class ModuleGameTest {
    @GameTest
    public void configDefaults(GameTestHelper h) {
        TrimsTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest
    public void templateToolWithoutSimplebuilding(GameTestHelper h) {
        TrimsTests.ALL.get("template_tool_without_simplebuilding").accept(h);
    }

    @GameTest
    public void templateToolWithSimplebuilding(GameTestHelper h) {
        TrimsTests.ALL.get("template_tool_with_simplebuilding").accept(h);
    }

    @GameTest
    public void toolModeFollowsThePartner(GameTestHelper h) {
        TrimsTests.ALL.get("tool_mode_follows_the_partner").accept(h);
    }

    @GameTest
    public void subModSwitch(GameTestHelper h) {
        TrimsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest
    public void ids(GameTestHelper h) {
        TrimsTests.ALL.get("ids").accept(h);
    }

    @GameTest
    public void registered(GameTestHelper h) {
        TrimsTests.ALL.get("registered").accept(h);
    }

    @GameTest
    public void migrationFromSimplebuilding(GameTestHelper h) {
        TrimsTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest
    public void smithingUpgrades(GameTestHelper h) {
        TrimsTests.ALL.get("smithing_upgrades").accept(h);
    }

    @GameTest
    public void pulsatingRecipeKeepsTheTool(GameTestHelper h) {
        TrimsTests.ALL.get("pulsating_recipe_keeps_the_tool").accept(h);
    }

    @GameTest
    public void trimInversion(GameTestHelper h) {
        TrimsTests.ALL.get("trim_inversion").accept(h);
    }

    @GameTest
    public void duplicationRecipes(GameTestHelper h) {
        TrimsTests.ALL.get("duplication_recipes").accept(h);
    }

    @GameTest
    public void placingTrimTemplates(GameTestHelper h) {
        TrimsTests.ALL.get("placing_trim_templates").accept(h);
    }

    @GameTest
    public void placedTemplatesIn3d(GameTestHelper h) {
        TrimsTests.ALL.get("placed_templates_in3d").accept(h);
    }

    @GameTest
    public void inWorldUpgrade(GameTestHelper h) {
        TrimsTests.ALL.get("in_world_upgrade").accept(h);
    }

    @GameTest
    public void orphanLightSwept(GameTestHelper h) {
        TrimsTests.ALL.get("orphan_light_swept").accept(h);
    }

    @GameTest
    public void radianceLight(GameTestHelper h) {
        TrimsTests.ALL.get("radiance_light").accept(h);
    }

    @GameTest
    public void heldLight(GameTestHelper h) {
        TrimsTests.ALL.get("held_light").accept(h);
    }

    @GameTest
    public void guideBook(GameTestHelper h) {
        TrimsTests.ALL.get("guide_book").accept(h);
    }

    @GameTest
    public void glowRules(GameTestHelper h) {
        TrimsTests.ALL.get("glow_rules").accept(h);
    }

    @GameTest
    public void plateHint(GameTestHelper h) {
        TrimsTests.ALL.get("plate_hint").accept(h);
    }

    @GameTest
    public void foreignLightUntouched(GameTestHelper h) {
        TrimsTests.ALL.get("foreign_light_untouched").accept(h);
    }
}
