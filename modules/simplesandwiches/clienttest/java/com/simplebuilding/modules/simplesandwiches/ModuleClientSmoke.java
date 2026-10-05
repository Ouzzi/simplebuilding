package com.simplebuilding.modules.simplesandwiches;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplesandwiches")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplesandwiches-title");
    }
}
