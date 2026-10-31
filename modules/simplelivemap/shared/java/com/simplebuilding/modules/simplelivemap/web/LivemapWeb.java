package com.simplebuilding.modules.simplelivemap.web;

import com.simplebuilding.modules.simplelivemap.SimpleLivemap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Built-in read-only web server of the live map (off by default, plan E10/E11): {@code GET}/{@code HEAD} only and three fixed
 * routes – the viewer page {@code /} (no data in it), {@code /live.json} (a string the server thread prepared) and
 * {@code /tiles/<dimension>/<z>/<x>/<y>.png} (files of the tile folder, numbers only). Anything else is 404; no percent
 * decoding, no {@code ..}, links out of the folder are not followed. Private map: data routes need
 * {@code Authorization: Bearer <token>} (the viewer reads the token from the URL fragment, which browsers never send, so it
 * stays out of server and proxy logs); an empty token never opens a private map. Limits: one request per connection, the
 * whole request within 5 s, short lines, few headers, a per-address budget per minute counted before reading, 4 daemon
 * worker threads with a short queue; the acceptor stops when the game server stops. Same hardening as Simple Maps'
 * {@code WebHttp} (rebuilt here: no classes of other mods). Own {@link ServerSocket} instead of {@code jdk.httpserver},
 * which trimmed server runtimes lack.
 */
public final class LivemapWeb implements AutoCloseable {
    public static final int TIMEOUT_MS = 5000, MAX_LINE = 2048, MAX_HEADERS = 64, THREADS = 4, QUEUE = 32;
    public static final long MAX_FILE = 1L << 20;
    public static final String INDEX = "index.html", LIVE = "live.json";
    private static final Pattern TILE = Pattern.compile("tiles/[a-z0-9_.-]{1,96}/[0-4]/-?\\d{1,7}/-?\\d{1,7}\\.png");
    private static final String CSP = "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' blob:; "
            + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

    /** What the server was started with; a different value restarts it. {@code token} empty + private = closed. */
    public record Settings(String bind, int port, String token, boolean open, int perMinute, Path tiles) {}

    private final Settings settings;
    private final Path tilesDir;
    private final ServerSocket socket;
    private final RateLimit limit;
    private final Supplier<String> live;
    private final BooleanSupplier alive;
    private final ThreadPoolExecutor pool;
    private volatile boolean closed;

    private LivemapWeb(Settings settings, ServerSocket socket, Supplier<String> live, BooleanSupplier alive) {
        this.settings = settings;
        this.tilesDir = settings.tiles().toAbsolutePath().normalize();
        this.socket = socket;
        this.limit = new RateLimit(settings.perMinute());
        this.live = live;
        this.alive = alive;
        this.pool = new ThreadPoolExecutor(THREADS, THREADS, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE), r -> {
            Thread t = new Thread(r, "simplelivemap-web");
            t.setDaemon(true);
            return t;
        });
        Thread acceptor = new Thread(this::accept, "simplelivemap-web-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** Binds and starts serving; {@code alive} false closes the server (checked about once per second). */
    public static LivemapWeb start(Settings settings, Supplier<String> live, BooleanSupplier alive) throws IOException {
        ServerSocket socket = new ServerSocket();
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName(settings.bind()), settings.port()), 50);
            socket.setSoTimeout(1000);
        } catch (IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
        return new LivemapWeb(settings, socket, live, alive);
    }

    public Settings settings() {
        return settings;
    }

    /** The bound port (differs from the setting when that was 0, tests). */
    public int port() {
        return socket.getLocalPort();
    }

    public boolean running() {
        return !closed && !socket.isClosed();
    }

    @Override
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        pool.shutdownNow();
    }

    private void accept() {
        try {
            while (!closed && alive.getAsBoolean()) {
                Socket client;
                try {
                    client = socket.accept();
                } catch (SocketTimeoutException e) {
                    continue;
                }
                try {
                    pool.execute(() -> serve(client));
                } catch (RejectedExecutionException e) {
                    quietClose(client);
                }
            }
        } catch (IOException e) {
            if (!closed) SimpleLivemap.LOG.warn("Simple Livemap: web server stopped: {}", e.toString());
        } finally {
            close();
        }
    }

    private static void quietClose(Socket s) {
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }

    private void serve(Socket client) {
        try (client) {
            client.setSoTimeout(TIMEOUT_MS);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000L;
            String ip = client.getInetAddress() == null ? "?" : client.getInetAddress().getHostAddress();
            if (!limit.allow(ip, System.currentTimeMillis())) {
                respond(out, 429, "Too Many Requests", "text/plain; charset=utf-8", bytes("too many requests"), false, "Retry-After: 60");
                return;
            }
            String requestLine = readLine(in, deadline);
            if (requestLine == null) return;
            String authorization = null;
            for (int i = 0; ; i++) {
                String header = readLine(in, deadline);
                if (header == null || i >= MAX_HEADERS) {
                    respond(out, 400, "Bad Request", "text/plain; charset=utf-8", bytes("bad request"), false);
                    return;
                }
                if (header.isEmpty()) break;
                int colon = header.indexOf(':');
                if (colon > 0 && header.substring(0, colon).trim().equalsIgnoreCase("Authorization")) authorization = header.substring(colon + 1).trim();
            }
            String[] parts = requestLine.split(" ");
            if (parts.length != 3 || !parts[2].startsWith("HTTP/1.")) {
                respond(out, 400, "Bad Request", "text/plain; charset=utf-8", bytes("bad request"), false);
                return;
            }
            boolean head = parts[0].equals("HEAD");
            if (!head && !parts[0].equals("GET")) {
                respond(out, 405, "Method Not Allowed", "text/plain; charset=utf-8", bytes("read only"), false, "Allow: GET, HEAD");
                return;
            }
            String name = route(parts[1]);
            if (name == null) {
                respond(out, 404, "Not Found", "text/plain; charset=utf-8", bytes("not found"), head);
                return;
            }
            if (name.equals(INDEX)) {
                byte[] page = viewer();
                if (page == null) respond(out, 404, "Not Found", "text/plain; charset=utf-8", bytes("not found"), head);
                else respond(out, 200, "OK", "text/html; charset=utf-8", page, head, "Cache-Control: no-cache");
                return;
            }
            if (!allowed(settings, authorization)) {
                respond(out, 401, "Unauthorized", "text/plain; charset=utf-8", bytes("token required"), head, "Cache-Control: no-store");
                return;
            }
            String cache = settings.open() ? "Cache-Control: public, max-age=30" : "Cache-Control: private, max-age=30";
            if (name.equals(LIVE)) {
                String json = live == null ? null : live.get();
                if (json == null) respond(out, 404, "Not Found", "text/plain; charset=utf-8", bytes("not found"), head);
                else respond(out, 200, "OK", "application/json; charset=utf-8", bytes(json), head, "Cache-Control: no-store");
                return;
            }
            byte[] body = file(tilesDir, name.substring("tiles/".length()));
            // A tile nobody explored yet is simply empty: 204 keeps browser consoles quiet.
            if (body == null) respond(out, 204, "No Content", "image/png", new byte[0], head, cache);
            else respond(out, 200, "OK", "image/png", body, head, cache);
        } catch (IOException | RuntimeException ignored) {
            // Client went away, timed out or sent garbage: the connection is simply closed.
        }
    }

    /** Open map, or a non-empty token given as {@code Authorization: Bearer} (constant-time compare). */
    public static boolean allowed(Settings s, String authorization) {
        if (s.open()) return true;
        String token = s.token();
        if (token == null || token.isEmpty() || authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return false;
        return MessageDigest.isEqual(bytes(token), bytes(authorization.substring(7).trim()));
    }

    /** The viewer page from the mod jar. */
    static byte[] viewer() {
        try (InputStream in = LivemapWeb.class.getResourceAsStream("/assets/" + SimpleLivemap.MOD_ID + "/web/index.html")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static void respond(OutputStream out, int code, String reason, String type, byte[] body, boolean head, String... extra) throws IOException {
        StringBuilder h = new StringBuilder("HTTP/1.1 ").append(code).append(' ').append(reason).append("\r\n")
                .append("Content-Type: ").append(type).append("\r\n")
                .append("Content-Length: ").append(body.length).append("\r\n")
                .append("Connection: close\r\n")
                .append("X-Content-Type-Options: nosniff\r\n")
                .append("Referrer-Policy: no-referrer\r\n")
                .append("Content-Security-Policy: ").append(CSP).append("\r\n");
        for (String e : extra) h.append(e).append("\r\n");
        h.append("\r\n");
        out.write(h.toString().getBytes(StandardCharsets.US_ASCII));
        if (!head) out.write(body);
        out.flush();
    }

    /** One CRLF/LF terminated line of at most {@link #MAX_LINE} bytes; null at end of stream, when too long or late. */
    static String readLine(InputStream in, long deadlineNanos) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int b; (b = in.read()) != -1; ) {
            if (System.nanoTime() - deadlineNanos > 0) return null;
            if (b == '\n') {
                byte[] raw = line.toByteArray();
                int n = raw.length > 0 && raw[raw.length - 1] == '\r' ? raw.length - 1 : raw.length;
                return new String(raw, 0, n, StandardCharsets.ISO_8859_1);
            }
            if (line.size() >= MAX_LINE) return null;
            line.write(b);
        }
        return null;
    }

    /**
     * Request target to {@code index.html}, {@code live.json} or {@code tiles/...png}; null for anything else. No percent
     * decoding: {@code %}, backslashes, {@code ..}, {@code //} and control characters are refused before the patterns. The
     * query string is ignored.
     */
    public static String route(String target) {
        if (target == null || target.isEmpty() || target.length() > MAX_LINE) return null;
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        if (!path.startsWith("/") || path.contains("%") || path.contains("\\") || path.contains("..") || path.contains("//")) return null;
        for (int i = 0; i < path.length(); i++) if (path.charAt(i) < 0x21 || path.charAt(i) > 0x7e) return null;
        String name = path.equals("/") ? INDEX : path.substring(1);
        if (name.equals(INDEX) || name.equals(LIVE)) return name;
        return TILE.matcher(name).matches() ? name : null;
    }

    /** Bytes of a tile file inside {@code dir}; null when missing, too big or (through a link) outside the folder. */
    static byte[] file(Path dir, String name) {
        try {
            Path root = dir.toAbsolutePath().normalize();
            Path file = root.resolve(name).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) return null;
            if (!file.toRealPath().startsWith(root.toRealPath())) return null;
            if (Files.size(file) > MAX_FILE) return null;
            return Files.readAllBytes(file);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Fixed one-minute window per client address; at most {@link #MAX_CLIENTS} addresses per window (new ones refused). */
    public static final class RateLimit {
        public static final int MAX_CLIENTS = 4096;
        private final int perMinute;
        private final Map<String, int[]> counts = new HashMap<>();
        private long window = Long.MIN_VALUE;

        public RateLimit(int perMinute) {
            this.perMinute = perMinute;
        }

        public synchronized boolean allow(String client, long nowMillis) {
            long w = nowMillis / 60000;
            if (w != window) {
                counts.clear();
                window = w;
            }
            int[] c = counts.get(client);
            if (c == null) {
                if (counts.size() >= MAX_CLIENTS) return false;
                counts.put(client, c = new int[1]);
            }
            return ++c[0] <= perMinute;
        }
    }
}
