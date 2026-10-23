package com.simplemaps;

import com.simplelib.api.image.PixelArt;
import com.simplelib.api.image.PixelPalette;
import net.minecraft.world.level.material.MapColor;

/**
 * Picture maps (AUD-6): turns a picture into the colour bytes of a wall of Vanilla maps. Pure calculation on top of
 * {@code simplelib.api.image}; the palette comes from the game's {@link MapColor}s (61 base colours x 4 shades, id 0 is
 * "free"), so versions stay right. Used by the client screen (converting) and the server (checking what arrives).
 */
public final class PictureMaps {
    /** Edge of one map in pixels and its colour bytes. */
    public static final int MAP = 128, BYTES = MAP * MAP;
    private static final int SHADES = 4;
    private static PixelPalette palette;
    private static int[] packed;

    private PictureMaps() {}

    /** A map colour byte the server accepts: 0 (free) or base colour 1..61 with any shade. */
    public static boolean valid(byte value) {
        int id = value & 255;
        return id == 0 || (id >> 2 >= 1 && MapColor.byId(id >> 2) != MapColor.NONE);
    }

    private static synchronized void init() {
        if (palette != null) return;
        java.util.List<Integer> rgb = new java.util.ArrayList<>(), ids = new java.util.ArrayList<>();
        for (int base = 1; base < 64; base++) {
            MapColor color = MapColor.byId(base);
            if (color == MapColor.NONE) continue;
            for (MapColor.Brightness shade : MapColor.Brightness.values()) {
                rgb.add(color.calculateARGBColor(shade) & 0xFFFFFF);
                ids.add(color.getPackedId(shade) & 255);
            }
        }
        palette = new PixelPalette(rgb.stream().mapToInt(Integer::intValue).toArray());
        packed = ids.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Number of palette colours (244 in Vanilla). */
    public static int colors() {
        init();
        return packed.length;
    }

    /** Result of {@link #convert}: {@code cols x rows} maps, {@code colors} = {@code cols*MAP} x {@code rows*MAP} bytes row by row. */
    public record Result(int cols, int rows, byte[] colors) {
        /** The colour bytes of map {@code (col, row)}. */
        public byte[] tile(int col, int row) {
            byte[] out = new byte[BYTES];
            int width = cols * MAP;
            for (int y = 0; y < MAP; y++) System.arraycopy(colors, (row * MAP + y) * width + col * MAP, out, y * MAP, MAP);
            return out;
        }
    }

    /**
     * {@code argb} ({@code width x height}, ARGB, row by row) as at most {@code maps x maps} maps: the longer edge becomes
     * {@code maps} maps, the aspect ratio stays, the picture sits in the middle and the rest stays free (0).
     */
    public static Result convert(int[] argb, int width, int height, int maps, PixelArt.Dither dither) {
        init();
        int[] size = PixelArt.fit(width, height, maps * MAP);
        // fit() only shrinks; a small picture is scaled up so that it fills the chosen number of maps.
        if (Math.max(width, height) < maps * MAP) {
            double k = (double) maps * MAP / Math.max(width, height);
            size = new int[]{Math.max(1, (int) Math.round(width * k)), Math.max(1, (int) Math.round(height * k))};
        }
        int w = Math.min(size[0], maps * MAP), h = Math.min(size[1], maps * MAP);
        int[] scaled = PixelArt.scale(argb, width, height, w, h);
        int[] index = PixelArt.quantize(scaled, w, h, palette, dither);
        int cols = (w + MAP - 1) / MAP, rows = (h + MAP - 1) / MAP;
        int totalW = cols * MAP, totalH = rows * MAP, ox = (totalW - w) / 2, oy = (totalH - h) / 2;
        byte[] out = new byte[totalW * totalH];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = index[y * w + x];
                if (i >= 0) out[(oy + y) * totalW + ox + x] = (byte) packed[i];
            }
        }
        return new Result(cols, rows, out);
    }

    /** RGB (0xRRGGBB) of a colour byte for the preview; free = -1. */
    public static int rgb(byte value) {
        int id = value & 255;
        if (id < 4) return -1;
        return MapColor.byId(id >> 2).calculateARGBColor(MapColor.Brightness.byId(id & 3)) & 0xFFFFFF;
    }
}
