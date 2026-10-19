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
    /**
     * One action. {@code action}: B broke, P placed, T took from a container, D deposited into a container, S edited a sign
     * (extra: side, old and new text), X destroyed by an explosion, F burned, I a hopper put into a container, O a hopper took out.
     * Actors that are no player have {@link #WORLD} as UUID and a name starting with {@code #} ({@code #fire}, {@code #hopper}, {@code #creeper}).
     */
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

    /** UUID of the non-player actors. */
    public static final UUID WORLD = new UUID(0L, 0L);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("simpletweaks-blocklog");
    private static final ConcurrentLinkedQueue<Entry> QUEUE = new ConcurrentLinkedQueue<>();
    private static ExecutorService io;
    private static Path root;
    private static long ticks;
    private static LocalDate lastPurge;
    /** Server thread: world events (explosions, fire, hoppers) still allowed in this tick. */
    private static int budget;
    /** Server thread: hopper moves of the current flush interval, summed per container, direction and item. */
    private static final Map<String, Entry> HOPPERS = new LinkedHashMap<>();
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
        io = null; root = null; QUEUE.clear(); HOPPERS.clear();
        Rollback.forget();
    }
    public static void tick(MinecraftServer server) {
        if (io == null) return;
        budget = Admin.config().blockLog().worldEventsPerTick();
        if (++ticks % (Admin.config().blockLog().flushSeconds() * 20L) == 0) { collect(); io.submit(BlockLog::drain); }
        if (ticks % 72000 == 0) io.submit(BlockLog::purge);
    }

    public static void record(ServerLevel level, ServerPlayer player, char action, BlockPos pos, String id, int count, String extra) {
        if (!active() || player == null) return;
        QUEUE.add(new Entry(clock.millis(), action, player.getUUID(), player.getGameProfile().name(), level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(), id, count, extra == null ? "" : extra));
    }

    /** A logged action of a non-player actor ({@code #name}). */
    public static void record(ServerLevel level, String actor, char action, BlockPos pos, String id, int count, String extra) {
        if (!active()) return;
        QUEUE.add(new Entry(clock.millis(), action, WORLD, actor, level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), id, count, extra == null ? "" : extra));
    }
    /** World events take one unit of the per-tick budget ({@code worldEventsPerTick}); false = drop the event. */
    public static boolean budget() {
        if (budget <= 0) return false;
        budget--;
        return true;
    }
    /** Tests: a fresh budget now. */
    public static void resetBudget() { budget = Admin.config().blockLog().worldEventsPerTick(); }
    /** Hopper move ({@code I} or {@code O}): summed until the next flush. Server thread. */
    public static void hopper(ServerLevel level, char action, BlockPos pos, String item, int count) {
        if (!active() || count <= 0) return;
        String dim = level.dimension().identifier().toString();
        String key = dim + '|' + pos.asLong() + '|' + action + '|' + item;
        var old = HOPPERS.get(key);
        if (old == null && !budget()) return;
        HOPPERS.put(key, old == null ? new Entry(clock.millis(), action, WORLD, "#hopper", dim, pos.getX(), pos.getY(), pos.getZ(), item, count, "")
                : new Entry(old.time(), action, WORLD, old.name(), dim, old.x(), old.y(), old.z(), item, old.count() + count, ""));
    }
    /** Server thread: the summed hopper moves go into the write queue. */
    static void collect() {
        if (HOPPERS.isEmpty()) return;
        QUEUE.addAll(HOPPERS.values());
        HOPPERS.clear();
    }

    /** Tests and shutdown: everything queued so far is on disk when this returns. */
    public static void flushNow() {
        if (io == null) return;
        collect();
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
        collect();
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
        return query(server, LogFilter.of(player, since, levelFilter == null ? null : levelFilter.dimension().identifier().toString(), center, radius), limit);
    }

    /** Entries matching {@code filter}, newest first, at most {@code limit} (IO thread; call from the server thread). */
    public static CompletableFuture<List<Entry>> query(MinecraftServer server, LogFilter filter, int limit) {
        var dims = new ArrayList<String>();
        if (filter.dims() != null) dims.addAll(filter.dims());
        else for (var l : server.getAllLevels()) dims.add(l.dimension().identifier().toString());
        var firstDay = filter.since() > 0 ? Instant.ofEpochMilli(filter.since()).atZone(clock.getZone()).toLocalDate() : null;
        collect();
        return CompletableFuture.supplyAsync(() -> {
            drain();
            var out = new ArrayList<Entry>();
            Comparator<Entry> newest = Comparator.comparingLong(Entry::time).reversed();
            for (String dim : dims) {
                Path dir = root.resolve(AdminFiles.folder(dim));
                if (!Files.isDirectory(dir)) continue;
                try (var regions = Files.list(dir)) {
                    for (var region : regions.toList()) {
                        if (filter.center() != null && !regionNear(region, filter.center(), filter.radius())) continue;
                        read(region, dim, filter::test, out, firstDay);
                        // keep memory bounded on huge logs: only the newest `limit` can survive
                        if (out.size() > 4 * limit) { out.sort(newest); out.subList(limit, out.size()).clear(); }
                    }
                } catch (IOException e) { LOG.warn("Could not list {}", dir, e); }
            }
            out.sort(newest);
            return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
        }, io);
    }

    /** Removes every line of {@code player} from all day files (privacy); the number of removed lines. */
    public static CompletableFuture<Integer> purgePlayer(UUID player) {
        collect();
        String id = player.toString();
        return CompletableFuture.supplyAsync(() -> {
            drain();
            int removed = 0;
            if (root == null || !Files.isDirectory(root)) return 0;
            try (var walk = Files.walk(root, 3)) {
                for (var file : walk.filter(p -> p.getFileName().toString().endsWith(".tsv")).toList()) {
                    var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    var keep = new ArrayList<String>(lines.size());
                    for (String line : lines) {
                        String[] f = line.split("\t", 4);
                        if (f.length >= 3 && f[2].equals(id)) removed++; else keep.add(line);
                    }
                    if (keep.size() == lines.size()) continue;
                    if (keep.isEmpty()) { Files.deleteIfExists(file); continue; }
                    Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                    Files.write(tmp, keep, StandardCharsets.UTF_8);
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
            } catch (IOException e) { LOG.warn("Could not purge {} from the block log", id, e); }
            return removed;
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
