package com.simplebuilding.modules.simplelivemap.hosted;

import com.simplebuilding.modules.simplelivemap.SimpleLivemap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Uploads the tiles of a connected world to livemap.simplemods.de on ONE background thread (never the server thread):
 * first a manifest of every tile with its SHA-256 (the service answers which ones it lacks - after reconnecting from the
 * archive only differences go up), then those as ZIP bundles at the pace the service asks for ({@code wait}), afterwards
 * only tiles the {@code TileStore} rewrote, plus {@code live.json} in the tier's rhythm. 401/410 (key unknown, map
 * disconnected on the website) end the link; 429/503 wait as told; network errors back off up to 30 minutes.
 */
public final class HostedUploader implements AutoCloseable {
    public static final int MANIFEST_CHUNK = 2000, MAX_BACKOFF = 1800;

    private final HostedLink link;
    private final Path tiles;
    private final Supplier<String> live;
    private final Runnable lost;
    private final HostedClient client;
    private final Set<String> changed = ConcurrentHashMap.newKeySet();
    private final Thread thread;
    private volatile boolean stop;
    private volatile String state = "starting";
    private volatile int progress;
    private int backoff = 5;

    /** {@code lost} runs (on this thread) when the service no longer knows the map: the caller forgets the link. */
    public HostedUploader(HostedLink link, Path tiles, Supplier<String> live, Runnable lost) {
        this.link = link;
        this.tiles = tiles;
        this.live = live;
        this.lost = lost;
        this.client = new HostedClient(link.endpoint(), link.key());
        this.thread = new Thread(this::run, "simplelivemap-hosted");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.MIN_PRIORITY);
    }

    public HostedUploader start() {
        thread.start();
        return this;
    }

    public HostedLink link() {
        return link;
    }

    /** "starting", "preparing", "ready", "waiting" (queue/limits), "offline" (network), "lost". */
    public String state() {
        return state;
    }

    public int progress() {
        return progress;
    }

    /** A tile file was (re)written: {@code <folder>/<z>/<x>/<y>}. Called from the tile thread. */
    public void changed(String path) {
        if (changed.size() < 1 << 18) changed.add(path);
    }

    @Override
    public void close() {
        stop = true;
        thread.interrupt();
    }

    private void run() {
        try {
            boolean synced = false;
            long nextLive = 0;
            int liveSeconds = 10, tileSeconds = 900, batchTiles = 100, batchBytes = 2 << 20;
            while (!stop) {
                HostedClient.Answer a;
                try {
                    if (!synced) {
                        a = sync(batchTiles, batchBytes);
                        if (a == null) continue;
                        if (!a.ok()) {
                            if (handle(a)) return;
                            continue;
                        }
                        synced = true;
                        liveSeconds = limit(a, "liveSeconds", liveSeconds);
                        tileSeconds = limit(a, "tileSeconds", tileSeconds);
                        batchTiles = limit(a, "batchTiles", batchTiles);
                        batchBytes = limit(a, "batchBytes", batchBytes);
                    }
                    long now = System.currentTimeMillis();
                    if (now >= nextLive) {
                        a = client.live(live.get());
                        if (!a.ok() && handle(a)) return;
                        nextLive = now + Math.max(1, a.seconds("wait", liveSeconds)) * 1000L;
                    }
                    if (!changed.isEmpty()) {
                        List<String> batch = new ArrayList<>(changed);
                        a = upload(batch, batchTiles, batchBytes);
                        if (a != null && !a.ok()) {
                            if (handle(a)) return;
                            continue;
                        }
                        if (a != null) note(a);
                        sleep(Math.min(tileSeconds, Math.max(1, a == null ? tileSeconds : a.seconds("wait", tileSeconds))));
                    } else {
                        sleep(Math.max(1, Math.min(liveSeconds, (int) ((nextLive - System.currentTimeMillis()) / 1000) + 1)));
                    }
                    backoff = 5;
                } catch (IOException e) {
                    state = "offline";
                    sleep(backoff);
                    backoff = Math.min(MAX_BACKOFF, backoff * 2);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            SimpleLivemap.LOG.warn("Simple Livemap: hosted upload stopped: {}", e.toString());
        }
    }

    private static int limit(HostedClient.Answer a, String field, int fallback) {
        try {
            var l = a.json().getAsJsonObject("limits");
            return l != null && l.has(field) ? Math.max(1, l.get(field).getAsInt()) : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Manifest of all tiles in chunks, then the tiles the service asked for. Null = interrupted by a wait. */
    private HostedClient.Answer sync(int batchTiles, int batchBytes) throws IOException, InterruptedException {
        Map<String, String> all = manifest(tiles);
        List<String> need = new ArrayList<>();
        List<String> paths = new ArrayList<>(all.keySet());
        HostedClient.Answer a = null;
        int i = 0;
        do {
            Map<String, String> chunk = new LinkedHashMap<>();
            for (int j = i; j < Math.min(paths.size(), i + MANIFEST_CHUNK); j++) chunk.put(paths.get(j), all.get(paths.get(j)));
            i += MANIFEST_CHUNK;
            a = client.manifest(chunk, i >= paths.size());
            if (!a.ok()) return a;
            note(a);
            batchTiles = limit(a, "batchTiles", batchTiles);
            batchBytes = limit(a, "batchBytes", batchBytes);
            if (a.json().has("need")) a.json().getAsJsonArray("need").forEach(e -> need.add(e.getAsString()));
        } while (i < paths.size());
        changed.removeAll(need);
        while (!need.isEmpty() && !stop) {
            HostedClient.Answer up = upload(need, batchTiles, batchBytes);
            if (up == null) break;
            if (!up.ok()) {
                if (up.status() == 429 || up.status() == 503) {
                    state = "waiting";
                    sleep(Math.max(5, up.retryAfter() > 0 ? up.retryAfter() : 60));
                    continue;
                }
                return up;
            }
            note(up);
            a = up;
            if (!need.isEmpty()) sleep(Math.max(1, up.seconds("wait", 1)));
        }
        return a;
    }

    /**
     * Uploads the first tiles of {@code paths} that fit one bundle and removes them from the list (and from the change set);
     * null when nothing was left to send.
     */
    private HostedClient.Answer upload(List<String> paths, int batchTiles, int batchBytes) throws IOException {
        Map<String, byte[]> bundle = new LinkedHashMap<>();
        long bytes = 0;
        var it = paths.iterator();
        while (it.hasNext() && bundle.size() < batchTiles) {
            String p = it.next();
            Path file = tiles.resolve(p + ".png");
            byte[] data = Files.isRegularFile(file) ? Files.readAllBytes(file) : null;
            if (data != null && bytes + data.length > batchBytes - 65536 && !bundle.isEmpty()) break;
            it.remove();
            changed.remove(p);
            if (data == null || data.length > 256 * 1024 || !HostedClient.TILE.matcher(p).matches()) continue;
            bundle.put(p, data);
            bytes += data.length;
        }
        if (bundle.isEmpty()) return null;
        HostedClient.Answer a = client.tiles(HostedClient.bundle(bundle));
        if (a.status() == 429 || a.status() == 503 || a.status() == 507) changed.addAll(bundle.keySet()); // try again later
        return a;
    }

    /** True when the link is gone for good (stop); otherwise waits as the service asks. */
    private boolean handle(HostedClient.Answer a) throws InterruptedException {
        if (a.status() == 401 || a.status() == 410) {
            state = "lost";
            SimpleLivemap.LOG.warn("Simple Livemap: the hosted map {} was disconnected on the website; uploads stop (connect again with a new code)", link.map());
            lost.run();
            return true;
        }
        state = a.status() == 429 || a.status() == 503 || a.status() == 507 ? "waiting" : "offline";
        sleep(a.retryAfter() > 0 ? a.retryAfter() : Math.min(MAX_BACKOFF, backoff *= 2));
        return false;
    }

    private void note(HostedClient.Answer a) {
        if (a.json().has("state")) state = a.json().get("state").getAsString();
        progress = a.seconds("progress", progress);
    }

    private void sleep(int seconds) throws InterruptedException {
        long until = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        while (!stop && System.currentTimeMillis() < until) Thread.sleep(Math.min(1000, Math.max(1, until - System.currentTimeMillis())));
    }

    /** {@code <folder>/<z>/<x>/<y>} -> SHA-256 hex of every tile file below {@code dir} (no temp files, nothing outside). */
    public static Map<String, String> manifest(Path dir) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) return out;
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        Path root = dir.toAbsolutePath().normalize();
        try (Stream<Path> s = Files.walk(root, 4)) {
            for (Path f : (Iterable<Path>) s.filter(p -> p.getFileName().toString().endsWith(".png") && Files.isRegularFile(p)).sorted()::iterator) {
                String rel = root.relativize(f).toString().replace('\\', '/');
                rel = rel.substring(0, rel.length() - 4);
                if (!HostedClient.TILE.matcher(rel).matches()) continue;
                out.put(rel, HexFormat.of().formatHex(sha.digest(Files.readAllBytes(f))));
            }
        }
        return out;
    }
}
