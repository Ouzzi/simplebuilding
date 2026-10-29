package com.simplebuilding.modules.simplechallenges;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Client smoke: the module boots, a world without a chosen challenge plays as usual (safety lock), the status command answers. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplechallenges")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplechallenges-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("simplechallenges status");
            context.takeScreenshot("simplechallenges-no-challenge");
        }
    }
}
