package com.simplemaps.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.simplemaps.JamNoise;
import com.simplemaps.SimpleMaps;
import com.simplemaps.Waypoint;
import com.simplemaps.Waypoints;
import com.simplemaps.WayfinderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

/**
 * A 128x128 Vanilla-sized map picture composed from cached tiles: the map in hand (holder in the middle) and framed
 * maps (Feature 4, the stored view). Registered with the texture manager so Vanilla's map renderer can draw it.
 * Waypoints are baked in as small coloured squares; the height-line mode (owner N65) is the saved display mode of the stack.
 */
public final class ViewTexture {
    public static final int SIZE = 128;
    public final Identifier id;
    private final DynamicTexture texture;
    private long lastHash = Long.MIN_VALUE;
    long lastUsed;

    public ViewTexture(String name) {
        this.id = SimpleMaps.id("view/" + name);
        this.texture = new DynamicTexture(() -> "simplemaps " + name, SIZE, SIZE, true);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    public void release() {
        Minecraft.getInstance().getTextureManager().release(id);
    }

    /** Requests the needed tiles and redraws when the centre, a tile or a waypoint changed. Render or tick thread. */
    public void compose(TileCache cache, int mapId, int zoom, int centerX, int centerZ, Waypoints waypoints, boolean contour) {
        compose(cache, mapId, zoom, 1, centerX, centerZ, waypoints, contour, -1, 0L);
    }

    /** A framed view (owner 21.10.): like the above, {@code mag} picture pixels per map pixel (8:1, 4:1, 2:1 at zoom 1). */
    public void compose(TileCache cache, int mapId, int zoom, int mag, int centerX, int centerZ, Waypoints waypoints, boolean contour) {
        compose(cache, mapId, zoom, mag, centerX, centerZ, waypoints, contour, -1, 0L);
    }

    /**
     * Like {@link #compose(TileCache, int, int, int, int, Waypoints, boolean)}; {@code jamTint >= 0} (map of another
     * dimension) lays the faint {@link JamNoise} over the unexplored pixels and redraws every tick.
     */
    public void compose(TileCache cache, int mapId, int zoom, int centerX, int centerZ, Waypoints waypoints, boolean contour, int jamTint, long tick) {
        compose(cache, mapId, zoom, 1, centerX, centerZ, waypoints, contour, jamTint, tick);
    }

    private void compose(TileCache cache, int mapId, int zoom, int mag, int centerX, int centerZ, Waypoints waypoints, boolean contour, int jamTint, long tick) {
        zoom = WayfinderData.validZoom(zoom);
        mag = zoom == 1 && mag > 1 && SIZE % mag == 0 ? mag : 1;
        // Map pixels (at this zoom) of the picture's left/top edge; the picture covers SIZE / mag of them.
        int span = SIZE / mag;
        int left = Math.floorDiv(centerX, zoom) - span / 2, top = Math.floorDiv(centerZ, zoom) - span / 2;
        int tx0 = Math.floorDiv(left, WayfinderData.TILE), tz0 = Math.floorDiv(top, WayfinderData.TILE);
        int tx1 = Math.floorDiv(left + span - 1, WayfinderData.TILE), tz1 = Math.floorDiv(top + span - 1, WayfinderData.TILE);
        long hash = (((long) mapId * 31 + zoom) * 31 + mag) * 961 + left * 31L + top;
        TileCache.Entry[][] tiles = new TileCache.Entry[tx1 - tx0 + 1][tz1 - tz0 + 1];
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                TileCache.Entry e = cache.want(new TileCache.Key(mapId, zoom, tx, tz));
                tiles[tx - tx0][tz - tz0] = e;
                hash = hash * 31 + e.version;
            }
        }
        hash = hash * 31 + waypoints.hashCode();
        hash = hash * 31 + (contour ? 1 : 0);
        if (jamTint >= 0) hash = (hash * 31 + jamTint) * 31 + (tick >> 1);
        if (hash == lastHash) return;
        lastHash = hash;
        NativeImage pixels = texture.getPixels();
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                int gx = left + px / mag, gz = top + py / mag;
                TileCache.Entry e = tiles[Math.floorDiv(gx, WayfinderData.TILE) - tx0][Math.floorDiv(gz, WayfinderData.TILE) - tz0];
                int tx = Math.floorMod(gx, WayfinderData.TILE), tz = Math.floorMod(gz, WayfinderData.TILE);
                int c = contour ? TileCache.contourColor(e, tx, tz) : TileCache.color(e, tx, tz);
                int argb = c == 0 ? 0 : contour ? c : MapColor.getColorFromPackedId(c);
                if (argb == 0 && jamTint >= 0) argb = JamNoise.argb(jamTint, px, py, tick);
                pixels.setPixel(px, py, argb);
            }
        }
        for (Waypoint w : waypoints.list()) {
            int px = (Math.floorDiv(w.x(), zoom) - left) * mag + mag / 2, py = (Math.floorDiv(w.z(), zoom) - top) * mag + mag / 2;
            square(pixels, px, py, 2, 0xFF1E1F23);
            square(pixels, px, py, 1, w.color());
        }
        texture.upload();
    }

    private static void square(NativeImage pixels, int cx, int cy, int r, int argb) {
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) pixels.setPixel(x, y, argb);
            }
        }
    }
}
