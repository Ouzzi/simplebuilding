package com.simplebuilding.integration.clienttest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * A-MIXCHECK31 (owner's client crashed at start on a client mixin the server gametests never apply): a real client with
 * every module enters a world, ticks it, then force-loads every class any mixin targets ({@link MixinEnvironment#audit()}),
 * so a client mixin whose 26.3 target is gone fails here and not only when a player opens that screen.
 * tools/testrunner target {@code mixinaudit-client-263}; log lines {@code [mixinaudit] ...}.
 */
public final class MixinAuditClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            context.waitTicks(100);
            System.out.println("[mixinaudit] world entered and ticked");
            context.runOnClient(client -> MixinEnvironment.getCurrentEnvironment().audit());
            System.out.println("[mixinaudit] audit done without error");
        }
    }
}
