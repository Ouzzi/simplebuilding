package com.simplefoundry;

import com.simplefoundry.block.SpoutBlock;
import com.simplefoundry.config.FoundryConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Multiblock check (concept section 5): flood fill from the desk through melting chambers only (six neighbours, any shape), every
 * neighbour of a chamber must be a chamber, the desk or a hull block (tag {@code simplefoundry:hull}). Needs at least one grate
 * (a lit fire grate next to the tank), stays inside {@code maxChambers} and 32 blocks per axis and never loads a chunk.
 */
public final class FoundryScan {
    /** A spout on the hull: where it stands, the way it pours, and the tank level (0 = lowest chamber layer) it draws from. */
    public record Spout(BlockPos pos, Direction facing, int level) {}

    public record Result(boolean valid, String reason, List<BlockPos> chambers, List<BlockPos> grates, List<Spout> spouts, int levels) {
        static Result fail(String reason) {
            return new Result(false, reason, List.of(), List.of(), List.of(), 0);
        }
    }

    public static Result scan(Level level, BlockPos desk) {
        int max = Math.min(FoundryConfig.maxChambers, FoundryConfig.MAX_CHAMBERS_LIMIT);
        Set<BlockPos> chambers = new HashSet<>();
        Set<BlockPos> grates = new HashSet<>();
        List<BlockPos> spoutPos = new ArrayList<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        for (Direction d : Direction.values()) {
            BlockPos next = desk.relative(d);
            if (isChamber(level, next) && chambers.add(next)) todo.add(next);
        }
        if (todo.isEmpty()) return Result.fail("no_chamber");
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        while (!todo.isEmpty()) {
            BlockPos pos = todo.poll();
            if (!level.isLoaded(pos)) return Result.fail("unloaded");
            if (chambers.size() > max) return Result.fail("too_big");
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!level.isLoaded(next)) return Result.fail("unloaded");
                BlockState state = level.getBlockState(next);
                if (state.is(FoundryRegistry.MELTING_CHAMBER)) {
                    if (chambers.add(next)) {
                        if (chambers.size() > max) return Result.fail("too_big");
                        todo.add(next);
                    }
                } else if (next.equals(desk)) {
                    continue;
                } else if (state.is(FoundryRegistry.DESK)) {
                    return Result.fail("second_desk");
                } else if (!state.is(FoundryRegistry.HULL)) {
                    return Result.fail("open_hull");
                } else if (state.is(FoundryRegistry.FIRE_GRATE)) {
                    grates.add(next);
                } else if (state.is(FoundryRegistry.FOUNDRY_SPOUT) && state.getValue(SpoutBlock.FACING) == d && !spoutPos.contains(next)) {
                    // the spout's back must touch this chamber: facing points away from the tank
                    spoutPos.add(next);
                }
            }
        }
        if (maxX - minX >= FoundryConfig.MAX_AXIS || maxY - minY >= FoundryConfig.MAX_AXIS || maxZ - minZ >= FoundryConfig.MAX_AXIS) {
            return Result.fail("too_big");
        }
        if (grates.isEmpty()) return Result.fail("no_grate");
        List<Spout> spouts = new ArrayList<>();
        for (BlockPos pos : spoutPos) {
            spouts.add(new Spout(pos, level.getBlockState(pos).getValue(SpoutBlock.FACING), Math.max(0, Math.min(maxY - minY, pos.getY() - minY))));
        }
        return new Result(true, "ok", List.copyOf(chambers), List.copyOf(grates), spouts, maxY - minY + 1);
    }

    private static boolean isChamber(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).is(FoundryRegistry.MELTING_CHAMBER);
    }

    private FoundryScan() {}
}
