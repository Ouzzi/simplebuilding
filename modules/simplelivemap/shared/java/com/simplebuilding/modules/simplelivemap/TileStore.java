package com.simplebuilding.modules.simplelivemap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.imageio.ImageIO;

/**
 * The tile pictures on disk: {@code <dir>/<dimension folder>/<z>/<x>/<y>.png}, 128 x 128 pixels, zoom 4 = one block per pixel,
 * every lower zoom halves (zoom 0 = 16 blocks per pixel). Only pixels of explored chunks are ever drawn, the rest stays
 * transparent (no seed leak, MAP28-2). All file work runs on <b>one</b> background thread (no PNG work on the server thread):
 * the server thread hands over finished chunk pictures; the thread keeps a small cache of zoom-4 tiles, writes changed tiles
 * every {@link #flush} (temp file + atomic move, so the web server never reads half a file) and rebuilds their parents.
 * Next to each dimension folder a height layer {@code <folder>.h} (same pyramid, {@link ChunkSampler#heightPixel}) feeds the
 * viewer's shaded and cartographer styles (N34c); map-item pixels have no height there (transparent).
 */
public final class TileStore implements AutoCloseable {
    public static final int TILE = 128, MAX_ZOOM = 4, CACHE = 256, QUEUE = 4096;
    /** Suffix of the height layer folder; a real dimension folder never ends with it (see {@link #folder}). */
    public static final String HEIGHTS = ".h";
    /** 94 + {@link #HEIGHTS} = the 96 characters the web server and the hosted service accept. */
    private static final Pattern FOLDER = Pattern.compile("[a-z0-9_.-]{1,94}");

    private final Path dir;
    private final ThreadPoolExecutor worker;
    /** Worker thread only: zoom-4 tiles, least recently used first. */
    private final LinkedHashMap<String, int[]> cache = new LinkedHashMap<>(64, 0.75f, true);
    private final Set<String> dirty = new HashSet<>();
    /** Told about every written tile ({@code <folder>/<z>/<x>/<y>}, tile thread): the hosted upload (N34). */
    private volatile java.util.function.Consumer<String> listener;

    public TileStore(Path dir) {
        this.dir = dir.toAbsolutePath().normalize();
        this.worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE), r -> {
            Thread t = new Thread(r, "simplelivemap-tiles");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }, new ThreadPoolExecutor.DiscardPolicy());
    }

    public Path dir() {
        return dir;
    }

    /** Sets (or clears with null) the listener for written tiles. */
    public void listener(java.util.function.Consumer<String> l) {
        listener = l;
    }

    /** Folder name of a dimension id ({@code minecraft:overworld} -> {@code minecraft_overworld}). */
    public static String folder(String dimension) {
        String f = dimension.toLowerCase(java.util.Locale.ROOT).replace(':', '_').replace('/', '_');
        return FOLDER.matcher(f).matches() && !f.endsWith(HEIGHTS) ? f : "dimension_" + Integer.toHexString(dimension.hashCode());
    }

    public static String path(String folder, int z, int x, int y) {
        return folder + "/" + z + "/" + x + "/" + y + ".png";
    }

    /** Queues a chunk picture (256 ARGB, see {@link ChunkSampler#sample}); false when the queue is full (redrawn later). */
    public boolean putChunk(String folder, int cx, int cz, int[] pixels) {
        return putChunk(folder, cx, cz, pixels, null);
    }

    /** Like {@link #putChunk(String, int, int, int[])} plus the chunk's height pixels for the {@link #HEIGHTS} layer (may be null). */
    public boolean putChunk(String folder, int cx, int cz, int[] pixels, int[] heights) {
        int[] copy = pixels.clone(), h = heights == null ? null : heights.clone();
        return submit(() -> {
            paste(folder, cx, cz, copy);
            if (h != null) paste(folder + HEIGHTS, cx, cz, h);
        });
    }

    /** Worker thread: one 16 x 16 chunk picture into its zoom-4 tile. */
    private void paste(String folder, int cx, int cz, int[] chunk) {
        int[] tile = tile(folder, cx >> 3, cz >> 3);
        int ox = (cx & 7) * 16, oz = (cz & 7) * 16;
        for (int z = 0; z < 16; z++) System.arraycopy(chunk, z * 16, tile, (oz + z) * TILE + ox, 16);
        dirty.add(key(folder, cx >> 3, cz >> 3));
    }

    /**
     * Queues the drawn area of a Vanilla map: {@code colors} 128 x 128 packed map pixels, one pixel per {@code 2^scale} blocks
     * from block {@code (x0, z0)}. Only pixels inside {@code explored} chunks that are still empty are filled (a real chunk
     * picture always wins).
     */
    public boolean putMap(String folder, int x0, int z0, int scale, byte[] colors, java.util.function.LongPredicate explored) {
        byte[] copy = colors.clone();
        int step = 1 << scale;
        // Which chunks may be filled is decided now (the visited store is not thread safe); bit set per map pixel.
        boolean[] allowed = new boolean[128 * 128];
        for (int i = 0; i < allowed.length; i++) {
            int bx = x0 + (i % 128) * step, bz = z0 + (i / 128) * step;
            allowed[i] = copy[i] != 0 && explored.test(chunkKey(bx >> 4, bz >> 4));
        }
        return submit(() -> {
            for (int i = 0; i < copy.length; i++) {
                if (!allowed[i]) continue;
                int argb = ChunkSampler.argbPacked(copy[i]);
                if (argb == 0) continue;
                int bx = x0 + (i % 128) * step, bz = z0 + (i / 128) * step;
                for (int dz = 0; dz < step; dz++) {
                    for (int dx = 0; dx < step; dx++) {
                        int x = bx + dx, z = bz + dz;
                        int[] tile = tile(folder, Math.floorDiv(x, TILE), Math.floorDiv(z, TILE));
                        int p = Math.floorMod(z, TILE) * TILE + Math.floorMod(x, TILE);
                        if (tile[p] == 0) {
                            tile[p] = argb;
                            dirty.add(key(folder, Math.floorDiv(x, TILE), Math.floorDiv(z, TILE)));
                        }
                    }
                }
            }
        });
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Writes every changed tile and its parents; the returned future completes when that is done. */
    public Future<?> flush() {
        try {
            return worker.submit(this::writeDirty);
        } catch (RuntimeException e) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
    }

    /** Flushes and waits (server stop, tests). */
    public void flushNow() {
        try {
            flush().get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            SimpleLivemap.LOG.warn("Simple Livemap: tiles not written: {}", e.toString());
        }
    }

    @Override
    public void close() {
        flushNow();
        worker.shutdown();
        try {
            worker.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean submit(Runnable job) {
        if (worker.isShutdown() || worker.getQueue().remainingCapacity() == 0) return false;
        worker.execute(() -> {
            try {
                job.run();
            } catch (RuntimeException e) {
                SimpleLivemap.LOG.warn("Simple Livemap: tile job failed: {}", e.toString());
            }
        });
        return true;
    }

    private static String key(String folder, int tx, int tz) {
        return folder + "/" + tx + "/" + tz;
    }

    /** Worker thread: the zoom-4 tile from the cache or disk (writes evicted dirty tiles first). */
    private int[] tile(String folder, int tx, int tz) {
        String k = key(folder, tx, tz);
        int[] t = cache.get(k);
        if (t != null) return t;
        t = read(dir.resolve(path(folder, MAX_ZOOM, tx, tz)));
        if (t == null) t = new int[TILE * TILE];
        cache.put(k, t);
        if (cache.size() > CACHE) {
            // Write changed tiles first (without trimming, so the tile handed out here stays cached), then drop the eldest.
            if (dirty.contains(cache.keySet().iterator().next())) writeChanged();
            var it = cache.keySet().iterator();
            while (cache.size() > CACHE && it.hasNext()) {
                String eldest = it.next();
                if (!eldest.equals(k) && !dirty.contains(eldest)) it.remove();
            }
        }
        return t;
    }

    private void writeDirty() {
        writeChanged();
        trim();
    }

    private void writeChanged() {
        if (dirty.isEmpty()) return;
        Map<String, TreeSet<Long>> parents = new LinkedHashMap<>();
        for (String k : dirty) {
            int a = k.indexOf('/'), b = k.lastIndexOf('/');
            String folder = k.substring(0, a);
            int tx = Integer.parseInt(k.substring(a + 1, b)), tz = Integer.parseInt(k.substring(b + 1));
            int[] t = cache.get(k);
            if (t == null) continue;
            write(folder, MAX_ZOOM, tx, tz, t);
            parents.computeIfAbsent(folder, f -> new TreeSet<>()).add(chunkKey(Math.floorDiv(tx, 2), Math.floorDiv(tz, 2)));
        }
        dirty.clear();
        for (Map.Entry<String, TreeSet<Long>> e : parents.entrySet()) {
            TreeSet<Long> level = e.getValue();
            for (int z = MAX_ZOOM - 1; z >= 0; z--) {
                TreeSet<Long> up = new TreeSet<>();
                for (long p : level) {
                    int px = (int) (p >> 32), pz = (int) p;
                    compose(e.getKey(), z, px, pz);
                    up.add(chunkKey(Math.floorDiv(px, 2), Math.floorDiv(pz, 2)));
                }
                level = up;
            }
        }
    }

    /** Drops the least recently used clean tiles down to {@link #CACHE} (between jobs only). */
    private void trim() {
        var it = cache.keySet().iterator();
        while (cache.size() > CACHE && it.hasNext()) if (!dirty.contains(it.next())) it.remove();
    }

    /** A zoom-{@code z} tile from its four children (first opaque pixel of each 2 x 2, so small explored spots stay). */
    private void compose(String folder, int z, int x, int y) {
        int[] out = new int[TILE * TILE];
        int half = TILE / 2;
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                int[] child = z + 1 == MAX_ZOOM ? cache.get(key(folder, x * 2 + dx, y * 2 + dy)) : null;
                if (child == null) child = read(dir.resolve(path(folder, z + 1, x * 2 + dx, y * 2 + dy)));
                if (child == null) continue;
                for (int v = 0; v < half; v++) {
                    for (int u = 0; u < half; u++) {
                        int c = 0;
                        for (int i = 0; i < 4 && (c >>> 24) == 0; i++) c = child[(v * 2 + (i >> 1)) * TILE + u * 2 + (i & 1)];
                        out[(dy * half + v) * TILE + dx * half + u] = c;
                    }
                }
            }
        }
        write(folder, z, x, y, out);
    }

    private static int[] read(Path file) {
        if (!Files.isRegularFile(file)) return null;
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null || image.getWidth() != TILE || image.getHeight() != TILE) return null;
            return image.getRGB(0, 0, TILE, TILE, null, 0, TILE);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Reads a written tile (tests, status); null when there is none. */
    public int[] readTile(String folder, int z, int x, int y) {
        return read(dir.resolve(path(folder, z, x, y)));
    }

    /**
     * A height tile as PNG (RGBA, 8 bit) with the "Up" filter on every row: heights change little from row to row, so this is
     * about 40 % smaller than ImageIO's writer for this data (measured N34c); colour tiles keep ImageIO, which is better there.
     */
    static byte[] pngUp(int[] argb) throws IOException {
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (DeflaterOutputStream z = new DeflaterOutputStream(idat, deflater)) {
            byte[] row = new byte[1 + TILE * 4], prev = new byte[TILE * 4], cur = new byte[TILE * 4];
            row[0] = 2;
            for (int y = 0; y < TILE; y++) {
                for (int x = 0; x < TILE; x++) {
                    int c = argb[y * TILE + x], i = x * 4;
                    cur[i] = (byte) (c >> 16);
                    cur[i + 1] = (byte) (c >> 8);
                    cur[i + 2] = (byte) c;
                    cur[i + 3] = (byte) (c >>> 24);
                }
                for (int i = 0; i < cur.length; i++) row[i + 1] = (byte) (cur[i] - prev[i]);
                z.write(row);
                byte[] t = prev;
                prev = cur;
                cur = t;
            }
        } finally {
            deflater.end();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(TILE).putInt(TILE).put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0).array());
        chunk(out, "IDAT", idat.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(t);
        out.writeBytes(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private void write(String folder, int z, int x, int y, int[] argb) {
        Path file = dir.resolve(path(folder, z, x, y));
        try {
            boolean empty = true;
            for (int c : argb) {
                if ((c >>> 24) != 0) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                Files.deleteIfExists(file);
                return;
            }
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            if (folder.endsWith(HEIGHTS)) {
                Files.write(tmp, pngUp(argb));
            } else {
                BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, TILE, TILE, argb, 0, TILE);
                if (!ImageIO.write(image, "png", tmp.toFile())) throw new IOException("no PNG writer");
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            var l = listener;
            if (l != null) l.accept(folder + "/" + z + "/" + x + "/" + y);
        } catch (IOException | RuntimeException e) {
            SimpleLivemap.LOG.warn("Simple Livemap: could not write tile {}: {}", file, e.toString());
        }
    }
}
