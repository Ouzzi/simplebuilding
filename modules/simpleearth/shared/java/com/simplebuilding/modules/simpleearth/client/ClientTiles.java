package com.simplebuilding.modules.simpleearth.client;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.net.EarthTilePayload;
import com.simplebuilding.modules.simpleearth.net.TileRequestPayload;
import com.simplebuilding.modules.simpleearth.terrain.TileCodec;
import com.simplebuilding.modules.simpleearth.terrain.TileMath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Terrain tiles the client got from the server (map colour ids + heights on the global grid), an LRU, and the request queue:
 * the renderer says which tiles it wants this frame, nearest first; each tick a batch goes out, a capped number stays in
 * flight. {@link #version} counts arrivals so face images know when to repaint.
 */
public final class ClientTiles {
    public static final int MAX_TILES = 2048, MAX_IN_FLIGHT = 96;
    public record Tile(byte[] colors, short[] heights) {}

    private static final Map<Long, Tile> TILES = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Tile> eldest) {
            return size() > MAX_TILES;
        }
    };
    private static final Set<Long> IN_FLIGHT = new HashSet<>();
    private static final LinkedHashSet<TileRequestPayload.Key> WANTED = new LinkedHashSet<>();
    private static long inFlightSince;
    public static int version;

    private ClientTiles() {}

    public static Tile get(int e, int tx, int tz) {
        return TILES.get(TileMath.key(e, tx, tz));
    }

    /** Asks for a tile unless it is here or on its way. */
    public static void want(int e, int tx, int tz) {
        long key = TileMath.key(e, tx, tz);
        if (TILES.containsKey(key) || IN_FLIGHT.contains(key) || WANTED.size() > 1024) return;
        WANTED.add(new TileRequestPayload.Key(e, tx, tz));
    }

    public static void receive(EarthTilePayload p) {
        byte[] colors = new byte[TileMath.AREA];
        short[] heights = new short[TileMath.AREA];
        long key = TileMath.key(p.e(), p.tx(), p.tz());
        IN_FLIGHT.remove(key);
        if (!TileCodec.decode(p.data(), colors, heights)) return;
        TILES.put(key, new Tile(colors, heights));
        version++;
    }

    /** Once per client tick: send the wanted tiles (first come = nearest, the renderer asks in that order). */
    public static void flush() {
        long now = System.currentTimeMillis();
        if (!IN_FLIGHT.isEmpty() && now - inFlightSince > 20_000) IN_FLIGHT.clear(); // lost answers (dimension change, server limit)
        if (WANTED.isEmpty() || IN_FLIGHT.size() >= MAX_IN_FLIGHT) {
            WANTED.clear();
            return;
        }
        List<TileRequestPayload.Key> batch = new ArrayList<>();
        for (TileRequestPayload.Key k : WANTED) {
            if (batch.size() >= TileRequestPayload.MAX || IN_FLIGHT.size() + batch.size() >= MAX_IN_FLIGHT) break;
            batch.add(k);
        }
        WANTED.clear();
        if (batch.isEmpty()) return;
        if (IN_FLIGHT.isEmpty()) inFlightSince = now;
        for (TileRequestPayload.Key k : batch) IN_FLIGHT.add(TileMath.key(k.e(), k.tx(), k.tz()));
        SimpleEarth.toServer.accept(new TileRequestPayload(batch));
    }

    public static void clear() {
        TILES.clear();
        IN_FLIGHT.clear();
        WANTED.clear();
        version++;
    }

    public static int size() {
        return TILES.size();
    }
}
