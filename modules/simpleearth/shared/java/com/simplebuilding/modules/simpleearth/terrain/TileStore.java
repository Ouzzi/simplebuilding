package com.simplebuilding.modules.simpleearth.terrain;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Cache of encoded tiles for one dimension (concept §4.3): an LRU in memory and files
 * {@code <world>/data/simpleearth/<namespace>/<path>/<e>/<tx>_<tz>.bin} on disk, the oldest files deleted beyond the size
 * limit. The grid is global, so every player shares it. {@code dir == null}: memory only (tests, explored-only mode).
 */
public final class TileStore {
    private final Path dir;
    private final int maxTiles;
    private final long maxBytes;
    private final Map<Long, byte[]> memory;
    private long diskBytes = -1;

    public TileStore(Path dir, int maxTiles, long maxBytes) {
        this.dir = dir;
        this.maxTiles = Math.max(4, maxTiles);
        this.maxBytes = maxBytes;
        this.memory = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
                return size() > TileStore.this.maxTiles;
            }
        };
    }

    public synchronized byte[] memory(int e, int tx, int tz) {
        return memory.get(TileMath.key(e, tx, tz));
    }

    /** Memory first, then disk (the disk hit is kept in memory). Null when not cached. */
    public byte[] get(int e, int tx, int tz) {
        byte[] hit = memory(e, tx, tz);
        if (hit != null || dir == null) return hit;
        Path file = file(e, tx, tz);
        try {
            if (!Files.isRegularFile(file)) return null;
            byte[] data = Files.readAllBytes(file);
            synchronized (this) {
                memory.put(TileMath.key(e, tx, tz), data);
            }
            return data;
        } catch (IOException ex) {
            return null;
        }
    }

    public void put(int e, int tx, int tz, byte[] data) {
        synchronized (this) {
            memory.put(TileMath.key(e, tx, tz), data);
        }
        if (dir == null || maxBytes <= 0) return;
        Path file = file(e, tx, tz);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, data);
            synchronized (this) {
                if (diskBytes >= 0) diskBytes += data.length;
            }
            prune();
        } catch (IOException ex) {
            SimpleEarth.LOG.debug("Could not cache earth tile {}: {}", file, ex.toString());
        }
    }

    public synchronized int memorySize() {
        return memory.size();
    }

    Path file(int e, int tx, int tz) {
        return dir.resolve(Integer.toString(e)).resolve(tx + "_" + tz + ".bin");
    }

    /** Deletes the oldest files once the folder is over its limit (counted once, then kept up to date). */
    private void prune() {
        long size;
        synchronized (this) {
            if (diskBytes < 0) diskBytes = folderSize();
            size = diskBytes;
        }
        if (size <= maxBytes) return;
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException ex) {
            return;
        }
        files.sort(Comparator.comparingLong(TileStore::modified));
        long target = maxBytes * 9 / 10;
        for (Path f : files) {
            if (size <= target) break;
            try {
                long len = Files.size(f);
                Files.deleteIfExists(f);
                size -= len;
            } catch (IOException ignored) {
            }
        }
        synchronized (this) {
            diskBytes = size;
        }
    }

    private long folderSize() {
        if (!Files.isDirectory(dir)) return 0;
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).mapToLong(f -> {
                try {
                    return Files.size(f);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private static long modified(Path f) {
        try {
            return Files.getLastModifiedTime(f).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }
}
