package com.simplebuilding.modules.simplelivemap.hosted;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * HTTP client of the hosted live map (livemap.simplemods.de, upload API v1, plan docs/ai/PLAN-A-LIVEMAP34-2026-11-12.md section 3).
 * {@link HttpURLConnection} only (in every Java runtime, also trimmed server ones). HTTPS is required; plain HTTP only to this
 * machine (tests, a local test service). The map key goes only into the {@code Authorization} header, never into a URL or a log
 * line. Answers are read up to 1 MiB.
 */
public final class HostedClient {
    public static final String DEFAULT_ENDPOINT = "https://livemap.simplemods.de";
    public static final int CONNECT_MS = 10_000, READ_MS = 30_000, MAX_ANSWER = 1 << 20;
    /** A tile path as the service accepts it: {@code <dimension folder>/<z>/<x>/<y>} (without .png). */
    public static final java.util.regex.Pattern TILE = java.util.regex.Pattern.compile("[a-z0-9_.-]{1,96}/[0-4]/-?\\d{1,7}/-?\\d{1,7}");

    private final String endpoint;
    private final String key;

    public HostedClient(String endpoint, String key) {
        this.endpoint = endpoint;
        this.key = key;
    }

    /** One answer: HTTP status, JSON body (an empty object when there is none), Retry-After seconds (0 = none). */
    public record Answer(int status, JsonObject json, int retryAfter) {
        public boolean ok() {
            return status == 200;
        }

        public int seconds(String field, int fallback) {
            try {
                return json.has(field) ? json.get(field).getAsInt() : fallback;
            } catch (RuntimeException e) {
                return fallback;
            }
        }
    }

    /**
     * The endpoint without a trailing slash, or null when it is no allowed address: {@code https://host[:port]} or
     * {@code http://} to 127.0.0.1 / localhost / [::1]; no user info, no path, no query.
     */
    public static String checkEndpoint(String value) {
        if (value == null) return null;
        String v = value.trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        try {
            URI u = new URI(v);
            String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
            if (host.isEmpty() || u.getRawUserInfo() != null || (u.getRawPath() != null && !u.getRawPath().isEmpty()) || u.getRawQuery() != null
                    || u.getRawFragment() != null) return null;
            boolean local = host.equals("127.0.0.1") || host.equals("localhost") || host.equals("[::1]") || host.equals("::1");
            if (scheme.equals("https") || (scheme.equals("http") && local)) return v;
        } catch (URISyntaxException e) {
            return null;
        }
        return null;
    }

    /** "abcd-efgh" / "ABCD EFGH" -> "ABCDEFGH"; null when it cannot be a connect code (8 of A-Z without I/O, 2-9). */
    public static String normalizeCode(String code) {
        if (code == null) return null;
        String c = code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return c.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}") ? c : null;
    }

    public Answer connect(String code, String world, String modVersion) throws IOException {
        JsonObject o = new JsonObject();
        o.addProperty("code", code);
        o.addProperty("world", world);
        o.addProperty("mod", modVersion);
        return send("POST", "connect", "application/json", o.toString().getBytes(StandardCharsets.UTF_8), false);
    }

    public Answer status() throws IOException {
        return send("GET", "status", null, null, true);
    }

    public Answer manifest(Map<String, String> tiles, boolean done) throws IOException {
        JsonObject o = new JsonObject();
        JsonObject t = new JsonObject();
        tiles.forEach(t::addProperty);
        o.add("tiles", t);
        o.addProperty("done", done);
        return send("POST", "manifest", "application/json", o.toString().getBytes(StandardCharsets.UTF_8), true);
    }

    public Answer tiles(byte[] zip) throws IOException {
        return send("POST", "tiles", "application/zip", zip, true);
    }

    public Answer live(String json) throws IOException {
        return send("PUT", "live", "application/json", json.getBytes(StandardCharsets.UTF_8), true);
    }

    public Answer disconnect() throws IOException {
        return send("POST", "disconnect", "application/json", new byte[0], true);
    }

    /** A ZIP bundle of tiles: entries {@code <path>.png} in the given order (fast deflate; PNGs hardly shrink). */
    public static byte[] bundle(Map<String, byte[]> tiles) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setLevel(Deflater.BEST_SPEED);
            for (Map.Entry<String, byte[]> e : tiles.entrySet()) {
                if (!TILE.matcher(e.getKey()).matches()) throw new IOException("not a tile path: " + e.getKey());
                zip.putNextEntry(new ZipEntry(e.getKey() + ".png"));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private Answer send(String method, String name, String type, byte[] body, boolean auth) throws IOException {
        HttpURLConnection c;
        try {
            c = (HttpURLConnection) URI.create(endpoint + "/api/v1/" + name).toURL().openConnection();
        } catch (IllegalArgumentException e) {
            throw new IOException("bad endpoint", e);
        }
        try {
            c.setConnectTimeout(CONNECT_MS);
            c.setReadTimeout(READ_MS);
            c.setInstanceFollowRedirects(false);
            c.setUseCaches(false);
            c.setRequestMethod(method);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "SimpleLivemap");
            if (auth) c.setRequestProperty("Authorization", "Bearer " + key);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", type);
                c.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body);
                }
            }
            int status = c.getResponseCode();
            int retry = 0;
            try {
                String r = c.getHeaderField("Retry-After");
                retry = r == null ? 0 : Math.max(0, Math.min(3600, Integer.parseInt(r.trim())));
            } catch (NumberFormatException ignored) {
            }
            InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
            JsonObject json = new JsonObject();
            if (in != null) {
                try (in) {
                    byte[] raw = in.readNBytes(MAX_ANSWER);
                    try {
                        JsonElement e = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8));
                        if (e.isJsonObject()) json = e.getAsJsonObject();
                    } catch (RuntimeException ignored) {
                        // not JSON (a proxy error page): status alone counts
                    }
                }
            }
            return new Answer(status, json, retry);
        } finally {
            c.disconnect();
        }
    }
}
