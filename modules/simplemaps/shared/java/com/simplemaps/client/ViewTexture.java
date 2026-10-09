package com.simplemaps.client;

import com.mojang.blaze3d.platform.NativeImage;
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
 * Waypoints are baked in as small coloured squares.
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
    public void compose(TileCache cache, int mapId, int zoom, int centerX, int centerZ, Waypoints waypoints) {
        zoom = WayfinderData.validZoom(zoom);
        int left = Math.floorDiv(centerX, zoom) - SIZE / 2, top = Math.floorDiv(centerZ, zoom) - SIZE / 2;
        int tx0 = Math.floorDiv(left, WayfinderData.TILE), tz0 = Math.floorDiv(top, WayfinderData.TILE);
        int tx1 = Math.floorDiv(left + SIZE - 1, WayfinderData.TILE), tz1 = Math.floorDiv(top + SIZE - 1, WayfinderData.TILE);
        long hash = ((long) mapId * 31 + zoom) * 961 + left * 31L + top;
        TileCache.Entry[][] tiles = new TileCache.Entry[tx1 - tx0 + 1][tz1 - tz0 + 1];
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                TileCache.Entry e = cache.want(new TileCache.Key(mapId, zoom, tx, tz));
                tiles[tx - tx0][tz - tz0] = e;
                hash = hash * 31 + e.version;
            }
        }
        hash = hash * 31 + waypoints.hashCode();
        if (hash == lastHash) return;
        lastHash = hash;
        NativeImage pixels = texture.getPixels();
        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                int gx = left + px, gz = top + py;
                TileCache.Entry e = tiles[Math.floorDiv(gx, WayfinderData.TILE) - tx0][Math.floorDiv(gz, WayfinderData.TILE) - tz0];
                int c = TileCache.color(e, Math.floorMod(gx, WayfinderData.TILE), Math.floorMod(gz, WayfinderData.TILE));
                pixels.setPixel(px, py, c == 0 ? 0 : MapColor.getColorFromPackedId(c));
            }
        }
        for (Waypoint w : waypoints.list()) {
            int px = Math.floorDiv(w.x(), zoom) - left, py = Math.floorDiv(w.z(), zoom) - top;
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
