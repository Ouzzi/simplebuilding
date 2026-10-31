package com.simplebuilding.modules.simplelivemap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.modules.simplelivemap.web.LivemapWeb;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Game test bodies (Fabric adapter {@code ModuleGameTest}): config defaults, recording, persistence, maps, tile colours,
 * explored-only tiles and the web server (off by default, private/public, traversal, player fields, rate limit). Servers in
 * tests bind to 127.0.0.1 on a free port; fake dimension ids keep test data apart from the real map.
 */
public final class LivemapTests {
    private static final String LOCAL = "127.0.0.1", TEST_DIM = "simplelivemap:gametest";

    private LivemapTests() {}

    static Path temp() {
        try {
            return Files.createTempDirectory("simplelivemap-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void delete(Path dir) {
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }

    /** Web off, local only, private with a made token, players off; numbers clamped; groups written to the file. */
    public static void configDefaults(GameTestHelper h) {
        JsonObject saved = LivemapConfig.toJson();
        try {
            LivemapConfig.apply(new JsonObject());
            h.assertTrue(!LivemapConfig.webEnabled && "127.0.0.1".equals(LivemapConfig.bind) && LivemapConfig.port == LivemapConfig.PORT_DEFAULT,
                    "web server off, 127.0.0.1, default port");
            h.assertTrue(!LivemapConfig.isPublic() && !LivemapConfig.showPlayers, "private, players hidden by default");
            h.assertTrue(LivemapConfig.enabled && LivemapConfig.radius == 2 && LivemapConfig.maps && LivemapConfig.shading, "record + render defaults");
            h.assertTrue(!LivemapRuntime.webWanted(), "no web server wanted with the defaults");
            JsonObject json = JsonParser.parseString("{\"record\":{\"radius\":99,\"intervalTicks\":0},\"web\":{\"port\":70000,\"bind\":\" \","
                    + "\"visibility\":\"everyone\",\"token\":\"short\",\"requestsPerMinute\":1}}").getAsJsonObject();
            LivemapConfig.apply(json);
            h.assertTrue(LivemapConfig.radius == LivemapConfig.RADIUS_MAX && LivemapConfig.intervalTicks == 5 && LivemapConfig.port == 65535
                    && "127.0.0.1".equals(LivemapConfig.bind) && LivemapConfig.requestsPerMinute == LivemapConfig.RATE_MIN, "values clamped");
            h.assertTrue(!LivemapConfig.isPublic() && LivemapConfig.token.isEmpty(), "unknown visibility = private, weak token refused");
            String t = LivemapConfig.newToken();
            h.assertTrue(t.matches("[0-9a-f]{32}") && !t.equals(LivemapConfig.newToken()), "random 128-bit tokens");
            JsonObject out = LivemapConfig.toJson();
            h.assertTrue(out.has("general") && out.has("record") && out.has("render") && out.getAsJsonObject("web").has("token"), "categories in the file");
        } finally {
            LivemapConfig.apply(saved);
        }
        h.succeed();
    }

    /** Per player and as union; radius square; a chunk counts as new only once; spectators are never recorded. */
    public static void visitRecords(GameTestHelper h) {
        VisitedStore s = new VisitedStore();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        h.assertTrue(s.mark("d", a, 0, 0) && !s.mark("d", a, 0, 0) && !s.mark("d", b, 0, 0), "new once, second player adds nothing to the union");
        h.assertTrue(s.exploredBy("d", a, 0, 0) && s.exploredBy("d", b, 0, 0) && !s.exploredBy("d", a, 1, 0), "per player");
        h.assertTrue(s.mark("d", b, -33, 40) && s.explored("d", -33, 40) && !s.exploredBy("d", a, -33, 40) && !s.explored("other", -33, 40),
                "negative coordinates, dimensions apart");
        h.assertTrue(s.count("d") == 2 && s.players().size() == 2, "count of explored chunks");
        UUID c = UUID.randomUUID();
        int fresh = LivemapRuntime.recordAround(TEST_DIM, c, 1000, 1000, 2);
        VisitedStore live = LivemapRuntime.store();
        h.assertTrue(fresh == 25 && live.explored(TEST_DIM, 998, 1002) && !live.explored(TEST_DIM, 997, 1000), "radius 2 = 5 x 5 chunks");
        h.assertTrue(LivemapRuntime.recordAround(TEST_DIM, c, 1000, 1000, 2) == 0, "revisit adds nothing");
        ServerPlayer spectator = h.makeMockServerPlayerInLevel();
        spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        spectator.setPos(5000.5, 100, 5000.5);
        LivemapRuntime.record(spectator);
        h.assertTrue(!live.exploredBy(spectator.level().dimension().identifier().toString(), spectator.getUUID(), 312, 312), "spectators do not explore");
        h.succeed();
    }

    /** Saved and loaded: union, per player and hidden players survive; a broken file throws. */
    public static void visitPersistence(GameTestHelper h) {
        Path dir = temp();
        try {
            VisitedStore s = new VisitedStore();
            UUID a = UUID.randomUUID(), b = UUID.randomUUID();
            s.mark("minecraft:overworld", a, 3, -7);
            s.mark("minecraft:the_nether", b, -100, 64);
            s.setHidden(b, true);
            Path file = dir.resolve("visited.bin");
            s.save(file);
            h.assertTrue(!s.dirty() && Files.isRegularFile(file) && !Files.exists(dir.resolve("visited.bin.tmp")), "saved atomically");
            VisitedStore r = VisitedStore.load(file);
            h.assertTrue(r.exploredBy("minecraft:overworld", a, 3, -7) && r.exploredBy("minecraft:the_nether", b, -100, 64)
                    && !r.exploredBy("minecraft:overworld", b, 3, -7) && r.explored("minecraft:the_nether", -100, 64), "chunks per player back");
            h.assertTrue(r.hidden(b) && !r.hidden(a) && r.count("minecraft:overworld") == 1, "hidden players back");
            h.assertTrue(VisitedStore.load(dir.resolve("missing.bin")).dimensions().isEmpty(), "missing file = empty");
            Files.write(dir.resolve("bad.bin"), new byte[] {1, 2, 3});
            boolean threw = false;
            try {
                VisitedStore.load(dir.resolve("bad.bin"));
            } catch (IOException | RuntimeException e) {
                threw = true;
            }
            h.assertTrue(threw, "a broken file is refused");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(dir);
        }
        h.succeed();
    }

    /** A held Vanilla map: chunks with drawn pixels count as explored, empty pixels do not. */
    public static void mapItemsExplore(GameTestHelper h) {
        MapItemSavedData data = MapItemSavedData.createFresh(-3000, 7000, (byte) 0, false, false, Level.OVERWORLD);
        int x0 = data.centerX - 64, z0 = data.centerZ - 64;
        // Scale 0: one pixel per block from (x0, z0). Draw one pixel at map (20, 40).
        data.colors[40 * 128 + 20] = (byte) (MapColor.GOLD.id * 4 + 1);
        UUID p = UUID.randomUUID();
        int fresh = LivemapRuntime.recordMapData(TEST_DIM, p, data);
        VisitedStore s = LivemapRuntime.store();
        h.assertTrue(fresh == 1 && s.exploredBy(TEST_DIM, p, (x0 + 20) >> 4, (z0 + 40) >> 4), "the drawn chunk counts: " + fresh);
        h.assertTrue(!s.explored(TEST_DIM, (x0 + 100) >> 4, (z0 + 100) >> 4), "an empty part of the map does not");
        h.succeed();
    }

    /** A gold block on top: its column samples as GOLD at its height; the chunk picture lands in the tile at the right pixel. */
    public static void tileColor(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 3, 1));
        h.setBlock(new BlockPos(1, 3, 1), Blocks.GOLD_BLOCK);
        LevelChunk chunk = h.getLevel().getChunkAt(pos);
        int[] s = new int[3];
        h.assertTrue(ChunkSampler.column(h.getLevel(), chunk, pos.getX(), pos.getZ(), new BlockPos.MutableBlockPos(), s), "column sampled");
        h.assertTrue(s[0] == MapColor.GOLD.id && s[1] == pos.getY(), "top block is the gold block: id " + s[0] + " y " + s[1]);
        int[] px = ChunkSampler.sample(h.getLevel(), chunk, true);
        int got = px[(pos.getZ() & 15) * 16 + (pos.getX() & 15)];
        boolean gold = false;
        for (MapColor.Brightness b : MapColor.Brightness.values()) gold |= got == ChunkSampler.argb(MapColor.GOLD, b);
        h.assertTrue(gold && (got >>> 24) == 0xFF, "pixel is an opaque gold shade: " + Integer.toHexString(got));
        int flat = ChunkSampler.sample(h.getLevel(), chunk, false)[(pos.getZ() & 15) * 16 + (pos.getX() & 15)];
        h.assertTrue(flat == ChunkSampler.argb(MapColor.GOLD, MapColor.Brightness.NORMAL), "without shading: normal brightness");
        Path dir = temp();
        try (TileStore tiles = new TileStore(dir)) {
            int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
            h.assertTrue(tiles.putChunk("t", cx, cz, px), "queued");
            tiles.flushNow();
            int[] tile = tiles.readTile("t", TileStore.MAX_ZOOM, Math.floorDiv(pos.getX(), 128), Math.floorDiv(pos.getZ(), 128));
            h.assertTrue(tile != null && tile[Math.floorMod(pos.getZ(), 128) * 128 + Math.floorMod(pos.getX(), 128)] == got, "tile pixel = chunk pixel");
            int otherCol = (Math.floorMod(cx, 8) + 1) % 8 * 16 + 5, row = Math.floorMod(cz, 8) * 16 + 5;
            h.assertTrue(tile[row * 128 + otherCol] == 0, "other chunks of the tile stay empty");
            h.assertTrue(tiles.readTile("t", 0, Math.floorDiv(pos.getX(), 128 * 16), Math.floorDiv(pos.getZ(), 128 * 16)) != null, "pyramid down to zoom 0");
        } finally {
            delete(dir);
        }
        h.succeed();
    }

    /** Map pictures fill only explored chunks and never overwrite a real chunk picture. */
    public static void tileExploredOnly(GameTestHelper h) {
        Path dir = temp();
        try (TileStore tiles = new TileStore(dir)) {
            int[] real = new int[256];
            Arrays.fill(real, 0xFF112233);
            tiles.putChunk("t", 0, 0, real);
            byte[] colors = new byte[128 * 128];
            Arrays.fill(colors, (byte) (MapColor.GOLD.id * 4 + 1));
            // Map at blocks 0..127: chunks (0,0) (real picture) and (1,0) explored, everything else not.
            Set<Long> explored = Set.of(TileStore.chunkKey(0, 0), TileStore.chunkKey(1, 0));
            tiles.putMap("t", 0, 0, 0, colors, explored::contains);
            tiles.flushNow();
            int[] tile = tiles.readTile("t", TileStore.MAX_ZOOM, 0, 0);
            int gold = ChunkSampler.argbPacked(MapColor.GOLD.id * 4 + 1);
            h.assertTrue(tile != null && tile[5 * 128 + 5] == 0xFF112233, "real chunk picture kept");
            h.assertTrue(tile[5 * 128 + 20] == gold, "explored chunk without picture gets the map colour");
            h.assertTrue(tile[5 * 128 + 40] == 0 && tile[40 * 128 + 5] == 0, "unexplored chunks stay transparent");
        } finally {
            delete(dir);
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- web

    record Reply(int status, String headers, byte[] body) {
        String text() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    static Reply request(int port, String requestLine, String... headers) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getByName(LOCAL), port), 2000);
            s.setSoTimeout(4000);
            StringBuilder b = new StringBuilder(requestLine).append("\r\nHost: localhost\r\n");
            for (String hd : headers) b.append(hd).append("\r\n");
            s.getOutputStream().write(b.append("\r\n").toString().getBytes(StandardCharsets.ISO_8859_1));
            s.getOutputStream().flush();
            byte[] all = s.getInputStream().readAllBytes();
            for (int i = 0; i + 3 < all.length; i++) {
                if (all[i] == '\r' && all[i + 1] == '\n' && all[i + 2] == '\r' && all[i + 3] == '\n') {
                    String head = new String(all, 0, i, StandardCharsets.ISO_8859_1);
                    return new Reply(Integer.parseInt(head.split(" ")[1]), head, Arrays.copyOfRange(all, i + 4, all.length));
                }
            }
            return new Reply(-1, "", new byte[0]);
        } catch (IOException | RuntimeException e) {
            return new Reply(-1, e.toString(), new byte[0]);
        }
    }

    static Reply get(int port, String path, String... headers) {
        return request(port, "GET " + path + " HTTP/1.1", headers);
    }

    private static LivemapWeb server(Path tiles, String token, boolean open, int perMinute, String live) {
        try {
            return LivemapWeb.start(new LivemapWeb.Settings(LOCAL, 0, token, open, perMinute, tiles), () -> live, () -> true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Default config: no server runs on the test server; switching web.enabled on opens 127.0.0.1, off closes it again. */
    public static void httpOffDefault(GameTestHelper h) {
        var srv = h.getLevel().getServer();
        JsonObject saved = LivemapConfig.toJson();
        try {
            LivemapConfig.apply(new JsonObject());
            LivemapRuntime.syncWeb(srv);
            h.assertTrue(LivemapRuntime.web() == null, "no web server by default");
            LivemapConfig.webEnabled = true;
            LivemapConfig.port = freePort();
            LivemapConfig.token = LivemapConfig.newToken();
            LivemapRuntime.syncWeb(srv);
            LivemapWeb web = LivemapRuntime.web();
            h.assertTrue(web != null && web.running() && InetAddress.getByName(web.settings().bind()).isLoopbackAddress(), "on: bound to the local machine");
            int port = web.port();
            h.assertTrue(get(port, "/live.json").status() == 401, "private by default: live data needs the key");
            LivemapConfig.webEnabled = false;
            LivemapRuntime.syncWeb(srv);
            h.assertTrue(LivemapRuntime.web() == null && !web.running(), "off: closed");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            LivemapConfig.apply(saved);
            LivemapRuntime.syncWeb(srv);
        }
        h.succeed();
    }

    private static int freePort() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0, 1, InetAddress.getByName(LOCAL))) {
            return s.getLocalPort();
        }
    }

    /** Private: page open, data only with the right Bearer key (never with an empty one); public: data for everyone. */
    public static void httpVisibility(GameTestHelper h) {
        Path dir = temp();
        LivemapWeb priv = null, closed = null, open = null;
        try {
            Files.createDirectories(dir.resolve("minecraft_overworld/4/0"));
            Files.write(dir.resolve("minecraft_overworld/4/0/0.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G'});
            String token = LivemapConfig.newToken();
            priv = server(dir, token, false, 1000, "{\"players\":[]}");
            int p = priv.port();
            h.assertTrue(get(p, "/").status() == 200 && get(p, "/").text().contains("<canvas"), "viewer page without key");
            h.assertTrue(get(p, "/live.json").status() == 401 && get(p, "/tiles/minecraft_overworld/4/0/0.png").status() == 401, "data needs the key");
            h.assertTrue(get(p, "/live.json?token=" + token).status() == 401, "no key in the URL query (stays out of logs)");
            h.assertTrue(get(p, "/live.json", "Authorization: Bearer wrong" + token).status() == 401, "wrong key refused");
            h.assertTrue(get(p, "/live.json", "Authorization: Bearer " + token).status() == 200
                    && get(p, "/tiles/minecraft_overworld/4/0/0.png", "Authorization: Bearer " + token).status() == 200, "right key opens");
            h.assertTrue(get(p, "/tiles/minecraft_overworld/4/9/9.png", "Authorization: Bearer " + token).status() == 204, "unexplored tile = empty");
            h.assertTrue(request(p, "POST /live.json HTTP/1.1").status() == 405, "read only");
            closed = server(dir, "", false, 1000, "{}");
            h.assertTrue(get(closed.port(), "/live.json", "Authorization: Bearer ").status() == 401, "private without a token stays closed");
            open = server(dir, "", true, 1000, "{\"players\":[]}");
            h.assertTrue(get(open.port(), "/live.json").status() == 200 && get(open.port(), "/tiles/minecraft_overworld/4/0/0.png").status() == 200, "public: open");
            Reply r = get(open.port(), "/live.json");
            h.assertTrue(r.headers().contains("Content-Security-Policy") && r.headers().contains("nosniff"), "security headers");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            for (LivemapWeb w : new LivemapWeb[] {priv, closed, open}) if (w != null) w.close();
            delete(dir);
        }
        h.succeed();
    }

    /** Only the fixed routes: no "..", no encodings, no other files, no links out of the tile folder. */
    public static void httpTraversal(GameTestHelper h) {
        for (String bad : List.of("/../visited.bin", "/tiles/../../visited.bin", "/%2e%2e/x.png", "/..%2fx.png", "/tiles/a/4/1/1.png/../../../x",
                "\\..\\x.png", "//etc/passwd", "/level.dat", "/visited.bin", "/tiles/a/9/1/1.png", "/tiles/a/4/1/1.txt", "/tiles/A/4/1/1.png",
                "/index.html/", "/tiles/a/4/1/1.png\u0000", "", "http://localhost/live.json", "/markers.json")) {
            h.assertTrue(LivemapWeb.route(bad) == null, "refused: " + bad);
        }
        h.assertTrue("index.html".equals(LivemapWeb.route("/")) && "live.json".equals(LivemapWeb.route("/live.json?x=1"))
                && "tiles/minecraft_overworld/4/-3/7.png".equals(LivemapWeb.route("/tiles/minecraft_overworld/4/-3/7.png")), "fixed routes pass");
        Path outer = temp();
        Path dir = outer.resolve("tiles");
        LivemapWeb web = null;
        try {
            Files.createDirectories(dir.resolve("a/4/0"));
            Files.writeString(outer.resolve("secret.png"), "secret");
            boolean linked;
            try {
                Files.createSymbolicLink(dir.resolve("a/4/0/0.png"), outer.resolve("secret.png"));
                linked = true;
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                linked = false;
            }
            web = server(dir, "", true, 1000, "{}");
            for (String bad : List.of("/../secret.png", "/tiles/../secret.png", "/%2e%2e/secret.png", "/secret.png")) {
                Reply r = get(web.port(), bad);
                h.assertTrue(r.status() == 404 && !r.text().contains("secret"), "not served: " + bad + " -> " + r.status());
            }
            if (linked) h.assertTrue(!get(web.port(), "/tiles/a/4/0/0.png").text().contains("secret"), "a link out of the folder is not followed");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if (web != null) web.close();
            delete(outer);
        }
        h.succeed();
    }

    /** live.json: players only with showPlayers, only name/dimension/x/z, hidden players and spectators never. */
    public static void httpPlayersFields(GameTestHelper h) {
        var srv = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        String name = player.getGameProfile().name();
        boolean before = LivemapConfig.showPlayers;
        try {
            LivemapConfig.showPlayers = false;
            h.assertTrue(!LivemapRuntime.buildLive(srv).contains("\"" + name + "\""), "players off by default");
            LivemapConfig.showPlayers = true;
            JsonObject live = JsonParser.parseString(LivemapRuntime.buildLive(srv)).getAsJsonObject();
            JsonObject me = null;
            for (JsonElement e : live.getAsJsonArray("players")) if (name.equals(e.getAsJsonObject().get("name").getAsString())) me = e.getAsJsonObject();
            h.assertTrue(me != null && me.keySet().equals(Set.of("name", "dimension", "x", "z")), "only name, dimension, x, z: " + me);
            h.assertTrue(!live.toString().contains(player.getUUID().toString()), "no UUID");
            LivemapRuntime.store().setHidden(player.getUUID(), true);
            h.assertTrue(!LivemapRuntime.buildLive(srv).contains("\"" + name + "\""), "/simplelivemap hide works");
            LivemapRuntime.store().setHidden(player.getUUID(), false);
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertTrue(!LivemapRuntime.buildLive(srv).contains("\"" + name + "\""), "spectators never");
        } finally {
            LivemapConfig.showPlayers = before;
            LivemapRuntime.store().setHidden(player.getUUID(), false);
        }
        h.succeed();
    }

    /** Requests per address and minute: over the budget = 429 before the request is read. */
    public static void httpRateLimit(GameTestHelper h) {
        LivemapWeb.RateLimit limit = new LivemapWeb.RateLimit(3);
        h.assertTrue(limit.allow("a", 0) && limit.allow("a", 1) && limit.allow("a", 2) && !limit.allow("a", 3) && limit.allow("b", 4), "3 per minute and address");
        h.assertTrue(limit.allow("a", 60_000), "next minute fresh");
        Path dir = temp();
        LivemapWeb web = server(dir, "", true, LivemapConfig.RATE_MIN, "{}");
        try {
            int refused = 0;
            // Twice at most: a minute boundary in the middle of the burst resets the window once.
            for (int round = 0; round < 2 && refused == 0; round++) {
                for (int i = 0; i < LivemapConfig.RATE_MIN + 5; i++) if (get(web.port(), "/live.json").status() == 429) refused++;
            }
            h.assertTrue(refused >= 1, "budget enforced: " + refused + " refused");
        } finally {
            web.close();
            delete(dir);
        }
        h.succeed();
    }
}
