package com.simplebuilding.modules.simpleovens;

import com.simpleovens.test.OvensTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link OvensTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simpleovens:empty")
    public void configDefaults(GameTestHelper h) {
        OvensTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void registered(GameTestHelper h) {
        OvensTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void subModSwitch(GameTestHelper h) {
        OvensTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        OvensTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simpleovens:empty", maxTicks = 400)
    public void tiersCookFaster(GameTestHelper h) {
        OvensTests.ALL.get("tiers_cook_faster").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void upperTiersPayDoubleExperience(GameTestHelper h) {
        OvensTests.ALL.get("upper_tiers_pay_double_experience").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void everyTierOpensItsVanillaMenu(GameTestHelper h) {
        OvensTests.ALL.get("every_tier_opens_its_vanilla_menu").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void longCookTimesReachTheClient(GameTestHelper h) {
        OvensTests.ALL.get("long_cook_times_reach_the_client").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void recipes(GameTestHelper h) {
        OvensTests.ALL.get("recipes").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void upgradeTable(GameTestHelper h) {
        OvensTests.ALL.get("upgrade_table").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void axeUpgradeAlone(GameTestHelper h) {
        OvensTests.ALL.get("axe_upgrade_alone").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void hammerClickPassesOn(GameTestHelper h) {
        OvensTests.ALL.get("hammer_click_passes_on").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void enderiteLootAlone(GameTestHelper h) {
        OvensTests.ALL.get("enderite_loot_alone").accept(h);
    }

    @GameTest(structure = "simpleovens:empty", maxTicks = 200)
    public void furnaceCartsRunOnFuel(GameTestHelper h) {
        OvensTests.ALL.get("furnace_carts_run_on_fuel").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void configAdoptsSimplebuildingSpeeds(GameTestHelper h) {
        OvensTests.ALL.get("config_adopts_simplebuilding_speeds").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        OvensTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        OvensTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simpleovens:empty")
    public void discoverFirstPickupTips(GameTestHelper h) {
        OvensTests.ALL.get("discover_first_pickup_tips").accept(h);
    }
}
