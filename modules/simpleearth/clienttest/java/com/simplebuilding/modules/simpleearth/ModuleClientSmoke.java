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
                if (!EarthClient.parked()) throw new AssertionError("earth not shown above the render switch");
                int mods = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
                boolean sb = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebuilding");
                System.out.println("[earth-measure] client runs with " + mods + " mods, simplebuilding " + sb + ", simpleinterfaces "
                        + net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpleinterfaces") + ", mousetweaks "
                        + net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("mousetweaks"));
                mc.player.setXRot(55);
            });
            // WD25-22: time until the V10 image is complete from cold caches, exact vs fast (Chunkbase-like) heights
            imageTime(context, world, false);
            context.takeScreenshot("simpleearth-v10-exact");
            imageTime(context, world, true);
            context.takeScreenshot("simpleearth-v10-fast");
            glide(context);
            elytraGlide(context, world);
            toTheEdge(context, world);
            topViewF5(context, world);
            // WE25-1: rain on the server, none on the client while the earth is shown
            world.getServer().runCommand("weather rain");
            context.waitTicks(60);
            check(context.computeOnClient(mc -> com.simplebuilding.modules.simpleearth.orbit.OrbitView.active && mc.level.getRainLevel(1) == 0),
                    "no weather on the client in orbit while it rains");
            world.getServer().runCommand("weather clear");
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
            // WF25-2: the owner's window (1920 x 1177, GUI scale auto = 4) and GUI scale 2, clicking while walking forward
            context.getInput().resizeWindow(1920, 1177);
            for (int gui : new int[]{0, 2}) {
                context.runOnClient(mc -> {
                    mc.options.guiScale().set(gui);
                    mc.resizeGui();
                });
                context.waitTicks(5);
                context.setScreen(EarthScreen::new);
                context.waitTicks(10);
                context.getInput().holdKey(o -> o.keyUp);
                scope = "1920x1177 GUI " + context.computeOnClient(mc -> mc.getWindow().getGuiScale()) + ": ";
                menu(context);
                scope = "";
                context.getInput().releaseKey(o -> o.keyUp);
            }
            context.runOnClient(mc -> {
                mc.options.guiScale().set(0);
                mc.resizeGui();
            });
            context.getInput().resizeWindow(854, 480);
            context.waitTicks(5);
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

    private static String scope = "";

    private static void check(boolean ok, String raw) {
        String what = scope + raw;
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

    /** WG26-1: forward flight above the switch moves the player himself (creative flight); A stays, nothing pulls back. */
    private static void glide(ClientGameTestContext context) {
        context.runOnClient(mc -> mc.player.setXRot(10));
        double[] a = context.computeOnClient(mc -> new double[]{mc.player.getX(), mc.player.getZ(), EarthClient.relX(1), EarthClient.relZ(1)});
        context.getInput().holdKeyFor(o -> o.keyUp, 40);
        context.waitTicks(5);
        double[] b = context.computeOnClient(mc -> new double[]{mc.player.getX(), mc.player.getZ(), EarthClient.relX(1), EarthClient.relZ(1)});
        double moved = Math.hypot(b[0] - a[0], b[1] - a[1]), viewMoved = Math.hypot(b[2] - a[2], b[3] - a[3]);
        check(moved > 10 && Math.abs(moved - viewMoved) < 1, String.format(java.util.Locale.ROOT,
                "creative flight above the switch: player moved %.1f blocks, earth position %.1f", moved, viewMoved));
    }

    /** WG26-1: elytra flight above the switch: the player glides on (no hover, no teleport) and the earth follows. */
    private static void elytraGlide(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA));
            p.getAbilities().flying = false;
            p.getAbilities().mayfly = false;
            p.onUpdateAbilities();
            p.startFallFlying();
        });
        context.waitTicks(3);
        context.runOnClient(mc -> {
            mc.player.setXRot(0);
            mc.player.setDeltaMovement(1.5, 0, 0);
        });
        double[] a = context.computeOnClient(mc -> new double[]{mc.player.getX(), mc.player.getZ(), EarthClient.relX(1), EarthClient.relZ(1)});
        context.waitTicks(30);
        double[] b = context.computeOnClient(mc -> new double[]{mc.player.getX(), mc.player.getZ(), EarthClient.relX(1), EarthClient.relZ(1)});
        boolean flying = context.computeOnClient(mc -> mc.player.isFallFlying());
        double moved = Math.hypot(b[0] - a[0], b[1] - a[1]), viewMoved = Math.hypot(b[2] - a[2], b[3] - a[3]);
        check(flying && moved > 10 && Math.abs(moved - viewMoved) < 1, String.format(java.util.Locale.ROOT,
                "elytra above the switch: still gliding %s, moved %.1f blocks, earth position %.1f", flying, moved, viewMoved));
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.stopFallFlying();
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        });
        context.waitTicks(5);
    }

    /** WG26-1: like a map - half an edge (1024 with the standard 2048) from the centre the next face lies under the player. */
    private static void toTheEdge(ClientGameTestContext context, TestSingleplayerContext world) {
        double ax = context.computeOnClient(mc -> com.simplebuilding.modules.simpleearth.client.EarthClient.ax());
        int edge = context.computeOnClient(mc -> com.simplebuilding.modules.simpleearth.client.EarthClient.edge());
        version(context, 6);
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.teleportTo(p.level(), ax + edge / 2.0 - 40, 1800, p.getZ(), java.util.Set.of(), p.getYRot(), 60, false);
        });
        context.waitTicks(40);
        int before = context.computeOnClient(mc -> EarthClient.view().currentTileX());
        context.takeScreenshot("simpleearth-edge-before");
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.teleportTo(p.level(), ax + edge / 2.0 + 40, 1800, p.getZ(), java.util.Set.of(), p.getYRot(), 60, false);
        });
        context.waitTicks(40);
        int after = context.computeOnClient(mc -> EarthClient.view().currentTileX());
        context.takeScreenshot("simpleearth-edge-after");
        check(edge == 2048 && before == 0 && after == 1, "edge " + edge + ": tile under the player " + before + " -> " + after + " across x = A + edge/2");
        world.getServer().runOnServer(server -> {
            var p = server.getPlayerList().getPlayers().getFirst();
            p.teleportTo(p.level(), ax, 1700, p.getZ(), java.util.Set.of(), p.getYRot(), 55, false);
        });
        context.waitTicks(40);
    }

    /** WF25-3: third person looking straight down at oneself below the old threshold (y 2778): the earth stays behind the player. */
    private static void topViewF5(ClientGameTestContext context, TestSingleplayerContext world) {
        teleport(context, world, 2700, 60);
        context.runOnClient(mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            mc.player.setXRot(90);
        });
        context.waitTicks(20);
        context.takeScreenshot("simpleearth-f5-top-2700");
        double nearest = context.computeOnClient(mc -> com.simplebuilding.modules.simpleearth.client.EarthRenderer.lastNearest);
        check(nearest >= 8, String.format(java.util.Locale.ROOT, "F5 from above at y 2700: nearest earth %.1f blocks (camera 4 blocks behind)", nearest));
        context.runOnClient(mc -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.player.setXRot(55);
        });
        teleport(context, world, 1700, 40);
    }

    /** WD25-23: the menu with real mouse events - version button, slider click and drag, list scroll, toggle. */
    private static void menu(ClientGameTestContext context) {
        double k = context.computeOnClient(mc -> mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth());
        int[] v10 = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testVersionButton(10));
        System.out.println("[earth-measure] V10 button at " + v10[0] + "/" + v10[1] + " (scale " + k + "), Vanilla dispatch finds: "
                + context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testChildAt(v10[0], v10[1])));
        context.getInput().setCursorPos(v10[0] * k, v10[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> EarthClientConfig.version) == 10, "click V10 selects version 10");
        context.runOnClient(mc -> EarthClientConfig.version = 10); // keep the rest of the checks meaningful
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
        // WG26-3: the poll path (real mouse button read by the menu itself) presses the same controls
        int[] v11 = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testVersionButton(11));
        context.runOnClient(mc -> ((EarthScreen) mc.gui.screen()).testPressByPoll(v11[0], v11[1]));
        check(context.computeOnClient(mc -> EarthClientConfig.version) == 11, "poll path: V11 selected");
        int[] tog = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testRowPoint("moonPhases"));
        boolean mp = context.computeOnClient(mc -> EarthClientConfig.renderOn("moonPhases"));
        context.runOnClient(mc -> ((EarthScreen) mc.gui.screen()).testPressByPoll(tog[0], tog[1]));
        check(context.computeOnClient(mc -> EarthClientConfig.renderOn("moonPhases")) != mp, "poll path: toggle");
        context.runOnClient(mc -> EarthClientConfig.version = 10);
        context.runOnClient(mc -> EarthClientConfig.setRender("detail", 5));
        int[] reset = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testButton(true));
        context.getInput().setCursorPos(reset[0] * k, reset[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> EarthClientConfig.render("detail")) == 3, "click Defaults resets the display values");
        context.takeScreenshot("simpleearth-menu-used");
        int[] done = context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).testButton(false));
        context.getInput().setCursorPos(done[0] * k, done[1] * k);
        context.getInput().pressMouse(0);
        context.waitTicks(3);
        check(context.computeOnClient(mc -> mc.gui.screen() == null), "click Done closes the menu");
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
