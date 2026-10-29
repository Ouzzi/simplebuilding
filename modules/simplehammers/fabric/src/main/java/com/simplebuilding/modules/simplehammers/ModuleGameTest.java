package com.simplebuilding.modules.simplehammers;

import com.simplehammers.test.HammerTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link HammerTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplehammers:empty")
    public void registered(GameTestHelper h) {
        HammerTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        HammerTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void configDefaults(GameTestHelper h) {
        HammerTests.ALL.get("config_defaults").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void configAdoptsSimplebuildingTools(GameTestHelper h) {
        HammerTests.ALL.get("config_adopts_simplebuilding_tools").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void subModSwitch(GameTestHelper h) {
        HammerTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void areaBreak(GameTestHelper h) {
        HammerTests.ALL.get("area_break").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void carvingMakesStairs(GameTestHelper h) {
        HammerTests.ALL.get("carving_makes_stairs").accept(h);
    }

    @GameTest(structure = "simplehammers:empty", maxTicks = 200)
    public void diamondBlockCrush(GameTestHelper h) {
        HammerTests.ALL.get("diamond_block_crush").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void hammerTempo(GameTestHelper h) {
        HammerTests.ALL.get("hammer_tempo").accept(h);
    }

    @GameTest(structure = "simplehammers:empty", maxTicks = 200)
    public void stoneCrush(GameTestHelper h) {
        HammerTests.ALL.get("stone_crush").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void upgradeTable(GameTestHelper h) {
        HammerTests.ALL.get("upgrade_table").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void upgradeTooltipHints(GameTestHelper h) {
        HammerTests.ALL.get("upgrade_tooltip_hints").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void hammerStaysInTheGrid(GameTestHelper h) {
        HammerTests.ALL.get("hammer_stays_in_the_grid").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void guideBook(GameTestHelper h) {
        HammerTests.ALL.get("guide_book").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void discoverRecipeUnlocks(GameTestHelper h) {
        HammerTests.ALL.get("discover_recipe_unlocks").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void discoverBehaviourHints(GameTestHelper h) {
        HammerTests.ALL.get("discover_behaviour_hints").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void discoverFirstPickupTips(GameTestHelper h) {
        HammerTests.ALL.get("discover_first_pickup_tips").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void anvilHammerCombine(GameTestHelper h) {
        HammerTests.ALL.get("anvil_hammer_combine").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void anvilHammerRepair(GameTestHelper h) {
        HammerTests.ALL.get("anvil_hammer_repair").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void jeiStandaloneWays(GameTestHelper h) {
        HammerTests.ALL.get("jei_standalone_ways").accept(h);
    }

    @GameTest(structure = "simplehammers:empty")
    public void jeiToolGrades(GameTestHelper h) {
        HammerTests.ALL.get("jei_tool_grades").accept(h);
    }
}
