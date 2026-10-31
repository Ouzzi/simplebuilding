package com.simplebuilding.modules.simplelivemap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter: ids module_game_test_<name>, bodies in {@link LivemapTests}. */
public final class ModuleGameTest {
    @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simplelivemap", false); }
    @GameTest public void configDefaults(GameTestHelper h) { LivemapTests.configDefaults(h); }
    @GameTest public void visitRecords(GameTestHelper h) { LivemapTests.visitRecords(h); }
    @GameTest public void visitPersistence(GameTestHelper h) { LivemapTests.visitPersistence(h); }
    @GameTest public void mapItemsExplore(GameTestHelper h) { LivemapTests.mapItemsExplore(h); }
    @GameTest(structure = "simplelivemap:empty") public void tileColor(GameTestHelper h) { LivemapTests.tileColor(h); }
    @GameTest public void tileExploredOnly(GameTestHelper h) { LivemapTests.tileExploredOnly(h); }
    @GameTest public void httpOffDefault(GameTestHelper h) { LivemapTests.httpOffDefault(h); }
    @GameTest public void httpVisibility(GameTestHelper h) { LivemapTests.httpVisibility(h); }
    @GameTest public void httpTraversal(GameTestHelper h) { LivemapTests.httpTraversal(h); }
    @GameTest public void httpPlayersFields(GameTestHelper h) { LivemapTests.httpPlayersFields(h); }
    @GameTest public void httpRateLimit(GameTestHelper h) { LivemapTests.httpRateLimit(h); }
}
