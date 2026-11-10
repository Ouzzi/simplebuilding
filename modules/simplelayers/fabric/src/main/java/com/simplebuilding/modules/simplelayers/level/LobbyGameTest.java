package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "lobby" (BACK22, owned by its level branch): one method per {@code lobby_*} test of {@code Band.tests}. */
public final class LobbyGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void lobbyRegistered(GameTestHelper h) {
        LayersTests.ALL.get("lobby_registered").accept(h);
    }
}
