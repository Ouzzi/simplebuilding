package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.simplebuilding.modules.simpletweaks.admin.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.storage.LevelResource;

/**
 * T10 block and container log. Server thread: {@link #record} only appends to a queue. One IO thread drains the queue
 * every {@code flushSeconds} into append-only day files, and answers lookups after draining (so they see everything).
 * Layout {@code <world>/simpletweaks/blocklog/<dimension>/<rx>_<rz>/<yyyy-MM-dd>.tsv}: the region folder (512×512 blocks)
 * is the position index, the day file the time index; retention deletes whole day files.
 */
public final class BlockLog {
    /** One action. {@code action}: B broke, P placed, T took from a container, D deposited into a container. */
    public record Entry(long time, char action, UUID player, String name, String dimension, int x, int y, int z, String id, int count, String extra) {
        public BlockPos pos() { return new BlockPos(x, y, z); }
        String line() {
            return time + "\t" + action + "\t" + player + "\t" + clean(name) + "\t" + x + "\t" + y + "\t" + z + "\t" + clean(id) + "\t" + count + "\t" + escape(extra);
        }
        static Entry parse(String dimension, String line) {
            String[] f = line.split("\t", -1);
            if (f.length < 10 || f[1].length() != 1) return null;
            try {
                return new Entry(Long.parseLong(f[0]), f[1].charAt(0), UUID.fromString(f[2]), f[3], dimension, Integer.parseInt(f[4]), Integer.parseInt(f[5]),
                        Integer.parseInt(f[6]), f[7], Integer.parseInt(f[8]), unescape(f[9]));
            } catch (IllegalArgumentException e) { return null; }
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-blocklog");
    private static final ConcurrentLinkedQueue<Entry> QUEUE = new ConcurrentLinkedQueue<>();
    private static ExecutorService io;
    private static Path root;
    private static long ticks;
    private static LocalDate lastPurge;
    /** Server thread: the server's zone for day files (tests may fix the clock). */
    static Clock clock = Clock.systemDefaultZone();

    public static boolean active() { return Admin.on() && root != null && Admin.config().blockLog().enabled(); }
    public static Path root() { return root; }

    public static void start(MinecraftServer server) {
        stop();
        if (!Admin.config().blockLog().enabled()) return;
        root = server.getWorldPath(LevelResource.ROOT).resolve("simpletweaks").resolve("blocklog");
        io = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r, "simpletweaks-blocklog"); t.setDaemon(true); return t; });
        lastPurge = null;
        io.submit(BlockLog::purge);
    }
    public static void stop() {
        if (io != null) {
            io.submit(BlockLog::drain);
            io.shutdown();
            try { if (!io.awaitTermination(10, TimeUnit.SECONDS)) LOG.warn("Block log writer did not finish in 10 s"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        io = null; root = null; QUEUE.clear();
    }
    public static void tick(MinecraftServer server) {
        if (io == null) return;
        if (++ticks % (Admin.config().blockLog().flushSeconds() * 20L) == 0) io.submit(BlockLog::drain);
        if (ticks % 72000 == 0) io.submit(BlockLog::purge);
    }

    public static void record(ServerLevel level, ServerPlayer player, char action, BlockPos pos, String id, int count, String extra) {
        if (!active() || player == null) return;
        QUEUE.add(new Entry(clock.millis(), action, player.getUUID(), player.getGameProfile().name(), level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), id, count, extra == null ? "" : extra));
    }

    /** Tests and shutdown: everything queued so far is on disk when this returns. */
    public static void flushNow() {
        if (io == null) return;
        try { io.submit(BlockLog::drain).get(30, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static void drain() {
        if (root == null) return;
        var byFile = new LinkedHashMap<Path, StringBuilder>();
        for (Entry e; (e = QUEUE.poll()) != null; ) {
            var day = Instant.ofEpochMilli(e.time()).atZone(clock.getZone()).toLocalDate();
            byFile.computeIfAbsent(regionDir(e.dimension(), e.x() >> 9, e.z() >> 9).resolve(day + ".tsv"), k -> new StringBuilder()).append(e.line()).append('\n');
        }
        for (var f : byFile.entrySet()) {
            try {
                Files.createDirectories(f.getKey().getParent());
                Files.writeString(f.getKey(), f.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) { LOG.warn("Could not append to {}", f.getKey(), e); }
        }
    }
    static Path regionDir(String dimension, int rx, int rz) { return root.resolve(AdminFiles.folder(dimension)).resolve(rx + "_" + rz); }

    /** Deletes day files older than the retention (runs on the IO thread at start and every hour). */
    static void purge() {
        if (root == null || !Files.isDirectory(root)) return;
        var today = LocalDate.now(clock);
        if (today.equals(lastPurge)) return;
        lastPurge = today;
        var oldest = today.minusDays(Admin.config().blockLog().retentionDays() - 1L);
        try (var walk = Files.walk(root, 3)) {
            for (var p : walk.filter(p -> p.getFileName().toString().endsWith(".tsv")).toList()) {
                var day = dayOf(p);
                if (day != null && day.isBefore(oldest)) Files.deleteIfExists(p);
            }
        } catch (IOException e) { LOG.warn("Could not purge the block log", e); }
    }
    /** Tests: run the retention now. */
    public static void purgeNow() {
        lastPurge = null;
        try { io.submit(BlockLog::purge).get(30, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static LocalDate dayOf(Path file) {
        String n = file.getFileName().toString();
        try { return LocalDate.parse(n.substring(0, n.length() - 4)); } catch (Exception e) { return null; }
    }

    /** History of one block, newest first (IO thread; complete with everything recorded before the call). */
    public static CompletableFuture<List<Entry>> at(ServerLevel level, BlockPos pos) {
        String dim = level.dimension().identifier().toString();
        return CompletableFuture.supplyAsync(() -> {
            drain();
            var out = new ArrayList<Entry>();
            read(regionDir(dim, pos.getX() >> 9, pos.getZ() >> 9), dim, e -> e.x() == pos.getX() && e.y() == pos.getY() && e.z() == pos.getZ(), out);
            out.sort(Comparator.comparingLong(Entry::time).reversed());
            return out;
        }, io);
    }

    /**
     * Actions of one player since {@code since} (ms), newest first. {@code center}/{@code radius} limit to a horizontal
     * square in {@code levelFilter}; without them every dimension is searched. At most {@code limit} entries.
     */
    public static CompletableFuture<List<Entry>> byPlayer(MinecraftServer server, UUID player, long since, ServerLevel levelFilter, BlockPos center, int radius, int limit) {
        var dims = new ArrayList<String>();
        if (levelFilter != null) dims.add(levelFilter.dimension().identifier().toString());
        else for (var l : server.getAllLevels()) dims.add(l.dimension().identifier().toString());
        var firstDay = Instant.ofEpochMilli(since).atZone(clock.getZone()).toLocalDate();
        return CompletableFuture.supplyAsync(() -> {
            drain();
            var out = new ArrayList<Entry>();
            for (String dim : dims) {
                Path dir = root.resolve(AdminFiles.folder(dim));
                if (!Files.isDirectory(dir)) continue;
                try (var regions = Files.list(dir)) {
                    for (var region : regions.toList()) {
                        if (center != null && !regionNear(region, center, radius)) continue;
                        read(region, dim, e -> e.player().equals(player) && e.time() >= since
                                && (center == null || Math.abs(e.x() - center.getX()) <= radius && Math.abs(e.z() - center.getZ()) <= radius), out, firstDay);
                    }
                } catch (IOException e) { LOG.warn("Could not list {}", dir, e); }
            }
            out.sort(Comparator.comparingLong(Entry::time).reversed());
            return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
        }, io);
    }
    private static boolean regionNear(Path region, BlockPos c, int r) {
        String[] p = region.getFileName().toString().split("_");
        if (p.length != 2) return false;
        try {
            int rx = Integer.parseInt(p[0]), rz = Integer.parseInt(p[1]);
            return (rx << 9) <= c.getX() + r && (rx << 9) + 511 >= c.getX() - r && (rz << 9) <= c.getZ() + r && (rz << 9) + 511 >= c.getZ() - r;
        } catch (NumberFormatException e) { return false; }
    }
    private static void read(Path region, String dim, java.util.function.Predicate<Entry> keep, List<Entry> out) { read(region, dim, keep, out, null); }
    private static void read(Path region, String dim, java.util.function.Predicate<Entry> keep, List<Entry> out, LocalDate firstDay) {
        if (!Files.isDirectory(region)) return;
        try (var days = Files.list(region)) {
            for (var file : days.filter(f -> f.getFileName().toString().endsWith(".tsv")).toList()) {
                var day = dayOf(file);
                if (firstDay != null && day != null && day.isBefore(firstDay)) continue;
                try (var lines = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    for (String line; (line = lines.readLine()) != null; ) {
                        var e = Entry.parse(dim, line);
                        if (e != null && keep.test(e)) out.add(e);
                    }
                }
            }
        } catch (IOException e) { LOG.warn("Could not read {}", region, e); }
    }

    static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); }
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }
    static String unescape(String s) {
        var out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                out.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else out.append(c);
        }
        return out.toString();
    }
    private BlockLog() {}
}
