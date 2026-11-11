package com.simplebuilding.modules.simpleoptimization;

import com.simplebuilding.framework.api.SubMods;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Super-mod checks shared by all loaders (Fabric wrapper ModuleGameTest, NeoForge ModuleNeoTests). */
public final class OptimizationTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("sub_mod_switch", OptimizationTests::subModSwitch);
        ALL.put("config_file", OptimizationTests::configFile);
        ALL.put("bench_headless", OptimizationTests::benchHeadless);
    }

    private OptimizationTests() {}

    /** "Enable Simple X" reaches exactly its sub-mod through framework SubMods; defaults on; restored afterwards. */
    public static void subModSwitch(GameTestHelper h) {
        var defaults = new SimpleOptimization.Config();
        for (String subMod : SimpleOptimization.SUB_MODS) h.assertTrue(defaults.enabled(subMod), "Default on: " + subMod);
        try {
            for (String off : SimpleOptimization.SUB_MODS) {
                var c = new SimpleOptimization.Config();
                c.enableSimpleOptRender = !off.equals("simpleoptrender");
                c.enableSimpleOptMemory = !off.equals("simpleoptmemory");
                c.enableSimpleOptServer = !off.equals("simpleoptserver");
                SimpleOptimization.apply(c);
                for (String subMod : SimpleOptimization.SUB_MODS)
                    h.assertTrue(SubMods.enabled(subMod) == !subMod.equals(off), "Switch for " + off + " reaches only it (" + subMod + ")");
            }
        } finally {
            SimpleOptimization.apply(SimpleOptimization.config());
        }
        for (String subMod : SimpleOptimization.SUB_MODS)
            h.assertTrue(SubMods.enabled(subMod) == SimpleOptimization.config().enabled(subMod), "Switch restored: " + subMod);
        h.succeed();
    }

    /** The config file is created complete, a broken file is quarantined, and an edited switch is read back. */
    public static void configFile(GameTestHelper h) {
        var live = SimpleOptimization.config();
        try {
            var dir = java.nio.file.Files.createTempDirectory("simpleoptimization-test");
            SimpleOptimization.load(dir);
            var file = dir.resolve(SimpleOptimization.FILE);
            h.assertTrue(java.nio.file.Files.readString(file).contains("enableSimpleOptServer"), "File written with every switch");
            java.nio.file.Files.writeString(file, "{\"enableSimpleOptMemory\": false}");
            SimpleOptimization.load(dir);
            h.assertTrue(!SimpleOptimization.config().enableSimpleOptMemory && SimpleOptimization.config().enableSimpleOptRender, "Edited switch read, others default");
            h.assertTrue(!SubMods.enabled("simpleoptmemory"), "Loaded switch applied");
            java.nio.file.Files.writeString(file, "{ broken");
            SimpleOptimization.load(dir);
            h.assertTrue(SimpleOptimization.config().enableSimpleOptMemory, "Broken file falls back to defaults");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } finally {
            SimpleOptimization.restore(live);
        }
        h.succeed();
    }
    /** Ticks of the headless bench: warmup, then {@link #BENCH_WINDOWS} windows of 100 server ticks (MSPT ring buffer). */
    public static final int BENCH_WARMUP = 100, BENCH_WINDOWS = 4, BENCH_MAX_TICKS = BENCH_WARMUP + 100 * BENCH_WINDOWS + 100;

    /**
     * Headless measurement (N27b, plan docs/ai/PLAN-B-OPTE4-2026-11-11.md): a fixed scene (120 cows in a pen, 64 idle
     * hoppers under 32 furnaces, a 6x6x6 persistent oak-leaf cube), then MSPT avg/p95 over 400 server ticks, heap after GC,
     * O4 state-table counts and the O1 leaf-face count (faces the Vanilla mesher would draw vs. with our leaf culling,
     * evaluated on the server level with the same rule). One line {@code SB-BENCH headless {json}}; tools/bench/headless_bench.py
     * runs it with every area off / default / all on and compares. Sub-mod classes only by reflection (super-mod rule).
     * Asserted is only plausibility, never a time (machine dependent).
     */
    public static void benchHeadless(GameTestHelper h) {
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var barrier = net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            h.setBlock(x, 0, z, stone);
            if (x == 0 || z == 0 || x == 15 || z == 15) { h.setBlock(x, 1, z, barrier); h.setBlock(x, 2, z, barrier); }
        }
        var cowList = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        for (int x = 1; x <= 14 && cowList.size() < 120; x++) for (int z = 1; z <= 14 && cowList.size() < 120; z++)
            cowList.add(h.spawn(net.minecraft.world.entity.EntityTypes.COW, x, 1, z));
        int cows = cowList.size();
        int hoppers = 0;
        for (int x = 0; x < 16; x++) for (int z = 17; z < 21; z++, hoppers++) {
            h.setBlock(x, 1, z, net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
            if (((x + z) & 1) == 0) h.setBlock(x, 2, z, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
        }
        var leaf = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        for (int x = 20; x < 26; x++) for (int y = 1; y < 7; y++) for (int z = 0; z < 6; z++) h.setBlock(x, y, z, leaf);
        final int cowCount = cows, hopperCount = hoppers;
        var server = h.getLevel().getServer();
        var ticks = new java.util.ArrayList<Long>();
        long[] heap = new long[1];
        h.runAfterDelay(BENCH_WARMUP, () -> {
            System.gc();
            System.gc();
            var mem = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            heap[0] = mem.getUsed();
        });
        for (int w = 1; w <= BENCH_WINDOWS; w++) {
            boolean last = w == BENCH_WINDOWS;
            h.runAfterDelay(BENCH_WARMUP + 100L * w, () -> {
                for (long t : server.getTickTimesNanos()) ticks.add(t);
                if (!last) return;
                long[] sorted = ticks.stream().mapToLong(Long::longValue).filter(t -> t > 0).sorted().toArray();
                double avg = java.util.Arrays.stream(sorted).average().orElse(0) / 1e6;
                double p95 = sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * 0.95) - 1)] / 1e6;
                int[] faces = leafFaces(h, 20, 1, 0, 6);
                long shared = longCall("com.simplebuilding.modules.simpleoptmemory.StateTables", "shared");
                long seen = longCall("com.simplebuilding.modules.simpleoptmemory.StateTables", "seen");
                long saved = longCall("com.simplebuilding.modules.simpleoptmemory.StateTables", "savedBytes");
                var areas = new java.util.TreeMap<String, Boolean>();
                for (var d : com.simplebuilding.framework.api.OptCompat.all()) areas.put(d.area().id(), d.active());
                var json = new com.google.gson.JsonObject();
                json.addProperty("mspt_avg", Math.round(avg * 1000) / 1000.0);
                json.addProperty("mspt_p95", Math.round(p95 * 1000) / 1000.0);
                json.addProperty("ticks", sorted.length);
                json.addProperty("heap_mb", Math.round(heap[0] / 1048576.0 * 10) / 10.0);
                json.addProperty("state_tables_seen", seen);
                json.addProperty("state_tables_shared", shared);
                json.addProperty("state_tables_saved_kib", saved / 1024);
                json.addProperty("leaf_faces_vanilla", faces[0]);
                json.addProperty("leaf_faces_ours", faces[1]);
                json.addProperty("cows", cowCount);
                json.addProperty("hoppers", hopperCount);
                json.addProperty("areas_on", String.join(",", areas.entrySet().stream().filter(java.util.Map.Entry::getValue).map(java.util.Map.Entry::getKey).toList()));
                SimpleOptimization.LOG.info("SB-BENCH headless {}", json);
                cowList.forEach(net.minecraft.world.entity.Entity::discard);
                h.assertTrue(sorted.length >= 300 && avg > 0, "MSPT window measured: " + sorted.length + " ticks");
                h.assertTrue(heap[0] > 0, "heap measured");
                h.assertTrue(faces[0] > 0 && faces[1] <= faces[0], "leaf faces: ours never more than Vanilla " + faces[1] + "/" + faces[0]);
                if (Boolean.TRUE.equals(areas.get("leaf_culling"))) h.assertTrue(faces[1] < faces[0], "leaf culling on hides inner faces");
                if (Boolean.TRUE.equals(areas.get("state_tables"))) h.assertTrue(shared > 0, "state tables on shares arrays");
                h.succeed();
            });
        }
    }

    /** {Vanilla faces, faces left with our leaf culling (only when that area is live)} for the leaf cube of size n. */
    private static int[] leafFaces(GameTestHelper h, int x0, int y0, int z0, int n) {
        var level = h.getLevel();
        java.lang.reflect.Method cull = null;
        int depth = 0;
        boolean live = com.simplebuilding.framework.api.OptCompat.active("simpleoptrender", "leaf_culling");
        try {
            cull = Class.forName("com.simplebuilding.modules.simpleoptrender.LeafCulling").getMethod("cull",
                    net.minecraft.world.level.BlockGetter.class, net.minecraft.world.level.block.state.BlockState.class,
                    net.minecraft.core.Direction.class, net.minecraft.core.BlockPos.class, int.class);
            depth = (int) longCall("com.simplebuilding.modules.simpleoptrender.RenderOpt", "leafDepth");
        } catch (ReflectiveOperationException e) {
            live = false;
        }
        int vanilla = 0, ours = 0;
        for (int x = x0; x < x0 + n; x++) for (int y = y0; y < y0 + n; y++) for (int z = z0; z < z0 + n; z++) {
            var pos = h.absolutePos(new net.minecraft.core.BlockPos(x, y, z));
            var state = level.getBlockState(pos);
            for (var dir : net.minecraft.core.Direction.values()) {
                var np = pos.relative(dir);
                if (!net.minecraft.world.level.block.Block.shouldRenderFace(state, level.getBlockState(np), dir)) continue;
                vanilla++;
                boolean hidden = false;
                if (live) try {
                    hidden = (boolean) cull.invoke(null, level, state, dir, np, depth);
                } catch (ReflectiveOperationException e) {
                    hidden = false;
                }
                if (!hidden) ours++;
            }
        }
        return new int[] {vanilla, ours};
    }

    private static long longCall(String cls, String method) {
        try {
            return ((Number) Class.forName(cls).getMethod(method).invoke(null)).longValue();
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}
