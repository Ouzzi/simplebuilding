package com.simplebuilding.integration;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * L9 / b-benchsb31 (docs/ai/PLAN-B-BENCHSB31-2026-11-11.md): the same fixed scene as Simple Optimization's headless bench
 * (120 cows in a pen, 64 hoppers under 32 furnaces, 6x6x6 persistent leaves), measured with plain vanilla API only, so it
 * runs in a Fabric-API-only server and in SimpleBuilding plus its mandatory mods alike. Active only with -Dsb.bench=true
 * (tools/bench/headless_bench.py --profile sb); otherwise it succeeds at once so the normal integration run is unaffected.
 * Prints one line {@code SB-BENCH sb {json}}: MSPT avg/p95 over 400 server ticks, heap after GC, loaded mod count and,
 * with -Dsb.bench.profile=true, a stack sampler of the server thread (top frames inside com.simplebuilding, by mod package).
 */
public final class SbBenchGameTest {
    private static final int WARMUP = 100, WINDOWS = 4;

    @GameTest(maxTicks = WARMUP + 100 * WINDOWS + 100)
    public void measure(GameTestHelper h) {
        if (!Boolean.getBoolean("sb.bench")) { h.succeed(); return; }
        var stone = Blocks.STONE.defaultBlockState();
        var barrier = Blocks.BARRIER.defaultBlockState();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            h.setBlock(x, 0, z, stone);
            if (x == 0 || z == 0 || x == 15 || z == 15) { h.setBlock(x, 1, z, barrier); h.setBlock(x, 2, z, barrier); }
        }
        List<Entity> cows = new ArrayList<>();
        for (int x = 1; x <= 14 && cows.size() < 120; x++) for (int z = 1; z <= 14 && cows.size() < 120; z++)
            cows.add(h.spawn(EntityTypes.COW, x, 1, z));
        int hoppers = 0;
        for (int x = 0; x < 16; x++) for (int z = 17; z < 21; z++, hoppers++) {
            h.setBlock(x, 1, z, Blocks.HOPPER.defaultBlockState());
            if (((x + z) & 1) == 0) h.setBlock(x, 2, z, Blocks.FURNACE.defaultBlockState());
        }
        var leaf = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 20; x < 26; x++) for (int y = 1; y < 7; y++) for (int z = 0; z < 6; z++) h.setBlock(x, y, z, leaf);
        final int hopperCount = hoppers;
        var server = h.getLevel().getServer();
        var ticks = new ArrayList<Long>();
        long[] heap = new long[1];
        var sampler = Boolean.getBoolean("sb.bench.profile") ? new Sampler() : null;
        h.runAfterDelay(WARMUP, () -> {
            System.gc();
            System.gc();
            heap[0] = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            if (sampler != null) sampler.start(Thread.currentThread());
        });
        for (int w = 1; w <= WINDOWS; w++) {
            boolean last = w == WINDOWS;
            h.runAfterDelay(WARMUP + 100L * w, () -> {
                for (long t : server.getTickTimesNanos()) ticks.add(t);
                if (!last) return;
                if (sampler != null) sampler.stop();
                long[] sorted = ticks.stream().mapToLong(Long::longValue).filter(t -> t > 0).sorted().toArray();
                double avg = Arrays.stream(sorted).average().orElse(0) / 1e6;
                double p95 = sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * 0.95) - 1)] / 1e6;
                var json = new JsonObject();
                json.addProperty("mspt_avg", Math.round(avg * 1000) / 1000.0);
                json.addProperty("mspt_p95", Math.round(p95 * 1000) / 1000.0);
                json.addProperty("ticks", sorted.length);
                json.addProperty("heap_mb", Math.round(heap[0] / 1048576.0 * 10) / 10.0);
                json.addProperty("mods", FabricLoader.getInstance().getAllMods().size());
                json.addProperty("sb_mods", FabricLoader.getInstance().getAllMods().stream()
                        .filter(m -> m.getMetadata().getId().startsWith("simple")).count());
                json.addProperty("cows", cows.size());
                json.addProperty("hoppers", hopperCount);
                if (sampler != null) json.add("hot", sampler.report());
                System.out.println("SB-BENCH sb " + json);
                cows.forEach(Entity::discard);
                h.assertTrue(sorted.length >= 300 && avg > 0, "MSPT window measured: " + sorted.length + " ticks");
                h.succeed();
            });
        }
    }

    /** Samples the server thread every 2 ms; counts the innermost com.simplebuilding frame (by mod package and by method). */
    private static final class Sampler {
        private final AtomicBoolean run = new AtomicBoolean();
        private final AtomicLong total = new AtomicLong(), ours = new AtomicLong();
        private final Map<String, Long> byMod = new ConcurrentHashMap<>(), byMethod = new ConcurrentHashMap<>();
        private Thread thread;

        void start(Thread target) {
            run.set(true);
            thread = new Thread(() -> {
                while (run.get()) {
                    var st = target.getStackTrace();
                    total.incrementAndGet();
                    for (var f : st) {
                        String c = f.getClassName();
                        if (!c.startsWith("com.simplebuilding.")) continue;
                        ours.incrementAndGet();
                        String[] p = c.split("\\.");
                        String mod = p.length > 3 && p[2].equals("modules") ? p[3] : (p.length > 2 ? p[2] : "core");
                        byMod.merge(mod, 1L, Long::sum);
                        byMethod.merge(c.substring(c.lastIndexOf('.') + 1) + "." + f.getMethodName(), 1L, Long::sum);
                        break;
                    }
                    try { Thread.sleep(2); } catch (InterruptedException e) { return; }
                }
            }, "sb-bench-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() { run.set(false); }

        JsonObject report() {
            var o = new JsonObject();
            o.addProperty("samples", total.get());
            o.addProperty("samples_in_simplebuilding", ours.get());
            o.add("by_mod", top(byMod, 12));
            o.add("by_method", top(byMethod, 15));
            return o;
        }

        private static JsonObject top(Map<String, Long> m, int n) {
            var o = new JsonObject();
            new TreeMap<>(m).entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(n)
                    .forEach(e -> o.addProperty(e.getKey(), e.getValue()));
            return o;
        }
    }
}
