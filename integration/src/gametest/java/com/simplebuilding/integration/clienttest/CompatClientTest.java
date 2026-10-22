package com.simplebuilding.integration.clienttest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Simple Optimization compat run (tools/testrunner target {@code compat-opt-263}): the client boots with the three
 * Simple Opt sub-mods next to Cull Leaves, More Culling, BadOptimizations and ServerCore, loads a world and ticks it.
 * The mixin plugins print their decision tables at startup; the runner reads them from the log. This test only proves the
 * game reaches a running world without a crash and prints the marker line.
 */
public final class CompatClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(100);
            System.out.println("[compat-opt] world ticked without crash");
        }
    }
}
