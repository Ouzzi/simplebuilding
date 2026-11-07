package com.simplebuilding.modules.simplechisels;

import com.simplechisels.test.ChiselTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link ChiselTests}. */
public final class ModuleGameTest {
    @GameTest(structure = "simplechisels:empty")
    public void registered(GameTestHelper h) {
        ChiselTests.ALL.get("registered").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void migrationFromSimplebuilding(GameTestHelper h) {
        ChiselTests.ALL.get("migration_from_simplebuilding").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void forwardAndBack(GameTestHelper h) {
        ChiselTests.ALL.get("forward_and_back").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void tiersInherit(GameTestHelper h) {
        ChiselTests.ALL.get("tiers_inherit").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void datapackTables(GameTestHelper h) {
        ChiselTests.ALL.get("datapack_tables").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void fastChiselingCooldown(GameTestHelper h) {
        ChiselTests.ALL.get("fast_chiseling_cooldown").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void configDefaultsAndAdoption(GameTestHelper h) {
        ChiselTests.ALL.get("config_defaults_and_adoption").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void subModSwitch(GameTestHelper h) {
        ChiselTests.ALL.get("sub_mod_switch").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void enderiteNeedsSimpleOres(GameTestHelper h) {
        ChiselTests.ALL.get("enderite_needs_simple_ores").accept(h);
    }

    @GameTest(structure = "simplechisels:empty")
    public void withSimplebuilding(GameTestHelper h) {
        ChiselTests.ALL.get("with_simplebuilding").accept(h);
    }
}
