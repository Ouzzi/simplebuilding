package com.simplebuilding.modules.simplecrucibles;

import com.simplecrucibles.test.CrucibleTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link CrucibleTests}. Keep in sync with CrucibleTests.ALL. */
public final class ModuleGameTest {
    @GameTest(structure = "simplecrucibles:empty")
    public void configBounds(GameTestHelper h) {
        CrucibleTests.ALL.get("config_bounds").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void configAdoptsSimplelibOptions(GameTestHelper h) {
        CrucibleTests.ALL.get("config_adopts_simplelib_options").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void brokenConfigIsQuarantined(GameTestHelper h) {
        CrucibleTests.ALL.get("broken_config_is_quarantined").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void registered(GameTestHelper h) {
        CrucibleTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void migrationFromSimplelibAndSimplebuilding(GameTestHelper h) {
        CrucibleTests.ALL.get("migration_from_simplelib_and_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleUpgradeHandler(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_upgrade_handler").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void creativeTabRouting(GameTestHelper h) {
        CrucibleTests.ALL.get("creative_tab_routing").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void tierLayout(GameTestHelper h) {
        CrucibleTests.ALL.get("tier_layout").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void heatSources(GameTestHelper h) {
        CrucibleTests.ALL.get("heat_sources").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void heatTwoBelow(GameTestHelper h) {
        CrucibleTests.ALL.get("heat_two_below").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void cooksIntoSlotBelow(GameTestHelper h) {
        CrucibleTests.ALL.get("cooks_into_slot_below").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void mediumHeatBlocksSmelting(GameTestHelper h) {
        CrucibleTests.ALL.get("medium_heat_blocks_smelting").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reservedSlotBlocksAndKeepsProgress(GameTestHelper h) {
        CrucibleTests.ALL.get("reserved_slot_blocks_and_keeps_progress").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void resultNotRecooked(GameTestHelper h) {
        CrucibleTests.ALL.get("result_not_recooked").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void warmsFoodAndEatsFaster(GameTestHelper h) {
        CrucibleTests.ALL.get("warms_food_and_eats_faster").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void afterglowKeepsHeat(GameTestHelper h) {
        CrucibleTests.ALL.get("afterglow_keeps_heat").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void hopperRules(GameTestHelper h) {
        CrucibleTests.ALL.get("hopper_rules").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleHitboxesMatchModel(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_hitboxes_match_model").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeBuildsIronCrucible(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_builds_iron_crucible").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void breakDropsContents(GameTestHelper h) {
        CrucibleTests.ALL.get("break_drops_contents").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradeKeepsContents(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrade_keeps_contents").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void villageKitchenInPools(GameTestHelper h) {
        CrucibleTests.ALL.get("village_kitchen_in_pools").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void barrelAttachAndResultsFirst(GameTestHelper h) {
        CrucibleTests.ALL.get("barrel_attach_and_results_first").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void barrelAttachedLaterTakesOverReservations(GameTestHelper h) {
        CrucibleTests.ALL.get("barrel_attached_later_takes_over_reservations").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void clientMenuShowsTheRaisedStackLimit(GameTestHelper h) {
        CrucibleTests.ALL.get("client_menu_shows_the_raised_stack_limit").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeClickReachesAxeNotMenu(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_click_reaches_axe_not_menu").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradesCauldron(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrades_cauldron").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCauldronHoldsBuckets(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_cauldron_holds_buckets").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCauldronInheritsVanilla(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_cauldron_inherits_vanilla").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void axeUpgradesBarrelToNetherite(GameTestHelper h) {
        CrucibleTests.ALL.get("axe_upgrades_barrel_to_netherite").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleBurnDamageFollowsConfig(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_burn_damage_follows_config").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void looseBarrelMenuKeepsTheRaisedLimit(GameTestHelper h) {
        CrucibleTests.ALL.get("loose_barrel_menu_keeps_the_raised_limit").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void reinforcedCrucibleLoadsOldNineSlots(GameTestHelper h) {
        CrucibleTests.ALL.get("reinforced_crucible_loads_old_nine_slots").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty")
    public void crucibleBarrelBesideInventoryCentred(GameTestHelper h) {
        CrucibleTests.ALL.get("crucible_barrel_beside_inventory_centred").accept(h);
    }

    @GameTest(structure = "simplecrucibles:empty") public void discoverRecipeUnlocks(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.recipeUnlocks(h); }
    @GameTest(structure = "simplecrucibles:empty") public void discoverBehaviourHints(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.behaviourHints(h); }
    @GameTest(structure = "simplecrucibles:empty") public void discoverFirstPickupTips(GameTestHelper h) { com.simplecrucibles.discover.CruciblesDiscover.firstPickupTips(h); }
}
