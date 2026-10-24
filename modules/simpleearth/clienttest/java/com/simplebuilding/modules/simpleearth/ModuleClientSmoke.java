package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.client.EarthClient;
import com.simplebuilding.modules.simpleearth.client.EarthScreen;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client smoke with screenshots (WC24-4 stage 6): fly up through the activation height and the render switch, let the
 * terrain tiles arrive, then the earth in several versions, gliding with the forward key, a far view and the menu.
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleearth")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simpleearth-title");
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            });
            version(context, 6);
            teleport(context, world, 1100, 30);
            teleport(context, world, 1700, 40);
            context.runOnClient(mc -> {
                if (!EarthClient.parked()) throw new AssertionError("not parked in orbit above the render switch");
                mc.player.setXRot(55);
            });
            context.waitTicks(300);
            context.takeScreenshot("simpleearth-orbit-v6");
            for (int v : new int[]{1, 3, 4, 5, 7, 8, 9}) {
                version(context, v);
                context.waitTicks(30);
                context.takeScreenshot("simpleearth-orbit-v" + v);
            }
            version(context, 6);
            context.getInput().holdKeyFor(o -> o.keyUp, 100);
            context.waitTicks(60);
            context.takeScreenshot("simpleearth-glide-v6");
            teleport(context, world, 5200, 120);
            context.runOnClient(mc -> mc.player.setXRot(70));
            context.waitTicks(30);
            context.takeScreenshot("simpleearth-far-v6");
            context.setScreen(EarthScreen::new);
            context.waitTicks(10);
            context.takeScreenshot("simpleearth-menu");
            context.setScreen(() -> null);
        }
    }

    private static void version(ClientGameTestContext context, int v) {
        context.runOnClient(mc -> {
            EarthClientConfig.version = v;
            EarthClient.resetView();
        });
    }

    private static void teleport(ClientGameTestContext context, TestSingleplayerContext world, int y, int ticks) {
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.teleportTo(p.level(), p.getX(), y, p.getZ(), java.util.Set.of(), p.getYRot(), p.getXRot(), false);
        });
        context.waitTicks(ticks);
    }
}
