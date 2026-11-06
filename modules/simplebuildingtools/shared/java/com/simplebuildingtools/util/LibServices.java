package com.simplebuildingtools.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** SimpleLib's picture and placement services as SimpleBuilding's {@code LibServices} offered them to the blueprint. */
public final class LibServices {
    private LibServices() {
    }

    public static boolean pictures() {
        return true;
    }

    /** Palette indices of one {@code pixels} row text, or null when malformed or longer than {@code maxLength}. */
    public static int @Nullable [] decodePixelRow(String text, int maxLength) {
        return com.simplelib.api.image.PixelRle.decode(text, maxLength);
    }

    /** Row text of {@code indices[from, to)} (-1 = free pixel). */
    public static String encodePixelRow(int[] indices, int from, int to) {
        return com.simplelib.api.image.PixelRle.encode(indices, from, to);
    }

    /** Palette index of a row symbol: 0..51, -1 for a free pixel, -2 for no symbol. */
    public static int pixelSymbolIndex(char c) {
        return com.simplelib.api.image.PixelRle.index(c);
    }

    /** {@code argb} fitted to {@code maxSide}, scaled and quantized: {@code {w, h, i0, i1, ...}}; dither 0 none, 1 Floyd-Steinberg, 2 Bayer. */
    public static int @Nullable [] convertPicture(int[] argb, int width, int height, int maxSide, int[] paletteRgb, int dither) {
        int[] size = com.simplelib.api.image.PixelArt.fit(width, height, maxSide);
        int[] scaled = com.simplelib.api.image.PixelArt.scale(argb, width, height, size[0], size[1]);
        int[] indices = com.simplelib.api.image.PixelArt.quantize(scaled, size[0], size[1], new com.simplelib.api.image.PixelPalette(paletteRgb),
                com.simplelib.api.image.PixelArt.Dither.values()[dither]);
        if (indices == null) return null;
        int[] out = new int[indices.length + 2];
        out[0] = size[0];
        out[1] = size[1];
        System.arraycopy(indices, 0, out, 2, indices.length);
        return out;
    }

    /** Block log and other listeners (Simple Tweaks T10) through SimpleLib. */
    public static void toolPlaced(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        com.simplelib.api.ToolPlacements.placed(level, player, pos, state);
    }
}
