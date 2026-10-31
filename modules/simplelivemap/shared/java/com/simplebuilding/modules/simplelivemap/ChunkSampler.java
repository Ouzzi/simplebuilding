package com.simplebuilding.modules.simplelivemap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Top-down picture of one loaded chunk: 16 x 16 ARGB pixels with Vanilla's map colours ({@link MapColor}) and Vanilla's map
 * relief (a column higher than its northern neighbour is brighter, a lower one darker; water by depth) – the 2.5D look of the
 * tiles. Same rules as {@code MapItem.update} (and Simple Maps' {@code Reveal}, rebuilt here: no classes of other mods).
 * Never loads a chunk: the northern neighbour is only read when it is loaded, else the first row is drawn flat. Under a
 * ceiling (Nether) the first floor below y 100 is drawn (dimension cut in stage 2, risk R21). Server thread only.
 */
public final class ChunkSampler {
    public static final int CEILING_TOP = 100;

    private ChunkSampler() {}

    /** 256 ARGB pixels, index {@code (z & 15) * 16 + (x & 15)}; 0 where nothing mappable was found. */
    public static int[] sample(ServerLevel level, LevelChunk chunk, boolean shading) {
        int[] out = new int[256];
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        LevelChunk north = level.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z() - 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] s = new int[3];
        for (int lx = 0; lx < 16; lx++) {
            int x = x0 + lx;
            int previous = north != null && column(level, north, x, z0 - 1, pos, s) ? s[1] : Integer.MIN_VALUE;
            for (int lz = 0; lz < 16; lz++) {
                int z = z0 + lz;
                if (!column(level, chunk, x, z, pos, s)) {
                    previous = Integer.MIN_VALUE;
                    continue;
                }
                MapColor color = MapColor.byId(s[0]);
                MapColor.Brightness b = shading ? brightness(color, s[1], previous, s[2], x, z) : MapColor.Brightness.NORMAL;
                out[lz * 16 + lx] = argb(color, b);
                previous = s[1];
            }
        }
        return out;
    }

    /** Opaque ARGB of a map colour at a brightness. */
    public static int argb(MapColor color, MapColor.Brightness brightness) {
        return argbPacked(color.getPackedId(brightness));
    }

    /** Opaque ARGB of a packed Vanilla map pixel (colour id * 4 + brightness), 0 for "nothing". */
    public static int argbPacked(int packed) {
        packed &= 0xFF;
        return packed >>> 2 == 0 ? 0 : 0xFF000000 | MapColor.getColorFromPackedId(packed);
    }

    /** Vanilla's relief rule ({@code MapItem.update}), with its 1-pixel dither. */
    public static MapColor.Brightness brightness(MapColor color, int y, int previous, int waterDepth, int x, int z) {
        int dither = (x + z) & 1;
        if (color == MapColor.WATER) {
            double d = waterDepth * 0.1 + dither * 0.2;
            return d < 0.5 ? MapColor.Brightness.HIGH : d > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        if (previous == Integer.MIN_VALUE) previous = y;
        double d = (y - previous) * 4.0 / 5.0 + (dither - 0.5) * 0.4;
        return d > 0.6 ? MapColor.Brightness.HIGH : d < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    /** Colour id, surface y and water depth of one column into {@code out}; false when nothing mappable was found. */
    static boolean column(ServerLevel level, LevelChunk chunk, int x, int z, BlockPos.MutableBlockPos pos, int[] out) {
        int minY = level.getMinY();
        int y;
        BlockState state;
        if (level.dimensionType().hasCeiling()) {
            y = Math.min(CEILING_TOP, minY + level.getHeight() - 1);
            pos.set(x, y, z);
            // Below the roof: first find open space, then the floor under it.
            while (y > minY && chunk.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) pos.setY(--y);
            do {
                pos.setY(--y);
                state = chunk.getBlockState(pos);
            } while (state.getMapColor(level, pos) == MapColor.NONE && y > minY);
        } else {
            y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
            pos.set(x, y, z);
            if (y <= minY) return false;
            do {
                pos.setY(--y);
                state = chunk.getBlockState(pos);
            } while (state.getMapColor(level, pos) == MapColor.NONE && y > minY);
        }
        int depth = 0;
        if (y > minY && !state.getFluidState().isEmpty()) {
            BlockPos.MutableBlockPos below = pos.mutable();
            int solidY = y - 1;
            BlockState belowState;
            do {
                below.setY(solidY--);
                belowState = chunk.getBlockState(below);
                depth++;
            } while (solidY > minY && !belowState.getFluidState().isEmpty() && depth < 64);
            if (!state.isFaceSturdy(level, pos, Direction.UP)) state = state.getFluidState().createLegacyBlock();
        }
        MapColor color = state.getMapColor(level, pos);
        if (color == MapColor.NONE) return false;
        out[0] = color.id;
        out[1] = y;
        out[2] = depth;
        return true;
    }
}
