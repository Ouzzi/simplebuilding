package com.simplebuilding.modules.simplechallenges.skyline;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;

/**
 * How a Skyline block stands in the air without block updates: blocks that cannot stand alone (torches, plants, doors, rails,
 * ladders, buttons...) get a Bedrock support next to them, falling blocks one below. Found generically: the block's own
 * {@code canSurvive} is asked in a tiny fake world where only one neighbour is Bedrock, below first, then above, south, east. So
 * every block of the registry (also those of other mods) is covered by the same rule. Every part lies in the 2 x 2 x 3 box
 * round the block (+x and +z only), so a chunk can place a block and its parts without touching its neighbour.
 */
public final class Support {
    /** One block to place at an offset from the line position. */
    public record Part(int dx, int dy, int dz, BlockState state) {}

    private static final ConcurrentHashMap<Block, List<Part>> CACHE = new ConcurrentHashMap<>();
    private static final BlockPos SELF = new BlockPos(0, 64, 0);
    private static final int[][] CANDIDATES = {{0, -1, 0}, {0, 1, 0}, {0, 0, 1}, {1, 0, 0}};

    private Support() {}

    /** The parts of one line block: the block itself first (at 0,0,0), then its second half and its support. */
    public static List<Part> parts(Block block) {
        return CACHE.computeIfAbsent(block, Support::compute);
    }

    private static List<Part> compute(Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.PERSISTENT)) state = state.setValue(BlockStateProperties.PERSISTENT, true);
        List<Part> out = new ArrayList<>();
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // a bed lies along +z: the foot here, the head one block south (inside the same chunk)
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
            out.add(new Part(0, 0, 0, state));
            out.add(new Part(0, 0, 1, block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).setValue(BlockStateProperties.BED_PART, BedPart.HEAD)));
            return List.copyOf(out);
        }
        out.add(new Part(0, 0, 0, state));
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            out.add(new Part(0, 1, 0, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
        }
        int[] at = null;
        if (block instanceof Fallable) {
            at = CANDIDATES[0];
        } else if (!survives(state, null)) {
            for (int[] c : CANDIDATES) {
                if (survives(state, SELF.offset(c[0], c[1], c[2]))) {
                    at = c;
                    break;
                }
            }
            if (at == null) at = CANDIDATES[0]; // nothing makes it stand (a lily pad needs water): the floor at least
        }
        if (at != null) out.add(new Part(at[0], at[1], at[2], Blocks.BEDROCK.defaultBlockState()));
        return List.copyOf(out);
    }

    /** True when the block would stay in a world that is empty except for Bedrock at {@code support} (null = nothing at all). */
    static boolean survives(BlockState state, BlockPos support) {
        try {
            return state.canSurvive(world(state, support), SELF);
        } catch (RuntimeException e) {
            return true; // a block that cannot be asked is placed as it is
        }
    }

    private static LevelReader world(BlockState self, BlockPos support) {
        InvocationHandler h = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getBlockState" -> {
                    BlockPos p = (BlockPos) args[0];
                    if (p.equals(SELF)) return self;
                    return p.equals(support) ? Blocks.BEDROCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
                }
                case "getFluidState" -> {
                    return Fluids.EMPTY.defaultFluidState();
                }
                case "getBlockEntity", "getChunk" -> {
                    return null;
                }
                case "getMinY" -> {
                    return -64;
                }
                case "getMaxY" -> {
                    return 319;
                }
                case "getHeight" -> {
                    if (args == null || args.length == 0) return 384;
                }
                case "isClientSide" -> {
                    return false;
                }
                case "getSeaLevel" -> {
                    return 63;
                }
                default -> {}
            }
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            Class<?> r = method.getReturnType();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == long.class) return 0L;
            if (r == float.class) return 0F;
            if (r == double.class) return 0D;
            return null;
        };
        return (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class<?>[]{LevelReader.class}, h);
    }
}
