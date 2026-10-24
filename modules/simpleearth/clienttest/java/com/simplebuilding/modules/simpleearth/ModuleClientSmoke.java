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
        // a normal noise world (the default test world is flat): the faces must show real biomes from the generator
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(ui -> {
            ui.setSeed("simpleearth");
            ui.getNormalPresetList().stream().filter(t -> t.preset().unwrapKey().map(k -> k.identifier().getPath().equals("normal")).orElse(false))
                    .findFirst().ifPresent(ui::setWorldType);
        }).create()) {
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
            context.runOnClient(mc -> mc.player.setXRot(12));
            context.waitTicks(20);
            context.takeScreenshot("simpleearth-horizon-v6");
            shot(context, 1, "simpleearth-orbit-v1");
            shot(context, 2, "simpleearth-orbit-v2");
            shot(context, 3, "simpleearth-orbit-v3");
            shot(context, 4, "simpleearth-orbit-v4");
            shot(context, 5, "simpleearth-orbit-v5");
            shot(context, 7, "simpleearth-orbit-v7");
            shot(context, 8, "simpleearth-orbit-v8");
            shot(context, 9, "simpleearth-orbit-v9");
            context.runOnClient(mc -> mc.player.setXRot(55));
            version(context, 6);
            context.getInput().holdKeyFor(o -> o.keyUp, 100);
            context.waitTicks(60);
            context.takeScreenshot("simpleearth-glide-v6");
            teleport(context, world, 5200, 120);
            context.runOnClient(mc -> mc.player.setXRot(70));
            context.waitTicks(30);
            context.takeScreenshot("simpleearth-far-v6");
            teleport(context, world, 2600, 60);
            context.runOnClient(mc -> mc.player.setXRot(35));
            context.waitTicks(40);
            context.takeScreenshot("simpleearth-mid-v6");
            context.setScreen(EarthScreen::new);
            context.waitTicks(10);
            context.takeScreenshot("simpleearth-menu");
            context.setScreen(() -> null);
        }
    }

    /** Switches the version (horizon view, pitch 12) and takes a screenshot (literal names: the runner reads them from the source). */
    private static void shot(ClientGameTestContext context, int v, String name) {
        version(context, v);
        context.waitTicks(30);
        context.takeScreenshot(name);
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
