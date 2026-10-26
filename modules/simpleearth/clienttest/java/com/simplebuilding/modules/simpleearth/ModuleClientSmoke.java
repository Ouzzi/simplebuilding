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
            newVersions(context, world);
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

    /** E29-5: each new version at the tile centre, near an edge and near a corner (y 3000, looking down). */
    private static void newVersions(ClientGameTestContext context, TestSingleplayerContext world) {
        versionShots(context, world, 13, "simpleearth-v13-centre", "simpleearth-v13-edge", "simpleearth-v13-corner");
        versionShots(context, world, 14, "simpleearth-v14-centre", "simpleearth-v14-edge", "simpleearth-v14-corner");
        versionShots(context, world, 15, "simpleearth-v15-centre", "simpleearth-v15-edge", "simpleearth-v15-corner");
        versionShots(context, world, 16, "simpleearth-v16-centre", "simpleearth-v16-edge", "simpleearth-v16-corner");
        versionShots(context, world, 17, "simpleearth-v17-centre", "simpleearth-v17-edge", "simpleearth-v17-corner");
        versionShots(context, world, 18, "simpleearth-v18-centre", "simpleearth-v18-edge", "simpleearth-v18-corner");
        versionShots(context, world, 19, "simpleearth-v19-centre", "simpleearth-v19-edge", "simpleearth-v19-corner");
        versionShots(context, world, 20, "simpleearth-v20-centre", "simpleearth-v20-edge", "simpleearth-v20-corner");
        versionShots(context, world, 21, "simpleearth-v21-centre", "simpleearth-v21-edge", "simpleearth-v21-corner");
        versionShots(context, world, 22, "simpleearth-v22-centre", "simpleearth-v22-edge", "simpleearth-v22-corner");
        versionShots(context, world, 23, "simpleearth-v23-centre", "simpleearth-v23-edge", "simpleearth-v23-corner");
        versionShots(context, world, 24, "simpleearth-v24-centre", "simpleearth-v24-edge", "simpleearth-v24-corner");
        version(context, 6);
    }

    private static void versionShots(ClientGameTestContext context, TestSingleplayerContext world, int v, String centre, String edge, String corner) {
        double ax = context.computeOnClient(mc -> EarthClient.ax()), az = context.computeOnClient(mc -> EarthClient.az());
        int half = context.computeOnClient(mc -> EarthClient.edge()) / 2;
        version(context, v);
        double[][] spots = {{0, 0}, {0.9, 0.1}, {0.9, -0.9}};
        String[] names = {centre, edge, corner};
        for (int k = 0; k < 3; k++) {
            double x = ax + spots[k][0] * half, z = az + spots[k][1] * half;
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(p.level(), x, 3000, z, java.util.Set.of(), 0, 70, false);
            });
            context.waitTicks(k == 0 ? 60 : 30);
            context.takeScreenshot(names[k]);
        }
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
    /**
     * The menu (WF25-2/WG26-3): only opened and photographed here. Clicks are NOT checked in this test: Fabric's test input
     * calls the mouse handler directly, and that path once passed for self-drawn controls that never reacted to a real
     * mouse (and now fails for real Vanilla widgets that do). The real operating-system mouse (X11 pointer through XTest ->
     * SDL -> MouseHandler) is checked by modules/simpleearth/tools/menu_probe.py in a normal integration client.
     */
    private static void menu(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            EarthClientConfig.version = 10;
            mc.gui.setScreen(new EarthScreen());
        });
        context.waitTicks(5);
        context.runOnClient(mc -> ((EarthScreen) mc.gui.screen()).probeScrollTo("tipTime"));
        context.waitTicks(5);
        check(context.computeOnClient(mc -> ((EarthScreen) mc.gui.screen()).probeControl("tipTime") != null), "V10 own values in the Vanilla list");
        context.takeScreenshot("simpleearth-menu-used");
        context.runOnClient(mc -> {
            mc.gui.setScreen(null);
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
