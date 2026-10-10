package com.simplemaps.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import com.simplemaps.net.MapStatePayload;
import com.simplemaps.net.TileCodec;
import com.simplemaps.net.TileDataPayload;
import com.simplemaps.net.TileRequestPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.level.material.MapColor;

/**
 * Client copy of the tiles the player looks at (map screen, map in hand, framed maps). Tiles are asked for with
 * their known version; the server only answers changed ones. Textures are built lazily on the render thread and
 * the oldest are dropped beyond {@link #MAX_ENTRIES}.
 */
public final class TileCache {
    public static final int MAX_ENTRIES = 768, FLUSH_TICKS = 5, REFRESH_TICKS = 40, RETRY_TICKS = 20;

    public record Key(int mapId, int zoom, int tx, int tz) {}

    public static final class Entry {
        /** -1 = never answered. */
        int version = -1;
        byte[] colors, heights;
        DynamicTexture texture, contour;
        boolean dirty, contourDirty;
        long asked = Long.MIN_VALUE / 2;

        public boolean known() {
            return version >= 0;
        }
    }

    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(256, 0.75f, true);
    private final Set<Key> pending = new LinkedHashSet<>();
    private final Map<Integer, String> dimensions = new HashMap<>();
    /** Discovered structures per map; kept across dimension changes (the server only resends them when they change). */
    private final Map<Integer, java.util.List<WayfinderData.Mark>> marks = new HashMap<>();
    private long ticks;

    public void tick() {
        ticks++;
        if (ticks % FLUSH_TICKS != 0 || pending.isEmpty()) return;
        List<TileRequestPayload.Entry> out = new ArrayList<>();
        Iterator<Key> it = pending.iterator();
        while (it.hasNext() && out.size() < TileRequestPayload.MAX) {
            Key k = it.next();
            it.remove();
            Entry e = entries.get(k);
            out.add(new TileRequestPayload.Entry(k.mapId(), k.zoom(), k.tx(), k.tz(), e == null ? -1 : e.version));
        }
        SimpleMaps.toServer.accept(new TileRequestPayload(out));
    }

    /** Marks a tile as needed now; it is (re-)requested when unknown or older than {@link #REFRESH_TICKS}. */
    public Entry want(Key key) {
        Entry e = entries.get(key);
        if (e == null) {
            e = new Entry();
            entries.put(key, e);
            trim();
        }
        if (ticks - e.asked >= (e.known() ? REFRESH_TICKS : RETRY_TICKS)) {
            e.asked = ticks;
            pending.add(key);
        }
        return e;
    }

    public Entry peek(Key key) {
        return entries.get(key);
    }

    public void receive(TileDataPayload p) {
        Entry e = entries.computeIfAbsent(new Key(p.mapId(), p.zoom(), p.tx(), p.tz()), k -> new Entry());
        byte[] raw = TileCodec.unpack(p.data());
        e.version = p.version();
        if (raw == null) {
            e.colors = null;
            e.heights = null;
        } else {
            e.colors = new byte[WayfinderData.AREA];
            e.heights = new byte[WayfinderData.AREA];
            System.arraycopy(raw, 0, e.colors, 0, WayfinderData.AREA);
            System.arraycopy(raw, WayfinderData.AREA, e.heights, 0, WayfinderData.AREA);
        }
        e.dirty = true;
        e.contourDirty = true;
        trim();
    }

    public void receive(MapStatePayload p) {
        dimensions.put(p.mapId(), p.dimension());
        if (p.sendMarks()) marks.put(p.mapId(), p.marks());
    }

    public java.util.List<WayfinderData.Mark> marks(int mapId) {
        return marks.getOrDefault(mapId, java.util.List.of());
    }

    /** The dimension the map is bound to ("" = unbound), or null while unknown. */
    public String dimension(int mapId) {
        return dimensions.get(mapId);
    }

    /** Packed map colour at a pixel of a tile (0 = unknown). */
    public static int color(Entry e, int px, int py) {
        return e == null || e.colors == null ? 0 : e.colors[py * WayfinderData.TILE + px] & 0xFF;
    }

    /** Colour texture of the tile, or null when nothing is explored there. Render thread only. */
    public DynamicTexture texture(Entry e) {
        if (e == null || e.colors == null) return null;
        if (e.texture == null) {
            e.texture = new DynamicTexture(() -> "simplemaps tile", WayfinderData.TILE, WayfinderData.TILE, true);
            e.dirty = true;
        }
        if (e.dirty) {
            NativeImage pixels = e.texture.getPixels();
            for (int i = 0; i < WayfinderData.AREA; i++) {
                int c = e.colors[i] & 0xFF;
                pixels.setPixel(i % WayfinderData.TILE, i / WayfinderData.TILE, c == 0 ? 0 : MapColor.getColorFromPackedId(c));
            }
            e.texture.upload();
            e.dirty = false;
        }
        return e.texture;
    }

    /** Height-line texture of the tile (Feature 5), or null when nothing is explored there. Render thread only. */
    public DynamicTexture contour(Entry e) {
        if (e == null || e.heights == null) return null;
        if (e.contour == null) {
            e.contour = new DynamicTexture(() -> "simplemaps contour", WayfinderData.TILE, WayfinderData.TILE, true);
            e.contourDirty = true;
        }
        if (e.contourDirty) {
            NativeImage pixels = e.contour.getPixels();
            for (int y = 0; y < WayfinderData.TILE; y++) {
                for (int x = 0; x < WayfinderData.TILE; x++) {
                    pixels.setPixel(x, y, Contours.color(e.colors, e.heights, x, y));
                }
            }
            e.contour.upload();
            e.contourDirty = false;
        }
        return e.contour;
    }

    private void trim() {
        Iterator<Map.Entry<Key, Entry>> it = entries.entrySet().iterator();
        while (entries.size() > MAX_ENTRIES && it.hasNext()) {
            Entry e = it.next().getValue();
            close(e);
            it.remove();
        }
    }

    private static void close(Entry e) {
        if (e.texture != null) e.texture.close();
        if (e.contour != null) e.contour.close();
        e.texture = null;
        e.contour = null;
    }

    public void clear() {
        entries.values().forEach(TileCache::close);
        entries.clear();
        pending.clear();
        dimensions.clear();
    }
}
