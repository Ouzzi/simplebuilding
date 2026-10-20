package com.simplemaps.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simplemaps.MapsIcons;
import com.simplemaps.SimpleMaps;
import com.simplemaps.net.LayerDataPayload;
import com.simplemaps.net.LayerRequestPayload;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Client side of the map layers and the coordinate labels of the wayfinder screen: the switches (saved in
 * {@code config/simplemaps-client.json}), the request to the server and the drawing of what comes back - loaded chunks
 * as a green wash, every other source (shops, chunk loaders ...) as small diamonds with a tooltip; shops carry Simple
 * Money's bill when that mod is loaded ({@link MapsIcons}, by id). The server decides what exists; this class imports
 * no other module.
 */
public final class MapLayers {
    public static final String CHUNKS = "chunks";
    private static final int RIM = 0xFF1A1208, WASH = 0x3C3CC864, REQUEST_MS = 1000, MAX_CHUNKS = LayerDataPayload.MAX_SPAN_CHUNKS, ICON = 10;
    private static ItemStack shopIcon;
    private static boolean loaded, coords;
    private static final Set<String> ON = new LinkedHashSet<>();
    private static LayerDataPayload data;
    private static long lastSent;

    private MapLayers() {}

    // ---------------------------------------------------------------- switches

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/simplemaps-client.json");
    }

    private static void load() {
        loaded = true;
        try {
            if (Files.isRegularFile(file()) && Files.size(file()) <= 16384) {
                JsonObject json = new Gson().fromJson(Files.readString(file()), JsonObject.class);
                if (json != null) {
                    coords = json.has("coords") && json.get("coords").getAsBoolean();
                    if (json.has("layers")) json.getAsJsonArray("layers").forEach(e -> {
                        if (ON.size() < 16 && e.getAsString().length() <= LayerRequestPayload.MAX_ID) ON.add(e.getAsString());
                    });
                }
            }
        } catch (Exception ignored) {
            ON.clear();
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("coords", coords);
        var array = new com.google.gson.JsonArray();
        ON.forEach(array::add);
        json.add("layers", array);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(json));
        } catch (Exception e) {
            SimpleMaps.LOG.warn("Cannot save map settings: {}", e.toString());
        }
    }

    public static boolean coordsOn() {
        if (!loaded) load();
        return coords;
    }

    public static void toggleCoords() {
        coordsOn();
        coords = !coords;
        save();
    }

    public static boolean on(String layer) {
        if (!loaded) load();
        return ON.contains(layer);
    }

    public static void toggle(String layer) {
        on(layer);
        if (!ON.remove(layer)) ON.add(layer);
        save();
    }

    /** The extra sources the server offers (empty until its first answer). */
    public static List<LayerDataPayload.Source> available() {
        return data == null ? List.of() : data.available();
    }

    public static void receive(LayerDataPayload payload) {
        data = payload;
    }

    public static void reset() {
        data = null;
    }

    // ---------------------------------------------------------------- request

    /** Asks for the layers of the visible area (about once a second, never more than the server's cap). */
    public static void request(int mapId, double originX, double originZ, int bpp, int mag, int mapW, int mapH) {
        long now = System.currentTimeMillis();
        if (now - lastSent < REQUEST_MS) return;
        lastSent = now;
        int minX = Mth.floor(originX), minZ = Mth.floor(originZ);
        int maxX = Mth.ceil(originX + (double) mapW / mag * bpp), maxZ = Mth.ceil(originZ + (double) mapH / mag * bpp);
        int minCx = minX >> 4, maxCx = maxX >> 4, minCz = minZ >> 4, maxCz = maxZ >> 4;
        if (maxCx - minCx + 1 > MAX_CHUNKS) {
            int mid = (minCx + maxCx) >> 1;
            minCx = mid - MAX_CHUNKS / 2 + 1;
            maxCx = mid + MAX_CHUNKS / 2;
        }
        if (maxCz - minCz + 1 > MAX_CHUNKS) {
            int mid = (minCz + maxCz) >> 1;
            minCz = mid - MAX_CHUNKS / 2 + 1;
            maxCz = mid + MAX_CHUNKS / 2;
        }
        List<String> sources = new ArrayList<>();
        for (String id : ON) if (!id.equals(CHUNKS) && sources.size() < LayerRequestPayload.MAX_SOURCES) sources.add(id);
        SimpleMaps.toServer.accept(new LayerRequestPayload(mapId, minCx << 4, minCz << 4, (maxCx << 4) + 15, (maxCz << 4) + 15, on(CHUNKS), sources));
    }

    // ---------------------------------------------------------------- drawing

    private static int sx(double block, int mapX, double originX, int bpp, int mag) {
        return mapX + (int) Math.floor((block - originX) / bpp * mag);
    }

    /** Draws the loaded-chunk wash and the source markers; call inside the map's scissor. */
    public static void draw(GuiGraphicsExtractor g, int mapId, int mapX, int mapY, int mapW, int mapH, double originX, double originZ, int bpp, int mag) {
        if (data == null || data.mapId() != mapId) return;
        LayerDataPayload.Area a = data.area();
        if (on(CHUNKS) && data.loaded().length > 0) {
            for (int dz = 0; dz < a.h(); dz++) {
                for (int dx = 0; dx < a.w(); dx++) {
                    int bit = dz * a.w() + dx;
                    if (bit >> 3 >= data.loaded().length || (data.loaded()[bit >> 3] >> (bit & 7) & 1) == 0) continue;
                    int x0 = sx((a.minCx() + dx) * 16.0, mapX, originX, bpp, mag), x1 = Math.max(x0 + 1, sx((a.minCx() + dx + 1) * 16.0, mapX, originX, bpp, mag));
                    int y0 = sx((a.minCz() + dz) * 16.0, mapY, originZ, bpp, mag), y1 = Math.max(y0 + 1, sx((a.minCz() + dz + 1) * 16.0, mapY, originZ, bpp, mag));
                    if (x1 < mapX || x0 > mapX + mapW || y1 < mapY || y0 > mapY + mapH) continue;
                    g.fill(x0, y0, x1, y1, WASH);
                }
            }
        }
        for (LayerDataPayload.Marker m : data.markers()) {
            if (!on(m.source())) continue;
            int[] area = m.area();
            if (area != null) {
                int x0 = sx(m.x(), mapX, originX, bpp, mag), x1 = Math.max(x0 + 1, sx(m.x() + 16.0 * area[2], mapX, originX, bpp, mag));
                int y0 = sx(m.z(), mapY, originZ, bpp, mag), y1 = Math.max(y0 + 1, sx(m.z() + 16.0, mapY, originZ, bpp, mag));
                if (x1 < mapX || x0 > mapX + mapW || y1 < mapY || y0 > mapY + mapH) continue;
                int rgb = area[0] & 0xFFFFFF;
                g.fill(x0, y0, x1, y1, 0x50000000 | rgb);
                int edge = 0xFF000000 | (area[1] == 1 ? 0xFFFFFF : rgb);
                g.fill(x0, y0, x1, y0 + 1, edge); g.fill(x0, y1 - 1, x1, y1, edge);
                if (area[1] == 1) { g.fill(x0, y0, x0 + 1, y1, edge); g.fill(x1 - 1, y0, x1, y1, edge); }
                continue;
            }
            int x = sx(m.x(), mapX, originX, bpp, mag) + mag / 2, y = sx(m.z(), mapY, originZ, bpp, mag) + mag / 2;
            if (x < mapX - 5 || x > mapX + mapW + 5 || y < mapY - 5 || y > mapY + mapH + 5) continue;
            ItemStack icon = icon(m);
            if (!icon.isEmpty()) {
                // Shop with Simple Money (owner 19.10.): the bill, 10 px, centred on the shop.
                g.pose().pushMatrix();
                g.pose().translate(x - ICON / 2.0F, y - ICON / 2.0F);
                g.pose().scale(ICON / 16.0F, ICON / 16.0F);
                g.fakeItem(icon, 0, 0);
                g.pose().popMatrix();
                continue;
            }
            int color = 0xFF000000 | Mth.hsvToRgb((m.source().hashCode() & 0xFFFF) / 65536.0F, 0.55F, 0.95F);
            for (int r = 0; r <= 4; r++) g.fill(x - (4 - r), y - r, x + (4 - r) + 1, y - r + 1, r == 4 ? RIM : color);
            for (int r = 1; r <= 4; r++) g.fill(x - (4 - r), y + r, x + (4 - r) + 1, y + r + 1, r == 4 ? RIM : color);
        }
    }

    /** The item drawn instead of the diamond: Simple Money's bill for a shop (when loaded), else empty. */
    private static ItemStack icon(LayerDataPayload.Marker m) {
        if (!MapsIcons.SHOP_KIND.equals(m.kind())) return ItemStack.EMPTY;
        if (shopIcon == null) shopIcon = MapsIcons.shopIcon().map(item -> new ItemStack(item)).orElse(ItemStack.EMPTY);
        return shopIcon;
    }

    /** Hover text of the marker under the cursor, drawn after the scissor. */
    public static void tooltip(GuiGraphicsExtractor g, Font font, int mapId, int mouseX, int mouseY, int mapX, int mapY, double originX, double originZ, int bpp, int mag) {
        if (data == null || data.mapId() != mapId) return;
        for (LayerDataPayload.Marker m : data.markers()) {
            if (!on(m.source())) continue;
            int[] area = m.area();
            int x = sx(m.x(), mapX, originX, bpp, mag) + mag / 2, y = sx(m.z(), mapY, originZ, bpp, mag) + mag / 2;
            int reach = icon(m).isEmpty() ? 4 : ICON / 2;
            boolean over = area == null ? Math.abs(x - mouseX) <= reach && Math.abs(y - mouseY) <= reach
                    : mouseX >= sx(m.x(), mapX, originX, bpp, mag) && mouseX < sx(m.x() + 16.0 * area[2], mapX, originX, bpp, mag)
                    && mouseY >= sx(m.z(), mapY, originZ, bpp, mag) && mouseY < sx(m.z() + 16.0, mapY, originZ, bpp, mag);
            if (over) {
                List<Component> lines = new ArrayList<>();
                for (String line : m.lines()) lines.add(Component.literal(line));
                if (area == null) lines.add(Component.literal(m.x() + ", " + m.z()));
                g.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    /** "x, z" next to a marker (the "show coordinates" switch). */
    public static void drawCoords(GuiGraphicsExtractor g, Font font, int x, int y, int bx, int bz) {
        g.text(font, bx + ", " + bz, x, y, 0xFFE8E2D0, true);
    }
}
