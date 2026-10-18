package com.simplebuilding.integration.clienttest;

import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Benchmark measuring client (tools/bench/run_bench.py, docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md §6). Not a
 * test: only runs with {@code -PmoduleClientTest=bench}, one scene per JVM, writes one JSON object to
 * {@code sb.bench.out}. The harness chooses the mods folder (a vanilla, b other mod, c ours, d both).
 *
 * <ul>
 * <li>{@code o1} (K1 forest): fixed seed, nearest dark forest, noon, no weather/mobs; circle flight around it looking
 * down. Frame time per tick (client gametests render one frame per tick), mesh rebuild time of all sections, MSPT.</li>
 * <li>{@code s5a} (S-fly): in-process dedicated server (view 12, simulation 8), the player flies straight through new
 * terrain at a fixed speed per server tick; MSPT and the view distance over time.</li>
 * </ul>
 */
public final class BenchClientTest implements FabricClientGameTest {
    private static final AtomicBoolean FLY = new AtomicBoolean();
    private static volatile double flySpeed = 1.5;
    private static volatile boolean flyHooked;

    @Override
    public void runTest(ClientGameTestContext ctx) {
        String scene = System.getProperty("sb.bench.scene", "o1");
        Path out = Path.of(System.getProperty("sb.bench.out", "bench-result.json"));
        long seed = Long.getLong("sb.bench.seed", 20261018L);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("scene", scene);
        r.put("seed", seed);
        r.put("startup_s", round((System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000.0));
        List<String> mods = new ArrayList<>();
        for (String id : List.of("simpleoptrender", "simpleoptserver", "simpleoptmemory", "cullleaves", "moreculling", "servercore",
                "badoptimizations", "sodium", "lithium", "ferritecore", "modernfix")) if (FabricLoader.getInstance().isModLoaded(id)) mods.add(id);
        r.put("loaded", mods);
        ctx.runOnClient(c -> {
            c.options.renderDistance().set(Integer.getInteger("sb.bench.view", 12));
            c.options.simulationDistance().set(8);
            c.options.framerateLimit().set(260);
            c.options.enableVsync().set(false);
            c.options.cutoutLeaves().set(true);
            r.put("window", c.getWindow().getWidth() + "x" + c.getWindow().getHeight());
            r.put("gl_renderer", com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().name());
        });
        try {
            if (scene.equals("s5a")) s5a(ctx, seed, r);
            else o1(ctx, seed, r);
        } finally {
            FLY.set(false);
            try {
                Files.createDirectories(out.toAbsolutePath().getParent());
                Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(r));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void quietWorld(TestServerContext server) {
        for (String cmd : List.of("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false",
                "time set noon", "weather clear", "gamemode creative @a", "kill @e[type=!player]")) server.runCommand(cmd);
    }

    private static void o1(ClientGameTestContext ctx, long seed, Map<String, Object> r) {
        int warm = Integer.getInteger("sb.bench.warmup", 100), frames = Integer.getInteger("sb.bench.frames", 400);
        long t = System.nanoTime();
        try (var world = ctx.worldBuilder().adjustSettings(s -> s.setSeed(Long.toString(seed))).create()) {
            r.put("worldload_s", secs(t));
            var server = world.getServer();
            quietWorld(server);
            BlockPos forest = server.computeOnServer(s -> {
                var level = s.overworld();
                var found = level.findClosestBiome3d(h -> h.is(Biomes.DARK_FOREST), BlockPos.ZERO, 6400, 32, 64);
                return found == null ? BlockPos.ZERO : found.getFirst();
            });
            int cx = forest.getX(), cz = forest.getZ();
            server.runCommand(String.format(Locale.ROOT, "tp @a %d 200 %d", cx, cz));
            world.getConnection().waitForChunksDownload();
            int ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz));
            r.put("forest", List.of(cx, ground, cz));
            double radius = 28, y = ground + 22;
            teleport(server, cx, cz, radius, y, 0);
            t = System.nanoTime();
            int ticks = world.getConnection().waitForChunksRender();
            r.put("chunkload_s", secs(t));
            r.put("chunkload_ticks", ticks);
            ctx.runOnClient(c -> c.levelRenderer.allChanged());
            t = System.nanoTime();
            world.getConnection().waitForChunksRender();
            r.put("meshbuild_s", secs(t));
            r.put("heap_load_mb", heapAfterGc());
            double[] ms = new double[frames];
            List<Double> mspt = new ArrayList<>();
            for (int i = -warm; i < frames; i++) {
                double a = 2 * Math.PI * (i + warm) / (frames + warm);
                teleport(server, cx, cz, radius, y, a);
                long f = System.nanoTime();
                ctx.waitTick();
                if (i >= 0) {
                    ms[i] = (System.nanoTime() - f) / 1e6;
                    if (i % 20 == 0) mspt.add(server.computeOnServer(s -> s.getAverageTickTimeNanos() / 1e6));
                }
            }
            frameStats(ms, r);
            r.put("mspt_avg", round(mspt.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
            r.put("mspt_p95", round(percentile(mspt.stream().mapToDouble(Double::doubleValue).toArray(), 95)));
            r.put("heap_mb", heapAfterGc());
        }
    }

    /** Circle point at angle {@code a}, looking at the centre and down. */
    private static void teleport(TestServerContext server, int cx, int cz, double radius, double y, double a) {
        double x = cx + radius * Math.cos(a), z = cz + radius * Math.sin(a);
        double yaw = Math.toDegrees(Math.atan2(-(cx - x), cz - z));
        server.runCommand(String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f 35", x, y, z, yaw));
    }

    private static void s5a(ClientGameTestContext ctx, long seed, Map<String, Object> r) {
        int warmS = Integer.getInteger("sb.bench.warmupSeconds", 20), measureS = Integer.getInteger("sb.bench.seconds", 90);
        flySpeed = Double.parseDouble(System.getProperty("sb.bench.flySpeed", "1.5"));
        hookFlight();
        Properties props = new Properties();
        props.setProperty("view-distance", "12");
        props.setProperty("simulation-distance", "8");
        props.setProperty("gamemode", "creative");
        props.setProperty("spawn-protection", "0");
        long t = System.nanoTime();
        try (var server = ctx.worldBuilder().adjustSettings(s -> s.setSeed(Long.toString(seed))).createServer(props);
             var conn = server.connect()) {
            r.put("worldload_s", secs(t));
            quietWorld(server);
            server.runCommand("tp @a 0 180 0 -90 10");
            t = System.nanoTime();
            conn.waitForChunksRender();
            r.put("chunkload_s", secs(t));
            server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(p -> {
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }));
            FLY.set(true);
            List<Double> mspt = new ArrayList<>();
            List<Integer> views = new ArrayList<>();
            long start = System.nanoTime(), lastSample = start;
            int frames = 0;
            while ((System.nanoTime() - start) / 1e9 < warmS + measureS) {
                ctx.waitTick();
                frames++;
                long now = System.nanoTime();
                if (now - lastSample >= 1_000_000_000L) {
                    lastSample = now;
                    double[] sample = server.computeOnServer(s -> new double[] {s.getAverageTickTimeNanos() / 1e6, s.getPlayerList().getViewDistance()});
                    if ((now - start) / 1e9 >= warmS) {
                        mspt.add(sample[0]);
                        views.add((int) sample[1]);
                    }
                }
            }
            FLY.set(false);
            double[] m = mspt.stream().mapToDouble(Double::doubleValue).toArray();
            r.put("mspt_avg", round(Arrays.stream(m).average().orElse(0)));
            r.put("mspt_p95", round(percentile(m, 95)));
            r.put("view_min", views.stream().mapToInt(Integer::intValue).min().orElse(0));
            r.put("view_final", views.isEmpty() ? 0 : views.get(views.size() - 1));
            r.put("view_trace", views);
            r.put("client_frames", frames);
            r.put("flown_blocks", round(server.computeOnServer(s -> s.getPlayerList().getPlayers().isEmpty() ? 0.0
                    : s.getPlayerList().getPlayers().get(0).getX())));
            r.put("heap_mb", heapAfterGc());
        }
    }

    /** Server-side fixed-speed flight along +X (independent of the client frame rate). */
    private static synchronized void hookFlight() {
        if (flyHooked) return;
        flyHooked = true;
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer s) -> {
            if (!FLY.get() || !s.isDedicatedServer()) return;
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
                    String.format(Locale.ROOT, "execute as @a at @s run tp @s ~%.2f 180 ~ -90 10", flySpeed));
        });
    }

    private static void frameStats(double[] ms, Map<String, Object> r) {
        double avg = Arrays.stream(ms).average().orElse(0);
        double[] sorted = ms.clone();
        Arrays.sort(sorted);
        int n = Math.max(1, sorted.length / 100);
        double slowest = 0;
        for (int i = sorted.length - n; i < sorted.length; i++) slowest += sorted[i];
        slowest /= n;
        r.put("frames", ms.length);
        r.put("frame_avg_ms", round(avg));
        r.put("fps_avg", round(avg > 0 ? 1000 / avg : 0));
        r.put("fps_1low", round(slowest > 0 ? 1000 / slowest : 0));
        r.put("frame_p99_ms", round(percentile(ms, 99)));
    }

    private static double percentile(double[] v, double p) {
        if (v.length == 0) return 0;
        double[] s = v.clone();
        Arrays.sort(s);
        return s[Math.min(s.length - 1, (int) Math.ceil(p / 100.0 * s.length) - 1)];
    }

    private static double heapAfterGc() {
        System.gc();
        System.gc();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Runtime rt = Runtime.getRuntime();
        return round((rt.totalMemory() - rt.freeMemory()) / 1048576.0);
    }

    private static double secs(long startNanos) {
        return round((System.nanoTime() - startNanos) / 1e9);
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
