package com.simplebuilding.modules.simpleearth.terrain;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthServerConfig;
import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.TileRequestPayload;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Serves terrain tiles to clients (concept §4.2): requests queue per player (deduplicated, capped), a token budget per player
 * ({@code tilesPerSecond}), cache hits go out at once, misses are sampled on a small low-priority worker pool - never on the
 * server thread - and handed back to the server thread for sending.
 */
public final class TileService {
    public static final int MAX_QUEUE = 512;
    private static final Map<UUID, Queue> QUEUES = new HashMap<>();
    private static final Map<ResourceKey<Level>, TileStore> STORES = new HashMap<>();
    private static ExecutorService pool;
    private static final AtomicInteger RUNNING = new AtomicInteger();
    /** Tiles sampled since start (tests, debug). */
    public static final AtomicInteger SAMPLED = new AtomicInteger();

    private static final class Queue {
        final ArrayDeque<TileRequestPayload.Key> keys = new ArrayDeque<>();
        final Set<TileRequestPayload.Key> known = new HashSet<>();
        double tokens;
    }

    private TileService() {}

    public static synchronized ExecutorService pool() {
        if (pool == null) {
            AtomicInteger n = new AtomicInteger();
            pool = Executors.newFixedThreadPool(EarthServerConfig.workerThreads, r -> {
                Thread t = new Thread(r, "Simple Earth sampler " + n.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
        }
        return pool;
    }

    public static synchronized void stop() {
        if (pool != null) pool.shutdownNow();
        pool = null;
        QUEUES.clear();
        STORES.clear();
    }

    public static synchronized TileStore store(ServerLevel level) {
        return STORES.computeIfAbsent(level.dimension(), k -> {
            boolean disk = !EarthServerConfig.exploredOnly && EarthServerConfig.diskCacheMiB > 0;
            var dir = disk ? level.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(SimpleEarth.MOD_ID)
                    .resolve(k.identifier().getNamespace()).resolve(k.identifier().getPath()) : null;
            return new TileStore(dir, EarthServerConfig.memoryTiles, EarthServerConfig.diskCacheMiB * 1024L * 1024L);
        });
    }

    public static boolean validKey(TileRequestPayload.Key k) {
        if (k.e() < TileMath.MIN_E || k.e() > TileMath.MAX_E) return false;
        long limit = TileMath.WORLD_LIMIT / ((long) TileMath.TILE << k.e()) + 1;
        return Math.abs((long) k.tx()) <= limit && Math.abs((long) k.tz()) <= limit;
    }

    public static synchronized void request(ServerPlayer player, TileRequestPayload payload) {
        Queue q = QUEUES.computeIfAbsent(player.getUUID(), id -> new Queue());
        for (TileRequestPayload.Key k : payload.keys()) {
            if (q.keys.size() >= MAX_QUEUE) break;
            if (validKey(k) && q.known.add(k)) q.keys.add(k);
        }
    }

    public static synchronized void forget(UUID player) {
        QUEUES.remove(player);
    }

    /** Server tick: hand out the budget, send cache hits, start sampling for misses. */
    public static void tick(MinecraftServer server) {
        Map<UUID, Queue> snapshot;
        synchronized (TileService.class) {
            snapshot = new HashMap<>(QUEUES);
        }
        for (Map.Entry<UUID, Queue> entry : snapshot.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                forget(entry.getKey());
                continue;
            }
            Queue q = entry.getValue();
            ServerLevel level = player.level();
            TileStore store = store(level);
            synchronized (TileService.class) {
                q.tokens = Math.min(EarthServerConfig.tilesPerSecond, q.tokens + EarthServerConfig.tilesPerSecond / 20.0);
                while (q.tokens >= 1 && !q.keys.isEmpty() && RUNNING.get() < EarthServerConfig.workerThreads * 4) {
                    TileRequestPayload.Key k = q.keys.poll();
                    q.tokens -= 1;
                    byte[] hit = store.memory(k.e(), k.tx(), k.tz());
                    if (hit != null) {
                        q.known.remove(k);
                        send(player, k, hit);
                        continue;
                    }
                    RUNNING.incrementAndGet();
                    UUID id = entry.getKey();
                    boolean explored = EarthServerConfig.exploredOnly;
                    pool().execute(() -> {
                        try {
                            byte[] data = fetch(level, store, k, explored);
                            server.execute(() -> {
                                synchronized (TileService.class) {
                                    Queue now = QUEUES.get(id);
                                    if (now != null) now.known.remove(k);
                                }
                                ServerPlayer p = server.getPlayerList().getPlayer(id);
                                if (p != null && p.level() == level) send(p, k, data);
                            });
                        } catch (RuntimeException ex) {
                            SimpleEarth.LOG.warn("Earth tile {} failed: {}", k, ex.toString());
                        } finally {
                            RUNNING.decrementAndGet();
                        }
                    });
                }
            }
        }
    }

    /** Cached tile or a freshly sampled one (stored afterwards). */
    public static byte[] fetch(ServerLevel level, TileStore store, TileRequestPayload.Key k, boolean exploredOnly) {
        byte[] data = store.get(k.e(), k.tx(), k.tz());
        if (data != null) return data;
        TerrainSampler.Tile tile = TerrainSampler.sample(level, k.e(), k.tx(), k.tz(), exploredOnly);
        data = TileCodec.encode(tile.colors(), tile.heights());
        SAMPLED.incrementAndGet();
        store.put(k.e(), k.tx(), k.tz(), data);
        return data;
    }

    private static void send(ServerPlayer player, TileRequestPayload.Key k, byte[] data) {
        SimpleEarth.toClient.accept(player, new EarthTilePayload(k.e(), k.tx(), k.tz(), data));
    }
}
