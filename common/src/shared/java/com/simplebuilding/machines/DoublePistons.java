package com.simplebuilding.machines;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Logic of the Double Piston (IG31-26, one block, two stages). Cells in the push direction {@code f} from the base {@code P}:
 * stage 1 head at {@code P+f}; stage 2 arm at {@code P+f} (front of the body, which moved half a block) and head at {@code P+2f}.
 * Signal 1-7 aims at stage 1, 8-15 at stage 2, 0 at stage 0. A move is always one block at a time over Vanilla's moving_piston
 * (animation, entity push, collision) and Vanilla's {@link PistonStructureResolver} (limit 12, slime and honey, no block entities,
 * obsidian stops it); after each step the base looks at the signal again and goes one more step or back. No block entity.
 */
public final class DoublePistons {
    /** The stage: 0 in, 1 head one block out, 2 body half a block and head two blocks out. */
    public static final IntegerProperty EXT = IntegerProperty.create("ext", 0, 2);
    /** Game ticks until a step has settled and the next one may start (a Vanilla block move takes 2 to 3). */
    public static final int SETTLE_TICKS = 3;

    private DoublePistons() {}

    /**
     * Shapes of a north-facing part (front at z = 0, boxes in pixels) for all six facings, turned like the blockstate turns the
     * model (no {@code Shapes.rotateAll}, which the 26.2 line may lack).
     */
    public static Map<Direction, VoxelShape> facing(double[]... boxes) {
        Map<Direction, VoxelShape> out = new java.util.EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double[] p = turn(d, b[0], b[1], b[2]);
                double[] q = turn(d, b[3], b[4], b[5]);
                shape = Shapes.or(shape, Block.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]), Math.min(p[2], q[2]),
                        Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
            }
            out.put(d, shape);
        }
        return out;
    }

    /** A point of the north-facing part for facing {@code d}: the front (z = 0) ends on the side {@code d}. */
    private static double[] turn(Direction d, double x, double y, double z) {
        return switch (d) {
            case NORTH -> new double[] {x, y, z};
            case SOUTH -> new double[] {16 - x, y, 16 - z};
            case EAST -> new double[] {16 - z, y, x};
            case WEST -> new double[] {z, y, 16 - x};
            case UP -> new double[] {x, 16 - z, y};
            case DOWN -> new double[] {x, z, 16 - y};
        };
    }

    /** The stage a signal strength asks for: 0 none, 1-7 one, 8-15 two. */
    public static int targetExt(int signal) {
        return signal <= 0 ? 0 : signal < 8 ? 1 : 2;
    }

    /**
     * The strongest signal that reaches the base like it reaches a Vanilla piston: every side but the front, the cell itself
     * from below and the cell above from all sides but below (quasi-connectivity, so Vanilla machines work the same).
     */
    public static int signal(Level level, BlockPos pos, Direction facing) {
        int best = 0;
        for (Direction d : Direction.values()) {
            if (d != facing) best = Math.max(best, level.getSignal(pos.relative(d), d));
        }
        best = Math.max(best, level.getSignal(pos, Direction.DOWN));
        BlockPos above = pos.above();
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN) best = Math.max(best, level.getSignal(above.relative(d), d));
        }
        return best;
    }

    public static boolean isBase(BlockState state, DoublePistonVariant variant, Direction f, int ext) {
        return state.getBlock() instanceof DoublePistonBlock b && b.variant() == variant && state.getValue(DirectionalBlock.FACING) == f
                && (ext < 0 || state.getValue(EXT) == ext);
    }

    public static boolean isHead(BlockState state, DoublePistonVariant variant, Direction f) {
        return state.getBlock() instanceof DoublePistonHeadBlock h && h.variant() == variant && state.getValue(DirectionalBlock.FACING) == f;
    }

    public static boolean isArm(BlockState state, DoublePistonVariant variant, Direction f) {
        return state.getBlock() instanceof DoublePistonArmBlock a && a.variant() == variant && state.getValue(DirectionalBlock.FACING) == f;
    }

    /** Whether {@code behind} (the cell behind a head) holds the head: the base at stage 1 or the arm. */
    public static boolean holdsHead(BlockState behind, BlockState head) {
        DoublePistonVariant v = ((DoublePistonHeadBlock) head.getBlock()).variant();
        Direction f = head.getValue(DirectionalBlock.FACING);
        return isBase(behind, v, f, 1) || isArm(behind, v, f);
    }

    /** True while a part still has a block move running (the next step waits for it). */
    public static boolean moving(Level level, BlockPos base, Direction f) {
        for (int i = 1; i <= 2; i++) {
            if (level.getBlockState(base.relative(f, i)).is(Blocks.MOVING_PISTON)) return true;
        }
        return false;
    }

    public static void schedule(Level level, BlockPos pos, Block block, int delay) {
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) level.scheduleTick(pos, block, delay);
    }

    /** Whether the parts in front of the base match its recorded stage. */
    public static boolean consistent(Level level, BlockPos base, DoublePistonVariant v, Direction f, int ext) {
        BlockState first = level.getBlockState(base.relative(f));
        return switch (ext) {
            case 0 -> true;
            case 1 -> isHead(first, v, f);
            default -> isArm(first, v, f) && isHead(level.getBlockState(base.relative(f, 2)), v, f);
        };
    }

    /** One step of the base at {@code pos} towards the stage its signal asks for; schedules the next look. */
    public static void step(ServerLevel level, BlockPos pos, BlockState base) {
        if (!MachineParts.doublePistonOn() || !(base.getBlock() instanceof DoublePistonBlock block)) return;
        Direction f = base.getValue(DirectionalBlock.FACING);
        int cur = base.getValue(EXT);
        if (!level.hasChunkAt(pos.relative(f, 3))) return;
        if (moving(level, pos, f)) {
            schedule(level, pos, block, 1);
            return;
        }
        if (!consistent(level, pos, block.variant(), f, cur)) {
            repair(level, pos, block, f);
            return;
        }
        int target = targetExt(signal(level, pos, f));
        if (target == cur) return;
        boolean moved = target > cur ? extend(level, pos, block, f, cur) : retract(level, pos, block, f, cur);
        if (moved) schedule(level, pos, block, SETTLE_TICKS);
    }

    /** A lost head or arm (explosion, command, an old 1x2 piston from Simple Redstone): clear what is left and go back to 0. */
    private static void repair(ServerLevel level, BlockPos base, DoublePistonBlock block, Direction f) {
        for (int i = 1; i <= 2; i++) {
            BlockPos cell = base.relative(f, i);
            BlockState state = level.getBlockState(cell);
            if (state.is(block.head()) || state.is(block.arm())) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        setExt(level, base, 0);
    }

    private static boolean extend(ServerLevel level, BlockPos base, DoublePistonBlock block, Direction f, int cur) {
        BlockPos pistonPos = base.relative(f, cur);
        BlockPos headPos = pistonPos.relative(f);
        PistonStructureResolver resolver = new PistonStructureResolver(level, pistonPos, f, true);
        if (!resolver.resolve()) return false;
        setExt(level, base, cur + 1);
        if (cur == 1) level.setBlock(pistonPos, block.arm().defaultBlockState().setValue(DirectionalBlock.FACING, f), 2 | 16);
        pushLine(level, resolver, f, headPos, block.head().defaultBlockState().setValue(DirectionalBlock.FACING, f));
        level.playSound(null, base, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.25F + 0.6F);
        return true;
    }

    private static boolean retract(ServerLevel level, BlockPos base, DoublePistonBlock block, Direction f, int cur) {
        BlockPos headPos = base.relative(f, cur);
        BlockState head = level.getBlockState(headPos);
        setExt(level, base, cur - 1);
        if (cur == 2) {
            // The head slides back one block over the arm's cell, which becomes the moving block; the body jumps back with the stage.
            BlockPos armPos = base.relative(f);
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 2 | 16 | 64);
            BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, f);
            level.setBlock(armPos, moving, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(armPos, moving, head, f, false, false));
        } else {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 3);
        }
        level.playSound(null, base, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.15F + 0.6F);
        return true;
    }

    /** Writes the stage into the base (no shape updates: the parts are mid-change). */
    public static void setExt(Level level, BlockPos base, int ext) {
        BlockState a = level.getBlockState(base);
        if (a.getBlock() instanceof DoublePistonBlock) level.setBlock(base, a.setValue(EXT, ext), 2 | 16);
    }

    /** The base cell behind a head or arm ({@code pos}, facing {@code f}), or null. */
    public static BlockPos findBase(Level level, BlockPos pos, Direction f) {
        for (int k = 1; k <= 2; k++) {
            BlockPos p = pos.relative(f.getOpposite(), k);
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof DoublePistonBlock && state.getValue(DirectionalBlock.FACING) == f) return p;
        }
        return null;
    }

    /** Creative breaking of a head or arm takes the base along without a drop (Vanilla's piston head does the same). */
    public static void creativeBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || !player.preventsBlockDrops()) return;
        BlockPos base = findBase(level, pos, state.getValue(DirectionalBlock.FACING));
        if (base != null) level.setBlock(base, Blocks.AIR.defaultBlockState(), 35);
    }

    /**
     * Vanilla's {@code PistonBaseBlock.moveBlocks} for extending, with the head as a moving block of its own: everything the
     * resolver found moves one cell, the head takes the cell the first block left.
     */
    private static void pushLine(Level level, PistonStructureResolver resolver, Direction direction, BlockPos headPos, BlockState headState) {
        Map<BlockPos, BlockState> deleteAfterMove = Maps.newHashMap();
        List<BlockPos> toPush = resolver.getToPush();
        List<BlockState> toPushShapes = Lists.newArrayList();
        for (BlockPos pos : toPush) {
            BlockState state = level.getBlockState(pos);
            toPushShapes.add(state);
            deleteAfterMove.put(pos, state);
        }
        List<BlockPos> toDestroy = resolver.getToDestroy();
        BlockState[] toUpdate = new BlockState[toPush.size() + toDestroy.size()];
        int updateIndex = 0;
        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockPos pos = toDestroy.get(i);
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, blockEntity);
            toUpdate[updateIndex++] = state;
        }
        for (int i = toPush.size() - 1; i >= 0; i--) {
            BlockPos pos = toPush.get(i);
            BlockState blockState = level.getBlockState(pos);
            pos = pos.relative(direction);
            deleteAfterMove.remove(pos);
            BlockState actualState = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction);
            level.setBlock(pos, actualState, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, actualState, toPushShapes.get(i), direction, true, false));
            toUpdate[updateIndex++] = blockState;
        }
        BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction);
        deleteAfterMove.remove(headPos);
        level.setBlock(headPos, moving, 324);
        level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(headPos, moving, headState, direction, true, false));
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos pos : deleteAfterMove.keySet()) {
            level.setBlock(pos, air, 82);
        }
        for (Map.Entry<BlockPos, BlockState> entry : deleteAfterMove.entrySet()) {
            BlockPos pos = entry.getKey();
            entry.getValue().updateIndirectNeighbourShapes(level, pos, 2);
            air.updateNeighbourShapes(level, pos, 2);
            air.updateIndirectNeighbourShapes(level, pos, 2);
        }
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, resolver.getPushDirection(), null);
        updateIndex = 0;
        for (int i = toDestroy.size() - 1; i >= 0; i--) {
            BlockState state = toUpdate[updateIndex++];
            BlockPos pos = toDestroy.get(i);
            if (level instanceof ServerLevel serverLevel) state.affectNeighborsAfterRemoval(serverLevel, pos, false);
            state.updateIndirectNeighbourShapes(level, pos, 2);
            level.updateNeighborsAt(pos, state.getBlock(), orientation);
        }
        for (int i = toPush.size() - 1; i >= 0; i--) {
            level.updateNeighborsAt(toPush.get(i), toUpdate[updateIndex++].getBlock(), orientation);
        }
        level.updateNeighborsAt(headPos, headState.getBlock(), orientation);
    }
}
