package com.simplemaps.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.Protection;
import com.simplemaps.MapsConfig;
import com.simplemaps.Reveal;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import com.simplemaps.web.WebExport;
import com.simplemaps.web.WebMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

/** Web map export (stage 1): tiles, throttle, exploration, claims, opt-in players, autoOffWith, command rights. */
final class WebTests {
    /** Far away so the chunks are never loaded in the test world (the export then keeps the map's pixels). */
    private static final int FAR_TILE = 4000;

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("web_tile_colors", WebTests::tileColors);
        all.put("web_throttle", WebTests::throttle);
        all.put("web_explored_only", WebTests::exploredOnly);
        all.put("web_hide_claims", WebTests::hideClaims);
        all.put("web_markers_opt_in", WebTests::markersOptIn);
        all.put("web_auto_off", WebTests::autoOff);
        all.put("web_command_permissions", WebTests::commandPermissions);
    }

    private WebTests() {}

    private static Path temp() {
        try {
            return Files.createTempDirectory("simplemaps-web-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void delete(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
        }
    }

    private static void run(WebExport job) {
        for (int i = 0; i < 10000 && !job.done(); i++) job.step(1 << 20);
    }

    private static BufferedImage png(Path dir, String folder, int z, int x, int y) {
        try {
            Path file = dir.resolve(WebExport.tilePath(folder, z, x, y));
            return Files.isRegularFile(file) ? ImageIO.read(file.toFile()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean anyBrightness(int argb, MapColor color) {
        for (MapColor.Brightness b : MapColor.Brightness.values()) if (argb == WebExport.argb(color.getPackedId(b) & 0xFF)) return true;
        return false;
    }

    /** A map of the overworld with one explored pixel per given block {x, z} (far away tiles, colour gold). */
    private static WayfinderData farMap(int[]... blocks) {
        WayfinderData data = new WayfinderData();
        data.bind(Level.OVERWORLD);
        for (int[] b : blocks) data.set(b[0], b[1], MapColor.GOLD.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 100, true);
        return data;
    }

    private static void tileColors(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 1, 1), pos = h.absolutePos(rel), lapis = pos.east();
        h.setBlock(rel, Blocks.GOLD_BLOCK);
        h.setBlock(rel.east(), Blocks.LAPIS_BLOCK);
        net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(h.getLevel().getChunkAt(pos),
                java.util.EnumSet.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE));
        WayfinderData data = new WayfinderData();
        data.bind(h.getLevel().dimension());
        for (int phase = 0; phase < 16; phase++) Reveal.step(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), data, 8, phase);
        h.assertTrue(data.color(pos.getX(), pos.getZ()) >> 2 == MapColor.GOLD.id, "map explored the gold block");
        // Changed after exploring: the export reads loaded chunks fresh, so the web tile shows emerald, the map still gold.
        h.setBlock(rel, Blocks.EMERALD_BLOCK);
        Path dir = temp();
        try {
            WebExport job = WebExport.of(h.getLevel().getServer(), dir, false, List.of(data));
            run(job);
            h.assertTrue(job.done() && job.tilesWritten() >= 1, "export finished with tiles");
            String folder = WebExport.folder(h.getLevel().dimension());
            int tx = Math.floorDiv(pos.getX(), 128), tz = Math.floorDiv(pos.getZ(), 128);
            BufferedImage img = png(dir, folder, WebExport.MAX_ZOOM, tx, tz);
            h.assertTrue(img != null && img.getWidth() == 128 && img.getHeight() == 128, "1:1 tile png written");
            int emerald = img.getRGB(Math.floorMod(pos.getX(), 128), Math.floorMod(pos.getZ(), 128));
            h.assertTrue(anyBrightness(emerald, MapColor.EMERALD), "fresh block from the loaded chunk in map palette, got " + Integer.toHexString(emerald));
            BufferedImage lapisTile = png(dir, folder, WebExport.MAX_ZOOM, Math.floorDiv(lapis.getX(), 128), Math.floorDiv(lapis.getZ(), 128));
            h.assertTrue(lapisTile != null, "tile of the lapis block written");
            int blue = lapisTile.getRGB(Math.floorMod(lapis.getX(), 128), Math.floorMod(lapis.getZ(), 128));
            h.assertTrue(anyBrightness(blue, MapColor.LAPIS), "lapis in map palette, got " + Integer.toHexString(blue));
            h.assertTrue((blue & 0xFF) > (blue >> 16 & 0xFF) && blue >>> 24 == 0xFF, "ARGB channel order (lapis is blue, opaque)");
            h.assertTrue(data.color(pos.getX(), pos.getZ()) >> 2 == MapColor.GOLD.id, "the export never writes into the map");
            boolean unknownClear = true;
            for (int i = 0; i < 128 * 128; i++) {
                int x = tx * 128 + i % 128, z = tz * 128 + i / 128;
                if (data.color(x, z) == 0 && img.getRGB(i % 128, i / 128) >>> 24 != 0) unknownClear = false;
            }
            h.assertTrue(unknownClear, "unexplored pixels stay transparent");
            for (int z = 0; z < WebExport.MAX_ZOOM; z++) {
                int f = 1 << (WebExport.MAX_ZOOM - z);
                h.assertTrue(png(dir, folder, z, Math.floorDiv(tx, f), Math.floorDiv(tz, f)) != null, "pyramid tile at zoom " + z);
            }
            h.assertTrue(Files.isRegularFile(dir.resolve("markers.json")) && Files.isRegularFile(dir.resolve("index.html")), "markers.json and viewer written");
        } finally {
            delete(dir);
        }
        h.succeed();
    }

    private static void throttle(GameTestHelper h) {
        int b = FAR_TILE * 128;
        WayfinderData data = farMap(new int[]{b + 1, b + 1}, new int[]{b + 129, b + 1});
        Path dir = temp();
        try {
            WebExport job = WebExport.of(h.getLevel().getServer(), dir, false, List.of(data));
            h.assertTrue(job.tilesTotal() == 2, "two explored tiles");
            h.assertTrue(job.step(4) == 4 && job.chunksDone() == 4, "a step reads at most its budget of chunks");
            h.assertTrue(job.step(4) == 4 && job.chunksDone() == 8 && job.tilesDone() == 0, "next step continues, tile not yet complete");
            h.assertTrue(job.step(60) == 60 && job.tilesDone() == 1, "64 chunks complete one tile");
            run(job);
            h.assertTrue(job.done() && job.chunksDone() == 2 * WebExport.CHUNKS_PER_TILE, "every chunk of the explored tiles read once");
            // A single explored pixel at an odd position survives the pyramid up to zoom 0.
            String folder = WebExport.folder(Level.OVERWORLD);
            int f = 1 << WebExport.MAX_ZOOM;
            h.assertTrue(png(dir, folder, 0, Math.floorDiv(FAR_TILE, f), Math.floorDiv(FAR_TILE, f)) != null, "single odd pixel kept at zoom 0");
        } finally {
            delete(dir);
        }
        JsonObject json = new JsonObject();
        json.addProperty("webChunksPerTick", 100000);
        json.addProperty("webIntervalMinutes", -5);
        try {
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.webChunksPerTick == MapsConfig.WEB_CHUNKS_MAX && MapsConfig.webIntervalMinutes == 0, "web bounds clamped");
            json.addProperty("webChunksPerTick", 0);
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.webChunksPerTick == MapsConfig.WEB_CHUNKS_MIN, "at least one chunk per tick");
        } finally {
            MapsConfig.apply(new JsonObject());
        }
        h.assertTrue(!MapsConfig.webEnabled && !MapsConfig.webHideClaims && MapsConfig.webIntervalMinutes == 0
                && MapsConfig.webChunksPerTick == MapsConfig.WEB_CHUNKS_DEFAULT, "defaults: export off, no claims hiding, no periodic export");
        h.succeed();
    }

    private static void exploredOnly(GameTestHelper h) {
        int b = FAR_TILE * 128;
        WayfinderData data = farMap(new int[]{b + 5, b + 7});
        WayfinderData unbound = new WayfinderData();
        unbound.set(b + 300, b + 300, MapColor.GOLD.getPackedId(MapColor.Brightness.NORMAL) & 0xFF, 100, true);
        Path dir = temp();
        try {
            WebExport job = WebExport.of(h.getLevel().getServer(), dir, false, List.of(data, unbound));
            h.assertTrue(job.tilesTotal() == 1 && job.dimensions() == 1, "only the explored tile of the bound map");
            run(job);
            String folder = WebExport.folder(Level.OVERWORLD);
            long files;
            try (Stream<Path> walk = Files.walk(dir.resolve("tiles/" + folder + "/" + WebExport.MAX_ZOOM))) {
                files = walk.filter(Files::isRegularFile).count();
            }
            h.assertTrue(files == 1, "exactly one 1:1 tile file, got " + files);
            BufferedImage img = png(dir, folder, WebExport.MAX_ZOOM, FAR_TILE, FAR_TILE);
            h.assertTrue(img != null && img.getRGB(5, 7) == WebExport.argb(MapColor.GOLD.getPackedId(MapColor.Brightness.NORMAL)),
                    "explored pixel kept from the map (chunk not loaded)");
            h.assertTrue(img.getRGB(6, 7) >>> 24 == 0, "unexplored neighbour pixel transparent");
            h.assertTrue(png(dir, folder, WebExport.MAX_ZOOM, FAR_TILE + 1, FAR_TILE) == null, "unexplored tile has no file");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            delete(dir);
        }
        h.succeed();
    }

    private static void hideClaims(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        int b = FAR_TILE * 128, claimedCx = b >> 4, claimedCz = b >> 4;
        WayfinderData data = farMap(new int[]{b + 1, b + 1}, new int[]{b + 17, b + 1});
        Protection.register(server, "simplemaps_web_test", t -> !((t.x() >> 4) == claimedCx && (t.z() >> 4) == claimedCz));
        Path dir = temp();
        try {
            String folder = WebExport.folder(Level.OVERWORLD);
            run(WebExport.of(server, dir, true, List.of(data)));
            BufferedImage hidden = png(dir, folder, WebExport.MAX_ZOOM, FAR_TILE, FAR_TILE);
            h.assertTrue(hidden != null && hidden.getRGB(1, 1) >>> 24 == 0, "claimed chunk stays empty with webHideClaims");
            h.assertTrue(hidden.getRGB(17, 1) >>> 24 == 0xFF, "free chunk next to it is shown");
            run(WebExport.of(server, dir, false, List.of(data)));
            BufferedImage shown = png(dir, folder, WebExport.MAX_ZOOM, FAR_TILE, FAR_TILE);
            h.assertTrue(shown != null && shown.getRGB(1, 1) >>> 24 == 0xFF, "without the switch the claimed chunk is shown");
        } finally {
            Protection.unregister(server, "simplemaps_web_test");
            delete(dir);
        }
        h.succeed();
    }

    private static int players(MinecraftServer server, Path dir, WayfinderData data, String name) {
        WebExport job = WebExport.of(server, dir, false, List.of(data));
        run(job);
        try {
            JsonArray players = com.google.gson.JsonParser.parseString(Files.readString(dir.resolve("markers.json"))).getAsJsonObject().getAsJsonArray("players");
            int found = 0;
            for (var p : players) if (p.getAsJsonObject().get("name").getAsString().equals(name)) found++;
            return players.isEmpty() ? 0 : found == 0 ? -players.size() : found;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void markersOptIn(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        String name = player.getGameProfile().name();
        WayfinderData data = farMap(new int[]{FAR_TILE * 128 + 1, FAR_TILE * 128 + 1});
        Path dir = temp();
        try {
            h.assertTrue(!WebMap.visible(server, player.getUUID()), "players are hidden by default");
            h.assertTrue(players(server, dir, data, name) == 0, "markers.json has no players without opt-in");
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), "simplemaps web show");
            h.assertTrue(WebMap.visible(server, player.getUUID()), "/simplemaps web show switches the player on");
            h.assertTrue(players(server, dir, data, name) == 1, "opted-in player is in markers.json");
            String json = Files.readString(dir.resolve("markers.json"));
            h.assertTrue(!json.contains(player.getUUID().toString()), "no UUIDs in markers.json");
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), "simplemaps web hide");
            h.assertTrue(!WebMap.visible(server, player.getUUID()) && players(server, dir, data, name) == 0, "/simplemaps web hide removes the player");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            WebMap.setVisible(server, player.getUUID(), false);
            delete(dir);
        }
        h.succeed();
    }

    private static void autoOff(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        h.assertTrue(MapsConfig.WEB_AUTO_OFF_DEFAULT.containsAll(List.of("bluemap", "dynmap", "squaremap", "pl3xmap", "journeymap")), "default autoOffWith list");
        h.assertTrue("dynmap".equals(WebMap.autoOffBy(List.of("bluemap", "dynmap"), id -> id.equals("dynmap"))), "first loaded partner wins");
        h.assertTrue(WebMap.autoOffBy(MapsConfig.WEB_AUTO_OFF_DEFAULT, id -> false) == null, "no partner, no auto off");
        Predicate<String> saved = SimpleMaps.modLoaded;
        boolean enabled = MapsConfig.webEnabled;
        try {
            MapsConfig.webEnabled = false;
            h.assertTrue(WebMap.start(server) == WebMap.Start.DISABLED && WebMap.running(server) == null, "export refused while webEnabled is off");
            MapsConfig.webEnabled = true;
            SimpleMaps.modLoaded = id -> id.equals("squaremap");
            h.assertTrue(!WebMap.active() && "squaremap".equals(WebMap.autoOff()), "simulated squaremap switches the web export off");
            h.assertTrue(WebMap.start(server) == WebMap.Start.AUTO_OFF && WebMap.running(server) == null, "export refused under autoOffWith");
            SimpleMaps.modLoaded = id -> false;
            h.assertTrue(WebMap.active() == MapsConfig.enabled, "without a partner the switch decides");
        } finally {
            SimpleMaps.modLoaded = saved;
            MapsConfig.webEnabled = enabled;
        }
        h.succeed();
    }

    private static void commandPermissions(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        var root = server.getCommands().getDispatcher().getRoot().getChild("simplemaps");
        h.assertTrue(root != null && root.getChild("web") != null, "/simplemaps web registered");
        var web = root.getChild("web");
        var none = server.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
        var gm = server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
        for (String admin : List.of("export", "status")) {
            h.assertTrue(web.getChild(admin) != null && !web.getChild(admin).canUse(none) && web.getChild(admin).canUse(gm), admin + " needs game master rights");
        }
        for (String own : List.of("show", "hide")) {
            h.assertTrue(web.getChild(own) != null && web.getChild(own).canUse(none), own + " is open to every player");
        }
        boolean enabled = MapsConfig.webEnabled;
        try {
            MapsConfig.webEnabled = false;
            server.getCommands().performPrefixedCommand(gm.withSuppressedOutput(), "simplemaps web export");
            h.assertTrue(WebMap.running(server) == null, "export command does nothing while webEnabled is off");
        } finally {
            MapsConfig.webEnabled = enabled;
        }
        h.succeed();
    }
}
