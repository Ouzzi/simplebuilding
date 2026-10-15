package com.simplebuilding.modules.simplewanderers;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Client smoke: the module boots on a client and the title screen renders. */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplewanderers")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplewanderers-title");
    }
}
