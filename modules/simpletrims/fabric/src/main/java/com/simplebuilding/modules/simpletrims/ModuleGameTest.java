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
    public void ids(GameTestHelper h) {
        TrimsTests.ALL.get("ids").accept(h);
    }
}
