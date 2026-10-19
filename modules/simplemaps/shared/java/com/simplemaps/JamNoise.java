package com.simplemaps;

/**
 * Owner N38, softened 19.10.: a wayfinder map held outside the dimension it charts still shows what it explored; only
 * the unexplored pixels carry a faint, tinted static (Nether reddish, End violet, Overworld grey) that flickers with the
 * tick. Pure maths (ARGB), no client class, so the picture is testable.
 */
public final class JamNoise {
    /** Highest alpha of a static pixel: the paper shows through, the noise never covers the map. */
    public static final int MAX_ALPHA = 0x70;

    private JamNoise() {}

    /** Tint (RGB) of the static for a map kind. */
    public static int tint(WayfinderMapItem.Kind kind) {
        return switch (kind) {
            case NETHER -> 0xFF5A46;
            case END -> 0xB874FF;
            case OVERWORLD -> 0xD2D2C4;
        };
    }

    /** ARGB of the static at map pixel {@code x, y} in tick {@code tick}; changes every second tick. */
    public static int argb(int tint, int x, int y, long tick) {
        long h = (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ ((tick >> 1) * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        int n = (int) (h & 0xFF);
        // Grainy but muted: brightness 60..160 of the tint, alpha 0x28..MAX_ALPHA.
        int v = 60 + (n * n >> 8) * 100 / 255, a = 0x28 + (n >> 2) * (MAX_ALPHA - 0x28) / 63;
        int r = (tint >> 16 & 0xFF) * v / 255, g = (tint >> 8 & 0xFF) * v / 255, b = (tint & 0xFF) * v / 255;
        return a << 24 | r << 16 | g << 8 | b;
    }
}
