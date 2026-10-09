package com.simplemaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

/**
 * Explores the area around a holder, with Vanilla's 1:1 colour and shading rules ({@code MapItem.update}). Like
 * Vanilla, each tick handles every 16th column; only already loaded chunks are read (never loads a chunk). Under a
 * ceiling (Nether) the floor below the holder is mapped instead of Vanilla's noise.
 */
public final class Reveal {
    /** Blocks scanned up and down from the holder under a ceiling. */
    public static final int CEILING_SCAN = 48;

    private Reveal() {}

    /** One reveal step (call once per tick while held); returns the number of changed pixels. */
    public static int step(ServerLevel level, Entity holder, WayfinderData data, int radius) {
        return step(level, Mth.floor(holder.getX()), Mth.floor(holder.getY()), Mth.floor(holder.getZ()), data, radius,
                (int) (level.getGameTime() & 15));
    }

    /** {@code phase} 0-15 selects the columns ({@code (x & 15) == phase}); tests pass every phase. */
    public static int step(ServerLevel level, int px, int py, int pz, WayfinderData data, int radius, int phase) {
        boolean ceiling = level.dimensionType().hasCeiling();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;
        int[] sample = new int[3];
        for (int x = px - radius; x <= px + radius; x++) {
            if ((x & 15) != phase) continue;
            int dx = x - px;
            int span = (int) Math.sqrt((double) radius * radius - (double) dx * dx);
            int previous = Integer.MIN_VALUE;
            for (int z = pz - span - 1; z <= pz + span; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (chunk == null || !sample(level, chunk, x, py, z, ceiling, pos, sample)) {
                    previous = Integer.MIN_VALUE;
                    continue;
                }
                int y = sample[1];
                if (z >= pz - span) {
                    MapColor color = MapColor.byId(sample[0]);
                    MapColor.Brightness brightness = brightness(color, y, previous, sample[2], x, z);
                    int height = Mth.clamp((y - level.getMinY()) / 2 + 1, 1, 255);
                    if (data.set(x, z, color.getPackedId(brightness) & 0xFF, height, true)) changed++;
                }
                previous = y;
            }
        }
        return changed;
    }

    static MapColor.Brightness brightness(MapColor color, int y, int previous, int waterDepth, int x, int z) {
        int dither = (x + z) & 1;
        if (color == MapColor.WATER) {
            double diff = waterDepth * 0.1 + dither * 0.2;
            return diff < 0.5 ? MapColor.Brightness.HIGH : diff > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        }
        if (previous == Integer.MIN_VALUE) previous = y;
        double diff = (y - previous) * 4.0 / 5.0 + (dither - 0.5) * 0.4;
        return diff > 0.6 ? MapColor.Brightness.HIGH : diff < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
    }

    /** Fills {@code out} with colour id, surface y and water depth; false when nothing mappable was found. */
    static boolean sample(ServerLevel level, LevelChunk chunk, int x, int py, int z, boolean ceiling,
                          BlockPos.MutableBlockPos pos, int[] out) {
        int minY = level.getMinY();
        BlockState state;
        int y;
        if (ceiling) {
            int top = Math.min(py + 2, minY + level.getHeight() - 1), bottom = Math.max(minY, py - CEILING_SCAN);
            y = top;
            pos.set(x, y, z);
            // Inside rock at head height: climb to the first open block so a tunnel still shows its floor.
            while (y < Math.min(py + CEILING_SCAN, minY + level.getHeight() - 1)
                    && chunk.getBlockState(pos).getMapColor(level, pos) != MapColor.NONE) {
                pos.setY(++y);
            }
            do {
                pos.setY(--y);
                state = chunk.getBlockState(pos);
            } while (state.getMapColor(level, pos) == MapColor.NONE && y > bottom);
            if (state.getMapColor(level, pos) == MapColor.NONE) return false;
        } else {
            y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
            pos.set(x, y, z);
            if (y <= minY) {
                state = Blocks.BEDROCK.defaultBlockState();
            } else {
                do {
                    pos.setY(--y);
                    state = chunk.getBlockState(pos);
                } while (state.getMapColor(level, pos) == MapColor.NONE && y > minY);
            }
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
            FluidState fluid = state.getFluidState();
            if (!state.isFaceSturdy(level, pos, Direction.UP)) state = fluid.createLegacyBlock();
        }
        MapColor color = state.getMapColor(level, pos);
        if (color == MapColor.NONE) return false;
        out[0] = color.id;
        out[1] = y;
        out[2] = depth;
        return true;
    }
}
