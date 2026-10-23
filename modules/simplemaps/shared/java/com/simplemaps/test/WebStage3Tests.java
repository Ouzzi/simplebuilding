package com.simplemaps.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.framework.api.Protection;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderMapItem;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.web.WebExport;
import com.simplemaps.web.WebMap;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

/** Web map stage 3: dirty tiles (only changed tiles again), pixelated claims, shared waypoints. */
final class WebStage3Tests {
    private static final int B = WebTests.FAR_TILE * 128;
    private static final String FOLDER = WebExport.folder(Level.OVERWORLD);

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("web_dirty_tiles", WebStage3Tests::dirtyTiles);
        all.put("web_dirty_block_change", WebStage3Tests::dirtyBlockChange);
        all.put("web_claims_pixelate", WebStage3Tests::claimsPixelate);
        all.put("web_waypoints", WebStage3Tests::waypoints);
    }

    private WebStage3Tests() {}

    private static WebExport run(MinecraftServer server, Path dir, WebExport.Claims claims, WayfinderData data, WebExport.State previous, Map<String, Set<Long>> dirty) {
        WebExport job = WebExport.of(server, dir, claims, List.of(data), previous, dirty);
        WebTests.run(job);
        return job;
    }

    private static Set<Long> keys(WebExport job) {
        return job.rendered(Level.OVERWORLD);
    }

    private static BufferedImage tile(Path dir, int tx) {
        try {
            Path file = dir.resolve(WebExport.tilePath(FOLDER, WebExport.MAX_ZOOM, tx, WebTests.FAR_TILE));
            return Files.isRegularFile(file) ? ImageIO.read(file.toFile()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static int packed(MapColor color) {
        return color.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
    }

    /** Owner request a-webmap3: after a full export only the changed tile is rendered again, nothing when nothing changed. */
    private static void dirtyTiles(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        WayfinderData data = WebTests.farMap(new int[]{B + 1, B + 1}, new int[]{B + 129, B + 1}, new int[]{B + 257, B + 1});
        long a = WayfinderData.key(WebTests.FAR_TILE, WebTests.FAR_TILE), b = WayfinderData.key(WebTests.FAR_TILE + 1, WebTests.FAR_TILE),
                c = WayfinderData.key(WebTests.FAR_TILE + 2, WebTests.FAR_TILE);
        Path dir = WebTests.temp();
        try {
            WebExport full = run(server, dir, WebExport.Claims.SHOW, data, null, null);
            h.assertTrue(!full.incremental() && full.tilesRendered() == 3 && full.tilesWritten() == 3, "first export renders every tile");
            WebExport same = run(server, dir, WebExport.Claims.SHOW, data, full.state(), Map.of());
            h.assertTrue(same.incremental() && same.tilesRendered() == 0 && same.tilesDone() == 3, "nothing changed: no tile rendered, all checked");
            h.assertTrue(same.tilesWritten() == 3 && same.chunksDone() == 0, "unchanged tiles keep their files, no chunk read");
            data.set(B + 140, B + 9, packed(MapColor.LAPIS), 100, true);
            WebExport pixel = run(server, dir, WebExport.Claims.SHOW, data, same.state(), Map.of());
            h.assertTrue(pixel.tilesRendered() == 1 && keys(pixel).equals(Set.of(b)), "a map pixel in tile B: exactly B again, got " + keys(pixel));
            BufferedImage img = tile(dir, WebTests.FAR_TILE + 1);
            h.assertTrue(img != null && img.getRGB(12, 9) == WebExport.argb(packed(MapColor.LAPIS)), "tile B shows the new pixel");
            WebExport block = run(server, dir, WebExport.Claims.SHOW, data, pixel.state(), Map.of(FOLDER, Set.of(c, WayfinderData.key(-5, -5))));
            h.assertTrue(block.tilesRendered() == 1 && keys(block).equals(Set.of(c)), "a block change in tile C: exactly C (unexplored dirty keys ignored), got " + keys(block));
            WebExport other = run(server, dir, WebExport.Claims.HIDE, data, block.state(), Map.of());
            h.assertTrue(!other.incremental() && other.tilesRendered() == 3, "another claim mode renders everything again");
            WebExport otherDir = run(server, WebTests.temp(), WebExport.Claims.HIDE, data, other.state(), Map.of());
            h.assertTrue(!otherDir.incremental(), "another folder renders everything");
            WebTests.delete(otherDir.dir());
            h.assertTrue(keys(full).contains(a), "tile A was in the first export");
        } finally {
            WebTests.delete(dir);
        }
        h.succeed();
    }

    /** A real block change (mixin on ServerLevel.sendBlockUpdated) marks its tile while the web map is on. */
    private static void dirtyBlockChange(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        BlockPos rel = new BlockPos(1, 2, 1), pos = h.absolutePos(rel);
        long key = WayfinderData.key(Math.floorDiv(pos.getX(), 128), Math.floorDiv(pos.getZ(), 128));
        boolean enabled = MapsConfig.webEnabled;
        try {
            MapsConfig.webEnabled = true;
            WebMap.tick(server);
            h.setBlock(rel, Blocks.GOLD_BLOCK);
            h.assertTrue(WebMap.dirty(server, h.getLevel().dimension(), key), "placing a block marks its tile for the next export");
        } finally {
            MapsConfig.webEnabled = enabled;
        }
        h.succeed();
    }

    /** webClaimStyle pixelate: a protected chunk shows one colour; a new claim re-renders exactly its tile. */
    private static void claimsPixelate(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        int claimedCx = B >> 4, claimedCz = B >> 4;
        WayfinderData data = WebTests.farMap(new int[]{B + 1, B + 1}, new int[]{B + 2, B + 1}, new int[]{B + 3, B + 1}, new int[]{B + 17, B + 1},
                new int[]{B + 129, B + 1});
        data.set(B + 4, B + 1, packed(MapColor.LAPIS), 100, true);
        h.assertTrue(MapsConfig.WEB_CLAIM_HIDE.equals(MapsConfig.webClaimStyle) && !MapsConfig.webWaypoints, "defaults: claims hidden (not pixelated), no waypoints");
        h.assertTrue(WebExport.Claims.of(false, MapsConfig.WEB_CLAIM_PIXELATE) == WebExport.Claims.SHOW
                && WebExport.Claims.of(true, MapsConfig.WEB_CLAIM_PIXELATE) == WebExport.Claims.PIXELATE
                && WebExport.Claims.of(true, MapsConfig.WEB_CLAIM_HIDE) == WebExport.Claims.HIDE, "style only matters with webHideClaims");
        JsonObject json = new JsonObject();
        json.addProperty("webClaimStyle", " Pixelate ");
        try {
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.WEB_CLAIM_PIXELATE.equals(MapsConfig.webClaimStyle), "config reads pixelate");
            json.addProperty("webClaimStyle", "blur");
            MapsConfig.apply(json);
            h.assertTrue(MapsConfig.WEB_CLAIM_HIDE.equals(MapsConfig.webClaimStyle), "unknown style falls back to hide");
        } finally {
            MapsConfig.apply(new JsonObject());
        }
        Path dir = WebTests.temp();
        try {
            WebExport before = run(server, dir, WebExport.Claims.PIXELATE, data, null, null);
            h.assertTrue(tile(dir, WebTests.FAR_TILE).getRGB(4, 1) == WebExport.argb(packed(MapColor.LAPIS)), "no claim yet: lapis pixel shown");
            Protection.register(server, "simplemaps_web_pixel_test", t -> !((t.x() >> 4) == claimedCx && (t.z() >> 4) == claimedCz));
            try {
                WebExport after = run(server, dir, WebExport.Claims.PIXELATE, data, before.state(), Map.of());
                h.assertTrue(after.incremental() && keys(after).equals(Set.of(WayfinderData.key(WebTests.FAR_TILE, WebTests.FAR_TILE))),
                        "a new claim re-renders exactly its tile, got " + keys(after));
                BufferedImage img = tile(dir, WebTests.FAR_TILE);
                int gold = WebExport.argb(packed(MapColor.GOLD));
                h.assertTrue(img.getRGB(4, 1) == gold && img.getRGB(1, 1) == gold, "claimed chunk: every explored pixel in its most common colour");
                h.assertTrue(img.getRGB(5, 1) >>> 24 == 0, "unexplored pixels of the claimed chunk stay transparent");
                h.assertTrue(img.getRGB(17, 1) == gold, "free chunk next to it unchanged");
                run(server, dir, WebExport.Claims.HIDE, data, after.state(), Map.of());
                h.assertTrue(tile(dir, WebTests.FAR_TILE).getRGB(4, 1) >>> 24 == 0, "hide style: claimed chunk empty");
            } finally {
                Protection.unregister(server, "simplemaps_web_pixel_test");
            }
        } finally {
            WebTests.delete(dir);
        }
        h.succeed();
    }

    private static JsonArray waypointsOf(MinecraftServer server, Path dir, boolean hideClaims, WayfinderData data) {
        WebTests.run(WebExport.of(server, dir, hideClaims, List.of(data)));
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dir.resolve("markers.json"))).getAsJsonObject();
            return root.has("waypoints") ? root.getAsJsonArray("waypoints") : null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Only waypoints the holder shared, only with webWaypoints, only explored and unprotected, without UUID or player name. */
    private static void waypoints(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(MapsItems.WAYFINDER_MAP);
        int id = WayfinderMapItem.ensureId(stack, server);
        WayfinderData data = WayfinderData.get(server, id);
        data.bind(Level.OVERWORLD);
        data.set(B + 5, B + 7, packed(MapColor.GOLD), 100, true);
        stack.set(MapsComponents.WAYPOINTS, new Waypoints(List.of(
                new Waypoint(0, B + 5, B + 7, "Home", 0xFF3AB3DA, null, 1),
                Waypoint.fresh(1, B + 128 * 5, B + 7))));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        boolean saved = MapsConfig.webWaypoints;
        Path dir = WebTests.temp();
        try {
            var cmd = player.createCommandSourceStack().withSuppressedOutput();
            MapsConfig.webWaypoints = false;
            server.getCommands().performPrefixedCommand(cmd, "simplemaps web waypoints share");
            h.assertTrue(waypointsOf(server, dir, false, data) == null, "webWaypoints off: no waypoints in markers.json");
            WebMap.unshare(server, player.getUUID(), -1);
            MapsConfig.webWaypoints = true;
            h.assertTrue(waypointsOf(server, dir, false, data).isEmpty(), "nothing shared: no waypoints");
            server.getCommands().performPrefixedCommand(cmd, "simplemaps web waypoints share");
            JsonArray list = waypointsOf(server, dir, false, data);
            h.assertTrue(list.size() == 1, "only the waypoint on an explored tile, got " + list);
            JsonObject w = list.get(0).getAsJsonObject();
            h.assertTrue(w.get("x").getAsInt() == B + 5 && w.get("z").getAsInt() == B + 7 && w.get("name").getAsString().equals("Home")
                    && w.get("color").getAsString().equals("#3ab3da") && w.get("dimension").getAsString().equals(Level.OVERWORLD.identifier().toString()),
                    "position, name, colour, dimension");
            String text = Files.readString(dir.resolve("markers.json"));
            h.assertTrue(!text.contains(player.getUUID().toString()) && !text.contains(player.getGameProfile().name()), "no UUID, no player name");
            stack.set(MapsComponents.WAYPOINTS, Waypoints.EMPTY);
            h.assertTrue(waypointsOf(server, dir, false, data).size() == 1, "a snapshot: later changes on the stack need another share");
            int cx = (B + 5) >> 4, cz = (B + 7) >> 4;
            Protection.register(server, "simplemaps_web_waypoint_test", t -> !((t.x() >> 4) == cx && (t.z() >> 4) == cz));
            try {
                h.assertTrue(waypointsOf(server, dir, true, data).isEmpty(), "webHideClaims hides a waypoint in a protected chunk");
            } finally {
                Protection.unregister(server, "simplemaps_web_waypoint_test");
            }
            server.getCommands().performPrefixedCommand(cmd, "simplemaps web waypoints unshare");
            h.assertTrue(waypointsOf(server, dir, false, data).isEmpty() && WebMap.shares(server).stream().noneMatch(s -> s.owner().equals(player.getUUID())),
                    "unshare takes it back");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            MapsConfig.webWaypoints = saved;
            WebMap.unshare(server, player.getUUID(), -1);
            WebTests.delete(dir);
        }
        h.succeed();
    }
}
