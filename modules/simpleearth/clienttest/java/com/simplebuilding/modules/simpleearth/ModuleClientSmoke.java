package com.simplebuilding.modules.simpleearth;

import com.simplebuilding.modules.simpleearth.client.EarthClient;
import com.simplebuilding.modules.simpleearth.client.EarthScreen;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client smoke with screenshots (WC24-4 stage 6): fly up through the activation height and the render switch, let the
 * terrain tiles arrive, then the earth in all twelve versions, three detail levels (WD25-16), gliding with the forward key,
 * far views (V6, V10-V12) and the menu.
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
            // WD25-22: time until the V10 image is complete from cold caches, exact vs fast (Chunkbase-like) heights
            imageTime(context, world, false);
            context.takeScreenshot("simpleearth-v10-exact");
            imageTime(context, world, true);
            context.takeScreenshot("simpleearth-v10-fast");
            glide(context);
            version(context, 6);
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
            shot(context, 10, "simpleearth-orbit-v10");
            shot(context, 11, "simpleearth-orbit-v11");
            shot(context, 12, "simpleearth-orbit-v12");
            // WD25-16 detail levels on the same spot (V6 looking down): 1 coarse, 3 standard, 5 finest
            context.runOnClient(mc -> mc.player.setXRot(55));
            version(context, 6);
            detail(context, 1, 120, "simpleearth-detail-1");
            detail(context, 3, 300, "simpleearth-detail-3");
            detail(context, 5, 500, "simpleearth-detail-5");
            context.runOnClient(mc -> EarthClientConfig.setRender("detail", 3));
            context.getInput().holdKeyFor(o -> o.keyUp, 100);
            context.waitTicks(60);
            context.takeScreenshot("simpleearth-glide-v6");
            teleport(context, world, 5200, 120);
            context.runOnClient(mc -> mc.player.setXRot(70));
            context.waitTicks(30);
            context.takeScreenshot("simpleearth-far-v6");
            version(context, 10);
            context.waitTicks(40);
            context.takeScreenshot("simpleearth-far-v10");
            version(context, 11);
            context.waitTicks(40);
            context.takeScreenshot("simpleearth-far-v11");
            version(context, 12);
            context.waitTicks(40);
            context.takeScreenshot("simpleearth-far-v12");
            version(context, 6);
            teleport(context, world, 2600, 60);
            context.runOnClient(mc -> mc.player.setXRot(35));
            context.waitTicks(40);
            context.takeScreenshot("simpleearth-mid-v6");
            context.setScreen(EarthScreen::new);
            context.waitTicks(10);
            context.takeScreenshot("simpleearth-menu");
            menu(context);
            context.takeScreenshot("simpleearth-menu-used");
            context.setScreen(() -> null);
            if (!FAILURES.isEmpty()) throw new AssertionError(String.join("; ", FAILURES));
        }
    }

    /** Switches the version (horizon view, pitch 12) and takes a screenshot (literal names: the runner reads them from the source). */
    private static void shot(ClientGameTestContext context, int v, String name) {
        version(context, v);
        context.waitTicks(30);
        context.takeScreenshot(name);
    }

    private static final java.util.List<String> FAILURES = new java.util.ArrayList<>();

    private static void check(boolean ok, String what) {
        System.out.println("[earth-measure] " + (ok ? "OK " : "FAIL ") + what);
        if (!ok) FAILURES.add(what);
    }

    /** Cold caches (server store, client tiles and images), then ticks until every face image of V10 is complete. */
    private static void imageTime(ClientGameTestContext context, TestSingleplayerContext world, boolean fast) {
        world.getServer().runOnServer(server -> {
            com.simplebuilding.modules.simpleearth.config.EarthServerConfig.fastHeights = fast;
            com.simplebuilding.modules.simpleearth.terrain.TileService.invalidate();
        });
        long nanos0 = com.simplebuilding.modules.simpleearth.terrain.TerrainSampler.NANOS.get();
        long tiles0 = com.simplebuilding.modules.simpleearth.terrain.TerrainSampler.TILES.get();
        context.runOnClient(mc -> {
            EarthClientConfig.version = 10;
            EarthClient.resetView();
            com.simplebuilding.modules.simpleearth.client.ClientTiles.clear();
            com.simplebuilding.modules.simpleearth.client.FaceTextures.clear();
        });
        int ticks = 0;
        boolean done = false;
        while (ticks < 1800 && !done) {
            context.waitTicks(10);
            ticks += 10;
            int[] f = context.computeOnClient(mc -> com.simplebuilding.modules.simpleearth.client.FaceTextures.lastFrame());
            done = f[0] > 0 && f[1] == 0;
        }
        long tiles = com.simplebuilding.modules.simpleearth.terrain.TerrainSampler.TILES.get() - tiles0;
        double ms = tiles == 0 ? 0 : (com.simplebuilding.modules.simpleearth.terrain.TerrainSampler.NANOS.get() - nanos0) / 1e6 / tiles;
        System.out.println(String.format(java.util.Locale.ROOT, "[earth-measure] V10 %s heights: image %s after %d ticks (%.1f s), %d tiles sampled, %.1f ms per tile",
                fast ? "fast" : "exact", done ? "complete" : "NOT complete", ticks, ticks / 20.0, tiles, ms));
        if (fast) check(done, "V10 image complete with fast heights within 90 s");
    }

    /** WD25-22: forward glide while parked moves the orbit position (client prediction and server). */
    private static void glide(ClientGameTestContext context) {
        context.runOnClient(mc -> mc.player.setXRot(55));
        double[] a = context.computeOnClient(mc -> new double[]{EarthClient.relX(1), EarthClient.relZ(1)});
        context.getInput().holdKeyFor(o -> o.keyUp, 40);
        context.waitTicks(2);
        double[] b = context.computeOnClient(mc -> new double[]{EarthClient.relX(1), EarthClient.relZ(1)});
        double moved = Math.hypot(b[0] - a[0], b[1] - a[1]);
        check(moved > 256, String.format(java.util.Locale.ROOT, "forward glide 40 ticks moved %.0f blocks", moved));
    }

    /** WD25-23: the menu with real mouse events - version button, slider click and drag, list scroll, toggle. */
    private static void menu(ClientGameTestContext context) {
        double k = context.computeOnClient(mc -> mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth());
        int[] v10 = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testVersionButton(10));
        context.getInput().setCursorPos(v10[0] * k, v10[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> EarthClientConfig.version) == 10, "click V10 selects version 10");
        context.runOnClient(mc -> EarthClientConfig.setRender("detail", 3));
        int[] end = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testSlider("detail", 1.0));
        context.getInput().setCursorPos(end[0] * k, end[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> EarthClientConfig.render("detail")) == 5, "click at the slider end sets detail 5");
        int[] from = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testSlider("tipTime", 0.0));
        int[] to = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testSlider("tipTime", 0.5));
        context.getInput().setCursorPos(from[0] * k, from[1] * k);
        context.getInput().holdMouse(0);
        context.waitTicks(2);
        context.getInput().setCursorPos(to[0] * k, to[1] * k);
        context.waitTicks(2);
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        double tip = context.computeOnClient(mc -> EarthClientConfig.get(10, "tipTime"));
        check(Math.abs(tip - 1.05) < 0.11, "drag the tip time slider to the middle: " + tip);
        int[] toggle = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testRowPoint("moonPhases"));
        boolean before = context.computeOnClient(mc -> EarthClientConfig.renderOn("moonPhases"));
        context.getInput().setCursorPos(toggle[0] * k, toggle[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> EarthClientConfig.renderOn("moonPhases")) != before, "click toggles moon phases");
        int[] list = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testListCentre());
        context.runOnClient(mc -> ((EarthScreen) mc.gui.screen()).testRowPoint("detail"));
        double s0 = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testScroll());
        context.getInput().setCursorPos(list[0] * k, list[1] * k);
        context.getInput().scroll(-3);
        context.waitTicks(3);
        double s1 = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testScroll());
        check(s1 > s0, "mouse wheel scrolls the list: " + s0 + " -> " + s1);
        check(context.computeOnClient(mc -> mc.gui.screen() instanceof EarthScreen), "menu still open");
        context.runOnClient(mc -> {
            EarthClientConfig.setRender("detail", 3);
            EarthClientConfig.setRender("moonPhases", 1);
            EarthClientConfig.version = 6;
        });
    }

    /** Sets the detail level, waits for the tiles and takes a screenshot (literal names for the runner). */
    private static void detail(ClientGameTestContext context, int level, int ticks, String name) {
        context.runOnClient(mc -> EarthClientConfig.setRender("detail", level));
        context.waitTicks(ticks);
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
