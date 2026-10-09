package com.simplemaps.client;

import com.simplemaps.WayfinderData;

/**
 * Height-line mode (Feature 5): explored land in bands of 8 blocks from low green over yellow and brown to white,
 * water blue, with a dark line wherever the band changes towards the right or lower neighbour. Pixels without a
 * height (taken over from a Vanilla map) keep a pale grey.
 */
public final class Contours {
    /** Height byte = (y - minY) / 2 + 1, so 4 units = 8 blocks per band. */
    public static final int BAND = 4;
    private static final int[] BANDS = {0xFF3E7A3A, 0xFF4F8C3F, 0xFF6A9C44, 0xFF8BAA4B, 0xFFB3B657, 0xFFCDB862, 0xFFC99E58,
            0xFFB5824B, 0xFF9C6A41, 0xFF8A5E40, 0xFF8F7A6B, 0xFFA8A29C, 0xFFC6C3C0, 0xFFE4E3E1, 0xFFF7F7F7};
    private static final int WATER_COLOR_ID = 12;

    private Contours() {}

    public static int color(byte[] colors, byte[] heights, int x, int y) {
        int i = y * WayfinderData.TILE + x, c = colors[i] & 0xFF, h = heights[i] & 0xFF;
        if (c == 0) return 0;
        if (c >> 2 == WATER_COLOR_ID) return h == 0 ? 0xFF6E8FC8 : 0xFF4A6FB8;
        if (h == 0) return 0xFFB8B8B0;
        // Overworld sea level (y 62, height byte 64) is band 16 → palette index 2; each further index is 8 blocks higher.
        int band = h / BAND, index = Math.max(0, Math.min(BANDS.length - 1, band - 14));
        int argb = BANDS[index];
        if (edge(colors, heights, band, x + 1, y) || edge(colors, heights, band, x, y + 1)) argb = darken(argb);
        return argb;
    }

    private static boolean edge(byte[] colors, byte[] heights, int band, int x, int y) {
        if (x >= WayfinderData.TILE || y >= WayfinderData.TILE) return false;
        int j = y * WayfinderData.TILE + x;
        int h = heights[j] & 0xFF;
        return (colors[j] & 0xFF) != 0 && h != 0 && h / BAND != band;
    }

    private static int darken(int argb) {
        int r = (argb >> 16 & 0xFF) * 3 / 5, g = (argb >> 8 & 0xFF) * 3 / 5, b = (argb & 0xFF) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
