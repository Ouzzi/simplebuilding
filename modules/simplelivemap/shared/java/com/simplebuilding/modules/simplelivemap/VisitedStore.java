package com.simplebuilding.modules.simplelivemap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Which chunks every player really explored, per dimension (MAP28-2 variant B): one bit per chunk in regions of 32 x 32 chunks
 * (16 longs = 128 bytes per region and player). The map shows the union of all players. Also holds the players that hid
 * themselves from the web map. Server thread only. File {@code <world>/simplelivemap/visited.bin}: gzip, magic, version.
 */
public final class VisitedStore {
    private static final int MAGIC = 0x534C4D56, VERSION = 1; // "SLMV"
    public static final int MAX_DIMENSIONS = 256, MAX_PLAYERS = 1 << 16, MAX_REGIONS = 1 << 20;

    private static final class Dim {
        final Map<UUID, Map<Long, long[]>> players = new HashMap<>();
        final Map<Long, long[]> union = new HashMap<>();
    }

    private final Map<String, Dim> dims = new TreeMap<>();
    private final Set<UUID> hidden = new LinkedHashSet<>();
    private boolean dirty;

    public static long region(int rx, int rz) {
        return ((long) rx << 32) | (rz & 0xFFFFFFFFL);
    }

    private static int bit(int cx, int cz) {
        return (cz & 31) * 32 + (cx & 31);
    }

    private static boolean get(Map<Long, long[]> m, int cx, int cz) {
        long[] r = m.get(region(cx >> 5, cz >> 5));
        int b = bit(cx, cz);
        return r != null && (r[b >>> 6] >>> (b & 63) & 1L) != 0;
    }

    /** Sets the bit; true when it was not set before. */
    private static boolean set(Map<Long, long[]> m, int cx, int cz) {
        long[] r = m.computeIfAbsent(region(cx >> 5, cz >> 5), k -> new long[16]);
        int b = bit(cx, cz);
        long mask = 1L << (b & 63);
        if ((r[b >>> 6] & mask) != 0) return false;
        r[b >>> 6] |= mask;
        return true;
    }

    /** Marks a chunk as explored by {@code player}; true when nobody had explored it yet (the map grows). */
    public boolean mark(String dimension, UUID player, int cx, int cz) {
        Dim d = dims.computeIfAbsent(dimension, k -> new Dim());
        if (set(d.players.computeIfAbsent(player, k -> new HashMap<>()), cx, cz)) dirty = true;
        boolean fresh = set(d.union, cx, cz);
        if (fresh) dirty = true;
        return fresh;
    }

    public boolean explored(String dimension, int cx, int cz) {
        Dim d = dims.get(dimension);
        return d != null && get(d.union, cx, cz);
    }

    public boolean exploredBy(String dimension, UUID player, int cx, int cz) {
        Dim d = dims.get(dimension);
        Map<Long, long[]> m = d == null ? null : d.players.get(player);
        return m != null && get(m, cx, cz);
    }

    /** Explored chunks of a dimension (all players together). */
    public long count(String dimension) {
        Dim d = dims.get(dimension);
        if (d == null) return 0;
        long n = 0;
        for (long[] r : d.union.values()) for (long l : r) n += Long.bitCount(l);
        return n;
    }

    public Set<String> dimensions() {
        return dims.keySet();
    }

    public boolean hidden(UUID player) {
        return hidden.contains(player);
    }

    public void setHidden(UUID player, boolean hide) {
        if (hide ? hidden.add(player) : hidden.remove(player)) dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    // ---------------------------------------------------------------- file

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream raw = Files.newOutputStream(tmp); DataOutputStream out = new DataOutputStream(new GZIPOutputStream(raw))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(hidden.size());
            for (UUID u : hidden) {
                out.writeLong(u.getMostSignificantBits());
                out.writeLong(u.getLeastSignificantBits());
            }
            out.writeInt(dims.size());
            for (Map.Entry<String, Dim> e : dims.entrySet()) {
                out.writeUTF(e.getKey());
                out.writeInt(e.getValue().players.size());
                for (Map.Entry<UUID, Map<Long, long[]>> p : e.getValue().players.entrySet()) {
                    out.writeLong(p.getKey().getMostSignificantBits());
                    out.writeLong(p.getKey().getLeastSignificantBits());
                    out.writeInt(p.getValue().size());
                    for (Map.Entry<Long, long[]> r : p.getValue().entrySet()) {
                        out.writeLong(r.getKey());
                        for (long l : r.getValue()) out.writeLong(l);
                    }
                }
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        dirty = false;
    }

    /** Reads a saved store; a missing file gives an empty store, a broken one throws (the caller keeps the file aside). */
    public static VisitedStore load(Path file) throws IOException {
        VisitedStore s = new VisitedStore();
        if (!Files.isRegularFile(file)) return s;
        try (InputStream raw = Files.newInputStream(file); DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
            if (in.readInt() != MAGIC) throw new IOException("not a Simple Livemap file");
            int version = in.readInt();
            if (version != VERSION) throw new IOException("unknown version " + version);
            int hiddenCount = bounded(in.readInt(), MAX_PLAYERS);
            for (int i = 0; i < hiddenCount; i++) s.hidden.add(new UUID(in.readLong(), in.readLong()));
            int dimCount = bounded(in.readInt(), MAX_DIMENSIONS);
            for (int i = 0; i < dimCount; i++) {
                Dim d = new Dim();
                s.dims.put(in.readUTF(), d);
                int players = bounded(in.readInt(), MAX_PLAYERS);
                for (int p = 0; p < players; p++) {
                    UUID id = new UUID(in.readLong(), in.readLong());
                    Map<Long, long[]> regions = new HashMap<>();
                    int count = bounded(in.readInt(), MAX_REGIONS);
                    for (int r = 0; r < count; r++) {
                        long key = in.readLong();
                        long[] bits = new long[16];
                        for (int b = 0; b < 16; b++) bits[b] = in.readLong();
                        regions.put(key, bits);
                        long[] u = d.union.computeIfAbsent(key, k -> new long[16]);
                        for (int b = 0; b < 16; b++) u[b] |= bits[b];
                    }
                    d.players.put(id, regions);
                }
            }
        }
        return s;
    }

    private static int bounded(int n, int max) throws IOException {
        if (n < 0 || n > max) throw new IOException("bad count " + n);
        return n;
    }

    /** Players with any explored chunk (tests, status). */
    public Set<UUID> players() {
        Set<UUID> out = new HashSet<>();
        for (Dim d : dims.values()) out.addAll(d.players.keySet());
        return out;
    }
}
