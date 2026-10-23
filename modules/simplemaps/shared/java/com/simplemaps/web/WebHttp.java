package com.simplemaps.web;

import com.simplemaps.SimpleMaps;
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
 * Optional built-in web server of the web map (stage 2, {@code docs/ai/PLAN-A-WEBMAP2-2026-10-23.md}): read-only,
 * {@code GET}/{@code HEAD} only, serves just the whitelisted files of the export folder plus {@code live.json} (a string the
 * server thread prepared). Own small implementation on {@link ServerSocket} (no {@code jdk.httpserver}, which trimmed
 * runtimes lack). Limits: one request per connection, 5 s read timeout, short request lines and few headers, a per-IP
 * request budget per minute, 4 worker threads with a short queue. All threads are daemons; the acceptor closes itself when
 * the game server stops running.
 */
public final class WebHttp implements AutoCloseable {
    public static final int TIMEOUT_MS = 5000, MAX_LINE = 2048, MAX_HEADERS = 64, THREADS = 4, QUEUE = 32;
    public static final long MAX_FILE = 4L << 20;
    public static final String LIVE = "live.json";
    /** Every file the server may answer with; anything else is 404. */
    private static final Pattern FILES = Pattern.compile(
            "index\\.html|markers\\.json|live\\.json|tiles/[a-z0-9_.-]{1,96}/[0-4]/-?\\d{1,7}/-?\\d{1,7}\\.png|icons/[a-z0-9_]{1,64}\\.png");
    private static final String CSP = "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self'; "
            + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

    /** What the server was started with; a different value restarts it. */
    public record Settings(String bind, int port, String token, int perMinute, Path dir) {}

    private final Settings settings;
    private final Path dir;
    private final ServerSocket socket;
    private final RateLimit limit;
    private final Supplier<String> live;
    private final BooleanSupplier alive;
    private final ThreadPoolExecutor pool;
    private volatile boolean closed;

    private WebHttp(Settings settings, ServerSocket socket, Supplier<String> live, BooleanSupplier alive) {
        this.settings = settings;
        this.dir = settings.dir().toAbsolutePath().normalize();
        this.socket = socket;
        this.limit = new RateLimit(settings.perMinute());
        this.live = live;
        this.alive = alive;
        this.pool = new ThreadPoolExecutor(THREADS, THREADS, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE), r -> {
            Thread t = new Thread(r, "simplemaps-web-http");
            t.setDaemon(true);
            return t;
        });
        Thread acceptor = new Thread(this::accept, "simplemaps-web-http-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /**
     * Binds and starts serving. {@code live} returns the current {@code live.json} or null (404); {@code alive} false closes
     * the server (checked about once per second).
     */
    public static WebHttp start(Settings settings, Supplier<String> live, BooleanSupplier alive) throws IOException {
        ServerSocket socket = new ServerSocket();
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName(settings.bind()), settings.port()), 50);
            socket.setSoTimeout(1000);
        } catch (IOException | RuntimeException e) {
            socket.close();
            throw e;
        }
        return new WebHttp(settings, socket, live, alive);
    }

    public Settings settings() {
        return settings;
    }

    /** The bound port (differs from the setting when that was 0). */
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
            if (!closed) SimpleMaps.LOG.warn("Simple Maps: web map server stopped: {}", e.toString());
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
            String requestLine = readLine(in);
            if (requestLine == null) return;
            String authorization = null;
            for (int i = 0; ; i++) {
                String header = readLine(in);
                if (header == null || i >= MAX_HEADERS) {
                    respond(out, 400, "Bad Request", "text/plain; charset=utf-8", bytes("bad request"), true);
                    return;
                }
                if (header.isEmpty()) break;
                int colon = header.indexOf(':');
                if (colon > 0 && header.substring(0, colon).trim().equalsIgnoreCase("Authorization")) authorization = header.substring(colon + 1).trim();
            }
            String ip = client.getInetAddress() == null ? "?" : client.getInetAddress().getHostAddress();
            if (!limit.allow(ip, System.currentTimeMillis())) {
                respond(out, 429, "Too Many Requests", "text/plain; charset=utf-8", bytes("too many requests"), true, "Retry-After: 60");
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length != 3 || !parts[2].startsWith("HTTP/1.")) {
                respond(out, 400, "Bad Request", "text/plain; charset=utf-8", bytes("bad request"), true);
                return;
            }
            boolean head = parts[0].equals("HEAD");
            if (!head && !parts[0].equals("GET")) {
                respond(out, 405, "Method Not Allowed", "text/plain; charset=utf-8", bytes("read only"), true, "Allow: GET, HEAD");
                return;
            }
            if (!tokenOk(settings.token(), parts[1], authorization)) {
                respond(out, 401, "Unauthorized", "text/plain; charset=utf-8", bytes("token required"), head);
                return;
            }
            String name = route(parts[1]);
            byte[] body = name == null ? null : name.equals(LIVE) ? liveBytes() : file(dir, name);
            if (body == null && "index.html".equals(name)) body = viewer();
            if (body == null) {
                respond(out, 404, "Not Found", "text/plain; charset=utf-8", bytes("not found"), head);
                return;
            }
            respond(out, 200, "OK", contentType(name), body, head, name.endsWith(".png") ? "Cache-Control: public, max-age=60" : "Cache-Control: no-cache");
        } catch (IOException | RuntimeException ignored) {
            // Client went away, timed out or sent garbage: the connection is simply closed.
        }
    }

    private byte[] liveBytes() {
        String json = live == null ? null : live.get();
        return json == null ? null : bytes(json);
    }

    /** The viewer from the mod jar, for a folder that has not been exported yet. */
    private static byte[] viewer() {
        try (InputStream in = WebHttp.class.getResourceAsStream("/assets/" + SimpleMaps.MOD_ID + "/web/index.html")) {
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

    /** One CRLF (or LF) terminated ASCII line of at most {@link #MAX_LINE} bytes; null at end of stream or when too long. */
    static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int b; (b = in.read()) != -1; ) {
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

    public static String contentType(String name) {
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        return "text/html; charset=utf-8";
    }

    /**
     * Request target to a file name relative to the export folder, or null when it is not one of the whitelisted files.
     * No percent-decoding at all: {@code %}, backslashes, {@code ..}, {@code //} and control characters are refused before
     * the whitelist, so no encoding trick reaches the file system. The query string is ignored here.
     */
    public static String route(String target) {
        if (target == null || target.isEmpty() || target.length() > MAX_LINE) return null;
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        if (!path.startsWith("/") || path.contains("%") || path.contains("\\") || path.contains("..") || path.contains("//")) return null;
        for (int i = 0; i < path.length(); i++) if (path.charAt(i) < 0x21 || path.charAt(i) > 0x7e) return null;
        String name = path.equals("/") ? "index.html" : path.substring(1);
        return FILES.matcher(name).matches() ? name : null;
    }

    /** Bytes of a whitelisted file inside {@code dir}; null when missing, too big or (through a link) outside the folder. */
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

    /** No token configured, or {@code ?token=} / {@code Authorization: Bearer} equals it (constant-time compare). */
    public static boolean tokenOk(String token, String target, String authorization) {
        if (token == null || token.isEmpty()) return true;
        String given = null;
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) given = authorization.substring(7).trim();
        int q = target == null ? -1 : target.indexOf('?');
        if (given == null && q >= 0) {
            for (String pair : target.substring(q + 1).split("&")) if (pair.startsWith("token=")) given = pair.substring(6);
        }
        return given != null && MessageDigest.isEqual(bytes(token), bytes(given));
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
