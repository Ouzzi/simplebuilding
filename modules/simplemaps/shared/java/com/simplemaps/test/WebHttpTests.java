package com.simplemaps.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplemaps.MapsConfig;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import com.simplemaps.web.WebExport;
import com.simplemaps.web.WebHttp;
import com.simplemaps.web.WebMap;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Web map stage 2: the built-in read-only web server (off by default, localhost, no path traversal, rate limit, token) and
 * the live players of {@code live.json} (opt-in only). Servers in tests bind to 127.0.0.1 on a free port (0).
 */
final class WebHttpTests {
    private static final String LOCAL = "127.0.0.1";

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("web_http_config_defaults", WebHttpTests::configDefaults);
        all.put("web_http_on_off", WebHttpTests::onOff);
        all.put("web_http_traversal", WebHttpTests::traversal);
        all.put("web_http_endpoints", WebHttpTests::endpoints);
        all.put("web_http_rate_limit", WebHttpTests::rateLimit);
        all.put("web_live_opt_in", WebHttpTests::liveOptIn);
    }

    private WebHttpTests() {}

    record Reply(int status, String headers, byte[] body) {
        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }

        boolean header(String line) {
            return headers.toLowerCase().contains(line.toLowerCase());
        }
    }

    /** One raw HTTP request (the server answers once and closes). */
    static Reply request(int port, String requestLine, String... headers) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(LOCAL), port), 2000);
            s.setSoTimeout(4000);
            StringBuilder b = new StringBuilder(requestLine).append("\r\nHost: localhost\r\n");
            for (String h : headers) b.append(h).append("\r\n");
            s.getOutputStream().write(b.append("\r\n").toString().getBytes(StandardCharsets.ISO_8859_1));
            s.getOutputStream().flush();
            byte[] all = s.getInputStream().readAllBytes();
            int split = -1;
            for (int i = 0; i + 3 < all.length; i++) {
                if (all[i] == '\r' && all[i + 1] == '\n' && all[i + 2] == '\r' && all[i + 3] == '\n') {
                    split = i;
                    break;
                }
            }
            if (split < 0) return new Reply(-1, "", new byte[0]);
            String head = new String(all, 0, split, StandardCharsets.ISO_8859_1);
            return new Reply(Integer.parseInt(head.split(" ")[1]), head, Arrays.copyOfRange(all, split + 4, all.length));
        } catch (IOException | RuntimeException e) {
            return new Reply(-1, e.toString(), new byte[0]);
        }
    }

    static Reply get(int port, String path, String... headers) {
        return request(port, "GET " + path + " HTTP/1.1", headers);
    }

    private static boolean connects(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(LOCAL), port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static WebHttp server(Path dir, String token, int perMinute, String live) {
        try {
            return WebHttp.start(new WebHttp.Settings(LOCAL, 0, token, perMinute, dir), () -> live, () -> true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Config: server off, local machine only, no token; values clamped to their bounds. */
    private static void configDefaults(GameTestHelper h) {
        JsonObject saved = MapsConfig.toJson();
        try {
            MapsConfig.apply(new JsonObject());
            h.assertTrue(!MapsConfig.webHttpEnabled && !MapsConfig.webEnabled, "web server and export are off by default");
            h.assertTrue("127.0.0.1".equals(MapsConfig.webHttpBind) && MapsConfig.webHttpToken.isEmpty(), "localhost, no token by default");
            h.assertTrue(MapsConfig.webHttpPort == MapsConfig.WEB_HTTP_PORT_DEFAULT && MapsConfig.webLiveSeconds == MapsConfig.WEB_LIVE_DEFAULT
                    && MapsConfig.webHttpRequestsPerMinute == MapsConfig.WEB_HTTP_RATE_DEFAULT, "default port, live interval and rate");
            h.assertTrue(!WebMap.httpWanted(), "no server wanted with the defaults");
            JsonObject json = new JsonObject();
            json.addProperty("webHttpPort", 70000);
            json.addProperty("webHttpRequestsPerMinute", 1);
            json.addProperty("webLiveSeconds", 0);
            json.addProperty("webHttpBind", "  ");
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.webHttpPort == MapsConfig.WEB_HTTP_PORT_MAX && MapsConfig.webHttpRequestsPerMinute == MapsConfig.WEB_HTTP_RATE_MIN
                    && MapsConfig.webLiveSeconds == MapsConfig.WEB_LIVE_MIN && "127.0.0.1".equals(MapsConfig.webHttpBind), "values clamped, empty bind = localhost");
            h.assertTrue(MapsConfig.toJson().has("webHttpEnabled") && MapsConfig.toJson().has("webHttpToken"), "keys written to the file");
        } finally {
            MapsConfig.apply(saved);
        }
        h.succeed();
    }

    /** Off by default; on only with webEnabled + webHttpEnabled and without an autoOffWith mod; off closes the port. */
    private static void onOff(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        JsonObject saved = MapsConfig.toJson();
        Predicate<String> loaded = SimpleMaps.modLoaded;
        try {
            SimpleMaps.modLoaded = id -> false;
            WebMap.syncHttp(server);
            h.assertTrue(WebMap.http(server) == null, "no web server with the default config");
            MapsConfig.webHttpEnabled = true;
            MapsConfig.webHttpPort = 0;
            MapsConfig.webEnabled = false;
            WebMap.syncHttp(server);
            h.assertTrue(WebMap.http(server) == null, "webHttpEnabled alone does not start it (needs webEnabled)");
            MapsConfig.webEnabled = true;
            SimpleMaps.modLoaded = id -> id.equals("bluemap");
            WebMap.syncHttp(server);
            h.assertTrue(WebMap.http(server) == null, "autoOffWith (simulated BlueMap) keeps it off");
            SimpleMaps.modLoaded = id -> false;
            WebMap.syncHttp(server);
            WebHttp http = WebMap.http(server);
            h.assertTrue(http != null && http.port() > 0 && connects(http.port()), "switched on: port open");
            h.assertTrue(InetAddress.getByName(http.settings().bind()).isLoopbackAddress(), "bound to the local machine");
            Reply live = get(http.port(), "/live.json");
            h.assertTrue(live.status() == 200 && live.text().contains("\"players\""), "live.json served: " + live.status());
            int port = http.port();
            MapsConfig.webHttpEnabled = false;
            WebMap.syncHttp(server);
            h.assertTrue(WebMap.http(server) == null && !http.running() && !connects(port), "switched off: port closed");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            SimpleMaps.modLoaded = loaded;
            MapsConfig.apply(saved);
            WebMap.syncHttp(server);
        }
        h.succeed();
    }

    /** Only whitelisted files of the export folder: no "..", no encoded dots, no backslashes, no links out of it. */
    private static void traversal(GameTestHelper h) {
        for (String bad : List.of("/../secret.png", "/tiles/../../secret.png", "/%2e%2e/secret.png", "/..%2fsecret.png", "/tiles/a/4/1/1.png/../../../../secret.png",
                "\\..\\secret.png", "/C:/Windows/win.ini", "//etc/passwd", "/markers.json%00.png", "/level.dat", "/secret.png", "http://localhost/markers.json",
                "/tiles/a/9/1/1.png", "/tiles/a/4/1/1.txt", "/icons/../markers.json", "/index.html/", "/tiles/a/4/1/1.png\u0000", "")) {
            h.assertTrue(WebHttp.route(bad) == null, "refused: " + bad);
        }
        h.assertTrue("index.html".equals(WebHttp.route("/")) && "markers.json".equals(WebHttp.route("/markers.json?token=x"))
                && "tiles/minecraft_overworld/4/-3/7.png".equals(WebHttp.route("/tiles/minecraft_overworld/4/-3/7.png?v=1"))
                && "live.json".equals(WebHttp.route("/live.json")), "whitelisted files pass");
        Path outer = WebTests.temp();
        Path dir = outer.resolve("web");
        WebHttp http = null;
        try {
            Files.createDirectories(dir.resolve("tiles/a/4/0"));
            Files.writeString(outer.resolve("secret.png"), "secret");
            Files.writeString(dir.resolve("markers.json"), "{}");
            boolean linked;
            try {
                Files.createSymbolicLink(dir.resolve("tiles/a/4/0/0.png"), outer.resolve("secret.png"));
                linked = true;
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                linked = false;
            }
            http = server(dir, "", 1000, null);
            for (String bad : List.of("/../secret.png", "/tiles/../../secret.png", "/%2e%2e/secret.png", "/..%2fsecret.png", "/secret.png", "//secret.png")) {
                Reply r = get(http.port(), bad);
                h.assertTrue((r.status() == 404 || r.status() == 400) && !r.text().contains("secret"), "not served: " + bad + " -> " + r.status());
            }
            if (linked) {
                Reply r = get(http.port(), "/tiles/a/4/0/0.png");
                h.assertTrue(r.status() == 404 && !r.text().contains("secret"), "a link out of the folder is not followed");
            }
            h.assertTrue(get(http.port(), "/markers.json").status() == 200, "a real file still works");
            h.assertTrue(get(http.port(), "/live.json").status() == 404, "no live data, no live.json");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (http != null) http.close();
            WebTests.delete(outer);
        }
        h.succeed();
    }

    /** Viewer, markers.json, a tile, live.json; 404 / 405 / HEAD / token. */
    private static void endpoints(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        int b = WebTests.FAR_TILE * 128;
        WayfinderData data = WebTests.farMap(new int[]{b + 1, b + 1});
        Path dir = WebTests.temp();
        WebHttp http = null, locked = null;
        try {
            WebTests.run(WebExport.of(server, dir, false, List.of(data)));
            String live = WebMap.liveJson(server);
            http = server(dir, "", 1000, live);
            int port = http.port();
            Reply index = get(port, "/");
            h.assertTrue(index.status() == 200 && index.header("Content-Type: text/html") && index.text().contains("Simple Maps"), "viewer at /: " + index.status());
            h.assertTrue(index.header("X-Content-Type-Options: nosniff") && index.header("Content-Security-Policy:") && index.header("Connection: close"), "safety headers");
            Reply markers = get(port, "/markers.json");
            h.assertTrue(markers.status() == 200 && markers.header("application/json")
                    && JsonParser.parseString(markers.text()).getAsJsonObject().get("format").getAsInt() == 1, "markers.json");
            String tile = "/" + WebExport.tilePath(WebExport.folder(Level.OVERWORLD), WebExport.MAX_ZOOM, WebTests.FAR_TILE, WebTests.FAR_TILE);
            Reply png = get(port, tile + "?v=1");
            h.assertTrue(png.status() == 200 && png.header("Content-Type: image/png") && png.body().length > 8 && png.body()[1] == 'P' && png.body()[2] == 'N'
                    && png.body()[3] == 'G', "tile PNG: " + png.status());
            Reply liveReply = get(port, "/live.json");
            h.assertTrue(liveReply.status() == 200 && JsonParser.parseString(liveReply.text()).getAsJsonObject().has("players")
                    && liveReply.header("Cache-Control: no-cache"), "live.json");
            h.assertTrue(get(port, "/tiles/minecraft_overworld/4/1/1.png").status() == 404, "missing tile 404");
            h.assertTrue(request(port, "POST /markers.json HTTP/1.1", "Content-Length: 0").status() == 405, "POST refused (read only)");
            h.assertTrue(request(port, "DELETE /markers.json HTTP/1.1").status() == 405 && request(port, "PUT /x HTTP/1.1").status() == 405, "no writing methods");
            Reply head = request(port, "HEAD /markers.json HTTP/1.1");
            h.assertTrue(head.status() == 200 && head.body().length == 0 && head.header("Content-Length: " + markers.body().length), "HEAD without body");
            h.assertTrue(request(port, "GET /" + "a".repeat(WebHttp.MAX_LINE + 10) + " HTTP/1.1").status() != 200, "over-long request line refused");
            h.assertTrue(request(port, "garbage").status() == 400, "malformed request 400");
            h.assertTrue(Files.isRegularFile(dir.resolve("markers.json")), "nothing deleted");
            locked = server(dir, "s3cret", 1000, live);
            int lp = locked.port();
            h.assertTrue(get(lp, "/markers.json").status() == 401 && get(lp, "/markers.json?token=wrong").status() == 401, "token required");
            h.assertTrue(get(lp, "/markers.json?v=2&token=s3cret").status() == 200 && get(lp, "/markers.json", "Authorization: Bearer s3cret").status() == 200,
                    "token by query or bearer");
            h.assertTrue(WebHttp.tokenOk("", "/", null) && !WebHttp.tokenOk("a", "/?token=b", null) && WebHttp.tokenOk("a", "/?x=1&token=a", null), "token rule");
        } finally {
            if (http != null) http.close();
            if (locked != null) locked.close();
            WebTests.delete(dir);
        }
        h.succeed();
    }

    /** Per-client budget per minute: over it 429, a new minute frees it; full table refuses new clients. */
    private static void rateLimit(GameTestHelper h) {
        WebHttp.RateLimit limit = new WebHttp.RateLimit(3);
        long t = 120_000;
        h.assertTrue(limit.allow("a", t) && limit.allow("a", t + 1) && limit.allow("a", t + 2) && !limit.allow("a", t + 3), "4th request in a minute refused");
        h.assertTrue(limit.allow("b", t + 4), "other clients count separately");
        h.assertTrue(limit.allow("a", t + 60_000), "next minute allowed again");
        WebHttp.RateLimit full = new WebHttp.RateLimit(5);
        for (int i = 0; i < WebHttp.RateLimit.MAX_CLIENTS; i++) full.allow("c" + i, t);
        h.assertTrue(!full.allow("new", t) && full.allow("c0", t), "full table refuses new clients only");
        // Slowloris: a sender that drips one byte per read runs into the total deadline, a prompt one does not.
        java.io.InputStream drip = new java.io.InputStream() {
            @Override
            public int read() {
                try { Thread.sleep(40); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return 'a';
            }
        };
        try {
            h.assertTrue(WebHttp.readLine(drip, System.nanoTime() + 200_000_000L) == null, "a slow sender hits the total deadline");
            h.assertTrue("GET / HTTP/1.1".equals(WebHttp.readLine(new java.io.ByteArrayInputStream("GET / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII)),
                    System.nanoTime() + 5_000_000_000L)), "a prompt request line is read");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Path dir = WebTests.temp();
        WebHttp http = null;
        try {
            Files.writeString(dir.resolve("markers.json"), "{}");
            http = server(dir, "", 3, null);
            boolean limited = false;
            Reply last = null;
            // 7 requests: even split over a minute boundary, one window gets 4 or more.
            for (int i = 0; i < 7; i++) {
                last = get(http.port(), "/markers.json");
                if (last.status() == 429) limited = true;
            }
            h.assertTrue(limited && last.status() == 429 && last.header("Retry-After: 60"), "live server answers 429 over the budget");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (http != null) http.close();
            WebTests.delete(dir);
        }
        h.succeed();
    }

    /** live.json shows only players who opted in (no UUIDs); everybody else stays hidden like in markers.json. */
    private static void liveOptIn(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        String name = player.getGameProfile().name();
        try {
            h.assertTrue(!WebMap.liveJson(server).contains("\"" + name + "\""), "hidden without opt-in");
            WebMap.setVisible(server, player.getUUID(), true);
            String live = WebMap.liveJson(server);
            h.assertTrue(live.contains("\"" + name + "\"") && !live.contains(player.getUUID().toString()), "opted in: name, no UUID");
            JsonObject root = JsonParser.parseString(live).getAsJsonObject();
            h.assertTrue(root.get("refreshSeconds").getAsInt() == MapsConfig.webLiveSeconds && root.has("exportedAt"), "refresh and export stamp");
        } finally {
            WebMap.setVisible(server, player.getUUID(), false);
        }
        h.succeed();
    }
}
