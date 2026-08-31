package com.simplebuilding.clienttest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Proves the client test harness works: boot a real client, create a world, join
 * it and take a screenshot. Renderer tests build on this.
 */
public final class SmokeClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.takeScreenshot("smoke-main-menu");

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.takeScreenshot("smoke-in-world");
            context.waitTicks(20);
        }
    }
}
