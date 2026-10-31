package com.simplebuilding.modules.simplefoundry;

import com.simplefoundry.test.FoundryTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link FoundryTests}, all on the 16 x 8 x 16 stone arena. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simplefoundry", false); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void registered(GameTestHelper h) { FoundryTests.ALL.get("registered").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void configClamps(GameTestHelper h) { FoundryTests.ALL.get("config_clamps").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void subModSwitch(GameTestHelper h) { FoundryTests.ALL.get("sub_mod_switch").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void layers(GameTestHelper h) { FoundryTests.ALL.get("layers").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void tables(GameTestHelper h) { FoundryTests.ALL.get("tables").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void scan(GameTestHelper h) { FoundryTests.ALL.get("scan").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void meltAndPour(GameTestHelper h) { FoundryTests.ALL.get("melt_and_pour").accept(h); }

    @GameTest(structure = "simplefoundry:arena", maxTicks = 100)
    public void capacityAndSave(GameTestHelper h) { FoundryTests.ALL.get("capacity_and_save").accept(h); }
}
