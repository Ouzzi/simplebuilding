package com.simplebuilding.modules.simplebeacons;

import com.simplebeacons.test.BeaconsTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link BeaconsTests}, all on the 15 x 12 x 15 arena. */
public final class ModuleGameTest {
    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void configDefaults(GameTestHelper h) {
        BeaconsTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void registered(GameTestHelper h) {
        BeaconsTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void strongItemsMythic(GameTestHelper h) {
        BeaconsTests.ALL.get("strong_items_mythic").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 200)
    public void pyramidLevels(GameTestHelper h) {
        BeaconsTests.ALL.get("pyramid_levels").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void rangeAndDuration(GameTestHelper h) {
        BeaconsTests.ALL.get("range_and_duration").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void effectsPerLevel(GameTestHelper h) {
        BeaconsTests.ALL.get("effects_per_level").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void effectsReachPlayers(GameTestHelper h) {
        BeaconsTests.ALL.get("effects_reach_players").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void menuChecksTheChoice(GameTestHelper h) {
        BeaconsTests.ALL.get("menu_checks_the_choice").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void menuCarriesServerSettings(GameTestHelper h) {
        BeaconsTests.ALL.get("menu_carries_server_settings").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void blockEntityKeepsChoice(GameTestHelper h) {
        BeaconsTests.ALL.get("block_entity_keeps_choice").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void upgradeInWorld(GameTestHelper h) {
        BeaconsTests.ALL.get("upgrade_in_world").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void upgradeNeedsStarAndStance(GameTestHelper h) {
        BeaconsTests.ALL.get("upgrade_needs_star_and_stance").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void conduitUpgradeKeepsWater(GameTestHelper h) {
        BeaconsTests.ALL.get("conduit_upgrade_keeps_water").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void recipes(GameTestHelper h) {
        BeaconsTests.ALL.get("recipes").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void conduitFrames(GameTestHelper h) {
        BeaconsTests.ALL.get("conduit_frames").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void conduitEffects(GameTestHelper h) {
        BeaconsTests.ALL.get("conduit_effects").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void strongUpgradeShard(GameTestHelper h) {
        BeaconsTests.ALL.get("strong_upgrade_shard").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) {
        BeaconsTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void upgradeToolFollowsThePartner(GameTestHelper h) {
        BeaconsTests.ALL.get("upgrade_tool_follows_the_partner").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void guideBook(GameTestHelper h) {
        BeaconsTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void discoverRecipeUnlocks(GameTestHelper h) {
        BeaconsTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void discoverBehaviourHints(GameTestHelper h) {
        BeaconsTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplebeacons:arena", maxTicks = 100)
    public void discoverFirstPickupTips(GameTestHelper h) {
        BeaconsTests.ALL.get("discover_first_pickup_tips").accept(h);
    }
}
