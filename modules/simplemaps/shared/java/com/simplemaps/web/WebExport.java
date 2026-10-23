package com.simplemaps.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simplebuilding.framework.api.MapLayerSources;
import com.simplebuilding.framework.api.Protection;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsIcons;
import com.simplemaps.Reveal;
import com.simplemaps.SimpleMaps;
import com.simplemaps.WayfinderData;
import com.simplemaps.WayfinderIds;
import com.simplemaps.net.LayerDataPayload;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;

/**
 * One static web map export (stage 1 of {@code docs/ai/KONZEPT-WEBKARTE-2026-10-19.md}): PNG tiles
 * {@code tiles/<dim>/<z>/<x>/<y>.png} (z 4 = one block per pixel, 128 blocks per tile; z 0 = 16 blocks per pixel),
 * {@code markers.json} and the viewer {@code index.html}.
 *
 * <p>Only what the wayfinder maps have explored is exported: a pixel is the union of all maps of that dimension. Where
 * the chunk is loaded right now, the explored pixels are read fresh from the world (Vanilla map colours and shading,
 * {@link Reveal#sample}); a chunk is never loaded. Work is split into steps with a chunk budget (one pyramid tile costs
 * {@link #PYRAMID_COST}), so the server thread only does a bounded amount per tick.
 */
public final class WebExport {
    public static final int TILE = WayfinderData.TILE, MAX_ZOOM = 4, CHUNKS_PER_TILE = (TILE / 16) * (TILE / 16), PYRAMID_COST = 16;
    /** Actor of the protection check for "hide claims": somebody who is member of nothing. */
    private static final UUID NOBODY = new UUID(0L, 0L);
    /** Layer id of Simple Shops' shops (framework {@link MapLayerSources}) and the most shops one export lists. */
    public static final String SHOP_SOURCE = "shops";
    public static final int MAX_SHOPS = 2048;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** One dimension: explored 1:1 tile keys (sorted), the maps that explored them, tiles touched per zoom. */
    private static final class Dim {
        final ServerLevel level;
        final String folder;
        final List<Long> tiles;
        final List<WayfinderData> maps;
        final List<TreeSet<Long>> touched = new ArrayList<>();
        int written;

        Dim(ServerLevel level, String folder, List<Long> tiles, List<WayfinderData> maps) {
            this.level = level;
            this.folder = folder;
            this.tiles = tiles;
            this.maps = maps;
            for (int z = 0; z <= MAX_ZOOM; z++) touched.add(new TreeSet<>());
        }
    }

    private final MinecraftServer server;
    private final Path dir;
    private final boolean hideClaims;
    private final List<Dim> dims;
    private final int tilesTotal;
    private int dim, tile, chunk = -1, zoom = MAX_ZOOM - 1, tilesDone, chunksDone, pyramidDim;
    private List<Long> parents;
    private byte[] colors, heights;
    private boolean done;

    private WebExport(MinecraftServer server, Path dir, boolean hideClaims, List<Dim> dims) {
        this.server = server;
        this.dir = dir;
        this.hideClaims = hideClaims;
        this.dims = dims;
        this.tilesTotal = dims.stream().mapToInt(d -> d.tiles.size()).sum();
    }

    /** Export of every wayfinder map on the server (loads their saved data once, like opening each map). */
    public static WebExport ofAllMaps(MinecraftServer server, Path dir, boolean hideClaims) {
        List<WayfinderData> maps = new ArrayList<>();
        for (int id = 0, n = WayfinderIds.count(server); id < n; id++) maps.add(WayfinderData.get(server, id));
        return of(server, dir, hideClaims, maps);
    }

    /** Export of exactly these maps (maps without a bound, existing dimension are skipped). */
    public static WebExport of(MinecraftServer server, Path dir, boolean hideClaims, List<WayfinderData> maps) {
        Map<String, Dim> byFolder = new TreeMap<>();
        Map<String, TreeSet<Long>> keys = new HashMap<>();
        for (WayfinderData map : maps) {
            Optional<ResourceKey<Level>> key = map.dimension();
            ServerLevel level = key.map(server::getLevel).orElse(null);
            if (level == null || map.tileCount() == 0) continue;
            String folder = folder(level.dimension());
            Dim d = byFolder.computeIfAbsent(folder, f -> new Dim(level, f, new ArrayList<>(), new ArrayList<>()));
            d.maps.add(map);
            keys.computeIfAbsent(folder, f -> new TreeSet<>()).addAll(map.tileKeys());
        }
        byFolder.forEach((folder, d) -> d.tiles.addAll(keys.get(folder)));
        return new WebExport(server, dir, hideClaims, new ArrayList<>(byFolder.values()));
    }

    /** {@code minecraft:the_nether} -> {@code minecraft_the_nether} (safe as one folder name). */
    public static String folder(ResourceKey<Level> key) {
        return (key.identifier().getNamespace() + "_" + key.identifier().getPath()).replaceAll("[^a-z0-9_.-]", "_");
    }

    /** Relative path of one tile file. */
    public static String tilePath(String folder, int z, int x, int y) {
        return "tiles/" + folder + "/" + z + "/" + x + "/" + y + ".png";
    }

    /** Vanilla map palette: packed colour (id * 4 + brightness) to ARGB, 0 (unknown) transparent. */
    public static int argb(int packed) {
        return (packed & 0xFF) == 0 ? 0 : MapColor.getColorFromPackedId(packed & 0xFF);
    }

    public boolean done() {
        return done;
    }

    public Path dir() {
        return dir;
    }

    public int tilesTotal() {
        return tilesTotal;
    }

    public int tilesDone() {
        return tilesDone;
    }

    public int chunksDone() {
        return chunksDone;
    }

    public int dimensions() {
        return dims.size();
    }

    /** 1:1 tiles written (not empty) over all dimensions. */
    public int tilesWritten() {
        return dims.stream().mapToInt(d -> d.written).sum();
    }

    /**
     * Runs the export until done or {@code budget} chunk units are used and returns the units used. While tiles are read
     * this never exceeds the budget; a pyramid tile ({@link #PYRAMID_COST}) runs at least once per step even on a smaller budget.
     */
    public int step(int budget) {
        int used = 0;
        try {
            while (!done && used < budget) {
                if (dim < dims.size()) {
                    if (tile < dims.get(dim).tiles.size()) {
                        stepTile();
                        used++;
                    } else {
                        dim++;
                        tile = 0;
                    }
                } else if (used == 0 || budget - used >= PYRAMID_COST) {
                    if (!stepPyramid()) finish();
                    else used += PYRAMID_COST;
                } else {
                    break;
                }
            }
        } catch (IOException | RuntimeException e) {
            SimpleMaps.LOG.warn("Simple Maps: web map export to {} stopped: {}", dir, e.toString());
            done = true;
        }
        return used;
    }

    /** One chunk of the current 1:1 tile; writes the tile after its last chunk. */
    private void stepTile() throws IOException {
        Dim d = dims.get(dim);
        long key = d.tiles.get(tile);
        int tx = WayfinderData.tileX(key), tz = WayfinderData.tileZ(key);
        if (chunk < 0) {
            colors = new byte[WayfinderData.AREA];
            heights = new byte[WayfinderData.AREA];
            for (WayfinderData map : d.maps) {
                WayfinderData.Tile t = map.tile(tx, tz);
                if (t == null) continue;
                for (int i = 0; i < WayfinderData.AREA; i++) {
                    if (colors[i] == 0 && t.colors[i] != 0) {
                        colors[i] = t.colors[i];
                        heights[i] = t.heights[i];
                    }
                }
            }
            chunk = 0;
        }
        int lx0 = (chunk % 8) * 16, lz0 = (chunk / 8) * 16, cx = tx * 8 + chunk % 8, cz = tz * 8 + chunk / 8;
        if (hideClaims && hidden(d.level, cx, cz)) {
            for (int lz = lz0; lz < lz0 + 16; lz++) {
                for (int lx = lx0; lx < lx0 + 16; lx++) colors[lz * TILE + lx] = 0;
            }
        } else {
            LevelChunk loaded = d.level.getChunkSource().getChunkNow(cx, cz);
            if (loaded != null) refresh(d.level, loaded, tx, tz, lx0, lz0);
        }
        chunksDone++;
        if (++chunk < CHUNKS_PER_TILE) return;
        int[] argb = new int[WayfinderData.AREA];
        for (int i = 0; i < argb.length; i++) argb[i] = argb(colors[i]);
        if (write(d.folder, MAX_ZOOM, tx, tz, argb)) d.written++;
        d.touched.get(MAX_ZOOM).add(key);
        chunk = -1;
        colors = heights = null;
        tile++;
        tilesDone++;
    }

    /** Reads the explored pixels of a loaded chunk fresh (unexplored pixels stay unknown). */
    private void refresh(ServerLevel level, LevelChunk loaded, int tx, int tz, int lx0, int lz0) {
        boolean ceiling = level.dimensionType().hasCeiling();
        int minY = level.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] sample = new int[3];
        for (int lx = lx0; lx < lx0 + 16; lx++) {
            int x = tx * TILE + lx;
            int previous = north(level, x, tz * TILE + lz0 - 1, lz0 > 0 ? heights[(lz0 - 1) * TILE + lx] & 0xFF : 0, ceiling, pos, sample);
            for (int lz = lz0; lz < lz0 + 16; lz++) {
                int i = lz * TILE + lx, z = tz * TILE + lz, known = heights[i] & 0xFF;
                // Under a ceiling the floor near the stored height is mapped; without a stored height keep the map's pixel.
                if (ceiling && known == 0 || !Reveal.sample(level, loaded, x, (known - 1) * 2 + minY, z, ceiling, pos, sample)) {
                    previous = Integer.MIN_VALUE;
                    continue;
                }
                if (colors[i] != 0) {
                    MapColor color = MapColor.byId(sample[0]);
                    colors[i] = color.getPackedId(Reveal.brightness(color, sample[1], previous, sample[2], x, z));
                    heights[i] = (byte) Mth.clamp((sample[1] - minY) / 2 + 1, 1, 255);
                }
                previous = sample[1];
            }
        }
    }

    /** Surface y of the block north of a chunk row (for the shading), or {@link Integer#MIN_VALUE} when unknown. */
    private static int north(ServerLevel level, int x, int z, int storedHeight, boolean ceiling, BlockPos.MutableBlockPos pos, int[] sample) {
        int py = storedHeight > 0 ? (storedHeight - 1) * 2 + level.getMinY() : Integer.MIN_VALUE;
        LevelChunk c = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (c != null && !(ceiling && storedHeight == 0) && Reveal.sample(level, c, x, py, z, ceiling, pos, sample)) return sample[1];
        return py;
    }

    /** Whether a protection provider (claims) forbids a stranger to build in this chunk. */
    private boolean hidden(ServerLevel level, int cx, int cz) {
        if (!Protection.active(server)) return false;
        return !Protection.allows(server, new Protection.Target(NOBODY, level.dimension().identifier().toString(),
                cx * 16 + 8, level.getSeaLevel(), cz * 16 + 8, false), Protection.Action.BUILD);
    }

    /** One tile of zoom {@link #zoom} from its four children; false when the pyramid is complete. */
    private boolean stepPyramid() throws IOException {
        while (true) {
            if (pyramidDim >= dims.size()) return false;
            Dim dm = dims.get(pyramidDim);
            if (parents == null) {
                TreeSet<Long> up = dm.touched.get(zoom);
                for (long child : dm.touched.get(zoom + 1)) {
                    up.add(WayfinderData.key(Math.floorDiv(WayfinderData.tileX(child), 2), Math.floorDiv(WayfinderData.tileZ(child), 2)));
                }
                parents = new ArrayList<>(up);
            }
            if (!parents.isEmpty()) {
                long key = parents.removeLast();
                compose(dm, zoom, WayfinderData.tileX(key), WayfinderData.tileZ(key));
                return true;
            }
            parents = null;
            if (--zoom < 0) {
                zoom = MAX_ZOOM - 1;
                pyramidDim++;
            }
        }
    }

    private void compose(Dim d, int z, int x, int y) throws IOException {
        int[] out = new int[WayfinderData.AREA];
        int half = TILE / 2;
        for (int dy = 0; dy < 2; dy++) {
            for (int dx = 0; dx < 2; dx++) {
                Path child = dir.resolve(tilePath(d.folder, z + 1, x * 2 + dx, y * 2 + dy));
                if (!Files.isRegularFile(child)) continue;
                BufferedImage image = ImageIO.read(child.toFile());
                if (image == null || image.getWidth() != TILE || image.getHeight() != TILE) continue;
                for (int v = 0; v < half; v++) {
                    for (int u = 0; u < half; u++) out[(dy * half + v) * TILE + dx * half + u] = downsample(image, u * 2, v * 2);
                }
            }
        }
        write(d.folder, z, x, y, out);
    }

    /** The first opaque pixel of a 2x2 block, so a small explored spot survives every zoom level. */
    private static int downsample(BufferedImage image, int x, int y) {
        for (int i = 0; i < 4; i++) {
            int argb = image.getRGB(x + (i & 1), y + (i >> 1));
            if ((argb >>> 24) != 0) return argb;
        }
        return 0;
    }

    /** Writes a tile (temp file, then move); an empty tile deletes an old file instead. Returns whether a file was written. */
    private boolean write(String folder, int z, int x, int y, int[] argb) throws IOException {
        Path file = dir.resolve(tilePath(folder, z, x, y));
        boolean empty = true;
        for (int c : argb) {
            if ((c >>> 24) != 0) {
                empty = false;
                break;
            }
        }
        if (empty) {
            Files.deleteIfExists(file);
            return false;
        }
        BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, TILE, TILE, argb, 0, TILE);
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        if (!ImageIO.write(image, "png", tmp.toFile())) throw new IOException("no PNG writer");
        move(tmp, file);
        return true;
    }

    private void finish() throws IOException {
        Files.createDirectories(dir);
        JsonObject markers = markers();
        writeText("markers.json", GSON.toJson(markers));
        if (markers.has("shops") && MapsIcons.shopIcon().isPresent()) writeShopIcon();
        try (InputStream in = WebExport.class.getResourceAsStream("/assets/" + SimpleMaps.MOD_ID + "/web/index.html")) {
            if (in != null) writeText("index.html", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            else SimpleMaps.LOG.warn("Simple Maps: web viewer index.html missing in the mod jar");
        }
        done = true;
    }

    /** Dimensions with tiles and the players who switched themselves on ({@code /simplemaps web show}); no UUIDs. */
    JsonObject markers() {
        JsonObject root = new JsonObject();
        root.addProperty("format", 1);
        root.addProperty("generatedAt", Instant.now().toString());
        root.addProperty("tileBlocks", TILE);
        root.addProperty("maxZoom", MAX_ZOOM);
        JsonArray dimensions = new JsonArray();
        for (Dim d : dims) {
            JsonObject o = new JsonObject();
            o.addProperty("id", d.level.dimension().identifier().toString());
            o.addProperty("folder", d.folder);
            o.addProperty("tiles", d.written);
            // Start view: the middle of the explored tiles.
            long sx = 0, sz = 0;
            for (long k : d.tiles) {
                sx += WayfinderData.tileX(k);
                sz += WayfinderData.tileZ(k);
            }
            int n = Math.max(1, d.tiles.size());
            o.addProperty("centerX", (int) (sx * TILE / n + TILE / 2));
            o.addProperty("centerZ", (int) (sz * TILE / n + TILE / 2));
            dimensions.add(o);
        }
        root.add("dimensions", dimensions);
        root.add("players", players(server));
        if (MapsConfig.webShops) root.add("shops", shops());
        return root;
    }

    /**
     * Online players who switched themselves on ({@code /simplemaps web show}), no spectators: name, dimension, x/z, no
     * UUIDs. Shared by {@code markers.json} and the live {@code live.json} of the web server (server thread only).
     */
    public static JsonArray players(MinecraftServer server) {
        JsonArray players = new JsonArray();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || !WebMap.visible(server, p.getUUID())) continue;
            JsonObject o = new JsonObject();
            o.addProperty("name", p.getGameProfile().name());
            o.addProperty("dimension", p.level().dimension().identifier().toString());
            o.addProperty("x", Mth.floor(p.getX()));
            o.addProperty("z", Mth.floor(p.getZ()));
            players.add(o);
        }
        return players;
    }

    /**
     * Shops of the {@code shops} map layer (Simple Shops, framework contract, absent without it) on explored tiles only,
     * never in a protected chunk with {@code webHideClaims}; only with {@code webShops} (default off, like players).
     * {@code icon}: {@code money_bill} when Simple Money is loaded (owner 19.10.), else {@code diamond}.
     */
    JsonArray shops() {
        JsonArray out = new JsonArray();
        MapLayerSources.Source source = MapLayerSources.get(SHOP_SOURCE);
        if (source == null) return out;
        for (Dim d : dims) {
            for (long key : d.tiles) {
                int x0 = WayfinderData.tileX(key) * TILE, z0 = WayfinderData.tileZ(key) * TILE;
                List<MapLayerSources.Marker> found;
                try {
                    found = MapLayerSources.collect(source, d.level, NOBODY, x0, z0, x0 + TILE - 1, z0 + TILE - 1);
                } catch (RuntimeException e) {
                    SimpleMaps.LOG.warn("Simple Maps web: shop layer failed: {}", e.toString());
                    return out;
                }
                for (MapLayerSources.Marker m : found) {
                    if (out.size() >= MAX_SHOPS) return out;
                    if (m.x() < x0 || m.x() >= x0 + TILE || m.z() < z0 || m.z() >= z0 + TILE) continue;
                    if (hideClaims && hidden(d.level, m.x() >> 4, m.z() >> 4)) continue;
                    JsonObject o = new JsonObject();
                    o.addProperty("dimension", d.level.dimension().identifier().toString());
                    o.addProperty("x", m.x());
                    o.addProperty("z", m.z());
                    o.addProperty("icon", MapsIcons.webIcon(m.kind()));
                    JsonArray lines = new JsonArray();
                    for (String line : m.lines()) {
                        if (lines.size() < LayerDataPayload.MAX_LINES) lines.add(line.length() > LayerDataPayload.MAX_LINE ? line.substring(0, LayerDataPayload.MAX_LINE) : line);
                    }
                    o.add("lines", lines);
                    out.add(o);
                }
            }
        }
        return out;
    }

    /** Copies Simple Money's bill texture next to the viewer (the viewer draws a plain bill when it is missing). */
    private void writeShopIcon() throws IOException {
        String resource = "assets/" + MapsIcons.MONEY_MOD + "/textures/item/" + MapsIcons.MONEY_BILL.getPath() + ".png";
        java.util.LinkedHashSet<ClassLoader> loaders = new java.util.LinkedHashSet<>();
        MapsIcons.shopIcon().ifPresent(item -> loaders.add(item.getClass().getClassLoader()));
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(WebExport.class.getClassLoader());
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) continue;
                Path file = dir.resolve("icons/" + MapsIcons.MONEY_BILL.getPath() + ".png"), tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.createDirectories(file.getParent());
                Files.write(tmp, in.readAllBytes());
                move(tmp, file);
                return;
            }
        }
    }

    private void writeText(String name, String text) throws IOException {
        Path file = dir.resolve(name), tmp = dir.resolve(name + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        move(tmp, file);
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
