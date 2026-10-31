package com.simplebuilding.modules.simplelivemap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.modules.simplelivemap.web.LivemapWeb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Server side of the live map: records explored chunks (players, held Vanilla maps), draws them with a per-tick budget, saves
 * the store, runs the optional web server and prepares {@code live.json}. All game access on the server thread; tile files
 * on the {@link TileStore} thread; the web server only reads files and the prepared string.
 */
public final class LivemapRuntime {
    public static final int SAVE_TICKS = 6000, MAX_QUEUE = 1 << 16, MAP_SECONDS = 30, MAX_SESSION = 1 << 20;

    private static MinecraftServer server;
    private static VisitedStore store = new VisitedStore();
    private static TileStore tiles;
    private static LivemapWeb web;
    private static Path root;
    private static int tick, webRetryTick;
    private static volatile String live = "{\"players\":[],\"dimensions\":[]}";
    /** Chunks waiting to be drawn, per dimension id (insertion order, no duplicates). */
    private static final Map<String, java.util.LinkedHashSet<Long>> queue = new HashMap<>();
    /** When a chunk was last drawn in this session (server tick), so revisits redraw only after refreshMinutes. */
    private static final Map<String, Map<Long, Integer>> drawn = new HashMap<>();
    /** Last chunk per player (only a chunk change records), and when a map id was last read. */
    private static final Map<UUID, Long> lastChunk = new HashMap<>();
    private static final Map<Integer, Integer> mapRead = new HashMap<>();

    private LivemapRuntime() {}

    public static VisitedStore store() {
        return store;
    }

    public static TileStore tiles() {
        return tiles;
    }

    public static LivemapWeb web() {
        return web;
    }

    public static String live() {
        return live;
    }

    public static Path root(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().resolve(SimpleLivemap.MOD_ID);
    }

    public static void started(MinecraftServer s) {
        server = s;
        root = root(s);
        Path file = root.resolve("visited.bin");
        try {
            store = VisitedStore.load(file);
        } catch (IOException | RuntimeException e) {
            SimpleLivemap.LOG.warn("Simple Livemap: {} unreadable ({}), kept aside, starting empty", file, e.toString());
            try {
                Files.move(file, file.resolveSibling("visited.bin.broken-" + System.currentTimeMillis()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
            }
            store = new VisitedStore();
        }
        tiles = new TileStore(root.resolve("tiles"));
        queue.clear();
        drawn.clear();
        lastChunk.clear();
        mapRead.clear();
        tick = 0;
        webRetryTick = 0;
        syncWeb(s);
    }

    public static void stopping(MinecraftServer s) {
        if (s != server) return;
        if (web != null) {
            web.close();
            web = null;
        }
        save();
        if (tiles != null) tiles.close();
        tiles = null;
        server = null;
    }

    public static void save() {
        if (root == null) return;
        try {
            if (store.dirty()) store.save(root.resolve("visited.bin"));
        } catch (IOException e) {
            SimpleLivemap.LOG.warn("Simple Livemap: could not save explored chunks: {}", e.toString());
        }
        if (tiles != null) tiles.flush();
    }

    public static void tick(MinecraftServer s) {
        if (s != server) return;
        tick++;
        if (tick % 20 == 0) syncWeb(s);
        if (!SimpleLivemap.on()) return;
        if (tick % LivemapConfig.intervalTicks == 0) {
            for (ServerPlayer p : s.getPlayerList().getPlayers()) record(p);
        }
        draw(s, LivemapConfig.chunksPerTick);
        if (tick % 100 == 0 && tiles != null) tiles.flush();
        if (tick % SAVE_TICKS == 0) save();
        if (tick % (LivemapConfig.liveSeconds * 20) == 0) live = buildLive(s);
    }

    // ---------------------------------------------------------------- recording

    /** One player: chunks around them (on a chunk change) and a held Vanilla map; never spectators. */
    public static void record(ServerPlayer p) {
        if (p.isSpectator()) return;
        String dim = p.level().dimension().identifier().toString();
        int cx = Mth.floor(p.getX()) >> 4, cz = Mth.floor(p.getZ()) >> 4;
        long here = TileStore.chunkKey(cx, cz) ^ dim.hashCode() * 0x9E3779B97F4A7C15L;
        Long before = lastChunk.put(p.getUUID(), here);
        if (before == null || before != here) recordAround(dim, p.getUUID(), cx, cz, LivemapConfig.radius);
        if (LivemapConfig.maps) {
            for (InteractionHand hand : InteractionHand.values()) recordMap(p, p.getItemInHand(hand));
        }
    }

    /** Marks the square of chunks around {@code (cx, cz)} and queues new or outdated ones for drawing. */
    public static int recordAround(String dim, UUID player, int cx, int cz, int radius) {
        int fresh = 0;
        Map<Long, Integer> times = drawn.computeIfAbsent(dim, k -> new HashMap<>());
        int stale = LivemapConfig.refreshMinutes * 1200;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = cx + dx, z = cz + dz;
                if (store.mark(dim, player, x, z)) fresh++;
                long key = TileStore.chunkKey(x, z);
                Integer at = times.get(key);
                if (at == null || tick - at >= stale) enqueue(dim, key);
            }
        }
        return fresh;
    }

    private static void enqueue(String dim, long key) {
        var q = queue.computeIfAbsent(dim, k -> new java.util.LinkedHashSet<>());
        if (q.size() < MAX_QUEUE) q.add(key);
    }

    /** A held filled map of the player's dimension: its drawn chunks count as explored (at most every 30 s per map). */
    public static int recordMap(ServerPlayer p, ItemStack stack) {
        if (!stack.is(Items.FILLED_MAP)) return 0;
        MapId id = stack.get(DataComponents.MAP_ID);
        if (id == null) return 0;
        Integer at = mapRead.get(id.id());
        if (at != null && tick - at < MAP_SECONDS * 20) return 0;
        mapRead.put(id.id(), tick);
        MapItemSavedData data = MapItem.getSavedData(stack, p.level());
        if (data == null || !data.dimension.equals(p.level().dimension())) return 0;
        return recordMapData(p.level().dimension().identifier().toString(), p.getUUID(), data);
    }

    /** Marks every chunk with a drawn pixel of {@code data} and hands the map picture to the tiles (empty pixels only). */
    public static int recordMapData(String dim, UUID player, MapItemSavedData data) {
        int scale = 1 << data.scale;
        int x0 = (Math.floorDiv(data.centerX, scale) - 64) * scale, z0 = (Math.floorDiv(data.centerZ, scale) - 64) * scale;
        Set<Long> chunks = new HashSet<>();
        for (int i = 0; i < 128 * 128; i++) {
            if (data.colors[i] == 0) continue;
            int bx = x0 + (i % 128) * scale, bz = z0 + (i / 128) * scale;
            for (int c = 0; c < Math.max(1, scale / 16); c++) {
                // A scaled pixel can cover several chunks (scale 32/64 blocks): mark each.
                for (int d = 0; d < Math.max(1, scale / 16); d++) chunks.add(TileStore.chunkKey((bx >> 4) + c, (bz >> 4) + d));
            }
        }
        int fresh = 0;
        for (long k : chunks) if (store.mark(dim, player, (int) (k >> 32), (int) k)) fresh++;
        if (tiles != null) tiles.putMap(TileStore.folder(dim), x0, z0, data.scale, data.colors, k -> store.explored(dim, (int) (k >> 32), (int) k));
        return fresh;
    }

    // ---------------------------------------------------------------- drawing

    /** Draws up to {@code budget} queued chunks that are loaded (others are dropped and drawn on the next visit). */
    public static int draw(MinecraftServer s, int budget) {
        if (tiles == null) return 0;
        int done = 0;
        for (ServerLevel level : s.getAllLevels()) {
            String dim = level.dimension().identifier().toString();
            var q = queue.get(dim);
            if (q == null) continue;
            var it = q.iterator();
            Map<Long, Integer> times = drawn.computeIfAbsent(dim, k -> new HashMap<>());
            while (done < budget && it.hasNext()) {
                long key = it.next();
                it.remove();
                int cx = (int) (key >> 32), cz = (int) key;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null || !store.explored(dim, cx, cz)) continue;
                tiles.putChunk(TileStore.folder(dim), cx, cz, ChunkSampler.sample(level, chunk, LivemapConfig.shading));
                if (times.size() >= MAX_SESSION) times.clear();
                times.put(key, tick);
                done++;
            }
            if (done >= budget) break;
        }
        return done;
    }

    /** Chunks waiting to be drawn (status). */
    public static int queued() {
        int n = 0;
        for (var q : queue.values()) n += q.size();
        return n;
    }

    // ---------------------------------------------------------------- web

    /**
     * {@code live.json}: the dimensions with explored chunks and, only with {@code web.showPlayers}, the online players that did
     * not hide themselves – name, dimension, x, z and nothing else (no UUID, no address, no health), never spectators.
     */
    public static String buildLive(MinecraftServer s) {
        JsonObject o = new JsonObject();
        JsonArray dims = new JsonArray();
        for (String d : store.dimensions()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", d);
            e.addProperty("folder", TileStore.folder(d));
            dims.add(e);
        }
        o.add("dimensions", dims);
        o.add("players", players(s));
        return o.toString();
    }

    public static JsonArray players(MinecraftServer s) {
        JsonArray out = new JsonArray();
        if (!LivemapConfig.showPlayers) return out;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.isSpectator() || store.hidden(p.getUUID())) continue;
            JsonObject e = new JsonObject();
            e.addProperty("name", p.getGameProfile().name());
            e.addProperty("dimension", p.level().dimension().identifier().toString());
            e.addProperty("x", Mth.floor(p.getX()));
            e.addProperty("z", Mth.floor(p.getZ()));
            out.add(e);
        }
        return out;
    }

    /** Whether the web server should run: module on and {@code web.enabled}. */
    public static boolean webWanted() {
        return SimpleLivemap.on() && LivemapConfig.webEnabled;
    }

    /** Starts, restarts (changed settings) or stops the web server to match the config; a failed bind retries after 60 s. */
    public static void syncWeb(MinecraftServer s) {
        if (!webWanted() || tiles == null) {
            if (web != null) {
                web.close();
                web = null;
            }
            return;
        }
        LivemapWeb.Settings wanted = new LivemapWeb.Settings(LivemapConfig.bind, LivemapConfig.port, LivemapConfig.isPublic() ? "" : LivemapConfig.token,
                LivemapConfig.isPublic(), LivemapConfig.requestsPerMinute, tiles.dir());
        if (web != null && web.running() && web.settings().equals(wanted)) return;
        if (web != null) {
            web.close();
            web = null;
        }
        if (tick < webRetryTick) return;
        try {
            live = buildLive(s);
            web = LivemapWeb.start(wanted, LivemapRuntime::live, s::isRunning);
            SimpleLivemap.LOG.info("Simple Livemap: web map on http://{}:{}/ ({})", wanted.bind(), web.port(), LivemapConfig.visibility);
        } catch (IOException | RuntimeException e) {
            webRetryTick = tick + 1200;
            SimpleLivemap.LOG.warn("Simple Livemap: web server could not start on {}:{}: {}", wanted.bind(), wanted.port(), e.toString());
        }
    }
}
