package com.simplemaps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * The explored area of one wayfinder map (owner F6: unlimited, stored optimised). Only explored 128x128-block tiles
 * exist; each keeps one Vanilla packed map colour and one height byte per block. Unknown = colour 0. Height byte =
 * (y - minY) / 2 + 1 (0 = unknown, e.g. taken over from a Vanilla map). The .dat file is gzip-compressed by Vanilla.
 */
public final class WayfinderData extends SavedData {
    public static final int TILE = 128, AREA = TILE * TILE;
    /** Zoom levels in blocks per map pixel (owner F9: 1, then 4, 8, 16, 64). */
    public static final int[] ZOOMS = {1, 4, 8, 16, 64};

    public static final class Tile {
        public final byte[] colors, heights;
        /** Changes whenever a pixel changes; not saved (clients only compare within one session). */
        public int version = 1;

        Tile(byte[] colors, byte[] heights) {
            this.colors = colors;
            this.heights = heights;
        }
    }

    private record TileEntry(int x, int z, byte[] colors, byte[] heights) {
        private static final Codec<byte[]> BYTES = Codec.BYTE_BUFFER.xmap(buffer -> {
            byte[] out = new byte[buffer.remaining()];
            buffer.duplicate().get(out);
            return out;
        }, ByteBuffer::wrap);
        static final Codec<TileEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(TileEntry::x),
                Codec.INT.fieldOf("z").forGetter(TileEntry::z),
                BYTES.fieldOf("colors").forGetter(TileEntry::colors),
                BYTES.fieldOf("heights").forGetter(TileEntry::heights)
        ).apply(i, TileEntry::new));
    }

    /** A structure the holder has seen or entered (Feature 3): registry id and centre of its bounding box. */
    public record Mark(String id, int x, int z) {
        public static final int MAX_ID = 128, MAX_PER_MAP = 256;
        public static final Codec<Mark> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.string(1, MAX_ID).fieldOf("id").forGetter(Mark::id),
                Codec.INT.fieldOf("x").forGetter(Mark::x),
                Codec.INT.fieldOf("z").forGetter(Mark::z)
        ).apply(i, Mark::new));
    }

    public static final Codec<WayfinderData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension").forGetter(d -> d.dimension),
            TileEntry.CODEC.listOf().fieldOf("tiles").forGetter(WayfinderData::entries),
            Mark.CODEC.listOf().optionalFieldOf("marks", List.of()).forGetter(d -> d.marks)
    ).apply(i, WayfinderData::new));

    private Optional<ResourceKey<Level>> dimension = Optional.empty();
    private final Map<Long, Tile> tiles = new HashMap<>();
    private final List<Mark> marks = new ArrayList<>();
    /** Changes whenever {@link #marks} change; not saved (only compared within one session). */
    private int marksVersion = 1;

    public WayfinderData() {}

    private WayfinderData(Optional<ResourceKey<Level>> dimension, List<TileEntry> entries, List<Mark> marks) {
        this.dimension = dimension;
        this.marks.addAll(marks);
        for (TileEntry e : entries) {
            if (e.colors().length == AREA && e.heights().length == AREA) tiles.put(key(e.x(), e.z()), new Tile(e.colors(), e.heights()));
        }
    }

    private List<TileEntry> entries() {
        List<TileEntry> out = new ArrayList<>(tiles.size());
        tiles.forEach((k, t) -> out.add(new TileEntry(tileX(k), tileZ(k), t.colors, t.heights)));
        return out;
    }

    public static SavedDataType<WayfinderData> type(int id) {
        return new SavedDataType<>(SimpleMaps.id("wayfinder_" + id), WayfinderData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    /** All wayfinder data lives in the overworld's storage, like Vanilla maps. */
    public static WayfinderData get(MinecraftServer server, int id) {
        return server.overworld().getDataStorage().computeIfAbsent(type(id));
    }

    public static long key(int tx, int tz) {
        return ((long) tx << 32) | (tz & 0xFFFFFFFFL);
    }

    public static int tileX(long key) {
        return (int) (key >> 32);
    }

    public static int tileZ(long key) {
        return (int) key;
    }

    public static int validZoom(int zoom) {
        for (int z : ZOOMS) if (z == zoom) return zoom;
        return 1;
    }

    public Optional<ResourceKey<Level>> dimension() {
        return dimension;
    }

    public void bind(ResourceKey<Level> level) {
        if (dimension.isEmpty()) {
            dimension = Optional.of(level);
            setDirty();
        }
    }

    public List<Mark> marks() {
        return java.util.Collections.unmodifiableList(marks);
    }

    public int marksVersion() {
        return marksVersion;
    }

    /** Remembers a discovered structure once (same id within 64 blocks counts as the same one). Returns whether new. */
    public boolean addMark(String id, int x, int z) {
        if (marks.size() >= Mark.MAX_PER_MAP || id.isEmpty() || id.length() > Mark.MAX_ID) return false;
        for (Mark m : marks) {
            if (m.id().equals(id) && Math.abs(m.x() - x) <= 64 && Math.abs(m.z() - z) <= 64) return false;
        }
        marks.add(new Mark(id, x, z));
        marksVersion++;
        setDirty();
        return true;
    }

    /** Owner F4: combining maps keeps the structures of both. */
    public void mergeMarks(WayfinderData other) {
        for (Mark m : other.marks) addMark(m.id(), m.x(), m.z());
    }

    public int tileCount() {
        return tiles.size();
    }

    public Tile tile(int tx, int tz) {
        return tiles.get(key(tx, tz));
    }

    /** Colour at a block (0 = unknown). */
    public int color(int x, int z) {
        Tile t = tiles.get(key(Math.floorDiv(x, TILE), Math.floorDiv(z, TILE)));
        return t == null ? 0 : t.colors[index(x, z)] & 0xFF;
    }

    public int height(int x, int z) {
        Tile t = tiles.get(key(Math.floorDiv(x, TILE), Math.floorDiv(z, TILE)));
        return t == null ? 0 : t.heights[index(x, z)] & 0xFF;
    }

    private static int index(int x, int z) {
        return Math.floorMod(z, TILE) * TILE + Math.floorMod(x, TILE);
    }

    /**
     * Writes one block; creates its tile unless the map already holds {@link MapsConfig#maxTilesPerMap} tiles.
     * {@code overwrite = false} only fills unknown pixels. Returns whether anything changed.
     */
    public boolean set(int x, int z, int color, int height, boolean overwrite) {
        if ((color & 0xFF) == 0) return false;
        long k = key(Math.floorDiv(x, TILE), Math.floorDiv(z, TILE));
        Tile t = tiles.get(k);
        if (t == null) {
            if (tiles.size() >= MapsConfig.maxTilesPerMap) return false;
            t = new Tile(new byte[AREA], new byte[AREA]);
            tiles.put(k, t);
        }
        int i = index(x, z);
        if (!overwrite && t.colors[i] != 0) return false;
        if (t.colors[i] == (byte) color && t.heights[i] == (byte) height) return false;
        t.colors[i] = (byte) color;
        t.heights[i] = (byte) height;
        t.version++;
        setDirty();
        return true;
    }

    /** Owner F4: two wayfinder maps → union; this map's pixels stay, the other fills the unknown ones. */
    public int mergeFrom(WayfinderData other) {
        int changed = 0;
        mergeMarks(other);
        for (Map.Entry<Long, Tile> e : other.tiles.entrySet()) {
            int bx = tileX(e.getKey()) * TILE, bz = tileZ(e.getKey()) * TILE;
            Tile t = e.getValue();
            for (int i = 0; i < AREA; i++) {
                if (t.colors[i] != 0 && set(bx + i % TILE, bz + i / TILE, t.colors[i] & 0xFF, t.heights[i] & 0xFF, false)) changed++;
            }
        }
        return changed;
    }

    /** Owner F4: a filled Vanilla map's area is taken over where this map is still unknown (no height information). */
    public int mergeFrom(MapItemSavedData vanilla) {
        int scale = 1 << vanilla.scale, changed = 0;
        for (int imgY = 0; imgY < 128; imgY++) {
            for (int imgX = 0; imgX < 128; imgX++) {
                int color = vanilla.colors[imgX + imgY * 128] & 0xFF;
                if (color == 0) continue;
                int minX = (vanilla.centerX / scale + imgX - 64) * scale, minZ = (vanilla.centerZ / scale + imgY - 64) * scale;
                for (int dz = 0; dz < scale; dz++) {
                    for (int dx = 0; dx < scale; dx++) {
                        if (set(minX + dx, minZ + dz, color, 0, false)) changed++;
                    }
                }
            }
        }
        return changed;
    }

    /** A tile of a zoom level, built by sampling the 1:1 tiles: colours, heights and a version (0 = nothing explored). */
    public record Rendered(byte[] colors, byte[] heights, int version) {}

    public Rendered render(int zoom, int tx, int tz) {
        zoom = validZoom(zoom);
        if (zoom == 1) {
            Tile t = tile(tx, tz);
            return t == null ? new Rendered(null, null, 0) : new Rendered(t.colors.clone(), t.heights.clone(), t.version);
        }
        int n = TILE / zoom, x0 = tx * zoom, z0 = tz * zoom;
        byte[] colors = null, heights = null;
        int version = 0;
        List<Map.Entry<Long, Tile>> covered = new ArrayList<>();
        if (tiles.size() < zoom * zoom) {
            for (Map.Entry<Long, Tile> e : tiles.entrySet()) {
                int ax = tileX(e.getKey()), az = tileZ(e.getKey());
                if (ax >= x0 && ax < x0 + zoom && az >= z0 && az < z0 + zoom) covered.add(e);
            }
        } else {
            for (int az = z0; az < z0 + zoom; az++) {
                for (int ax = x0; ax < x0 + zoom; ax++) {
                    Tile t = tile(ax, az);
                    if (t != null) covered.add(Map.entry(key(ax, az), t));
                }
            }
        }
        for (Map.Entry<Long, Tile> e : covered) {
            if (colors == null) {
                colors = new byte[AREA];
                heights = new byte[AREA];
            }
            int ax = tileX(e.getKey()), az = tileZ(e.getKey());
            Tile t = e.getValue();
            version = version * 31 + (int) (e.getKey() ^ (e.getKey() >>> 32)) * 17 + t.version;
            int ox = (ax - x0) * n, oz = (az - z0) * n, half = zoom / 2;
            for (int v = 0; v < n; v++) {
                for (int u = 0; u < n; u++) {
                    int src = (v * zoom + half) * TILE + u * zoom + half, dst = (oz + v) * TILE + ox + u;
                    colors[dst] = t.colors[src];
                    heights[dst] = t.heights[src];
                }
            }
        }
        if (colors != null && version == 0) version = 1;
        return new Rendered(colors, heights, version);
    }
}
