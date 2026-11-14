package com.simplebuilding.integration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * a-hang50 (owner 14.11.: "spawned a mod mob, died, flew back to its chunk - nothing loaded any more, closing hung the whole
 * game; reopening the world took ages and closing hung again"). Every entity type of the family (plus a Vanilla zombie and cow
 * as the control) is spawned twice in far chunks - once with AI as from a spawn egg, once still like the test centre's mob
 * buttons (no AI, silent, persistent, tagged) - and has to survive the round trip that broke the owner's world:
 * <ol>
 *   <li>A: forced chunks - spawn, tick, release, wait until the chunks (and their entity sections) are unloaded, force them
 *   again, wait until the entities are back, tick, every still mob present exactly once, flush-save the level (the same
 *   {@code PersistentEntitySectionManager#saveAll} loop that runs when a world is closed);</li>
 *   <li>B: a player - walks there, the mobs come, a family mob kills him, he respawns far away, the chunks unload, he comes
 *   back, the same checks and the flush-save.</li>
 * </ol>
 * A watchdog thread dumps every thread into the log and ends this process when the server thread stalls for
 * {@link #STALL_MS} (the test itself could never time out: it runs on that thread).
 */
public final class MobReloadGameTest {
    static final long STALL_MS = 60_000L, AFTER_FINISH_MS = 240_000L;
    static final int WAIT_LOAD_TICKS = 600, WAIT_UNLOAD_TICKS = 1200, LIVE_TICKS = 100;
    static final String STILL_TAG = "sb_testcentre_mob";

    @GameTest(maxTicks = 12000)
    public void mobsSurviveUnloadReloadAndSave(GameTestHelper h) {
        Run run = new Run(h);
        Watchdog.start(run);
        h.onEachTick(run::tick);
    }

    /** One spawned entity: what it is and whether it must come back (the still copies are persistent). */
    private record Spawned(UUID id, String label, boolean mustReturn, ChunkPos chunk) {}

    private static final class Run {
        final GameTestHelper h;
        final ServerLevel level;
        final MinecraftServer server;
        final List<EntityType<?>> types = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        final List<Spawned> spawned = new ArrayList<>();
        final Set<ChunkPos> chunks = new LinkedHashSet<>();
        final Map<EntityType<?>, ChunkPos> slot = new LinkedHashMap<>();
        Difficulty difficultyBefore;
        volatile String phase = "start";
        volatile long beat = System.currentTimeMillis();
        long lastTickAt = 0L, worstGap = 0L;
        String worstGapPhase = "";
        int step = 0, waited = 0;
        ServerPlayer player;
        BlockPos playerSpot;

        Run(GameTestHelper h) {
            this.h = h;
            this.level = h.getLevel();
            this.server = level.getServer();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id == null || "minecraft".equals(id.getNamespace()) || !type.canSummon()) continue;
                types.add(type);
            }
            types.add(EntityTypes.ZOMBIE);
            types.add(EntityTypes.COW);
            log("types " + types.size() + ": " + types.stream().map(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()).toList());
        }

        void log(String text) {
            System.out.println("HANG50 [" + phase + "] " + text);
        }

        void phase(String next) {
            phase = next;
            waited = 0;
            log("phase " + next + " loadedChunks=" + level.getChunkSource().getLoadedChunksCount()
                    + " entities=" + countEntities());
        }

        int countEntities() {
            int n = 0;
            for (Entity ignored : level.getAllEntities()) n++;
            return n;
        }

        void tick() {
            long now = System.currentTimeMillis();
            beat = now;
            if (lastTickAt != 0L && now - lastTickAt > worstGap) {
                worstGap = now - lastTickAt;
                worstGapPhase = phase;
            }
            if (lastTickAt != 0L && now - lastTickAt > 1000L) log("slow tick " + (now - lastTickAt) + " ms");
            lastTickAt = now;
            waited++;
            switch (step) {
                case 0 -> {
                    difficultyBefore = server.getWorldData().getDifficulty();
                    server.setDifficulty(Difficulty.NORMAL, true);
                    for (int i = 0; i < types.size(); i++) {
                        ChunkPos c = new ChunkPos(512 + (i % 8) * 3, 512 + (i / 8) * 3);
                        slot.put(types.get(i), c);
                        chunks.add(c);
                    }
                    phase("A-force");
                    for (ChunkPos c : chunks) level.setChunkForced(c.x(), c.z(), true);
                    step = 1;
                }
                case 1 -> {
                    if (allLoaded()) {
                        phase("A-spawn");
                        spawnAll(false);
                        phase("A-live");
                        step = 2;
                    } else if (waited > WAIT_LOAD_TICKS) {
                        finish("A: forced chunks never loaded: " + notLoaded());
                    }
                }
                case 2 -> {
                    if (waited >= LIVE_TICKS) {
                        phase("A-release");
                        for (ChunkPos c : chunks) level.setChunkForced(c.x(), c.z(), false);
                        step = 3;
                    }
                }
                case 3 -> {
                    if (allUnloaded() || waited > WAIT_UNLOAD_TICKS) {
                        if (!allUnloaded()) note("A: chunks still loaded " + WAIT_UNLOAD_TICKS + " ticks after release: " + stillLoaded());
                        phase("A-reforce");
                        for (ChunkPos c : chunks) level.setChunkForced(c.x(), c.z(), true);
                        step = 4;
                    }
                }
                case 4 -> {
                    if (allLoaded() || waited > WAIT_LOAD_TICKS) {
                        if (!allLoaded()) note("A: entities of these chunks never loaded again: " + notLoaded());
                        phase("A-live2");
                        step = 5;
                    }
                }
                case 5 -> {
                    if (waited >= LIVE_TICKS) {
                        checkReturned("A");
                        flushSave("A");
                        phase("A-done");
                        for (ChunkPos c : chunks) level.setChunkForced(c.x(), c.z(), false);
                        step = 10;
                    }
                }
                // ---------------------------------------------------------------- B: a player dies there and comes back
                case 10 -> {
                    chunks.clear();
                    slot.clear();
                    for (int i = 0; i < types.size(); i++) {
                        int j = i / 2;
                        ChunkPos c = new ChunkPos(-512 + (j % 5) - 2, 512 + (j / 5) - 2);
                        slot.put(types.get(i), c);
                        chunks.add(c);
                    }
                    phase("B-goto");
                    level.setChunkForced(-512, 512, true);
                    step = 11;
                }
                case 11 -> {
                    if (level.areEntitiesLoaded(ChunkPos.pack(-512, 512)) && level.getChunkSource().getChunkNow(-512, 512) != null) {
                        playerSpot = ground(-512 * 16 + 8, 512 * 16 + 8);
                        player = h.makeMockServerPlayerInLevel();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                        player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 32));
                        player.getInventory().setItem(9, new ItemStack(Items.BREAD, 16));
                        moveTo(player, playerSpot);
                        level.setChunkForced(-512, 512, false);
                        phase("B-arrive");
                        step = 12;
                    } else if (waited > WAIT_LOAD_TICKS) {
                        finish("B: target chunk never loaded");
                    }
                }
                case 12 -> {
                    if (allLoaded() || waited > WAIT_LOAD_TICKS) {
                        if (!allLoaded()) note("B: chunks around the player never loaded: " + notLoaded());
                        phase("B-spawn");
                        spawned.clear();
                        spawnAll(true);
                        phase("B-live");
                        step = 13;
                    }
                }
                case 13 -> {
                    if (waited >= 60) {
                        phase("B-die");
                        killPlayer();
                        phase("B-respawn");
                        step = 14;
                    }
                }
                case 14 -> {
                    if (allUnloaded() || waited > WAIT_UNLOAD_TICKS) {
                        if (!allUnloaded()) note("B: chunks still loaded " + WAIT_UNLOAD_TICKS + " ticks after the player left: " + stillLoaded());
                        phase("B-return");
                        moveTo(player, playerSpot.above(2));
                        step = 15;
                    }
                }
                case 15 -> {
                    if (allLoaded() || waited > WAIT_LOAD_TICKS) {
                        if (!allLoaded()) note("B: entities of these chunks never loaded again after the return: " + notLoaded());
                        phase("B-live2");
                        step = 16;
                    }
                }
                case 16 -> {
                    if (waited >= LIVE_TICKS) {
                        checkReturned("B");
                        flushSave("B");
                        phase("B-done");
                        try {
                            server.getPlayerList().remove(player);
                        } catch (RuntimeException e) {
                            log("player remove: " + e);
                        }
                        finish(null);
                    }
                }
                default -> { }
            }
        }

        void note(String problem) {
            problems.add(problem);
            log("PROBLEM " + problem);
        }

        void finish(String fatal) {
            if (fatal != null) note(fatal);
            if (difficultyBefore != null) server.setDifficulty(difficultyBefore, true);
            log("worst tick gap " + worstGap + " ms in " + worstGapPhase);
            Watchdog.finished();
            step = 99;
            if (problems.isEmpty()) {
                log("RESULT ok");
                h.succeed();
            } else {
                log("RESULT " + problems.size() + " problem(s)");
                h.fail(String.join(" | ", problems));
            }
        }

        BlockPos ground(int x, int z) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY() + 1) {
                y = 64;
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) level.setBlockAndUpdate(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE.defaultBlockState());
            }
            return new BlockPos(x, y, z);
        }

        void moveTo(ServerPlayer p, BlockPos pos) {
            p.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), 0.0F, 0.0F, false);
            level.getChunkSource().move(p);
        }

        void spawnAll(boolean nearPlayer) {
            Map<ChunkPos, Integer> used = new LinkedHashMap<>();
            for (EntityType<?> type : types) {
                ChunkPos c = slot.get(type);
                int n = used.merge(c, 1, Integer::sum) - 1;
                int x = c.getMinBlockX() + 4 + (n % 2) * 6, z = c.getMinBlockZ() + 4;
                BlockPos at = ground(x, z);
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                spawnOne(type, at, id + "/ai", false, c);
                spawnOne(type, ground(x + 2, z + 6), id + "/still", true, c);
            }
            log("spawned " + spawned.size() + " entities (" + spawned.stream().filter(Spawned::mustReturn).count() + " must return)");
        }

        void spawnOne(EntityType<?> type, BlockPos at, String label, boolean still, ChunkPos c) {
            try {
                Entity e = type.spawn(level, at, still ? EntitySpawnReason.COMMAND : EntitySpawnReason.SPAWN_ITEM_USE);
                if (e == null) {
                    log("spawn " + label + " gave nothing");
                    return;
                }
                boolean mustReturn = false;
                if (still && e instanceof Mob mob) {
                    mob.setNoAi(true);
                    mob.setSilent(true);
                    mob.setPersistenceRequired();
                    mob.addTag(STILL_TAG);
                    mustReturn = true;
                }
                spawned.add(new Spawned(e.getUUID(), label, mustReturn, c));
            } catch (Throwable t) {
                note("spawning " + label + " threw " + t);
                t.printStackTrace(System.out);
            }
        }

        void killPlayer() {
            LivingEntity killer = null;
            for (Spawned s : spawned) {
                if (s.label().startsWith("minecraft:")) continue;
                if (level.getEntity(s.id()) instanceof Mob mob && mob.isAlive()) {
                    killer = mob;
                    break;
                }
            }
            try {
                if (player.isAlive()) {
                    player.hurtServer(level, killer != null ? level.damageSources().mobAttack(killer) : level.damageSources().genericKill(), 10_000.0F);
                }
                if (player.isAlive()) player.die(level.damageSources().genericKill());
                log("player dead=" + !player.isAlive() + " killer=" + (killer == null ? "-" : BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType())));
                ServerPlayer fresh = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
                player = fresh;
                if (player.blockPosition().closerThan(playerSpot, 1000)) moveTo(player, ground(0, 0).above(2));
                log("respawned at " + player.blockPosition());
            } catch (Throwable t) {
                note("death/respawn threw " + t);
                t.printStackTrace(System.out);
                moveTo(player, ground(0, 0).above(2));
            }
        }

        void checkReturned(String part) {
            List<String> missing = new ArrayList<>();
            List<String> doubled = new ArrayList<>();
            for (Spawned s : spawned) {
                if (!s.mustReturn()) continue;
                if (level.getEntity(s.id()) == null) missing.add(s.label());
            }
            Map<String, Integer> still = new LinkedHashMap<>();
            for (Entity e : level.getAllEntities()) {
                if (e.entityTags().contains(STILL_TAG)) still.merge(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(), 1, Integer::sum);
            }
            still.forEach((id, n) -> { if (n > 1) doubled.add(id + " x" + n); });
            if (!missing.isEmpty()) note(part + ": still mobs missing after the reload: " + missing);
            if (!doubled.isEmpty()) note(part + ": still mobs doubled after the reload: " + doubled);
            log("returned check: missing=" + missing.size() + " doubled=" + doubled.size());
        }

        void flushSave(String part) {
            long t0 = System.currentTimeMillis();
            phase(part + "-save");
            level.save(null, true, false);
            long dt = System.currentTimeMillis() - t0;
            log("flush save took " + dt + " ms");
            if (dt > 20_000L) note(part + ": flush save took " + dt + " ms");
        }

        boolean allLoaded() {
            for (ChunkPos c : chunks) {
                if (level.getChunkSource().getChunkNow(c.x(), c.z()) == null || !level.areEntitiesLoaded(c.pack())) return false;
            }
            return true;
        }

        boolean allUnloaded() {
            for (ChunkPos c : chunks) {
                if (level.getChunkSource().getChunkNow(c.x(), c.z()) != null || level.areEntitiesLoaded(c.pack())) return false;
            }
            return true;
        }

        List<String> notLoaded() {
            List<String> out = new ArrayList<>();
            for (ChunkPos c : chunks) {
                if (level.getChunkSource().getChunkNow(c.x(), c.z()) == null || !level.areEntitiesLoaded(c.pack())) out.add(c + typesIn(c));
            }
            return out;
        }

        List<String> stillLoaded() {
            List<String> out = new ArrayList<>();
            for (ChunkPos c : chunks) {
                if (level.getChunkSource().getChunkNow(c.x(), c.z()) != null || level.areEntitiesLoaded(c.pack())) out.add(c + typesIn(c));
            }
            return out;
        }

        String typesIn(ChunkPos c) {
            List<String> ids = new ArrayList<>();
            slot.forEach((type, at) -> { if (at.equals(c)) ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()); });
            return ids.toString();
        }
    }

    /** Dumps every thread and ends the process when the server thread stalls (or the run outlives the test by far). */
    private static final class Watchdog {
        private static volatile Run run;
        private static volatile long finishedAt;
        private static Thread thread;

        static synchronized void start(Run r) {
            run = r;
            finishedAt = 0L;
            if (thread != null) return;
            thread = new Thread(Watchdog::loop, "hang50-watchdog");
            thread.setDaemon(true);
            thread.start();
        }

        static void finished() {
            finishedAt = System.currentTimeMillis();
        }

        private static void loop() {
            while (true) {
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException e) {
                    return;
                }
                Run r = run;
                long now = System.currentTimeMillis();
                if (finishedAt != 0L) {
                    if (now - finishedAt > AFTER_FINISH_MS) {
                        dump("process still alive " + (now - finishedAt) + " ms after the test finished (server stop hangs?)");
                        Runtime.getRuntime().halt(4);
                    }
                } else if (r != null && now - r.beat > STALL_MS) {
                    dump("server thread stalled " + (now - r.beat) + " ms in phase " + r.phase);
                    Runtime.getRuntime().halt(3);
                }
            }
        }

        static void dump(String why) {
            StringBuilder out = new StringBuilder("HANG50-DUMP ").append(why).append('\n');
            Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
            List<Thread> order = new ArrayList<>(all.keySet());
            order.sort((a, b) -> Boolean.compare(!"Server thread".equals(a.getName()), !"Server thread".equals(b.getName())));
            for (Thread t : order) {
                StackTraceElement[] st = all.get(t);
                if (st.length == 0) continue;
                out.append("HANG50-DUMP --- ").append(t.getName()).append(" (").append(t.getState()).append(")\n");
                for (int i = 0; i < Math.min(st.length, "Server thread".equals(t.getName()) ? 200 : 40); i++) {
                    out.append("HANG50-DUMP     at ").append(st[i]).append('\n');
                }
            }
            System.out.print(out);
            System.out.flush();
        }
    }
}
