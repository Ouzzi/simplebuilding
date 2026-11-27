package com.simplebuilding.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * O8-B audit 6 (release audit, docs in /root/sb-tools-b/audit-o8.md): heavier headless scenes than {@link SbBenchGameTest},
 * vanilla blocks only so a Fabric-API-only server and the 60 release mods see the same scene. Each test is a no-op unless its
 * system property is set (tools/bench/headless_bench.py --test load|endgen, soak via -PbenchProps):
 * <ul>
 * <li>{@code load} ({@code -Dsb.bench.load=true}): 32x32 pen with 120 cows, 60 sheep, 40 chickens, 20 villagers, 10 iron
 * golems; 16 observer clocks each driving a sticky piston (block moves back and forth), an 8-long dust line and a lamp; 16 loops of 4 hoppers with items
 * circulating; 32 furnaces smelting. MSPT avg/p95 over 400 ticks after 100 warmup (phase A, compared). Phase B then adds
 * the modded machines that exist in this server (Simple Machines hoppers, Simple Ovens furnaces) and measures 200 more
 * ticks ({@code mspt_b_avg}); in a vanilla server phase B is the unchanged scene.</li>
 * <li>{@code soak} ({@code -Dsb.bench.soak=<minutes>}, max 30): the load scene for that long, cows killed and respawned every
 * 10 s (entity churn); heap used after two System.gc() once per minute ({@code heap_series_mb}).</li>
 * <li>{@code endgen} ({@code -Dsb.bench.endgen=true}): generates 256 End chunks (outer islands, chunk x 96..111, z 0..15)
 * synchronously to FULL, 4 per tick, and reports the mean wall time per chunk.</li>
 * </ul>
 * Prints {@code SB-BENCH sb {json}} like the base bench.
 */
public final class SbLoadBenchGameTest {
    private static final int WARMUP = 100, WINDOW = 400, SETTLE_B = 100, WINDOW_B = 200;
    private static final int SOAK_MAX_MIN = 30;

    @GameTest(maxTicks = WARMUP + WINDOW + SETTLE_B + WINDOW_B + 100)
    public void load(GameTestHelper h) {
        if (!Boolean.getBoolean("sb.bench.load")) { h.succeed(); return; }
        Scene scene = build(h);
        var server = h.getLevel().getServer();
        long[] heap = new long[1];
        List<Long> a = new ArrayList<>(), b = new ArrayList<>();
        h.runAfterDelay(WARMUP, () -> heap[0] = heapAfterGc());
        for (int w = 1; w <= WINDOW / 100; w++) h.runAfterDelay(WARMUP + 100L * w, () -> collect(server.getTickTimesNanos(), a));
        int[] modded = new int[1];
        h.runAfterDelay(WARMUP + WINDOW + 1, () -> modded[0] = placeModded(h));
        for (int w = 1; w <= WINDOW_B / 100; w++) {
            boolean last = w == WINDOW_B / 100;
            h.runAfterDelay(WARMUP + WINDOW + SETTLE_B + 100L * w, () -> {
                collect(server.getTickTimesNanos(), b);
                if (!last) return;
                var json = stats(a);
                json.addProperty("mspt_b_avg", stats(b).get("mspt_avg").getAsDouble());
                json.addProperty("modded_machines", modded[0]);
                json.addProperty("heap_mb", Math.round(heap[0] / 1048576.0 * 10) / 10.0);
                json.addProperty("entities", scene.entities.size());
                mods(json);
                System.out.println("SB-BENCH sb " + json);
                scene.entities.forEach(Entity::discard);
                h.assertTrue(a.size() >= 300, "phase A measured: " + a.size() + " ticks");
                h.succeed();
            });
        }
    }

    @GameTest(maxTicks = SOAK_MAX_MIN * 1200 + 400)
    public void soak(GameTestHelper h) {
        int minutes = Math.min(SOAK_MAX_MIN, Integer.getInteger("sb.bench.soak", 0));
        if (minutes <= 0) { h.succeed(); return; }
        Scene scene = build(h);
        var server = h.getLevel().getServer();
        JsonArray series = new JsonArray();
        List<Long> ticks = new ArrayList<>();
        for (int m = 0; m <= minutes; m++) {
            int minute = m;
            h.runAfterDelay(100L + 1200L * m, () -> {
                series.add(Math.round(heapAfterGc() / 1048576.0 * 10) / 10.0);
                if (minute > 0) collect(server.getTickTimesNanos(), ticks);
                if (minute < minutes) return;
                var json = stats(ticks);
                json.add("heap_series_mb", series);
                json.addProperty("soak_minutes", minutes);
                json.addProperty("churn_spawns", scene.churned);
                mods(json);
                System.out.println("SB-BENCH sb " + json);
                scene.entities.forEach(Entity::discard);
                h.succeed();
            });
        }
        // entity churn every 10 s: the 120 cows die and come back (leaks in entity tracking/listeners show up as heap growth)
        for (long t = 200; t < 100L + 1200L * minutes; t += 200) {
            h.runAfterDelay(t, () -> {
                scene.cows.forEach(Entity::discard);
                scene.entities.removeAll(scene.cows);
                scene.cows.clear();
                for (int i = 0; i < 120; i++) scene.cows.add(h.spawn(EntityTypes.COW, 2 + i % 28, 1, 2 + (i / 28) * 2));
                scene.entities.addAll(scene.cows);
                scene.churned += 120;
            });
        }
    }

    @GameTest(maxTicks = 400)
    public void endgen(GameTestHelper h) {
        if (!Boolean.getBoolean("sb.bench.endgen")) { h.succeed(); return; }
        ServerLevel end = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(end != null, "End level exists");
        List<int[]> chunks = new ArrayList<>();
        for (int x = 96; x < 112; x++) for (int z = 0; z < 16; z++) chunks.add(new int[] {x, z});
        long[] total = new long[1], worst = new long[1];
        int[] done = new int[1], already = new int[1];
        int perTick = 4;
        for (int i = 0; i < chunks.size(); i += perTick) {
            int from = i;
            h.runAfterDelay(20 + i / perTick, () -> {
                for (int k = from; k < Math.min(chunks.size(), from + perTick); k++) {
                    int[] c = chunks.get(k);
                    if (end.getChunkSource().getChunkNow(c[0], c[1]) != null) already[0]++;
                    long t0 = System.nanoTime();
                    end.getChunk(c[0], c[1], ChunkStatus.FULL, true);
                    long dt = System.nanoTime() - t0;
                    total[0] += dt;
                    worst[0] = Math.max(worst[0], dt);
                    done[0]++;
                }
            });
        }
        h.runAfterDelay(20 + chunks.size() / perTick + 5, () -> {
            var json = new JsonObject();
            json.addProperty("endgen_chunks", done[0]);
            json.addProperty("endgen_already_loaded", already[0]);
            json.addProperty("endgen_ms_per_chunk", Math.round(total[0] / 1e6 / Math.max(1, done[0]) * 1000) / 1000.0);
            json.addProperty("endgen_worst_ms", Math.round(worst[0] / 1e6 * 10) / 10.0);
            json.addProperty("endgen_total_s", Math.round(total[0] / 1e9 * 100) / 100.0);
            mods(json);
            System.out.println("SB-BENCH sb " + json);
            h.assertTrue(done[0] == chunks.size(), "all chunks generated");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ scene
    private static final class Scene {
        final List<Entity> entities = new ArrayList<>(), cows = new ArrayList<>();
        int churned;
    }

    private static Scene build(GameTestHelper h) {
        Scene s = new Scene();
        BlockState stone = Blocks.STONE.defaultBlockState(), barrier = Blocks.BARRIER.defaultBlockState();
        // pen 32 x 32 (x 0..31, z 0..31)
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
            h.setBlock(x, 0, z, stone);
            if (x == 0 || z == 0 || x == 31 || z == 31) { h.setBlock(x, 1, z, barrier); h.setBlock(x, 2, z, barrier); }
        }
        for (int i = 0; i < 120; i++) s.cows.add(h.spawn(EntityTypes.COW, 2 + i % 28, 1, 2 + (i / 28) * 2));
        s.entities.addAll(s.cows);
        spawnMany(h, s, EntityTypes.SHEEP, 60, 12);
        spawnMany(h, s, EntityTypes.CHICKEN, 40, 18);
        spawnMany(h, s, EntityTypes.VILLAGER, 20, 23);
        spawnMany(h, s, EntityTypes.IRON_GOLEM, 10, 27);
        // redstone: 16 rows at x 34.., z 0..31 step 2: piston <- observer pair -> dust x8 -> lamp
        for (int r = 0; r < 16; r++) {
            int z = r * 2;
            for (int x = 33; x < 48; x++) h.setBlock(x, 0, z, stone);
            h.setBlock(34, 1, z, Blocks.STONE.defaultBlockState());
            h.setBlock(35, 1, z, Blocks.STICKY_PISTON.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST));
            h.setBlock(36, 1, z, Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
            h.setBlock(37, 1, z, Blocks.OBSERVER.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST));
            for (int x = 38; x < 46; x++) h.setBlock(x, 1, z, Blocks.REDSTONE_WIRE.defaultBlockState());
            h.setBlock(46, 1, z, Blocks.REDSTONE_LAMP.defaultBlockState());
        }
        // 16 hopper loops (2 x 2, each hopper points to the next) with one stack each, x 0..31, z 34..41
        for (int l = 0; l < 16; l++) {
            int bx = (l % 8) * 4, bz = 34 + (l / 8) * 4;
            hopperLoop(h, Blocks.HOPPER, bx, bz, new ItemStack(Items.COBBLESTONE, 64));
        }
        // 32 furnaces smelting (raw iron + coal), x 0..31, z 44
        for (int x = 0; x < 32; x++) {
            h.setBlock(x, 0, 44, stone);
            h.setBlock(x, 1, 44, Blocks.FURNACE.defaultBlockState());
            fill(h, new BlockPos(x, 1, 44), new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.COAL, 64));
        }
        return s;
    }

    private static void spawnMany(GameTestHelper h, Scene s, EntityType<?> type, int n, int row) {
        for (int i = 0; i < n; i++) s.entities.add(h.spawn(type, 2 + (i % 28), 1, row + (i / 28)));
    }

    private static void hopperLoop(GameTestHelper h, Block hopper, int bx, int bz, ItemStack stack) {
        int[][] pos = {{bx, bz}, {bx + 1, bz}, {bx + 1, bz + 1}, {bx, bz + 1}};
        Direction[] to = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        for (int i = 0; i < 4; i++) {
            h.setBlock(pos[i][0], 0, pos[i][1], Blocks.STONE.defaultBlockState());
            BlockState st = hopper.defaultBlockState();
            if (st.hasProperty(BlockStateProperties.FACING_HOPPER)) st = st.setValue(BlockStateProperties.FACING_HOPPER, to[i]);
            h.setBlock(pos[i][0], 1, pos[i][1], st);
        }
        fill(h, new BlockPos(bx, 1, bz), stack);
    }

    private static void fill(GameTestHelper h, BlockPos rel, ItemStack... stacks) {
        if (h.getLevel().getBlockEntity(h.absolutePos(rel)) instanceof Container c)
            for (int i = 0; i < stacks.length && i < c.getContainerSize(); i++) c.setItem(i, stacks[i].copy());
    }

    /** Phase B: modded machines that exist in this server (none in Vanilla): 8 hopper loops, 16 furnaces. */
    private static int placeModded(GameTestHelper h) {
        int n = 0;
        String[] hoppers = {"simplemachines:netherite_hopper", "simplemachines:enderite_hopper", "simplemachines:reinforced_hopper"};
        String[] furnaces = {"simpleovens:netherite_furnace", "simpleovens:enderite_furnace", "simpleovens:reinforced_furnace",
                "simpleovens:netherite_blast_furnace"};
        for (int l = 0; l < 8; l++) {
            Block b = block(hoppers[l % hoppers.length]);
            if (b == null) continue;
            hopperLoop(h, b, 34 + (l % 4) * 3, 34 + (l / 4) * 3, new ItemStack(Items.COBBLESTONE, 64));
            n += 4;
        }
        for (int i = 0; i < 16; i++) {
            Block b = block(furnaces[i % furnaces.length]);
            if (b == null) continue;
            BlockPos p = new BlockPos(i * 2, 1, 47);
            h.setBlock(i * 2, 0, 47, Blocks.STONE.defaultBlockState());
            h.setBlock(p, b.defaultBlockState());
            fill(h, p, new ItemStack(Items.RAW_IRON, 64), new ItemStack(Items.COAL, 64));
            n++;
        }
        return n;
    }

    private static Block block(String id) {
        int colon = id.indexOf(':');
        var key = Identifier.fromNamespaceAndPath(id.substring(0, colon), id.substring(colon + 1));
        return BuiltInRegistries.BLOCK.containsKey(key) ? BuiltInRegistries.BLOCK.getValue(key) : null;
    }

    // ------------------------------------------------------------------ numbers
    private static long heapAfterGc() {
        System.gc();
        System.gc();
        return java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static void collect(long[] ring, List<Long> into) {
        for (long t : ring) if (t > 0) into.add(t);
    }

    private static JsonObject stats(List<Long> ticks) {
        long[] sorted = ticks.stream().mapToLong(Long::longValue).sorted().toArray();
        double avg = Arrays.stream(sorted).average().orElse(0) / 1e6;
        double p95 = sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) Math.ceil(sorted.length * 0.95) - 1)] / 1e6;
        var json = new JsonObject();
        json.addProperty("mspt_avg", Math.round(avg * 1000) / 1000.0);
        json.addProperty("mspt_p95", Math.round(p95 * 1000) / 1000.0);
        json.addProperty("ticks", sorted.length);
        return json;
    }

    private static void mods(JsonObject json) {
        json.addProperty("mods", FabricLoader.getInstance().getAllMods().size());
        json.addProperty("sb_mods", FabricLoader.getInstance().getAllMods().stream().filter(m -> m.getMetadata().getId().startsWith("simple")).count());
    }
}
